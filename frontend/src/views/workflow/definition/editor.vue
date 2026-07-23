<template>
	<el-dialog v-model="visible" fullscreen destroy-on-close :close-on-click-modal="false" class="workflow-editor-dialog">
		<template #header>
			<div class="editor-title">
				<div>
					<div class="title-line">
						<h2>{{ definition?.name }}</h2>
						<el-tag :type="readonly ? 'success' : 'info'">{{ readonly ? '已发布 · 只读' : '草稿 · 可编辑' }}</el-tag>
					</div>
					<p>{{ definition?.code }} · v{{ definition?.version }}</p>
				</div>
				<div class="header-actions">
					<el-button icon="Refresh" :loading="loading" @click="loadGraph">刷新</el-button>
					<el-button v-if="!readonly" v-auth="'workflow_definition_edit'" icon="Plus" type="primary" @click="openNodeForm()">添加节点</el-button>
					<el-button v-if="!readonly" v-auth="'workflow_definition_edit'" icon="Share" @click="openTransitionForm()" :disabled="nodes.length < 1"
						>添加规则</el-button
					>
					<el-button v-if="!readonly" v-auth="'workflow_definition_publish'" icon="Promotion" type="success" @click="publish">发布流程</el-button>
				</div>
			</div>
		</template>

		<div v-loading="loading" class="editor-shell">
			<section class="canvas-section">
				<div class="section-heading">
					<div>
						<h3>流程画布</h3>
						<p>按照排序展示默认执行顺序，节点下方显示动作分支。</p>
					</div>
					<div class="graph-stats">
						<span>{{ nodes.length }} 个节点</span><span>{{ transitions.length }} 条规则</span>
					</div>
				</div>

				<div v-if="nodes.length" class="flow-canvas">
					<template v-for="(node, index) in nodes" :key="node.id">
						<article
							:class="['node-card', `node-${node.nodeType.toLowerCase()}`, { active: selectedNode?.id === node.id }]"
							@click="selectedNode = node"
						>
							<div class="node-topline">
								<span class="node-order">{{ String(index + 1).padStart(2, '0') }}</span
								><el-tag v-if="node.startNode" size="small">开始</el-tag><el-tag v-if="node.endNode" size="small" type="success">结束</el-tag>
							</div>
							<div class="node-icon">{{ nodeTypeMeta(node.nodeType).icon }}</div>
							<h4>{{ node.nodeName }}</h4>
							<p>{{ node.nodeKey }}</p>
							<span class="node-type">{{ nodeTypeMeta(node.nodeType).label }}</span>
							<div v-if="outgoingTransitions(node.nodeKey).length" class="branch-list">
								<span v-for="rule in outgoingTransitions(node.nodeKey)" :key="rule.id"
									>{{ actionLabel(rule.action) }} → {{ nodeName(rule.targetNodeKey) }}</span
								>
							</div>
						</article>
						<div v-if="index < nodes.length - 1" class="flow-arrow">
							<span>{{ node.nextNodeKey ? `默认至 ${nodeName(node.nextNodeKey)}` : '排序' }}</span
							><b>→</b>
						</div>
					</template>
				</div>
				<el-empty v-else description="还没有节点，请从添加节点开始">
					<el-button v-if="!readonly" type="primary" @click="openNodeForm()">添加第一个节点</el-button>
				</el-empty>

				<div class="rule-panel">
					<div class="section-heading compact">
						<div>
							<h3>动作流转规则</h3>
							<p>同一来源节点按优先级匹配动作与条件。</p>
						</div>
					</div>
					<el-table :data="transitions" border empty-text="暂无动作流转规则">
						<el-table-column label="来源节点" min-width="150"
							><template #default="{ row }">{{ nodeName(row.sourceNodeKey) }}</template></el-table-column
						>
						<el-table-column label="动作" width="110"
							><template #default="{ row }"
								><el-tag effect="plain" :type="actionType(row.action)">{{ actionLabel(row.action) }}</el-tag></template
							></el-table-column
						>
						<el-table-column label="目标节点" min-width="150"
							><template #default="{ row }">{{ nodeName(row.targetNodeKey) }}</template></el-table-column
						>
						<el-table-column prop="conditionExpression" label="条件表达式" min-width="180"
							><template #default="{ row }">{{ row.conditionExpression || '始终匹配' }}</template></el-table-column
						>
						<el-table-column prop="priority" label="优先级" width="80" align="center" />
						<el-table-column label="默认" width="70" align="center"
							><template #default="{ row }">{{ row.defaultTransition ? '是' : '否' }}</template></el-table-column
						>
						<el-table-column v-if="!readonly" label="操作" width="120" fixed="right"
							><template #default="{ row }"
								><el-button text type="primary" @click="openTransitionForm(row)">编辑</el-button
								><el-button text type="danger" @click="removeTransition(row)">删除</el-button></template
							></el-table-column
						>
					</el-table>
				</div>
			</section>

			<aside class="inspector">
				<template v-if="selectedNode">
					<div class="inspector-heading">
						<div>
							<span>节点属性</span>
							<h3>{{ selectedNode.nodeName }}</h3>
						</div>
						<el-button v-if="!readonly" text type="primary" @click="openNodeForm(selectedNode)">编辑</el-button>
					</div>
					<el-descriptions :column="1" border>
						<el-descriptions-item label="节点标识">{{ selectedNode.nodeKey }}</el-descriptions-item>
						<el-descriptions-item label="节点类型">{{ nodeTypeMeta(selectedNode.nodeType).label }}</el-descriptions-item>
						<el-descriptions-item label="默认下一节点">{{ nodeName(selectedNode.nextNodeKey) }}</el-descriptions-item>
						<el-descriptions-item label="排序">{{ selectedNode.sortOrder }}</el-descriptions-item>
					</el-descriptions>
					<div class="config-block">
						<span>节点配置</span>
						<pre>{{ prettyJson(selectedNode.configJson) }}</pre>
					</div>
					<el-button v-if="!readonly" v-auth="'workflow_definition_edit'" class="delete-node" plain type="danger" @click="removeNode(selectedNode)"
						>删除该节点</el-button
					>
				</template>
				<el-empty v-else description="选择画布中的节点查看属性" />
			</aside>
		</div>

		<el-dialog v-model="nodeFormVisible" :title="nodeForm.id ? '编辑节点' : '添加节点'" width="620px" append-to-body destroy-on-close>
			<el-form ref="nodeFormRef" :model="nodeForm" :rules="nodeRules" label-width="105px">
				<el-row :gutter="16"
					><el-col :span="12"
						><el-form-item label="节点名称" prop="nodeName"><el-input v-model="nodeForm.nodeName" /></el-form-item></el-col
					><el-col :span="12"
						><el-form-item label="节点标识" prop="nodeKey"
							><el-input v-model="nodeForm.nodeKey" placeholder="例如 ui_review" :disabled="Boolean(nodeForm.id)" /></el-form-item></el-col
				></el-row>
				<el-row :gutter="16"
					><el-col :span="12"
						><el-form-item label="节点类型" prop="nodeType"
							><el-select v-model="nodeForm.nodeType" style="width: 100%"
								><el-option
									v-for="item in nodeTypes"
									:key="item.value"
									:label="`${item.icon} ${item.label}`"
									:value="item.value" /></el-select></el-form-item></el-col
					><el-col :span="12"
						><el-form-item label="排序"><el-input-number v-model="nodeForm.sortOrder" :min="1" :step="10" /></el-form-item></el-col
				></el-row>
				<el-row :gutter="16"
					><el-col :span="12"
						><el-form-item label="节点位置"
							><el-checkbox v-model="nodeForm.startNode">开始节点</el-checkbox
							><el-checkbox v-model="nodeForm.endNode">结束节点</el-checkbox></el-form-item
						></el-col
					><el-col :span="12"
						><el-form-item label="默认下一节点"
							><el-select v-model="nodeForm.nextNodeKey" clearable :disabled="nodeForm.endNode" style="width: 100%"
								><el-option
									v-for="node in nextNodeOptions"
									:key="node.id"
									:label="node.nodeName"
									:value="node.nodeKey" /></el-select></el-form-item></el-col
				></el-row>
				<el-form-item label="JSON 配置"
					><el-input v-model="nodeForm.configJson" type="textarea" :rows="6" placeholder='例如 {"candidateRoleId":1}'
				/></el-form-item>
			</el-form>
			<template #footer
				><el-button @click="nodeFormVisible = false">取消</el-button
				><el-button type="primary" :loading="saving" @click="submitNode">保存节点</el-button></template
			>
		</el-dialog>

		<el-dialog
			v-model="transitionFormVisible"
			:title="transitionForm.id ? '编辑流转规则' : '添加流转规则'"
			width="620px"
			append-to-body
			destroy-on-close
		>
			<el-form ref="transitionFormRef" :model="transitionForm" :rules="transitionRules" label-width="105px">
				<el-row :gutter="16"
					><el-col :span="12"
						><el-form-item label="来源节点" prop="sourceNodeKey"
							><el-select v-model="transitionForm.sourceNodeKey" style="width: 100%"
								><el-option v-for="node in nodes" :key="node.id" :label="node.nodeName" :value="node.nodeKey" /></el-select></el-form-item></el-col
					><el-col :span="12"
						><el-form-item label="动作" prop="action"
							><el-select v-model="transitionForm.action" style="width: 100%"
								><el-option v-for="item in actions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col
				></el-row>
				<el-row :gutter="16"
					><el-col :span="12"
						><el-form-item label="目标节点"
							><el-select v-model="transitionForm.targetNodeKey" clearable style="width: 100%"
								><el-option v-for="node in nodes" :key="node.id" :label="node.nodeName" :value="node.nodeKey" /></el-select></el-form-item></el-col
					><el-col :span="12"
						><el-form-item label="优先级"><el-input-number v-model="transitionForm.priority" :min="0" /></el-form-item></el-col
				></el-row>
				<el-form-item label="条件表达式"
					><el-input v-model="transitionForm.conditionExpression" placeholder="可选，留空表示无附加条件"
				/></el-form-item>
				<el-form-item label="默认规则"
					><el-switch v-model="transitionForm.defaultTransition" /><span class="field-tip">同一动作没有其他条件命中时使用</span></el-form-item
				>
			</el-form>
			<template #footer
				><el-button @click="transitionFormVisible = false">取消</el-button
				><el-button type="primary" :loading="saving" @click="submitTransition">保存规则</el-button></template
			>
		</el-dialog>
	</el-dialog>
