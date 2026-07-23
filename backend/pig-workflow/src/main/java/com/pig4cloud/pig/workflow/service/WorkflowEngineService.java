package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.workflow.constant.WorkflowAction;
import com.pig4cloud.pig.workflow.constant.WorkflowStatus;
import com.pig4cloud.pig.workflow.dto.StartWorkflowRequest;
import com.pig4cloud.pig.workflow.dto.TaskActionRequest;
import com.pig4cloud.pig.workflow.dto.TaskResultRequest;
import com.pig4cloud.pig.workflow.entity.WorkflowDefinition;
import com.pig4cloud.pig.workflow.entity.WorkflowApproval;
import com.pig4cloud.pig.workflow.entity.WorkflowExecutionLog;
import com.pig4cloud.pig.workflow.entity.WorkflowInstance;
import com.pig4cloud.pig.workflow.entity.WorkflowNodeDefinition;
import com.pig4cloud.pig.workflow.entity.WorkflowTask;
import com.pig4cloud.pig.workflow.entity.WorkflowTransition;
import com.pig4cloud.pig.workflow.mapper.WorkflowDefinitionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowApprovalMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowExecutionLogMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowInstanceMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowNodeDefinitionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowTaskMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowTransitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 最小可运行工作流引擎。
 */
@Service
@RequiredArgsConstructor
public class WorkflowEngineService {

	private final WorkflowDefinitionMapper definitionMapper;
	private final WorkflowNodeDefinitionMapper nodeMapper;
	private final WorkflowInstanceMapper instanceMapper;
	private final WorkflowTaskMapper taskMapper;
	private final WorkflowTransitionMapper transitionMapper;
	private final WorkflowApprovalMapper approvalMapper;
	private final WorkflowExecutionLogMapper logMapper;
	private final ObjectMapper objectMapper;

