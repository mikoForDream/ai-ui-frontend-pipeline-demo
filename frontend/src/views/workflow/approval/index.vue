<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view workflow-page">
			<div class="page-heading">
				<div>
					<h2>人工审核</h2>
					<p>集中处理待领取、待决策和已完成的人工审核节点。</p>
				</div>
				<el-button icon="Refresh" @click="loadData">刷新待办</el-button>
			</div>
			<div class="summary-grid">
				<div class="summary-card">
					<span>当前列表</span><strong>{{ pagination.total }}</strong>
				</div>
				<div class="summary-card pending">
					<span>待领取</span><strong>{{ countByStatus('PENDING') }}</strong>
				</div>
				<div class="summary-card claimed">
					<span>处理中</span><strong>{{ countByStatus('CLAIMED') }}</strong>
				</div>
				<div class="summary-card decided">
					<span>已决策</span><strong>{{ countByStatus('DECIDED') }}</strong>
				</div>
			</div>
			<el-form :inline="true" :model="query" class="search-form">
				<el-form-item label="实例 ID"><el-input v-model="query.instanceId" clearable placeholder="输入实例 ID" /></el-form-item>
				<el-form-item label="状态"
					><el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px"
						><el-option label="待领取" value="PENDING" /><el-option label="处理中" value="CLAIMED" /><el-option
							label="已决策"
							value="DECIDED" /></el-select
				></el-form-item>
				<el-form-item
					><el-button type="primary" icon="Search" @click="loadData">查询</el-button
					><el-button icon="Refresh" @click="reset">重置</el-button></el-form-item
				>
			</el-form>

			<el-table v-loading="loading" :data="rows" border>
				<el-table-column prop="instanceId" label="实例 ID" min-width="180" show-overflow-tooltip />
				<el-table-column prop="approvalType" label="审核类型" width="140" />
				<el-table-column prop="status" label="状态" width="110" align="center"
					><template #default="{ row }"
						><el-tag :type="approvalStatusType(row.status)">{{ approvalStatusLabel(row.status) }}</el-tag></template
					></el-table-column
				>
				<el-table-column prop="decision" label="审核决定" width="110" align="center"
					><template #default="{ row }"
						><el-tag v-if="row.decision" :type="decisionType(row.decision)" effect="plain">{{ decisionLabel(row.decision) }}</el-tag
						><span v-else>—</span></template
					></el-table-column
				>
				<el-table-column prop="reviewerName" label="审核人" width="120"
					><template #default="{ row }">{{ row.reviewerName || '未领取' }}</template></el-table-column
				>
				<el-table-column prop="comment" label="审核意见" min-width="200" show-overflow-tooltip />
				<el-table-column prop="createTime" label="创建时间" width="170" />
				<el-table-column label="操作" width="190" fixed="right" align="center">
					<template #default="{ row }">
						<el-button v-if="row.status === 'PENDING'" v-auth="'workflow_approval_claim'" text type="primary" @click="claim(row)">领取</el-button>
						<el-button v-if="row.status !== 'DECIDED'" v-auth="'workflow_approval_decide'" text type="success" @click="openDecision(row)"
							>提交决定</el-button
						>
						<el-button v-if="row.status === 'DECIDED'" text type="primary" @click="showResult(row)">查看结果</el-button>
					</template>
				</el-table-column>
			</el-table>
			<pagination v-bind="pagination" @current-change="changePage" @size-change="changeSize" />
		</div>

		<el-dialog v-model="decisionVisible" title="提交审核决定" width="560px" destroy-on-close>
			<el-alert
				title="审核决定会立即推动流程进入下一节点，请确认意见与产物后再提交。"
				type="warning"
				show-icon
				:closable="false"
				class="decision-alert"
			/>
			<el-form label-width="90px">
				<el-form-item label="决定"
					><el-radio-group v-model="decisionForm.decision"
						><el-radio-button value="APPROVE">通过</el-radio-button><el-radio-button value="REJECT">驳回</el-radio-button
						><el-radio-button value="RETURN">退回</el-radio-button></el-radio-group
					></el-form-item
				>
				<el-form-item label="审核意见"
					><el-input v-model="decisionForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit
				/></el-form-item>
				<el-form-item label="输出参数"><el-input v-model="decisionForm.outputJson" type="textarea" :rows="4" placeholder="可选 JSON" /></el-form-item>
			</el-form>
			<template #footer
				><el-button @click="decisionVisible = false">取消</el-button
				><el-button type="primary" :loading="submitting" @click="submitDecision">确认提交</el-button></template
			>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="workflowApproval">