</template>

<script setup lang="ts" name="workflowDefinitionEditor">
import type { FormInstance, FormRules, TagProps } from 'element-plus';
import {
	deleteDefinitionNode,
	deleteDefinitionTransition,
	getDefinitionNodes,
	getDefinitionTransitions,
	publishDefinition,
	saveDefinitionNode,
	saveDefinitionTransition,
	type WorkflowDefinition,
	type WorkflowNodeDefinition,
	type WorkflowTransition,
} from '/@/api/workflow';
import { useMessage, useMessageBox } from '/@/hooks/message';

const emit = defineEmits(['published', 'changed']);
const nodeTypes = [
	{ value: 'AI', label: 'AI 生成', icon: '✦' },
	{ value: 'SERVICE', label: '服务执行', icon: '⚙' },
	{ value: 'MANUAL_REVIEW', label: '人工审核', icon: '✓' },
	{ value: 'DELIVERY', label: '交付', icon: '▣' },
] as const;
const actions = [
	{ value: 'COMPLETE', label: '完成' },
	{ value: 'APPROVE', label: '通过' },
	{ value: 'REJECT', label: '驳回' },
	{ value: 'RETURN', label: '退回' },
	{ value: 'RETRY', label: '重试' },
	{ value: 'CANCEL', label: '取消' },
	{ value: 'SKIP', label: '跳过' },
	{ value: 'FAIL', label: '失败' },
] as const;

