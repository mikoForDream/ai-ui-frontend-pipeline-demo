<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view workflow-page">
			<div class="page-heading">
				<div>
					<h2>流程实例</h2>
					<p>跟踪每次工作流执行的当前节点、任务状态和完整时间线。</p>
				</div>
				<el-button v-auth="'workflow_instance_start'" type="primary" icon="VideoPlay" @click="openStart">启动流程</el-button>
			</div>
			<el-form :inline="true" :model="query" class="search-form">
				<el-form-item label="实例标题"><el-input v-model="query.title" clearable placeholder="输入标题" @keyup.enter="loadData" /></el-form-item>
				<el-form-item label="状态">
					<el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px">
						<el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
					</el-select>
				</el-form-item>
				<el-form-item
					><el-button type="primary" icon="Search" @click="loadData">查询</el-button
					><el-button icon="Refresh" @click="reset">重置</el-button></el-form-item
				>
			</el-form>

			<el-table v-loading="loading" :data="rows" border>
				<el-table-column prop="title" label="实例标题" min-width="200" show-overflow-tooltip />
				<el-table-column prop="businessKey" label="业务标识" min-width="170" show-overflow-tooltip />
				<el-table-column prop="currentNodeKey" label="当前节点" width="140"
					><template #default="{ row }">{{ row.currentNodeKey || '—' }}</template></el-table-column
				>
				<el-table-column prop="status" label="状态" width="110" align="center"
					><template #default="{ row }"
						><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template
					></el-table-column
				>
				<el-table-column prop="startedBy" label="发起人" width="120" />
				<el-table-column prop="startedAt" label="启动时间" width="170" />
				<el-table-column label="操作" width="100" fixed="right" align="center"
					><template #default="{ row }"><el-button text type="primary" @click="showDetail(row)">执行详情</el-button></template></el-table-column
				>
			</el-table>
			<pagination v-bind="pagination" @current-change="changePage" @size-change="changeSize" />
		</div>

		<el-dialog v-model="startVisible" title="启动工作流" width="600px" destroy-on-close>
			<el-form ref="startFormRef" :model="startForm" :rules="rules" label-width="100px">
				<el-form-item label="流程定义" prop="definitionId"
					><el-select v-model="startForm.definitionId" filterable placeholder="选择已发布流程" style="width: 100%"
						><el-option v-for="item in definitions" :key="item.id" :label="`${item.name} · v${item.version}`" :value="item.id" /></el-select
				></el-form-item>
				<el-form-item label="实例标题" prop="title"><el-input v-model="startForm.title" maxlength="100" /></el-form-item>
				<el-form-item label="业务标识" prop="businessKey"><el-input v-model="startForm.businessKey" maxlength="100" /></el-form-item>
				<el-form-item label="输入参数"
					><el-input v-model="startForm.inputJson" type="textarea" :rows="6" placeholder='可选，例如 {"requirementId":"REQ-001"}'
				/></el-form-item>
			</el-form>
			<template #footer
				><el-button @click="startVisible = false">取消</el-button
				><el-button type="primary" :loading="submitting" @click="submitStart">启动</el-button></template
			>
		</el-dialog>

		<el-drawer v-model="detailVisible" :title="detail.instance?.title || '执行详情'" size="720px">
			<el-descriptions v-if="detail.instance" :column="2" border>
				<el-descriptions-item label="状态"
					><el-tag :type="statusType(detail.instance.status)">{{ statusLabel(detail.instance.status) }}</el-tag></el-descriptions-item
				>
				<el-descriptions-item label="当前节点">{{ detail.instance.currentNodeKey || '—' }}</el-descriptions-item>
				<el-descriptions-item label="业务标识">{{ detail.instance.businessKey }}</el-descriptions-item>
				<el-descriptions-item label="发起人">{{ detail.instance.startedBy }}</el-descriptions-item>
			</el-descriptions>
			<el-tabs class="detail-tabs">
				<el-tab-pane :label="`任务 ${detail.tasks.length}`">
					<el-table :data="detail.tasks" border
						><el-table-column prop="nodeName" label="节点" min-width="140" /><el-table-column
							prop="taskType"
							label="类型"
							width="120" /><el-table-column prop="status" label="状态" width="110"
							><template #default="{ row }"
								><el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template
							></el-table-column
						><el-table-column prop="retryCount" label="重试" width="70" /><el-table-column prop="createTime" label="创建时间" width="170"
					/></el-table>
				</el-tab-pane>
				<el-tab-pane :label="`审核 ${detail.approvals.length}`"
					><el-table :data="detail.approvals" border
						><el-table-column prop="approvalType" label="审核类型" width="130" /><el-table-column
							prop="status"
							label="状态"
							width="100" /><el-table-column prop="decision" label="决定" width="100" /><el-table-column
							prop="reviewerName"
							label="审核人"
							width="110" /><el-table-column prop="comment" label="意见" min-width="180" /></el-table
				></el-tab-pane>
				<el-tab-pane :label="`日志 ${detail.logs.length}`">
					<el-timeline
						><el-timeline-item v-for="log in detail.logs" :key="log.id" :timestamp="log.createTime" placement="top"
							><strong>{{ log.eventType }}</strong
							><el-tag class="log-status" size="small" :type="statusType(log.status)">{{ statusLabel(log.status) }}</el-tag>
							<p>
								{{ log.nodeKey }}<span v-if="log.errorMessage"> · {{ log.errorMessage }}</span>
							</p></el-timeline-item
						></el-timeline
					>
				</el-tab-pane>
			</el-tabs>
		</el-drawer>
	</div>