import type { TagProps } from 'element-plus';
import { claimApproval, decideApproval, getApprovalPage, type WorkflowApproval } from '/@/api/workflow';
import { useMessage, useMessageBox } from '/@/hooks/message';

const loading = ref(false);
const submitting = ref(false);
const rows = ref<WorkflowApproval[]>([]);
const query = reactive({ instanceId: '', status: 'PENDING' });
const pagination = reactive({ current: 1, size: 10, total: 0 });
const decisionVisible = ref(false);
const activeApproval = ref<WorkflowApproval>();
const decisionForm = reactive<{ decision: 'APPROVE' | 'REJECT' | 'RETURN'; comment: string; outputJson: string }>({
	decision: 'APPROVE',
	comment: '',
	outputJson: '',
});

const approvalStatusLabel = (status: string) => ({ PENDING: '待领取', CLAIMED: '处理中', DECIDED: '已决策' })[status] || status;
const approvalStatusType = (status: string): TagProps['type'] =>
	(({ PENDING: 'warning', CLAIMED: 'primary', DECIDED: 'success' })[status] as TagProps['type']) || 'info';
const decisionLabel = (decision: string) => ({ APPROVE: '通过', REJECT: '驳回', RETURN: '退回' })[decision] || decision;
const decisionType = (decision: string): TagProps['type'] =>
	(({ APPROVE: 'success', REJECT: 'danger', RETURN: 'warning' })[decision] as TagProps['type']) || 'info';
const countByStatus = (status: string) => rows.value.filter((item) => item.status === status).length;

const loadData = async () => {
	loading.value = true;
	try {
		const res = await getApprovalPage({ ...query, current: pagination.current, size: pagination.size });
		rows.value = res.data.records;
		pagination.total = res.data.total;
	} finally {
		loading.value = false;
	}
};
const reset = () => {
	query.instanceId = '';
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

const claim = async (row: WorkflowApproval) => {
	await claimApproval(row.id);
	useMessage().success('审核待办已领取');
	loadData();
};

const openDecision = (row: WorkflowApproval) => {
	activeApproval.value = row;
	Object.assign(decisionForm, { decision: 'APPROVE', comment: '', outputJson: '' });
	decisionVisible.value = true;
};

const submitDecision = async () => {
	if (!activeApproval.value) return;
	if (decisionForm.outputJson) {
		try {
			JSON.parse(decisionForm.outputJson);
		} catch {
			useMessage().error('输出参数必须是合法 JSON');
			return;
		}
	}
	submitting.value = true;
	try {
		await decideApproval(activeApproval.value.id, { ...decisionForm, operationKey: crypto.randomUUID() });
		useMessage().success('审核决定已提交');
		decisionVisible.value = false;
		loadData();
	} finally {
		submitting.value = false;
	}
};

const showResult = (row: WorkflowApproval) => {
	useMessageBox().info(`审核决定：${decisionLabel(row.decision || '')}\n审核人：${row.reviewerName || '—'}\n审核意见：${row.comment || '无'}`);
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
.page-heading p {
	margin: 0;
	color: var(--el-text-color-secondary);
}
.summary-grid {
	display: grid;
	grid-template-columns: repeat(4, minmax(140px, 1fr));
	gap: 12px;
	margin-bottom: 16px;
}
.summary-card {
	padding: 16px;
	border: 1px solid var(--el-border-color-lighter);
	border-radius: 8px;
	background: var(--el-bg-color);
}
.summary-card span {
	color: var(--el-text-color-secondary);
}
.summary-card strong {
	display: block;
	margin-top: 8px;
	font-size: 26px;
}
.summary-card.pending {
	border-left: 4px solid var(--el-color-warning);
}
.summary-card.claimed {
	border-left: 4px solid var(--el-color-primary);
}
.summary-card.decided {
	border-left: 4px solid var(--el-color-success);
}
.search-form {
	padding: 16px 16px 0;
	margin-bottom: 16px;
	background: var(--el-fill-color-light);
	border-radius: 8px;
}
.decision-alert {
	margin-bottom: 18px;
}
@media (max-width: 900px) {
	.summary-grid {
		grid-template-columns: repeat(2, 1fr);
	}
}
</style>