const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const definition = ref<WorkflowDefinition>();
const nodes = ref<WorkflowNodeDefinition[]>([]);
const transitions = ref<WorkflowTransition[]>([]);
const selectedNode = ref<WorkflowNodeDefinition>();
const readonly = computed(() => definition.value?.status === 'PUBLISHED');
const nodeFormVisible = ref(false);
const transitionFormVisible = ref(false);
const nodeFormRef = ref<FormInstance>();
const transitionFormRef = ref<FormInstance>();
const nodeForm = reactive<any>({});
const transitionForm = reactive<any>({});
const nodeRules: FormRules = {
	nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
	nodeKey: [{ required: true, message: '请输入节点标识', trigger: 'blur' }],
	nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
};
const transitionRules: FormRules = {
	sourceNodeKey: [{ required: true, message: '请选择来源节点', trigger: 'change' }],
	action: [{ required: true, message: '请选择动作', trigger: 'change' }],
};
const nextNodeOptions = computed(() => nodes.value.filter((item) => item.id !== nodeForm.id));

const nodeTypeMeta = (type: string) => nodeTypes.find((item) => item.value === type) || { label: type, icon: '•' };
const actionLabel = (action: string) => actions.find((item) => item.value === action)?.label || action;
const actionType = (action: string): TagProps['type'] =>
	(({ APPROVE: 'success', REJECT: 'danger', RETURN: 'warning', FAIL: 'danger', CANCEL: 'info' })[action] as TagProps['type']) || 'primary';