	@Transactional(rollbackFor = Exception.class)
	public WorkflowInstance start(StartWorkflowRequest request) {
		WorkflowDefinition definition = definitionMapper.selectById(request.getDefinitionId());
		if (definition == null || !WorkflowStatus.DEFINITION_PUBLISHED.equals(definition.getStatus())) {
			throw new CheckedException("只能启动已发布的工作流");
		}
		Long duplicate = instanceMapper.selectCount(Wrappers.<WorkflowInstance>lambdaQuery()
			.eq(WorkflowInstance::getDefinitionId, request.getDefinitionId())
			.eq(WorkflowInstance::getBusinessKey, request.getBusinessKey()));
		if (duplicate > 0) {
			throw new CheckedException("同一工作流下业务标识不能重复");
		}
		WorkflowNodeDefinition startNode = nodeMapper.selectOne(Wrappers.<WorkflowNodeDefinition>lambdaQuery()
			.eq(WorkflowNodeDefinition::getDefinitionId, request.getDefinitionId())
			.eq(WorkflowNodeDefinition::getStartNode, true));
		if (startNode == null) {
			throw new CheckedException("工作流没有可执行的开始节点");
		}

		LocalDateTime now = LocalDateTime.now();
		WorkflowInstance instance = new WorkflowInstance();
		instance.setProjectId(definition.getProjectId());
		instance.setDefinitionId(definition.getId());
		instance.setBusinessKey(request.getBusinessKey());
		instance.setTitle(request.getTitle());
		instance.setStatus(WorkflowStatus.INSTANCE_RUNNING);
		instance.setCurrentNodeKey(startNode.getNodeKey());
		instance.setInputJson(request.getInputJson());
		instance.setStartedBy(SecurityUtils.getUser().getUsername());
		instance.setStartedAt(now);
		instanceMapper.insert(instance);

		WorkflowTask task = createTask(instance, startNode, request.getInputJson());
		writeLog(instance.getId(), task.getId(), startNode.getNodeKey(), "INSTANCE_STARTED",
			WorkflowStatus.INSTANCE_RUNNING, request.getInputJson(), null, null, null);
		return instance;
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowInstance completeTask(Long taskId, TaskResultRequest request) {
		return advanceTask(taskId, request, WorkflowAction.COMPLETE);
	}

	/**
	 * 执行统一任务动作。
	 * @param taskId 任务 ID
	 * @param request 动作及执行结果
	 * @return 更新后的实例或任务
	 */
	@Transactional(rollbackFor = Exception.class)
	public Object executeAction(Long taskId, TaskActionRequest request) {
		String action = request.getAction().trim().toUpperCase(Locale.ROOT);
		if (!WorkflowAction.isSupported(action)) {
			throw new CheckedException("不支持的任务动作: " + action);
		}
		if (WorkflowAction.RETRY.equals(action)) {
			return retryTask(taskId);
		}
		TaskResultRequest result = new TaskResultRequest();
		result.setOutputJson(request.getOutputJson());
		result.setErrorMessage(request.getErrorMessage());
		if (WorkflowAction.FAIL.equals(action)) {
			return failTask(taskId, result);
		}
		if (WorkflowAction.CANCEL.equals(action)) {
			return cancelTask(taskId, result);
		}
		return advanceTask(taskId, result, action);
	}

	private WorkflowInstance advanceTask(Long taskId, TaskResultRequest request, String action) {
		WorkflowTask task = requireActiveTask(taskId);
		WorkflowInstance instance = requireRunningInstance(task.getInstanceId());
		LocalDateTime now = LocalDateTime.now();
		String completedStatus = taskStatusForAction(action);
		int changed = taskMapper.update(null, Wrappers.<WorkflowTask>lambdaUpdate()
			.eq(WorkflowTask::getId, taskId)
			.in(WorkflowTask::getStatus, WorkflowStatus.TASK_READY, WorkflowStatus.TASK_RUNNING)
			.set(WorkflowTask::getStatus, completedStatus)
			.set(WorkflowTask::getOutputJson, request.getOutputJson())
			.set(WorkflowTask::getFinishedAt, now)
			.set(WorkflowTask::getErrorMessage, null));
		if (changed != 1) {
			throw new CheckedException("任务状态已变化，请刷新后重试");
		}

		WorkflowNodeDefinition currentNode = nodeMapper.selectById(task.getNodeDefinitionId());
		writeLog(instance.getId(), taskId, task.getNodeKey(), "TASK_" + action, completedStatus,
			task.getInputJson(), request.getOutputJson(), null, duration(task.getStartedAt(), now));

		List<WorkflowTransition> transitions = transitionMapper.selectList(Wrappers.<WorkflowTransition>lambdaQuery()
			.eq(WorkflowTransition::getDefinitionId, instance.getDefinitionId())
			.eq(WorkflowTransition::getSourceNodeKey, currentNode.getNodeKey())
			.eq(WorkflowTransition::getAction, action)
			.orderByDesc(WorkflowTransition::getPriority));
		WorkflowTransition selected = transitions.isEmpty() ? null : transitions.get(0);
		String targetNodeKey = selected == null ? currentNode.getNextNodeKey() : selected.getTargetNodeKey();
		boolean finishInstance = selected != null ? !StringUtils.hasText(targetNodeKey)
				: Boolean.TRUE.equals(currentNode.getEndNode()) || !StringUtils.hasText(targetNodeKey);
		if (finishInstance) {
			instance.setStatus(WorkflowStatus.INSTANCE_COMPLETED);
			instance.setCurrentNodeKey(null);
			instance.setOutputJson(request.getOutputJson());
			instance.setFinishedAt(now);
			instanceMapper.updateById(instance);
			writeLog(instance.getId(), null, task.getNodeKey(), "INSTANCE_COMPLETED",
				WorkflowStatus.INSTANCE_COMPLETED, null, request.getOutputJson(), null, null);
			return instance;
		}

		WorkflowNodeDefinition nextNode = nodeMapper.selectOne(Wrappers.<WorkflowNodeDefinition>lambdaQuery()
			.eq(WorkflowNodeDefinition::getDefinitionId, instance.getDefinitionId())
			.eq(WorkflowNodeDefinition::getNodeKey, targetNodeKey));
		if (nextNode == null) {
			throw new CheckedException("下一节点不存在，工作流定义可能已损坏");
		}
		instance.setCurrentNodeKey(nextNode.getNodeKey());
		instanceMapper.updateById(instance);
		createTask(instance, nextNode, request.getOutputJson());
		return instance;
	}

	private String taskStatusForAction(String action) {
		return switch (action) {
			case WorkflowAction.REJECT -> WorkflowStatus.TASK_REJECTED;
			case WorkflowAction.RETURN -> WorkflowStatus.TASK_RETURNED;
			case WorkflowAction.SKIP -> WorkflowStatus.TASK_SKIPPED;
			default -> WorkflowStatus.TASK_COMPLETED;
		};
	}

	private WorkflowInstance cancelTask(Long taskId, TaskResultRequest request) {
		WorkflowTask task = requireActiveTask(taskId);
		WorkflowInstance instance = requireRunningInstance(task.getInstanceId());
		LocalDateTime now = LocalDateTime.now();
		int changed = taskMapper.update(null, Wrappers.<WorkflowTask>lambdaUpdate()
			.eq(WorkflowTask::getId, taskId)
			.in(WorkflowTask::getStatus, WorkflowStatus.TASK_READY, WorkflowStatus.TASK_RUNNING)
			.set(WorkflowTask::getStatus, WorkflowStatus.TASK_CANCELLED)
			.set(WorkflowTask::getOutputJson, request.getOutputJson())
			.set(WorkflowTask::getFinishedAt, now));
		if (changed != 1) {
			throw new CheckedException("任务状态已变化，请刷新后重试");
		}
		instance.setStatus(WorkflowStatus.INSTANCE_CANCELLED);
		instance.setCurrentNodeKey(null);
		instance.setOutputJson(request.getOutputJson());
		instance.setFinishedAt(now);
		instanceMapper.updateById(instance);
		writeLog(instance.getId(), taskId, task.getNodeKey(), "TASK_CANCEL", WorkflowStatus.TASK_CANCELLED,
			task.getInputJson(), request.getOutputJson(), null, duration(task.getStartedAt(), now));
		return instance;
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowInstance failTask(Long taskId, TaskResultRequest request) {
		WorkflowTask task = requireActiveTask(taskId);
		WorkflowInstance instance = requireRunningInstance(task.getInstanceId());
		LocalDateTime now = LocalDateTime.now();
		task.setStatus(WorkflowStatus.TASK_FAILED);
		task.setErrorMessage(request.getErrorMessage());
		task.setOutputJson(request.getOutputJson());
		task.setFinishedAt(now);
		taskMapper.updateById(task);
		instance.setStatus(WorkflowStatus.INSTANCE_FAILED);
		instance.setOutputJson(request.getOutputJson());
		instance.setFinishedAt(now);
		instanceMapper.updateById(instance);
		writeLog(instance.getId(), taskId, task.getNodeKey(), "TASK_FAILED", WorkflowStatus.TASK_FAILED,
			task.getInputJson(), request.getOutputJson(), request.getErrorMessage(), duration(task.getStartedAt(), now));
		return instance;
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowTask retryTask(Long taskId) {
		WorkflowTask task = taskMapper.selectById(taskId);
		if (task == null || !WorkflowStatus.TASK_FAILED.equals(task.getStatus())) {
			throw new CheckedException("只有失败任务可以重试");
		}
		WorkflowInstance instance = instanceMapper.selectById(task.getInstanceId());
		task.setStatus(WorkflowStatus.TASK_READY);
		task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
		task.setStartedAt(LocalDateTime.now());
		task.setFinishedAt(null);
		task.setErrorMessage(null);
		taskMapper.updateById(task);
		instance.setStatus(WorkflowStatus.INSTANCE_RUNNING);
		instance.setCurrentNodeKey(task.getNodeKey());
		instance.setFinishedAt(null);
		instanceMapper.updateById(instance);
		writeLog(instance.getId(), taskId, task.getNodeKey(), "TASK_RETRIED", WorkflowStatus.TASK_READY,
			task.getInputJson(), null, null, null);
		return task;
	}

	private WorkflowTask createTask(WorkflowInstance instance, WorkflowNodeDefinition node, String inputJson) {
		WorkflowTask task = new WorkflowTask();
		task.setInstanceId(instance.getId());
		task.setNodeDefinitionId(node.getId());
		task.setNodeKey(node.getNodeKey());
		task.setNodeName(node.getNodeName());
		task.setTaskType(node.getNodeType());
		task.setStatus(WorkflowStatus.TASK_READY);
		task.setInputJson(inputJson);
		task.setRetryCount(0);
		task.setStartedAt(LocalDateTime.now());
		taskMapper.insert(task);
		if ("MANUAL_REVIEW".equalsIgnoreCase(node.getNodeType()) || "MANUAL".equalsIgnoreCase(node.getNodeType())) {
			WorkflowApproval approval = new WorkflowApproval();
			approval.setInstanceId(instance.getId());
			approval.setTaskId(task.getId());
			approval.setApprovalType(node.getNodeKey());
			approval.setStatus("PENDING");
			approval.setOperationKey("CREATE:" + task.getId());
			if (StringUtils.hasText(node.getConfigJson())) {
				try {
					JsonNode config = objectMapper.readTree(node.getConfigJson());
					if (config.hasNonNull("candidateReviewerId")) {
						approval.setCandidateReviewerId(config.get("candidateReviewerId").longValue());
					}
					if (config.hasNonNull("candidateRoleId")) {
						approval.setCandidateRoleId(config.get("candidateRoleId").longValue());
					}
				} catch (Exception exception) {
					throw new CheckedException("人工审核节点配置不是有效的 JSON");
				}
			}
			approvalMapper.insert(approval);
		}
		writeLog(instance.getId(), task.getId(), node.getNodeKey(), "TASK_CREATED", WorkflowStatus.TASK_READY,
			inputJson, null, null, null);
		return task;
	}

	private WorkflowTask requireActiveTask(Long taskId) {
		WorkflowTask task = taskMapper.selectById(taskId);
		if (task == null || !(WorkflowStatus.TASK_READY.equals(task.getStatus())
			|| WorkflowStatus.TASK_RUNNING.equals(task.getStatus()))) {
			throw new CheckedException("任务不存在或当前状态不可执行");
		}
		return task;
	}

	private WorkflowInstance requireRunningInstance(Long instanceId) {
		WorkflowInstance instance = instanceMapper.selectById(instanceId);
		if (instance == null || !WorkflowStatus.INSTANCE_RUNNING.equals(instance.getStatus())) {
			throw new CheckedException("工作流实例不存在或不在运行中");
		}
		return instance;
	}

	private Long duration(LocalDateTime start, LocalDateTime end) {
		return start == null ? null : Duration.between(start, end).toMillis();
	}

	private void writeLog(Long instanceId, Long taskId, String nodeKey, String eventType, String status,
			String requestJson, String responseJson, String errorMessage, Long durationMs) {
		WorkflowExecutionLog log = new WorkflowExecutionLog();
		log.setInstanceId(instanceId);
		log.setTaskId(taskId);
		log.setNodeKey(nodeKey);
		log.setEventType(eventType);
		log.setStatus(status);
		log.setRequestJson(requestJson);
		log.setResponseJson(responseJson);
		log.setErrorMessage(errorMessage);
		log.setDurationMs(durationMs);
		logMapper.insert(log);
	}

}
