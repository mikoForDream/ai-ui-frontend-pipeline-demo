<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view workflow-page">
			<div class="page-heading">
				<div>
					<h2>流程定义</h2>
					<p>维护工作流版本，检查节点结构并发布可执行流程。</p>
				</div>
				<el-button v-auth="'workflow_definition_add'" type="primary" icon="Plus" @click="openCreate">新建流程</el-button>
			</div>

			<el-form :inline="true" :model="query" class="search-form">
				<el-form-item label="流程名称"><el-input v-model="query.name" clearable placeholder="输入名称" @keyup.enter="loadData" /></el-form-item>
				<el-form-item label="状态">
					<el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px">
						<el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" />
					</el-select>
				</el-form-item>
				<el-form-item
					><el-button type="primary" icon="Search" @click="loadData">查询</el-button
					><el-button icon="Refresh" @click="reset">重置</el-button></el-form-item
				>
			</el-form>

			<el-table v-loading="loading" :data="rows" border>
				<el-table-column prop="name" label="流程名称" min-width="180" show-overflow-tooltip />
				<el-table-column prop="code" label="流程编码" min-width="160" show-overflow-tooltip />
				<el-table-column prop="version" label="版本" width="80" align="center"
					><template #default="{ row }">v{{ row.version }}</template></el-table-column
				>
				<el-table-column prop="status" label="状态" width="110" align="center">
					<template #default="{ row }"
						><el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}</el-tag></template
					>
				</el-table-column>
				<el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
				<el-table-column prop="createTime" label="创建时间" width="170" />
				<el-table-column label="操作" width="190" fixed="right" align="center">
					<template #default="{ row }">
						<el-button text type="primary" @click="showStructure(row)">查看结构</el-button>
						<el-button v-if="row.status === 'DRAFT'" v-auth="'workflow_definition_publish'" text type="success" @click="publish(row)">发布</el-button>
					</template>
				</el-table-column>
			</el-table>
			<pagination v-bind="pagination" @current-change="changePage" @size-change="changeSize" />
		</div>

		<el-dialog v-model="createVisible" title="新建流程定义" width="560px" destroy-on-close>
			<el-form ref="createFormRef" :model="createForm" :rules="rules" label-width="90px">
				<el-form-item label="流程名称" prop="name"><el-input v-model="createForm.name" maxlength="64" /></el-form-item>
				<el-form-item label="流程编码" prop="code"
					><el-input v-model="createForm.code" placeholder="例如 product_ui_delivery" maxlength="64"
				/></el-form-item>
				<el-form-item label="版本" prop="version"><el-input-number v-model="createForm.version" :min="1" /></el-form-item>
				<el-form-item label="说明"
					><el-input v-model="createForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit
				/></el-form-item>
			</el-form>
			<template #footer
				><el-button @click="createVisible = false">取消</el-button
				><el-button type="primary" :loading="submitting" @click="submitCreate">创建草稿</el-button></template
			>
		</el-dialog>

		<el-drawer v-model="structureVisible" :title="`${activeDefinition?.name || ''} · 流程结构`" size="520px">
			<el-empty v-if="!nodes.length" description="尚未配置节点" />
			<el-timeline v-else>
				<el-timeline-item
					v-for="node in nodes"
					:key="node.id"
					:type="node.endNode ? 'success' : node.startNode ? 'primary' : ''"
					:timestamp="node.nodeType"
					placement="top"
				>
					<el-card shadow="never"
						><strong>{{ node.nodeName }}</strong
						><span class="node-key">{{ node.nodeKey }}</span>
						<p>下一节点：{{ node.nextNodeKey || (node.endNode ? '流程结束' : '按动作路由') }}</p></el-card
					>
				</el-timeline-item>
			</el-timeline>
		</el-drawer>
	</div>
</template>

<script setup lang="ts" name="workflowDefinition">
import type { FormInstance, FormRules } from 'element-plus';
import { createDefinition, getDefinitionNodes, getDefinitionPage, publishDefinition, type WorkflowDefinition } from '/@/api/workflow';
import { useMessage, useMessageBox } from '/@/hooks/message';

const loading = ref(false);
const submitting = ref(false);
const rows = ref<WorkflowDefinition[]>([]);
const query = reactive({ name: '', status: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });
const createVisible = ref(false);
const structureVisible = ref(false);
const activeDefinition = ref<WorkflowDefinition>();
const nodes = ref<any[]>([]);
const createFormRef = ref<FormInstance>();
const createForm = reactive({ name: '', code: '', version: 1, description: '' });
const rules: FormRules = {
	name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
	code: [{ required: true, message: '请输入流程编码', trigger: 'blur' }],
};

const loadData = async () => {
	loading.value = true;
	try {
		const res = await getDefinitionPage({ ...query, current: pagination.current, size: pagination.size });
		rows.value = res.data.records;
		pagination.total = res.data.total;
	} finally {
		loading.value = false;
	}
};

const reset = () => {
	query.name = '';
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
const openCreate = () => {
	Object.assign(createForm, { name: '', code: '', version: 1, description: '' });
	createVisible.value = true;
};

const submitCreate = async () => {
	if (!(await createFormRef.value?.validate())) return;
	submitting.value = true;
	try {
		await createDefinition(createForm);
		useMessage().success('流程草稿已创建');
		createVisible.value = false;
		loadData();
	} finally {
		submitting.value = false;
	}
};

const showStructure = async (row: WorkflowDefinition) => {
	activeDefinition.value = row;
	structureVisible.value = true;
	const res = await getDefinitionNodes(row.id);
	nodes.value = res.data || [];
};

const publish = async (row: WorkflowDefinition) => {
	try {
		await useMessageBox().confirm(`确认发布流程“${row.name}”吗？发布后不能再修改当前版本。`);
	} catch {
		return;
	}
	await publishDefinition(row.id);
	useMessage().success('流程已发布');
	loadData();
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
.search-form {
	padding: 16px 16px 0;
	margin-bottom: 16px;
	background: var(--el-fill-color-light);
	border-radius: 8px;
}
.node-key {
	margin-left: 10px;
	color: var(--el-text-color-secondary);
	font-family: monospace;
}
.el-card p {
	margin: 10px 0 0;
	color: var(--el-text-color-secondary);
}
</style>