const nodeName = (key?: string) => (key ? nodes.value.find((item) => item.nodeKey === key)?.nodeName || key : '流程结束');
const outgoingTransitions = (key: string) => transitions.value.filter((item) => item.sourceNodeKey === key);
const prettyJson = (value?: string) => {
	if (!value) return '未配置';
	try {
		return JSON.stringify(JSON.parse(value), null, 2);
	} catch {
		return value;
	}
};

const loadGraph = async () => {
	if (!definition.value) return;
	loading.value = true;
	try {
		const [nodeResult, transitionResult] = await Promise.all([
			getDefinitionNodes(definition.value.id),
			getDefinitionTransitions(definition.value.id),
		]);
		nodes.value = nodeResult.data || [];
		transitions.value = transitionResult.data || [];
		selectedNode.value = selectedNode.value ? nodes.value.find((item) => item.id === selectedNode.value?.id) : nodes.value[0];
	} finally {
		loading.value = false;
	}
};

const open = async (item: WorkflowDefinition) => {
	definition.value = { ...item };
	visible.value = true;
	selectedNode.value = undefined;
	await loadGraph();
};

const openNodeForm = (node?: WorkflowNodeDefinition) => {
	Object.assign(
		nodeForm,
		node
			? { ...node }
			: {
					id: undefined,
					nodeName: '',
					nodeKey: '',
					nodeType: 'AI',
					sortOrder: (nodes.value.at(-1)?.sortOrder || 0) + 10,
					startNode: nodes.value.length === 0,
					endNode: false,
					nextNodeKey: '',
					configJson: '',
				}
	);
	nodeFormVisible.value = true;
};

