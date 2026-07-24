<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view project-page">
			<div class="page-heading">
				<div>
					<h2>研发项目</h2>
					<p>从资料收集、功能点审核开始，逐步推进原型、UI 和全栈开发。</p>
				</div>
				<el-button v-auth="'workflow_project_edit'" type="primary" icon="Plus" @click="openCreate">新建项目</el-button>
			</div>

			<el-form :inline="true" :model="query" class="search-form">
				<el-form-item label="项目名称"><el-input v-model="query.name" clearable placeholder="输入名称" @keyup.enter="loadData" /></el-form-item>
				<el-form-item label="状态">
					<el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px">
						<el-option label="进行中" value="ACTIVE" /><el-option label="已归档" value="ARCHIVED" />
					</el-select>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" icon="Search" @click="loadData">查询</el-button>
					<el-button icon="Refresh" @click="reset">重置</el-button>
				</el-form-item>
			</el-form>

			<el-table v-loading="loading" :data="rows" border>
				<el-table-column prop="name" label="项目名称" min-width="180" />
				<el-table-column prop="projectCode" label="项目编码" min-width="150" />
				<el-table-column label="当前阶段" width="150">
					<template #default="{ row }"><el-tag effect="plain">{{ stageLabel(row.currentStage) }}</el-tag></template>
				</el-table-column>
				<el-table-column prop="techStack" label="技术栈" min-width="190" show-overflow-tooltip />
				<el-table-column prop="description" label="项目说明" min-width="220" show-overflow-tooltip />
				<el-table-column prop="createTime" label="创建时间" width="170" />
				<el-table-column label="操作" width="110" fixed="right" align="center">
					<template #default="{ row }"><el-button text type="primary" @click="openWorkspace(row)">进入项目</el-button></template>
				</el-table-column>
			</el-table>
			<pagination v-bind="pagination" @current-change="changePage" @size-change="changeSize" />
		</div>

		<el-dialog v-model="createVisible" title="新建研发项目" width="620px" destroy-on-close>
			<el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
				<el-row :gutter="16">
					<el-col :span="12"><el-form-item label="项目名称" prop="name"><el-input v-model="createForm.name" maxlength="128" /></el-form-item></el-col>
					<el-col :span="12"><el-form-item label="项目编码" prop="projectCode"><el-input v-model="createForm.projectCode" placeholder="例如 CRM_V1" maxlength="64" /></el-form-item></el-col>
				</el-row>
				<el-form-item label="技术栈"><el-input v-model="createForm.techStack" placeholder="例如 Vue 3 + Spring Boot + MySQL" /></el-form-item>
				<el-form-item label="项目说明"><el-input v-model="createForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="createVisible = false">取消</el-button>
				<el-button type="primary" :loading="submitting" @click="submitCreate">创建并进入</el-button>
			</template>
		</el-dialog>

		<el-dialog v-model="workspaceVisible" fullscreen destroy-on-close :close-on-click-modal="false" class="project-workspace-dialog">
			<template #header>
				<div class="workspace-header">
					<div>
						<h2>{{ workspace.project?.name }}</h2>
						<p>{{ workspace.project?.projectCode }} · {{ stageLabel(workspace.project?.currentStage) }}</p>
					</div>
					<el-button icon="Refresh" :loading="workspaceLoading" @click="loadWorkspace">刷新</el-button>
				</div>
			</template>

			<div v-loading="workspaceLoading" class="workspace-shell">
				<div class="stage-band">
					<el-steps :active="activeStage" align-center finish-status="success">
						<el-step title="资料" /><el-step title="功能点" /><el-step title="原型" /><el-step title="UI" />
						<el-step title="前端" /><el-step title="后端" /><el-step title="联调" /><el-step title="交付" />
					</el-steps>
				</div>

				<el-tabs v-model="activeTab" class="workspace-tabs">
					<el-tab-pane label="项目资料" name="materials">
						<div class="toolbar">
							<div><strong>需求资料</strong><span>{{ workspace.materials.length }} 个文件</span></div>
							<div class="toolbar-actions">
								<el-upload v-auth="'workflow_material_upload'" :show-file-list="false" :http-request="uploadMaterial" :accept="acceptedFiles">
									<el-button icon="Upload" :loading="uploading">上传资料</el-button>
								</el-upload>
								<el-button v-auth="'workflow_material_analyze'" type="primary" icon="MagicStick" :loading="analyzing" :disabled="!canAnalyze" @click="analyzeMaterials">分析并生成功能点</el-button>
							</div>
						</div>
						<el-alert type="info" :closable="false" show-icon title="文本、Markdown、Word 和 Excel 会立即抽取内容；PDF、图片和演示文稿先保存，等待 AI 解析器处理。" />
						<el-table :data="workspace.materials" border class="data-table">
							<el-table-column prop="originalName" label="文件名" min-width="240" show-overflow-tooltip />
							<el-table-column prop="extension" label="类型" width="80" align="center"><template #default="{ row }">{{ row.extension.toUpperCase() }}</template></el-table-column>
							<el-table-column label="大小" width="100"><template #default="{ row }">{{ formatSize(row.fileSize) }}</template></el-table-column>
							<el-table-column label="解析状态" width="130"><template #default="{ row }"><el-tag :type="parseStatusType(row.parseStatus)">{{ parseStatusLabel(row.parseStatus) }}</el-tag></template></el-table-column>
							<el-table-column prop="parseMessage" label="处理结果" min-width="240" show-overflow-tooltip />
							<el-table-column label="操作" width="160" fixed="right" align="center">
								<template #default="{ row }">
									<el-button text type="primary" icon="Download" @click="downloadMaterial(row)">下载</el-button>
									<el-button v-if="row.parseStatus === 'FAILED'" text type="warning" @click="reparse(row)">重试</el-button>
								</template>
							</el-table-column>
						</el-table>
					</el-tab-pane>

					<el-tab-pane :label="`功能点 ${workspace.features.length}`" name="features">
						<div class="toolbar">
							<div><strong>功能点审核</strong><span>逐条确认后才进入原型设计</span></div>
							<el-tag :type="approvedCount === workspace.features.length && approvedCount > 0 ? 'success' : 'warning'">已通过 {{ approvedCount }} / {{ workspace.features.length }}</el-tag>
						</div>
						<el-empty v-if="!workspace.features.length" description="上传资料并执行分析后，这里会生成待审核功能点" />
						<div v-for="module in workspace.modules" v-else :key="module.id" class="module-section">
							<div class="module-heading">
								<div><h3>{{ module.name }}</h3><span>{{ module.moduleCode }}</span></div>
								<el-tag :type="module.status === 'REQUIREMENT_APPROVED' ? 'success' : 'warning'">{{ module.status === 'REQUIREMENT_APPROVED' ? '需求已确认' : '审核中' }}</el-tag>
							</div>
							<el-table :data="moduleFeatures(module.id)" border>
								<el-table-column prop="featureCode" label="编号" width="105" />
								<el-table-column prop="name" label="功能点" min-width="200" />
								<el-table-column prop="acceptanceCriteria" label="验收标准" min-width="280" show-overflow-tooltip />
								<el-table-column label="优先级" width="90"><template #default="{ row }">{{ priorityLabel(row.priority) }}</template></el-table-column>
								<el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="featureStatusType(row.status)">{{ featureStatusLabel(row.status) }}</el-tag></template></el-table-column>
								<el-table-column label="操作" width="210" fixed="right" align="center">
									<template #default="{ row }">
										<el-button v-if="row.status !== 'APPROVED'" v-auth="'workflow_feature_edit'" text type="primary" @click="openFeatureEdit(row)">编辑</el-button>
										<el-button v-if="row.status !== 'APPROVED'" v-auth="'workflow_feature_review'" text type="success" @click="reviewFeature(row, 'APPROVE')">通过</el-button>
										<el-button v-if="row.status !== 'APPROVED'" v-auth="'workflow_feature_review'" text type="danger" @click="reviewFeature(row, 'REJECT')">驳回</el-button>
									</template>
								</el-table-column>
							</el-table>
						</div>
					</el-tab-pane>
				</el-tabs>
			</div>
		</el-dialog>

		<el-dialog v-model="featureEditVisible" title="编辑功能点" width="640px" append-to-body destroy-on-close>
			<el-form ref="featureFormRef" :model="featureForm" :rules="featureRules" label-width="90px">
				<el-form-item label="功能名称" prop="name"><el-input v-model="featureForm.name" maxlength="200" /></el-form-item>
				<el-form-item label="功能说明"><el-input v-model="featureForm.description" type="textarea" :rows="3" /></el-form-item>
				<el-form-item label="验收标准"><el-input v-model="featureForm.acceptanceCriteria" type="textarea" :rows="4" /></el-form-item>
				<el-form-item label="优先级"><el-select v-model="featureForm.priority"><el-option label="高" value="HIGH" /><el-option label="中" value="MEDIUM" /><el-option label="低" value="LOW" /></el-select></el-form-item>
			</el-form>
			<template #footer><el-button @click="featureEditVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitFeatureEdit">保存并提交审核</el-button></template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="workflowProject">