</template>

<script setup lang="ts" name="workflowInstance">
import type { FormInstance, FormRules, TagProps } from 'element-plus';
import {
	getDefinitionPage,
	getInstanceDetail,
	getInstancePage,
	startInstance,
	type WorkflowDefinition,
	type WorkflowInstance,
} from '/@/api/workflow';
import { useMessage } from '/@/hooks/message';

const statusOptions = [
	{ label: '运行中', value: 'RUNNING' },
	{ label: '已完成', value: 'COMPLETED' },
	{ label: '失败', value: 'FAILED' },
	{ label: '已取消', value: 'CANCELLED' },
];
const statusLabel = (status: string) =>
	({ RUNNING: '运行中', READY: '待执行', COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消', REJECTED: '已驳回', RETURNED: '已退回' })[
		status
	] || status;
const statusType = (status: string): TagProps['type'] =>
	(({ COMPLETED: 'success', RUNNING: 'primary', READY: 'warning', FAILED: 'danger', REJECTED: 'danger', RETURNED: 'warning', CANCELLED: 'info' })[
		status
	] as TagProps['type']) || 'info';

const loading = ref(false);
const submitting = ref(false);
const rows = ref<WorkflowInstance[]>([]);
const definitions = ref<WorkflowDefinition[]>([]);
const query = reactive({ title: '', status: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });
const startVisible = ref(false);
const detailVisible = ref(false);
const startFormRef = ref<FormInstance>();
const startForm = reactive({ definitionId: '', title: '', businessKey: '', inputJson: '' });
const detail = reactive<{ instance?: WorkflowInstance; tasks: any[]; approvals: any[]; logs: any[] }>({ tasks: [], approvals: [], logs: [] });
const rules: FormRules = {
	definitionId: [{ required: true, message: '请选择流程定义', trigger: 'change' }],
	title: [{ required: true, message: '请输入实例标题', trigger: 'blur' }],
	businessKey: [{ required: true, message: '请输入业务标识', trigger: 'blur' }],
};

const loadData = async () => {
	loading.value = true;
	try {
		const res = await getInstancePage({ ...query, current: pagination.current, size: pagination.size });
		rows.value = res.data.records;
		pagination.total = res.data.total;
	} finally {
		loading.value = false;
	}
};
const reset = () => {
	query.title = '';
	query.status = '';
	pagination.current = 1;
	loadData();
};
const changePage = (value: number) => {
	pagination.current = value;
	loadData();
};
const changeSize = (value: number) => {
	pagination.size = value;
	pagination.current = 1;
	loadData();
};

const openStart = async () => {
	Object.assign(startForm, { definitionId: '', title: '', businessKey: `WF-${Date.now()}`, inputJson: '' });
	const res = await getDefinitionPage({ current: 1, size: 100, status: 'PUBLISHED' });
	definitions.value = res.data.records;
	startVisible.value = true;
};

const submitStart = async () => {
	if (!(await startFormRef.value?.validate())) return;
	if (startForm.inputJson) {
		try {
			JSON.parse(startForm.inputJson);
		} catch {
			useMessage().error('输入参数必须是合法 JSON');
			return;
		}
	}
	submitting.value = true;
	try {
		await startInstance(startForm);
		useMessage().success('工作流已启动');
		startVisible.value = false;
		loadData();
	} finally {
		submitting.value = false;
	}
};

const showDetail = async (row: WorkflowInstance) => {
	detailVisible.value = true;
	const res = await getInstanceDetail(row.id);
	Object.assign(detail, res.data);
};

onMounted(loadData);
</script>

<style scoped>
.workflow-page {
	padding: 20px;
}
.page-heading {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	margin-bottom: 20px;
}
.page-heading h2 {
	margin: 0 0 6px;
	font-size: 22px;
}
.page-heading p,
.el-timeline-item p {
	margin: 0;
	color: var(--el-text-color-secondary);
}
.search-form {
	padding: 16px 16px 0;
	margin-bottom: 16px;
	background: var(--el-fill-color-light);
	border-radius: 8px;
}
.detail-tabs {
	margin-top: 20px;
}
.log-status {
	margin-left: 10px;
}
</style>