const submitNode = async () => {
	if (!(await nodeFormRef.value?.validate()) || !definition.value) return;
	if (nodeForm.configJson) {
		try {
			JSON.parse(nodeForm.configJson);
		} catch {
			useMessage().error('节点配置必须是合法 JSON');
			return;
		}
	}
	if (nodeForm.endNode) nodeForm.nextNodeKey = '';
	saving.value = true;
	try {
		if (nodeForm.startNode) {
			await Promise.all(
				nodes.value
					.filter((item) => item.startNode && item.id !== nodeForm.id)
					.map((item) => saveDefinitionNode(definition.value!.id, { ...item, startNode: false }))
			);
		}
		const result = await saveDefinitionNode(definition.value.id, { ...nodeForm });
		selectedNode.value = result.data;
		nodeFormVisible.value = false;
		useMessage().success('节点已保存');
		await loadGraph();
		emit('changed');
	} finally {
		saving.value = false;
	}
};

const removeNode = async (node: WorkflowNodeDefinition) => {
	const related = transitions.value.filter((item) => item.sourceNodeKey === node.nodeKey || item.targetNodeKey === node.nodeKey);
	try {
		await useMessageBox().confirm(`确认删除节点“${node.nodeName}”吗？${related.length ? `关联的 ${related.length} 条流转规则也会删除。` : ''}`);
	} catch {
		return;
	}
	await Promise.all(related.map((item) => deleteDefinitionTransition(item.id)));
	await deleteDefinitionNode(node.id);
	selectedNode.value = undefined;
	useMessage().success('节点已删除');
	await loadGraph();
	emit('changed');
};

const openTransitionForm = (transition?: WorkflowTransition) => {
	Object.assign(
		transitionForm,
		transition
			? { ...transition }
			: {
					id: undefined,
					sourceNodeKey: selectedNode.value?.nodeKey || nodes.value[0]?.nodeKey || '',
					targetNodeKey: '',
					action: 'COMPLETE',
					conditionExpression: '',
					priority: 0,
					defaultTransition: true,
				}
	);
	transitionFormVisible.value = true;
};

const submitTransition = async () => {
	if (!(await transitionFormRef.value?.validate()) || !definition.value) return;
	saving.value = true;
	try {
		await saveDefinitionTransition(definition.value.id, { ...transitionForm });
		transitionFormVisible.value = false;
		useMessage().success('流转规则已保存');
		await loadGraph();
		emit('changed');
	} finally {
		saving.value = false;
	}
};

const removeTransition = async (transition: WorkflowTransition) => {
	try {
		await useMessageBox().confirm(`确认删除“${nodeName(transition.sourceNodeKey)} → ${actionLabel(transition.action)}”规则吗？`);
	} catch {
		return;
	}
	await deleteDefinitionTransition(transition.id);
	useMessage().success('流转规则已删除');
	await loadGraph();
	emit('changed');
};

const publish = async () => {
	if (!definition.value) return;
	try {
		await useMessageBox().confirm(`确认发布流程“${definition.value.name}”吗？系统会校验开始节点、结束节点和所有流转目标。`);
	} catch {
		return;
	}
	await publishDefinition(definition.value.id);
	definition.value.status = 'PUBLISHED';
	useMessage().success('流程已发布，编辑器已切换为只读模式');
	emit('published');
};

defineExpose({ open });
</script>