import type { FormInstance, FormRules, TagProps, UploadRequestOptions } from 'element-plus';
import {
	analyzeProjectMaterials,
	createProject,
	getProjectPage,
	getProjectWorkspace,
	parseProjectMaterial,
	reviewProjectFeature,
	updateProjectFeature,
	uploadProjectMaterial,
	type WorkflowFeature,
	type WorkflowMaterial,
	type WorkflowProject,
	type WorkflowProjectWorkspace,
} from '/@/api/workflow';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { downBlobFile } from '/@/utils/other';

const acceptedFiles = '.txt,.md,.csv,.json,.yaml,.yml,.doc,.docx,.xls,.xlsx,.pdf,.png,.jpg,.jpeg,.ppt,.pptx';
const stageOptions = [
	['MATERIAL_COLLECTION', '资料收集'], ['FEATURE_REVIEW', '功能点审核'], ['PROTOTYPE_READY', '原型准备'], ['UI_DESIGN', 'UI 设计'],
	['FRONTEND_DEVELOPMENT', '前端开发'], ['BACKEND_DEVELOPMENT', '后端开发'], ['INTEGRATION', '联调测试'], ['DELIVERY', '交付'],
] as const;
const loading = ref(false);
const submitting = ref(false);
const uploading = ref(false);
const analyzing = ref(false);
const workspaceLoading = ref(false);
const rows = ref<WorkflowProject[]>([]);
const query = reactive({ name: '', status: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });
const createVisible = ref(false);
const workspaceVisible = ref(false);
const featureEditVisible = ref(false);
const activeTab = ref('materials');
const createFormRef = ref<FormInstance>();
const featureFormRef = ref<FormInstance>();
const createForm = reactive({ name: '', projectCode: '', techStack: 'Vue 3 + Spring Boot + MySQL', description: '' });
const featureForm = reactive<Partial<WorkflowFeature>>({});
const workspace = reactive<WorkflowProjectWorkspace>({ project: {} as WorkflowProject, materials: [], modules: [], features: [] });
const createRules: FormRules = {
	name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
	projectCode: [{ required: true, message: '请输入项目编码', trigger: 'blur' }],
};
const featureRules: FormRules = { name: [{ required: true, message: '请输入功能点名称', trigger: 'blur' }] };
const activeStage = computed(() => Math.max(0, stageOptions.findIndex(([value]) => value === workspace.project?.currentStage)));
const approvedCount = computed(() => workspace.features.filter((item) => item.status === 'APPROVED').length);
const canAnalyze = computed(() => workspace.materials.some((item) => item.parseStatus === 'PARSED') && workspace.features.length === 0);

