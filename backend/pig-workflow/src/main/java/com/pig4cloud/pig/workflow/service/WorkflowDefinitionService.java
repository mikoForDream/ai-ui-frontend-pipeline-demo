package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.constant.WorkflowAction;
import com.pig4cloud.pig.workflow.constant.WorkflowStatus;
import com.pig4cloud.pig.workflow.entity.WorkflowDefinition;
import com.pig4cloud.pig.workflow.entity.WorkflowNodeDefinition;
import com.pig4cloud.pig.workflow.entity.WorkflowTransition;
import com.pig4cloud.pig.workflow.mapper.WorkflowDefinitionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowNodeDefinitionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowTransitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 工作流定义管理服务。
 */
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

	private final WorkflowDefinitionMapper definitionMapper;

	private final WorkflowNodeDefinitionMapper nodeMapper;

	private final WorkflowTransitionMapper transitionMapper;

	@Transactional(rollbackFor = Exception.class)
	public WorkflowDefinition create(WorkflowDefinition definition) {
		Long count = definitionMapper.selectCount(Wrappers.<WorkflowDefinition>lambdaQuery()
			.eq(WorkflowDefinition::getCode, definition.getCode())
			.eq(WorkflowDefinition::getVersion, definition.getVersion() == null ? 1 : definition.getVersion()));
		if (count > 0) {
			throw new CheckedException("工作流编码和版本已存在");
		}
		definition.setVersion(definition.getVersion() == null ? 1 : definition.getVersion());
		definition.setStatus(WorkflowStatus.DEFINITION_DRAFT);
		definitionMapper.insert(definition);
		return definition;
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowNodeDefinition saveNode(WorkflowNodeDefinition node) {
		WorkflowDefinition definition = requireDraftDefinition(node.getDefinitionId());
		Long count = nodeMapper.selectCount(Wrappers.<WorkflowNodeDefinition>lambdaQuery()
			.eq(WorkflowNodeDefinition::getDefinitionId, definition.getId())
			.eq(WorkflowNodeDefinition::getNodeKey, node.getNodeKey())
			.ne(node.getId() != null, WorkflowNodeDefinition::getId, node.getId()));
		if (count > 0) {
			throw new CheckedException("同一工作流内节点标识不能重复");
		}
		if (node.getId() == null) {
			nodeMapper.insert(node);
		}
		else {
			nodeMapper.updateById(node);
		}
		return node;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteNode(Long nodeId) {
		WorkflowNodeDefinition node = nodeMapper.selectById(nodeId);
		if (node == null) {
			throw new CheckedException("节点不存在");
		}
		requireDraftDefinition(node.getDefinitionId());
		nodeMapper.deleteById(nodeId);
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowTransition saveTransition(WorkflowTransition transition) {
		requireDraftDefinition(transition.getDefinitionId());
		String action = transition.getAction() == null ? "" : transition.getAction().trim().toUpperCase(Locale.ROOT);
		if (!WorkflowAction.isSupported(action)) {
			throw new CheckedException("不支持的流转动作: " + action);
		}
		transition.setAction(action);
		transition.setPriority(transition.getPriority() == null ? 0 : transition.getPriority());
		requireNode(transition.getDefinitionId(), transition.getSourceNodeKey(), "来源节点不存在");
		if (StringUtils.hasText(transition.getTargetNodeKey())) {
			requireNode(transition.getDefinitionId(), transition.getTargetNodeKey(), "目标节点不存在");
		}
		if (transition.getId() == null) {
			transitionMapper.insert(transition);
		}
		else {
			transitionMapper.updateById(transition);
		}
		return transition;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteTransition(Long transitionId) {
		WorkflowTransition transition = transitionMapper.selectById(transitionId);
		if (transition == null) {
			throw new CheckedException("流转规则不存在");
		}
		requireDraftDefinition(transition.getDefinitionId());
		transitionMapper.deleteById(transitionId);
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowDefinition publish(Long definitionId) {
		WorkflowDefinition definition = requireDraftDefinition(definitionId);
		List<WorkflowNodeDefinition> nodes = nodeMapper.selectList(Wrappers.<WorkflowNodeDefinition>lambdaQuery()
			.eq(WorkflowNodeDefinition::getDefinitionId, definitionId)
			.orderByAsc(WorkflowNodeDefinition::getSortOrder));
		List<WorkflowTransition> transitions = transitionMapper.selectList(Wrappers.<WorkflowTransition>lambdaQuery()
			.eq(WorkflowTransition::getDefinitionId, definitionId));
		validateGraph(nodes, transitions);
		definition.setStatus(WorkflowStatus.DEFINITION_PUBLISHED);
		definitionMapper.updateById(definition);
		return definition;
	}

	private WorkflowDefinition requireDraftDefinition(Long definitionId) {
		WorkflowDefinition definition = definitionMapper.selectById(definitionId);
		if (definition == null) {
			throw new CheckedException("工作流定义不存在");
		}
		if (!WorkflowStatus.DEFINITION_DRAFT.equals(definition.getStatus())) {
			throw new CheckedException("只有草稿状态的工作流允许修改");
		}
		return definition;
	}

	private void validateGraph(List<WorkflowNodeDefinition> nodes, List<WorkflowTransition> transitions) {
		if (nodes.isEmpty()) {
			throw new CheckedException("工作流至少需要一个节点");
		}
		long startCount = nodes.stream().filter(node -> Boolean.TRUE.equals(node.getStartNode())).count();
		long endCount = nodes.stream().filter(node -> Boolean.TRUE.equals(node.getEndNode())).count();
		if (startCount != 1) {
			throw new CheckedException("工作流必须且只能有一个开始节点");
		}
		if (endCount < 1) {
			throw new CheckedException("工作流至少需要一个结束节点");
		}
		Set<String> keys = new HashSet<>();
		for (WorkflowNodeDefinition node : nodes) {
			if (!StringUtils.hasText(node.getNodeKey()) || !keys.add(node.getNodeKey())) {
				throw new CheckedException("节点标识不能为空且不能重复");
			}
		}
		for (WorkflowNodeDefinition node : nodes) {
			boolean hasTransition = transitions.stream()
				.anyMatch(transition -> node.getNodeKey().equals(transition.getSourceNodeKey()));
			if (!Boolean.TRUE.equals(node.getEndNode()) && !StringUtils.hasText(node.getNextNodeKey()) && !hasTransition) {
				throw new CheckedException("非结束节点必须指定下一节点");
			}
			if (StringUtils.hasText(node.getNextNodeKey()) && !keys.contains(node.getNextNodeKey())) {
				throw new CheckedException("节点 " + node.getNodeKey() + " 指向了不存在的下一节点");
			}
		}
		for (WorkflowTransition transition : transitions) {
			if (!keys.contains(transition.getSourceNodeKey())) {
				throw new CheckedException("流转规则指向了不存在的来源节点");
			}
			if (StringUtils.hasText(transition.getTargetNodeKey()) && !keys.contains(transition.getTargetNodeKey())) {
				throw new CheckedException("流转规则指向了不存在的目标节点");
			}
		}
	}

	private WorkflowNodeDefinition requireNode(Long definitionId, String nodeKey, String message) {
		WorkflowNodeDefinition node = nodeMapper.selectOne(Wrappers.<WorkflowNodeDefinition>lambdaQuery()
			.eq(WorkflowNodeDefinition::getDefinitionId, definitionId)
			.eq(WorkflowNodeDefinition::getNodeKey, nodeKey));
		if (node == null) {
			throw new CheckedException(message);
		}
		return node;
	}

}