<style scoped>
.editor-title,
.title-line,
.header-actions,
.section-heading,
.graph-stats,
.node-topline,
.inspector-heading {
	display: flex;
	align-items: center;
}
.editor-title {
	justify-content: space-between;
	padding-right: 28px;
}
.title-line {
	gap: 12px;
}
.title-line h2 {
	margin: 0;
	font-size: 22px;
}
.editor-title p,
.section-heading p {
	margin: 5px 0 0;
	color: var(--el-text-color-secondary);
}
.header-actions {
	gap: 8px;
}
.editor-shell {
	display: grid;
	grid-template-columns: minmax(0, 1fr) 300px;
	gap: 18px;
	min-height: calc(100vh - 96px);
	padding: 4px 2px 20px;
	background: var(--el-fill-color-lighter);
}
.canvas-section,
.inspector {
	background: var(--el-bg-color);
	border: 1px solid var(--el-border-color-lighter);
	border-radius: 12px;
}
.canvas-section {
	min-width: 0;
	padding: 20px;
}
.section-heading {
	justify-content: space-between;
	margin-bottom: 18px;
}
.section-heading h3,
.inspector-heading h3 {
	margin: 0;
}
.section-heading.compact {
	margin-top: 30px;
}
.graph-stats {
	gap: 8px;
}
.graph-stats span {
	padding: 6px 10px;
	border-radius: 16px;
	background: var(--el-fill-color);
	color: var(--el-text-color-secondary);
}
.flow-canvas {
	display: flex;
	align-items: center;
	min-height: 270px;
	padding: 30px;
	overflow-x: auto;
	border: 1px dashed var(--el-border-color);
	border-radius: 12px;
	background-image: radial-gradient(var(--el-border-color-lighter) 1px, transparent 1px);
	background-size: 18px 18px;
}
.node-card {
	flex: 0 0 190px;
	min-height: 190px;
	padding: 14px;
	border: 2px solid var(--el-border-color-light);
	border-top: 5px solid var(--el-color-primary);
	border-radius: 12px;
	background: var(--el-bg-color);
	box-shadow: var(--el-box-shadow-light);
	cursor: pointer;
	transition: 0.2s ease;
}
.node-card:hover,
.node-card.active {
	transform: translateY(-3px);
	border-color: var(--el-color-primary);
}
.node-card.node-manual_review {
	border-top-color: var(--el-color-warning);
}
.node-card.node-delivery {
	border-top-color: var(--el-color-success);
}
.node-card.node-service {
	border-top-color: var(--el-color-info);
}
.node-topline {
	min-height: 24px;
	gap: 5px;
}
.node-order {
	margin-right: auto;
	color: var(--el-text-color-placeholder);
	font-weight: 700;
}
.node-icon {
	margin-top: 12px;
	font-size: 28px;
}
.node-card h4 {
	margin: 8px 0 3px;
	font-size: 16px;
}
.node-card > p {
	margin: 0;
	color: var(--el-text-color-secondary);
	font-family: monospace;
}
.node-type {
	display: block;
	margin-top: 10px;
	font-size: 12px;
	color: var(--el-text-color-secondary);
}
.branch-list {
	display: flex;
	flex-direction: column;
	gap: 4px;
	margin-top: 10px;
	padding-top: 8px;
	border-top: 1px solid var(--el-border-color-lighter);
}
.branch-list span {
	font-size: 11px;
	color: var(--el-text-color-secondary);
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.flow-arrow {
	flex: 0 0 96px;
	text-align: center;
	color: var(--el-text-color-secondary);
}
.flow-arrow span {
	display: block;
	height: 18px;
	font-size: 11px;
}
.flow-arrow b {
	font-size: 28px;
	color: var(--el-color-primary);
}
.inspector {
	padding: 18px;
}
.inspector-heading {
	justify-content: space-between;
	margin-bottom: 16px;
}
.inspector-heading span,
.config-block > span {
	color: var(--el-text-color-secondary);
	font-size: 12px;
}
.inspector-heading h3 {
	margin-top: 4px;
}
.config-block {
	margin-top: 18px;
}
.config-block pre {
	min-height: 80px;
	padding: 12px;
	overflow: auto;
	border-radius: 8px;
	background: var(--el-fill-color-light);
	white-space: pre-wrap;
	word-break: break-word;
}
.delete-node {
	width: 100%;
	margin-top: 16px;
}
.field-tip {
	margin-left: 10px;
	color: var(--el-text-color-secondary);
	font-size: 12px;
}
@media (max-width: 1100px) {
	.editor-shell {
		grid-template-columns: 1fr;
	}
	.inspector {
		min-height: 220px;
	}
	.editor-title {
		align-items: flex-start;
	}
	.header-actions {
		flex-wrap: wrap;
		justify-content: flex-end;
	}
}
</style>