const stageLabel = (value?: string) => stageOptions.find(([key]) => key === value)?.[1] || value || '资料收集';
const parseStatusLabel = (value: string) => ({ UPLOADED: '已上传', PARSED: '已解析', READY_FOR_AI: '等待 AI', FAILED: '解析失败' })[value] || value;
const parseStatusType = (value: string): TagProps['type'] => ({ PARSED: 'success', READY_FOR_AI: 'warning', FAILED: 'danger' })[value] as TagProps['type'] || 'info';
const featureStatusLabel = (value: string) => ({ DRAFT: '待修改', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已驳回' })[value] || value;
const featureStatusType = (value: string): TagProps['type'] => ({ APPROVED: 'success', REJECTED: 'danger', DRAFT: 'warning' })[value] as TagProps['type'] || 'info';
const priorityLabel = (value: string) => ({ HIGH: '高', MEDIUM: '中', LOW: '低' })[value] || value;
const formatSize = (size: number) => size < 1024 * 1024 ? `${(size / 1024).toFixed(1)} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`;
const moduleFeatures = (moduleId: string) => workspace.features.filter((item) => item.moduleId === moduleId);

const loadData = async () => {
	loading.value = true;
	try {
		const result = await getProjectPage({ ...query, current: pagination.current, size: pagination.size });
		rows.value = result.data.records;
		pagination.total = result.data.total;
	} finally { loading.value = false; }
};
const reset = () => { Object.assign(query, { name: '', status: '' }); pagination.current = 1; loadData(); };
const changePage = (value: number) => { pagination.current = value; loadData(); };
const changeSize = (value: number) => { pagination.size = value; pagination.current = 1; loadData(); };
const openCreate = () => { Object.assign(createForm, { name: '', projectCode: '', techStack: 'Vue 3 + Spring Boot + MySQL', description: '' }); createVisible.value = true; };
const submitCreate = async () => {
	if (!(await createFormRef.value?.validate())) return;
	submitting.value = true;
	try {
		const result = await createProject(createForm);
		createVisible.value = false;
		useMessage().success('研发项目已创建');
		await loadData();
		await openWorkspace(result.data);
	} finally { submitting.value = false; }
};
const openWorkspace = async (project: WorkflowProject) => { workspace.project = project; workspaceVisible.value = true; activeTab.value = 'materials'; await loadWorkspace(); };
const loadWorkspace = async () => {
	if (!workspace.project?.id) return;
	workspaceLoading.value = true;
	try { Object.assign(workspace, (await getProjectWorkspace(workspace.project.id)).data); }
	finally { workspaceLoading.value = false; }
};
const uploadMaterial = async (options: UploadRequestOptions) => {
	uploading.value = true;
	try {
		const formData = new FormData(); formData.append('file', options.file);
		await uploadProjectMaterial(workspace.project.id, formData);
		options.onSuccess({}); useMessage().success('资料已上传并进入解析流程'); await loadWorkspace();
	} catch (error) { options.onError(error as Error); }
	finally { uploading.value = false; }
};
const reparse = async (material: WorkflowMaterial) => { await parseProjectMaterial(material.id); useMessage().success('资料已重新解析'); await loadWorkspace(); };
const downloadMaterial = (material: WorkflowMaterial) => downBlobFile(`/admin/workflow/materials/${material.id}/download`, {}, material.originalName);
const analyzeMaterials = async () => {
	try { await useMessageBox().confirm('确认根据当前已解析资料生成模块和功能点草稿吗？生成结果必须逐条审核。'); } catch { return; }
	analyzing.value = true;
	try {
		const result = await analyzeProjectMaterials(workspace.project.id);
		useMessage().success(`已生成 ${result.data.moduleCount} 个模块、${result.data.featureCount} 个功能点`);
		activeTab.value = 'features'; await loadWorkspace(); await loadData();
	} finally { analyzing.value = false; }
};
const openFeatureEdit = (feature: WorkflowFeature) => { Object.assign(featureForm, feature); featureEditVisible.value = true; };
const submitFeatureEdit = async () => {
	if (!(await featureFormRef.value?.validate()) || !featureForm.id) return;
	submitting.value = true;
	try { await updateProjectFeature(featureForm.id, featureForm); featureEditVisible.value = false; useMessage().success('功能点已更新并重新提交审核'); await loadWorkspace(); }
	finally { submitting.value = false; }
};
const reviewFeature = async (feature: WorkflowFeature, action: 'APPROVE' | 'REJECT') => {
	let comment = '';
	if (action === 'REJECT') {
		try { const result = await useMessageBox().prompt(`填写“${feature.name}”的驳回意见`, '驳回功能点', { inputValidator: (value) => Boolean(value?.trim()) || '必须填写驳回意见' }); comment = result.value; }
		catch { return; }
	} else {
		try { await useMessageBox().confirm(`确认通过功能点“${feature.name}”吗？`); } catch { return; }
	}
	await reviewProjectFeature(feature.id, { action, comment }); useMessage().success(action === 'APPROVE' ? '功能点已通过' : '功能点已驳回'); await loadWorkspace(); await loadData();
};

onMounted(loadData);
</script>

<style scoped>
.project-page { padding: 20px; }
.page-heading, .workspace-header, .toolbar, .module-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.page-heading { margin-bottom: 20px; }
.page-heading h2, .workspace-header h2, .module-heading h3 { margin: 0 0 6px; }
.page-heading p, .workspace-header p, .toolbar span, .module-heading span { margin: 0; color: var(--el-text-color-secondary); }
.search-form { padding: 16px 16px 0; margin-bottom: 16px; border-radius: 8px; background: var(--el-fill-color-light); }
.workspace-header { padding-right: 28px; }
.workspace-shell { min-height: calc(100vh - 90px); padding-bottom: 24px; }
.stage-band { padding: 20px 24px; margin-bottom: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 8px; background: var(--el-fill-color-lighter); }
.workspace-tabs { padding: 0 8px; }
.toolbar { align-items: center; min-height: 54px; margin-bottom: 14px; }
.toolbar > div:first-child { display: flex; flex-direction: column; gap: 5px; }
.toolbar-actions { display: flex; align-items: center; gap: 8px; }
.data-table { margin-top: 14px; }
.module-section { margin-bottom: 22px; }
.module-heading { align-items: center; padding: 12px 14px; border-left: 4px solid var(--el-color-primary); background: var(--el-fill-color-light); }
.module-heading h3 { font-size: 16px; }
@media (max-width: 900px) {
	.page-heading, .toolbar, .workspace-header { flex-direction: column; }
	.toolbar-actions { width: 100%; flex-wrap: wrap; }
	.stage-band { overflow-x: auto; }
	.stage-band :deep(.el-steps) { min-width: 760px; }
}
</style>
