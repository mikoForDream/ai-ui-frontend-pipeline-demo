<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view project-page">
			<div class="page-heading">
				<div>
					<h2>研发项目</h2>
					<p>从资料收集、功能点审核开始，逐步推进原型、UI 和全栈开发。</p>
				</div>
				<div class="heading-actions">
					<el-tag :type="aiModelStatus?.configured ? 'success' : 'danger'" effect="plain">
						{{ aiModelStatus?.configured ? `${aiModelStatus.model} 已配置` : 'AI 未配置' }}
					</el-tag>
					<el-button v-auth="'workflow_material_analyze'" icon="Connection" :loading="checkingAi" :disabled="!aiModelStatus?.configured" @click="checkAiConnectivity">检测连接</el-button>
					<el-button v-auth="'workflow_project_edit'" type="primary" icon="Plus" @click="openCreate">新建项目</el-button>
				</div>
			</div>
			<el-alert v-if="aiModelStatus && !aiModelStatus.configured" class="ai-config-alert" type="warning" :closable="false" show-icon
				title="AI 模型服务未配置，生成操作暂不可用。请在后端设置 OPENAI_API_KEY 后重启服务。" />

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
						<el-alert type="info" :closable="false" show-icon title="文本、Markdown、Word 和 Excel 会立即抽取内容；PDF、图片和演示文稿由已配置的 AI 模型解析。" />
						<el-table :data="workspace.materials" border class="data-table">
							<el-table-column prop="originalName" label="文件名" min-width="240" show-overflow-tooltip />
							<el-table-column prop="extension" label="类型" width="80" align="center"><template #default="{ row }">{{ row.extension.toUpperCase() }}</template></el-table-column>
							<el-table-column label="大小" width="100"><template #default="{ row }">{{ formatSize(row.fileSize) }}</template></el-table-column>
							<el-table-column label="解析状态" width="130"><template #default="{ row }"><el-tag :type="parseStatusType(row.parseStatus)">{{ parseStatusLabel(row.parseStatus) }}</el-tag></template></el-table-column>
							<el-table-column prop="parseMessage" label="处理结果" min-width="240" show-overflow-tooltip />
							<el-table-column label="操作" width="160" fixed="right" align="center">
								<template #default="{ row }">
									<el-button text type="primary" icon="Download" @click="downloadMaterial(row)">下载</el-button>
									<el-button v-if="['FAILED', 'READY_FOR_AI'].includes(row.parseStatus)" text type="warning" :disabled="!aiModelStatus?.configured" @click="reparse(row)">
										{{ row.parseStatus === 'READY_FOR_AI' ? 'AI 解析' : '重试' }}
									</el-button>
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
					<el-tab-pane :label="`原型 ${workspace.prototypes.length}`" name="prototypes">
						<div class="toolbar">
							<div><strong>模块原型审核</strong><span>按模块生成和确认交互流程</span></div>
							<el-tag :type="workspace.frozenSpecVersion ? 'success' : 'info'">需求规格 {{ workspace.frozenSpecVersion || '未冻结' }}</el-tag>
						</div>
						<el-empty v-if="!workspace.modules.length" description="功能点审核完成后可按模块生成原型" />
						<el-table v-else :data="workspace.modules" border>
							<el-table-column prop="moduleCode" label="模块编号" width="125" />
							<el-table-column prop="name" label="模块" min-width="180" />
							<el-table-column label="功能点" width="90" align="center"><template #default="{ row }">{{ moduleFeatures(row.id).length }}</template></el-table-column>
							<el-table-column label="原型版本" width="110"><template #default="{ row }">{{ modulePrototype(row.id)?.versionNo || '-' }}</template></el-table-column>
							<el-table-column label="审核状态" width="120"><template #default="{ row }"><el-tag :type="prototypeStatusType(modulePrototype(row.id)?.status)">{{ prototypeStatusLabel(modulePrototype(row.id)?.status) }}</el-tag></template></el-table-column>
							<el-table-column label="审核意见" min-width="220" show-overflow-tooltip><template #default="{ row }">{{ modulePrototype(row.id)?.reviewComment || '-' }}</template></el-table-column>
							<el-table-column label="操作" width="270" fixed="right" align="center">
								<template #default="{ row }">
									<el-button v-if="canGeneratePrototype(row.status)" v-auth="'workflow_prototype_generate'" text type="primary" :loading="prototypeBusyId === row.id" @click="generatePrototype(row)">{{ modulePrototype(row.id) ? '重新生成' : '生成原型' }}</el-button>
									<el-button v-if="modulePrototype(row.id)" text icon="View" @click="openPrototype(row.name, modulePrototype(row.id))">预览</el-button>
									<template v-if="modulePrototype(row.id)?.status === 'PENDING_REVIEW'">
										<el-button v-auth="'workflow_prototype_review'" text type="success" @click="reviewPrototype(row.name, modulePrototype(row.id), 'APPROVE')">通过</el-button>
										<el-button v-auth="'workflow_prototype_review'" text type="danger" @click="reviewPrototype(row.name, modulePrototype(row.id), 'REJECT')">驳回</el-button>
									</template>
								</template>
							</el-table-column>
						</el-table>
					</el-tab-pane>
					<el-tab-pane :label="`UI 设计 ${workspace.uiDesigns.length}`" name="ui-designs">
						<div class="toolbar">
							<div><strong>模块 UI 设计审核</strong><span>工作流生成设计草稿，也可以直接上传人工设计图</span></div>
							<el-tag :type="uiApprovedCount === workspace.modules.length && uiApprovedCount > 0 ? 'success' : 'warning'">已通过 {{ uiApprovedCount }} / {{ workspace.modules.length }}</el-tag>
						</div>
						<el-empty v-if="!workspace.modules.length" description="模块原型审核完成后可进入 UI 设计" />
						<el-table v-else :data="workspace.modules" border>
							<el-table-column prop="moduleCode" label="模块编号" width="125" />
							<el-table-column prop="name" label="模块" min-width="170" />
							<el-table-column label="版本" width="90"><template #default="{ row }">{{ moduleUiDesign(row.id)?.versionNo || '-' }}</template></el-table-column>
							<el-table-column label="来源" width="120"><template #default="{ row }">{{ uiSourceLabel(moduleUiDesign(row.id)?.sourceType) }}</template></el-table-column>
							<el-table-column label="审核状态" width="110"><template #default="{ row }"><el-tag :type="prototypeStatusType(moduleUiDesign(row.id)?.status)">{{ prototypeStatusLabel(moduleUiDesign(row.id)?.status) }}</el-tag></template></el-table-column>
							<el-table-column label="审核意见" min-width="180" show-overflow-tooltip><template #default="{ row }">{{ moduleUiDesign(row.id)?.reviewComment || '-' }}</template></el-table-column>
							<el-table-column label="操作" width="410" fixed="right" align="center">
								<template #default="{ row }">
									<el-button v-if="canGenerateUiDesign(row.status)" v-auth="'workflow_ui_generate'" text type="primary" icon="MagicStick" :loading="uiBusyId === row.id" @click="generateUiDesign(row)">{{ moduleUiDesign(row.id) ? '重新生成' : '生成草稿' }}</el-button>
									<el-upload v-if="canCreateUiDesign(row.status)" v-auth="'workflow_ui_upload'" class="inline-upload" :show-file-list="false" :http-request="(options) => uploadUiDesign(row, options)" accept=".png,.jpg,.jpeg,.webp">
										<el-button text type="primary" icon="Upload" :loading="uiBusyId === row.id">上传设计图</el-button>
									</el-upload>
									<el-button v-if="moduleUiDesign(row.id)" text icon="View" @click="openUiDesign(row.name, moduleUiDesign(row.id))">预览</el-button>
									<template v-if="moduleUiDesign(row.id)?.status === 'PENDING_REVIEW'">
										<el-button v-auth="'workflow_ui_review'" text type="success" @click="reviewUiDesign(row.name, moduleUiDesign(row.id), 'APPROVE')">通过</el-button>
										<el-button v-auth="'workflow_ui_review'" text type="danger" @click="reviewUiDesign(row.name, moduleUiDesign(row.id), 'REJECT')">驳回</el-button>
									</template>
								</template>
							</el-table-column>
						</el-table>
					</el-tab-pane>
					<el-tab-pane :label="`前端开发 ${workspace.frontendCodes.length}`" name="frontend-codes">
						<div class="toolbar">
							<div><strong>模块前端开发与代码审核</strong><span>基于已通过的 UI 设计生成可预览、可下载的 Vue 3 代码草稿</span></div>
							<el-tag :type="frontendApprovedCount === workspace.modules.length && frontendApprovedCount > 0 ? 'success' : 'warning'">已通过 {{ frontendApprovedCount }} / {{ workspace.modules.length }}</el-tag>
						</div>
						<el-empty v-if="!workspace.modules.length" description="模块 UI 设计审核完成后可进入前端开发" />
						<el-table v-else :data="workspace.modules" border>
							<el-table-column prop="moduleCode" label="模块编号" width="125" />
							<el-table-column prop="name" label="模块" min-width="150" />
							<el-table-column label="前端实现逻辑" min-width="240" show-overflow-tooltip><template #default="{ row }">{{ row.frontendLogic || '未补充，按功能点验收标准生成' }}</template></el-table-column>
							<el-table-column label="代码版本" width="100"><template #default="{ row }">{{ moduleFrontendCode(row.id)?.versionNo || '-' }}</template></el-table-column>
							<el-table-column label="文件" width="70" align="center"><template #default="{ row }">{{ moduleFrontendCode(row.id)?.fileCount ?? '-' }}</template></el-table-column>
							<el-table-column label="生成器" width="180"><template #default="{ row }">{{ moduleFrontendCode(row.id)?.generator || '-' }}</template></el-table-column>
							<el-table-column label="审核状态" width="110"><template #default="{ row }"><el-tag :type="prototypeStatusType(moduleFrontendCode(row.id)?.status)">{{ prototypeStatusLabel(moduleFrontendCode(row.id)?.status) }}</el-tag></template></el-table-column>
							<el-table-column label="审核意见" min-width="180" show-overflow-tooltip><template #default="{ row }">{{ moduleFrontendCode(row.id)?.reviewComment || '-' }}</template></el-table-column>
							<el-table-column label="操作" width="360" fixed="right" align="center">
								<template #default="{ row }">
									<el-button v-if="canDevelopFrontend(row.status)" v-auth="'workflow_frontend_edit'" text type="primary" icon="Edit" @click="editFrontendLogic(row)">实现逻辑</el-button>
									<el-button v-if="canGenerateFrontend(row.status)" v-auth="'workflow_frontend_generate'" text type="primary" icon="MagicStick" :loading="frontendBusyId === row.id" @click="generateFrontendCode(row)">{{ moduleFrontendCode(row.id) ? '重新生成' : '生成代码' }}</el-button>
									<el-button v-if="moduleFrontendCode(row.id)" text icon="View" @click="openFrontendCode(row.name, moduleFrontendCode(row.id))">查看</el-button>
									<el-button v-if="moduleFrontendCode(row.id)" text icon="Download" @click="downloadFrontendCode(row.name, moduleFrontendCode(row.id))">下载</el-button>
									<template v-if="moduleFrontendCode(row.id)?.status === 'PENDING_REVIEW'">
										<el-button v-auth="'workflow_frontend_review'" text type="success" @click="reviewFrontendCode(row.name, moduleFrontendCode(row.id), 'APPROVE')">通过</el-button>
										<el-button v-auth="'workflow_frontend_review'" text type="danger" @click="reviewFrontendCode(row.name, moduleFrontendCode(row.id), 'REJECT')">驳回</el-button>
									</template>
								</template>
							</el-table-column>
						</el-table>
					</el-tab-pane>
					<el-tab-pane :label="`后端开发 ${workspace.backendCodes.length}`" name="backend-codes">
						<div class="toolbar">
							<div><strong>模块后端开发与代码审核</strong><span>补充后端逻辑后生成 Java/Spring Boot 代码包</span></div>
							<el-tag :type="backendApprovedCount === workspace.modules.length && backendApprovedCount > 0 ? 'success' : 'warning'">已通过 {{ backendApprovedCount }} / {{ workspace.modules.length }}</el-tag>
						</div>
						<el-empty v-if="!workspace.modules.length" description="前端代码全部通过后可进入后端开发" />
						<el-table v-else :data="workspace.modules" border>
							<el-table-column prop="moduleCode" label="模块编号" width="125" />
							<el-table-column prop="name" label="模块" min-width="150" />
							<el-table-column label="后端实现逻辑" min-width="240" show-overflow-tooltip><template #default="{ row }">{{ row.backendLogic || '未补充，按功能点验收标准生成' }}</template></el-table-column>
							<el-table-column label="代码版本" width="100"><template #default="{ row }">{{ moduleBackendCode(row.id)?.versionNo || '-' }}</template></el-table-column>
							<el-table-column label="文件" width="70" align="center"><template #default="{ row }">{{ moduleBackendCode(row.id)?.fileCount ?? '-' }}</template></el-table-column>
							<el-table-column label="审核状态" width="110"><template #default="{ row }"><el-tag :type="prototypeStatusType(moduleBackendCode(row.id)?.status)">{{ prototypeStatusLabel(moduleBackendCode(row.id)?.status) }}</el-tag></template></el-table-column>
							<el-table-column label="接口摘要" min-width="220" show-overflow-tooltip><template #default="{ row }">{{ moduleBackendCode(row.id)?.apiSummary || '-' }}</template></el-table-column>
							<el-table-column label="操作" width="390" fixed="right" align="center">
								<template #default="{ row }">
									<el-button v-if="canDevelopBackend(row.status)" v-auth="'workflow_backend_edit'" text type="primary" icon="Edit" @click="editBackendLogic(row)">实现逻辑</el-button>
									<el-button v-if="canGenerateBackend(row.status)" v-auth="'workflow_backend_generate'" text type="primary" icon="MagicStick" :loading="backendBusyId === row.id" @click="generateBackendCode(row)">{{ moduleBackendCode(row.id) ? '重新生成' : '生成代码' }}</el-button>
									<el-button v-if="moduleBackendCode(row.id)" text icon="View" @click="openBackendCode(row.name, moduleBackendCode(row.id))">查看</el-button>
									<el-button v-if="moduleBackendCode(row.id)" text icon="Download" @click="downloadBackendCode(row.name, moduleBackendCode(row.id))">下载</el-button>
									<template v-if="moduleBackendCode(row.id)?.status === 'PENDING_REVIEW'">
										<el-button v-auth="'workflow_backend_review'" text type="success" @click="reviewBackendCode(row.name, moduleBackendCode(row.id), 'APPROVE')">通过</el-button>
										<el-button v-auth="'workflow_backend_review'" text type="danger" @click="reviewBackendCode(row.name, moduleBackendCode(row.id), 'REJECT')">驳回</el-button>
									</template>
								</template>
							</el-table-column>
						</el-table>
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

		<el-dialog v-model="prototypeVisible" :title="prototypeTitle" width="94%" top="3vh" append-to-body destroy-on-close>
			<div v-loading="prototypeLoading" class="prototype-preview">
				<iframe v-if="prototypeHtml" :srcdoc="prototypeHtml" sandbox="allow-scripts allow-forms" title="模块交互原型预览" />
			</div>
		</el-dialog>

		<el-dialog v-model="uiPreviewVisible" :title="uiPreviewTitle" width="94%" top="3vh" append-to-body destroy-on-close @closed="clearUiPreview">
			<div v-loading="uiPreviewLoading" class="ui-preview">
				<iframe v-if="uiPreviewHtml" :srcdoc="uiPreviewHtml" sandbox="allow-scripts allow-forms" title="模块 UI 设计预览" />
				<img v-else-if="uiPreviewImageUrl" :src="uiPreviewImageUrl" alt="用户上传的模块 UI 设计图" />
			</div>
		</el-dialog>

		<el-dialog v-model="frontendLogicVisible" title="模块前端实现逻辑" width="680px" append-to-body destroy-on-close>
			<el-form label-position="top">
				<el-form-item :label="frontendLogicModule?.name || '模块'">
					<el-input v-model="frontendLogicText" type="textarea" :rows="9" maxlength="5000" show-word-limit placeholder="补充页面状态、交互规则、校验、异常反馈等实现要求；留空时按功能点验收标准生成。" />
				</el-form-item>
			</el-form>
			<template #footer><el-button @click="frontendLogicVisible = false">取消</el-button><el-button type="primary" :loading="frontendBusyId === frontendLogicModule?.id" @click="saveFrontendLogic">保存</el-button></template>
		</el-dialog>

		<el-dialog v-model="frontendCodeVisible" :title="frontendCodeTitle" width="96%" top="2vh" append-to-body destroy-on-close>
			<div v-loading="frontendCodeLoading" class="frontend-code-review">
				<div class="code-summary">
					<el-tag effect="plain">{{ frontendCodeDetail?.generator }}</el-tag>
					<span>{{ frontendCodeDetail?.files.length || 0 }} 个文件</span>
					<span>UI 版本 ID：{{ frontendCodeDetail?.uiDesignVersionId }}</span>
				</div>
				<el-tabs v-if="frontendCodeDetail" v-model="frontendCodeTab" class="code-tabs">
					<el-tab-pane label="运行预览" name="preview"><iframe :srcdoc="frontendCodeDetail.previewHtml" sandbox="allow-scripts allow-forms" title="模块前端代码运行预览" /></el-tab-pane>
					<el-tab-pane label="代码文件" name="files">
						<div class="code-browser">
							<el-menu :default-active="selectedCodePath" @select="selectCodeFile">
								<el-menu-item v-for="file in frontendCodeDetail.files" :key="file.path" :index="file.path"><span>{{ file.path }}</span></el-menu-item>
							</el-menu>
							<div class="code-content"><div>{{ selectedCodeFile?.path }}</div><pre><code>{{ selectedCodeFile?.content }}</code></pre></div>
						</div>
					</el-tab-pane>
					<el-tab-pane label="实现逻辑" name="logic"><pre class="logic-content">{{ frontendCodeDetail.frontendLogic || '未补充' }}</pre></el-tab-pane>
				</el-tabs>
			</div>
		</el-dialog>

		<el-dialog v-model="backendLogicVisible" title="模块后端实现逻辑" width="680px" append-to-body destroy-on-close>
			<el-form label-position="top"><el-form-item :label="backendLogicModule?.name || '模块'"><el-input v-model="backendLogicText" type="textarea" :rows="10" maxlength="10000" show-word-limit placeholder="补充接口、权限、数据模型、事务、校验、异常和外部依赖等后端实现要求；留空时按功能点验收标准生成。" /></el-form-item></el-form>
			<template #footer><el-button @click="backendLogicVisible = false">取消</el-button><el-button type="primary" :loading="backendBusyId === backendLogicModule?.id" @click="saveBackendLogic">保存</el-button></template>
		</el-dialog>

		<el-dialog v-model="backendCodeVisible" :title="backendCodeTitle" width="96%" top="2vh" append-to-body destroy-on-close>
			<div v-loading="backendCodeLoading" class="frontend-code-review"><div class="code-summary"><el-tag effect="plain">{{ backendCodeDetail?.generator }}</el-tag><span>{{ backendCodeDetail?.files.length || 0 }} 个文件</span></div>
				<el-tabs v-if="backendCodeDetail" v-model="backendCodeTab" class="code-tabs"><el-tab-pane label="接口摘要" name="summary"><pre class="logic-content">{{ backendCodeDetail.apiSummary }}</pre></el-tab-pane><el-tab-pane label="代码文件" name="files"><div class="code-browser"><el-menu :default-active="backendSelectedCodePath" @select="(path) => backendSelectedCodePath = path"><el-menu-item v-for="file in backendCodeDetail.files" :key="file.path" :index="file.path"><span>{{ file.path }}</span></el-menu-item></el-menu><div class="code-content"><div>{{ backendSelectedCodeFile?.path }}</div><pre><code>{{ backendSelectedCodeFile?.content }}</code></pre></div></div></el-tab-pane><el-tab-pane label="实现逻辑" name="logic"><pre class="logic-content">{{ backendCodeDetail.backendLogic || '未补充' }}</pre></el-tab-pane></el-tabs>
			</div>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="workflowProject">
import type { FormInstance, FormRules, TagProps, UploadRequestOptions } from 'element-plus';
import {
	analyzeProjectMaterials,
	checkAiModelConnectivity,
	createProject,
	generateModulePrototype,
	generateModuleUiDesign,
	generateModuleFrontendCode,
	generateModuleBackendCode,
	getModuleFrontendCode,
	getModuleBackendCode,
	getModulePrototype,
	getModuleUiDesign,
	getModuleUiDesignContent,
	getAiModelStatus,
	getProjectPage,
	getProjectWorkspace,
	parseProjectMaterial,
	reviewProjectFeature,
	reviewModulePrototype,
	reviewModuleUiDesign,
	reviewModuleFrontendCode,
	saveFrontendDevelopmentSpec,
	saveBackendDevelopmentSpec,
	updateProjectFeature,
	uploadModuleUiDesign,
	uploadProjectMaterial,
	reviewModuleBackendCode,
	type WorkflowFeature,
	type WorkflowMaterial,
	type ModulePrototype,
	type ModuleUiDesign,
	type ModuleFrontendCode,
	type ModuleFrontendCodeDetail,
	type ModuleBackendCode,
	type ModuleBackendCodeDetail,
	type GeneratedCodeFile,
	type AiModelStatus,
	type WorkflowProject,
	type WorkflowProjectWorkspace,
} from '/@/api/workflow';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { downBlobFile } from '/@/utils/other';

const acceptedFiles = '.txt,.md,.csv,.json,.yaml,.yml,.doc,.docx,.xls,.xlsx,.pdf,.png,.jpg,.jpeg,.ppt,.pptx';
const stageOptions = [
	['MATERIAL_COLLECTION', '资料收集'], ['FEATURE_REVIEW', '功能点审核'], ['PROTOTYPE_READY', '原型准备'], ['PROTOTYPE_REVIEW', '原型审核'], ['UI_READY', 'UI 准备'], ['UI_DESIGN', 'UI 设计'],
	['UI_REVIEW', 'UI 审核'], ['FRONTEND_READY', '前端开发准备'], ['FRONTEND_DEVELOPMENT', '前端开发'], ['FRONTEND_REVIEW', '前端代码审核'], ['BACKEND_READY', '后端开发准备'], ['BACKEND_DEVELOPMENT', '后端开发'], ['BACKEND_REVIEW', '后端代码审核'], ['INTEGRATION', '联调测试'], ['DELIVERY', '交付'],
] as const;
const loading = ref(false);
const submitting = ref(false);
const uploading = ref(false);
const analyzing = ref(false);
const workspaceLoading = ref(false);
const checkingAi = ref(false);
const aiModelStatus = ref<AiModelStatus>();
const rows = ref<WorkflowProject[]>([]);
const query = reactive({ name: '', status: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });
const createVisible = ref(false);
const workspaceVisible = ref(false);
const featureEditVisible = ref(false);
const prototypeVisible = ref(false);
const prototypeLoading = ref(false);
const prototypeBusyId = ref('');
const prototypeHtml = ref('');
const prototypeTitle = ref('模块交互原型');
const uiBusyId = ref('');
const uiPreviewVisible = ref(false);
const uiPreviewLoading = ref(false);
const uiPreviewHtml = ref('');
const uiPreviewImageUrl = ref('');
const uiPreviewTitle = ref('模块 UI 设计');
const frontendBusyId = ref('');
const frontendLogicVisible = ref(false);
const frontendLogicModule = ref<WorkflowProjectWorkspace['modules'][number]>();
const frontendLogicText = ref('');
const frontendCodeVisible = ref(false);
const frontendCodeLoading = ref(false);
const frontendCodeTitle = ref('模块前端代码');
const frontendCodeDetail = ref<ModuleFrontendCodeDetail>();
const frontendCodeTab = ref('preview');
const selectedCodePath = ref('');
const backendBusyId = ref('');
const backendLogicVisible = ref(false);
const backendLogicModule = ref<WorkflowProjectWorkspace['modules'][number]>();
const backendLogicText = ref('');
const backendCodeVisible = ref(false);
const backendCodeLoading = ref(false);
const backendCodeTitle = ref('模块后端代码');
const backendCodeDetail = ref<ModuleBackendCodeDetail>();
const backendCodeTab = ref('summary');
const backendSelectedCodePath = ref('');
const activeTab = ref('materials');
const createFormRef = ref<FormInstance>();
const featureFormRef = ref<FormInstance>();
const createForm = reactive({ name: '', projectCode: '', techStack: 'Vue 3 + Spring Boot + MySQL', description: '' });
const featureForm = reactive<Partial<WorkflowFeature>>({});
const workspace = reactive<WorkflowProjectWorkspace>({ project: {} as WorkflowProject, materials: [], modules: [], features: [], prototypes: [], uiDesigns: [], frontendCodes: [], backendCodes: [] });
const createRules: FormRules = {
	name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
	projectCode: [{ required: true, message: '请输入项目编码', trigger: 'blur' }],
};
const featureRules: FormRules = { name: [{ required: true, message: '请输入功能点名称', trigger: 'blur' }] };
const stageStep: Record<string, number> = { MATERIAL_COLLECTION: 0, FEATURE_REVIEW: 1, PROTOTYPE_READY: 2, PROTOTYPE_REVIEW: 2, UI_READY: 3, UI_DESIGN: 3, UI_REVIEW: 3, FRONTEND_READY: 4, FRONTEND_DEVELOPMENT: 4, FRONTEND_REVIEW: 4, BACKEND_READY: 5, BACKEND_DEVELOPMENT: 5, BACKEND_REVIEW: 5, INTEGRATION: 6, DELIVERY: 7 };
const activeStage = computed(() => stageStep[workspace.project?.currentStage] ?? 0);
const approvedCount = computed(() => workspace.features.filter((item) => item.status === 'APPROVED').length);
const uiApprovedCount = computed(() => workspace.uiDesigns.filter((item) => item.status === 'APPROVED').length);
const frontendApprovedCount = computed(() => workspace.frontendCodes.filter((item) => item.status === 'APPROVED').length);
const backendApprovedCount = computed(() => workspace.backendCodes.filter((item) => item.status === 'APPROVED').length);
const selectedCodeFile = computed<GeneratedCodeFile | undefined>(() => frontendCodeDetail.value?.files.find((file) => file.path === selectedCodePath.value));
const backendSelectedCodeFile = computed<GeneratedCodeFile | undefined>(() => backendCodeDetail.value?.files.find((file) => file.path === backendSelectedCodePath.value));
const canAnalyze = computed(() => Boolean(aiModelStatus.value?.configured) && workspace.materials.some((item) => item.parseStatus === 'PARSED') && workspace.features.length === 0);

const stageLabel = (value?: string) => stageOptions.find(([key]) => key === value)?.[1] || value || '资料收集';
const parseStatusLabel = (value: string) => ({ UPLOADED: '已上传', PARSED: '已解析', READY_FOR_AI: '等待 AI', FAILED: '解析失败' })[value] || value;
const parseStatusType = (value: string): TagProps['type'] => ({ PARSED: 'success', READY_FOR_AI: 'warning', FAILED: 'danger' })[value] as TagProps['type'] || 'info';
const featureStatusLabel = (value: string) => ({ DRAFT: '待修改', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已驳回' })[value] || value;
const featureStatusType = (value: string): TagProps['type'] => ({ APPROVED: 'success', REJECTED: 'danger', DRAFT: 'warning' })[value] as TagProps['type'] || 'info';
const priorityLabel = (value: string) => ({ HIGH: '高', MEDIUM: '中', LOW: '低' })[value] || value;
const formatSize = (size: number) => size < 1024 * 1024 ? `${(size / 1024).toFixed(1)} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`;
const moduleFeatures = (moduleId: string) => workspace.features.filter((item) => item.moduleId === moduleId);
const modulePrototype = (moduleId: string) => workspace.prototypes.find((item) => item.moduleId === moduleId);
const prototypeStatusLabel = (value?: string) => ({ PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已驳回', RETURNED: '已退回' })[value || ''] || '未生成';
const prototypeStatusType = (value?: string): TagProps['type'] => ({ APPROVED: 'success', REJECTED: 'danger', RETURNED: 'warning' })[value || ''] as TagProps['type'] || 'info';
const canGeneratePrototype = (status: string) => Boolean(aiModelStatus.value?.configured) && ['REQUIREMENT_APPROVED', 'PROTOTYPE_REVISION'].includes(status);
const moduleUiDesign = (moduleId: string) => workspace.uiDesigns.find((item) => item.moduleId === moduleId);
const uiSourceLabel = (value?: string) => ({ AI_RESPONSES_V1: 'AI 生成', RULE_BASED_UI_V1: '旧版规则生成', USER_UPLOAD: '人工上传' })[value || ''] || '-';
const canCreateUiDesign = (status: string) => ['PROTOTYPE_APPROVED', 'UI_REVISION'].includes(status);
const canGenerateUiDesign = (status: string) => Boolean(aiModelStatus.value?.configured) && canCreateUiDesign(status);
const moduleFrontendCode = (moduleId: string) => workspace.frontendCodes.find((item) => item.moduleId === moduleId);
const canDevelopFrontend = (status: string) => ['UI_APPROVED', 'FRONTEND_REVISION'].includes(status);
const canGenerateFrontend = (status: string) => Boolean(aiModelStatus.value?.configured) && canDevelopFrontend(status);
const moduleBackendCode = (moduleId: string) => workspace.backendCodes.find((item) => item.moduleId === moduleId);
const canDevelopBackend = (status: string) => ['FRONTEND_APPROVED', 'BACKEND_READY', 'BACKEND_REVISION'].includes(status);
const canGenerateBackend = (status: string) => Boolean(aiModelStatus.value?.configured) && canDevelopBackend(status);

const loadAiModelStatus = async () => { aiModelStatus.value = (await getAiModelStatus()).data; };
const checkAiConnectivity = async () => {
	checkingAi.value = true;
	try {
		aiModelStatus.value = (await checkAiModelConnectivity()).data;
		useMessage().success(`AI 模型 ${aiModelStatus.value?.model} 连接正常`);
	} finally { checkingAi.value = false; }
};

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
const generatePrototype = async (module: WorkflowProjectWorkspace['modules'][number]) => {
	try { await useMessageBox().confirm(`确认根据已冻结功能点生成“${module.name}”的交互原型新版本吗？`); } catch { return; }
	prototypeBusyId.value = module.id;
	try {
		await generateModulePrototype(module.id);
		useMessage().success('模块原型已生成并提交审核');
		await loadWorkspace(); await loadData();
	} finally { prototypeBusyId.value = ''; }
};
const openPrototype = async (moduleName: string, prototype?: ModulePrototype) => {
	if (!prototype) return;
	prototypeVisible.value = true; prototypeLoading.value = true; prototypeHtml.value = '';
	prototypeTitle.value = `${moduleName} · ${prototype.versionNo}`;
	try { prototypeHtml.value = (await getModulePrototype(prototype.versionId)).data.html; }
	finally { prototypeLoading.value = false; }
};
const reviewPrototype = async (moduleName: string, prototype: ModulePrototype | undefined, action: 'APPROVE' | 'REJECT') => {
	if (!prototype) return;
	let comment = '';
	if (action === 'REJECT') {
		try { const result = await useMessageBox().prompt(`填写“${moduleName}”原型的修改意见`, '驳回原型', { inputValidator: (value) => Boolean(value?.trim()) || '必须填写修改意见' }); comment = result.value; }
		catch { return; }
	} else {
		try { await useMessageBox().confirm(`确认通过“${moduleName}”的 ${prototype.versionNo} 原型吗？`); } catch { return; }
	}
	prototypeBusyId.value = prototype.moduleId;
	try {
		await reviewModulePrototype(prototype.versionId, { action, comment });
		useMessage().success(action === 'APPROVE' ? '模块原型已通过' : '模块原型已驳回');
		await loadWorkspace(); await loadData();
	} finally { prototypeBusyId.value = ''; }
};

const generateUiDesign = async (module: WorkflowProjectWorkspace['modules'][number]) => {
	try { await useMessageBox().confirm(`确认根据已通过原型生成“${module.name}”的 UI 设计新版本吗？`); } catch { return; }
	uiBusyId.value = module.id;
	try {
		await generateModuleUiDesign(module.id);
		useMessage().success('UI 设计草稿已生成并提交审核');
		await loadWorkspace(); await loadData();
	} finally { uiBusyId.value = ''; }
};
const uploadUiDesign = async (module: WorkflowProjectWorkspace['modules'][number], options: UploadRequestOptions) => {
	uiBusyId.value = module.id;
	try {
		const formData = new FormData(); formData.append('file', options.file);
		await uploadModuleUiDesign(module.id, formData);
		options.onSuccess({}); useMessage().success('设计图已上传并提交审核');
		await loadWorkspace(); await loadData();
	} catch (error) { options.onError(error as Error); }
	finally { uiBusyId.value = ''; }
};
const clearUiPreview = () => {
	if (uiPreviewImageUrl.value) URL.revokeObjectURL(uiPreviewImageUrl.value);
	uiPreviewImageUrl.value = '';
	uiPreviewHtml.value = '';
};
const openUiDesign = async (moduleName: string, design?: ModuleUiDesign) => {
	if (!design) return;
	clearUiPreview();
	uiPreviewVisible.value = true; uiPreviewLoading.value = true;
	uiPreviewTitle.value = `${moduleName} · ${design.versionNo} · ${uiSourceLabel(design.sourceType)}`;
	try {
		const detail = (await getModuleUiDesign(design.versionId)).data;
		if (detail.contentKind === 'HTML') uiPreviewHtml.value = detail.html || '';
		else uiPreviewImageUrl.value = URL.createObjectURL(await getModuleUiDesignContent(design.versionId));
	} finally { uiPreviewLoading.value = false; }
};
const reviewUiDesign = async (moduleName: string, design: ModuleUiDesign | undefined, action: 'APPROVE' | 'REJECT') => {
	if (!design) return;
	let comment = '';
	if (action === 'REJECT') {
		try { const result = await useMessageBox().prompt(`填写“${moduleName}”UI 设计的修改意见`, '驳回 UI 设计', { inputValidator: (value) => Boolean(value?.trim()) || '必须填写修改意见' }); comment = result.value; }
		catch { return; }
	} else {
		try { await useMessageBox().confirm(`确认通过“${moduleName}”的 ${design.versionNo} UI 设计吗？`); } catch { return; }
	}
	uiBusyId.value = design.moduleId;
	try {
		await reviewModuleUiDesign(design.versionId, { action, comment });
		useMessage().success(action === 'APPROVE' ? '模块 UI 设计已通过' : '模块 UI 设计已驳回');
		await loadWorkspace(); await loadData();
	} finally { uiBusyId.value = ''; }
};

const editFrontendLogic = (module: WorkflowProjectWorkspace['modules'][number]) => {
	frontendLogicModule.value = module;
	frontendLogicText.value = module.frontendLogic || '';
	frontendLogicVisible.value = true;
};
const saveFrontendLogic = async () => {
	const module = frontendLogicModule.value;
	if (!module) return;
	frontendBusyId.value = module.id;
	try {
		await saveFrontendDevelopmentSpec(module.id, { logic: frontendLogicText.value });
		frontendLogicVisible.value = false;
		useMessage().success('模块前端实现逻辑已保存');
		await loadWorkspace();
	} finally { frontendBusyId.value = ''; }
};
const generateFrontendCode = async (module: WorkflowProjectWorkspace['modules'][number]) => {
	try { await useMessageBox().confirm(`确认根据已通过 UI 设计生成“${module.name}”的前端代码新版本吗？`); } catch { return; }
	frontendBusyId.value = module.id;
	try {
		await generateModuleFrontendCode(module.id);
		useMessage().success('前端代码已生成并提交审核');
		await loadWorkspace(); await loadData();
	} finally { frontendBusyId.value = ''; }
};
const selectCodeFile = (path: string) => { selectedCodePath.value = path; };
const openFrontendCode = async (moduleName: string, code?: ModuleFrontendCode) => {
	if (!code) return;
	frontendCodeVisible.value = true; frontendCodeLoading.value = true; frontendCodeDetail.value = undefined;
	frontendCodeTitle.value = `${moduleName} · ${code.versionNo} · 前端代码审核`;
	frontendCodeTab.value = 'preview'; selectedCodePath.value = '';
	try {
		frontendCodeDetail.value = (await getModuleFrontendCode(code.versionId)).data;
		selectedCodePath.value = frontendCodeDetail.value.files[0]?.path || '';
	} finally { frontendCodeLoading.value = false; }
};
const downloadFrontendCode = (moduleName: string, code?: ModuleFrontendCode) => {
	if (!code) return;
	downBlobFile(`/admin/workflow/frontend-codes/${code.versionId}/download`, {}, `${moduleName}-${code.versionNo}-frontend.zip`);
};
const reviewFrontendCode = async (moduleName: string, code: ModuleFrontendCode | undefined, action: 'APPROVE' | 'REJECT') => {
	if (!code) return;
	let comment = '';
	if (action === 'REJECT') {
		try { const result = await useMessageBox().prompt(`填写“${moduleName}”前端代码的修改意见`, '驳回前端代码', { inputValidator: (value) => Boolean(value?.trim()) || '必须填写修改意见' }); comment = result.value; }
		catch { return; }
	} else {
		try { await useMessageBox().confirm(`确认通过“${moduleName}”的 ${code.versionNo} 前端代码吗？`); } catch { return; }
	}
	frontendBusyId.value = code.moduleId;
	try {
		await reviewModuleFrontendCode(code.versionId, { action, comment });
		useMessage().success(action === 'APPROVE' ? '模块前端代码已通过' : '模块前端代码已驳回');
		await loadWorkspace(); await loadData();
	} finally { frontendBusyId.value = ''; }
};

const editBackendLogic = (module: WorkflowProjectWorkspace['modules'][number]) => {
	backendLogicModule.value = module;
	backendLogicText.value = module.backendLogic || '';
	backendLogicVisible.value = true;
};
const saveBackendLogic = async () => {
	const module = backendLogicModule.value;
	if (!module) return;
	backendBusyId.value = module.id;
	try {
		await saveBackendDevelopmentSpec(module.id, { logic: backendLogicText.value });
		backendLogicVisible.value = false;
		useMessage().success('模块后端实现逻辑已保存');
		await loadWorkspace();
	} finally { backendBusyId.value = ''; }
};
const generateBackendCode = async (module: WorkflowProjectWorkspace['modules'][number]) => {
	try { await useMessageBox().confirm(`确认根据“${module.name}”的功能点生成后端代码新版本吗？`); } catch { return; }
	backendBusyId.value = module.id;
	try {
		await generateModuleBackendCode(module.id);
		useMessage().success('后端代码已生成并提交审核');
		await loadWorkspace(); await loadData();
	} finally { backendBusyId.value = ''; }
};
const openBackendCode = async (moduleName: string, code?: ModuleBackendCode) => {
	if (!code) return;
	backendCodeVisible.value = true; backendCodeLoading.value = true; backendCodeDetail.value = undefined;
	backendCodeTitle.value = `${moduleName} · ${code.versionNo} · 后端代码审核`;
	backendCodeTab.value = 'summary'; backendSelectedCodePath.value = '';
	try {
		backendCodeDetail.value = (await getModuleBackendCode(code.versionId)).data;
		backendSelectedCodePath.value = backendCodeDetail.value.files[0]?.path || '';
	} finally { backendCodeLoading.value = false; }
};
const downloadBackendCode = (moduleName: string, code?: ModuleBackendCode) => {
	if (!code) return;
	downBlobFile(`/admin/workflow/backend-codes/${code.versionId}/download`, {}, `${moduleName}-${code.versionNo}-backend.zip`);
};
const reviewBackendCode = async (moduleName: string, code: ModuleBackendCode | undefined, action: 'APPROVE' | 'REJECT') => {
	if (!code) return;
	let comment = '';
	if (action === 'REJECT') {
		try { const result = await useMessageBox().prompt(`填写“${moduleName}”后端代码的修改意见`, '驳回后端代码', { inputValidator: (value) => Boolean(value?.trim()) || '必须填写修改意见' }); comment = result.value; }
		catch { return; }
	} else {
		try { await useMessageBox().confirm(`确认通过“${moduleName}”的 ${code.versionNo} 后端代码吗？`); } catch { return; }
	}
	backendBusyId.value = code.moduleId;
	try {
		await reviewModuleBackendCode(code.versionId, { action, comment });
		useMessage().success(action === 'APPROVE' ? '模块后端代码已通过' : '模块后端代码已驳回');
		await loadWorkspace(); await loadData();
	} finally { backendBusyId.value = ''; }
};

onMounted(() => { loadData(); loadAiModelStatus(); });
onBeforeUnmount(clearUiPreview);
</script>

<style scoped>
.project-page { padding: 20px; }
.page-heading, .workspace-header, .toolbar, .module-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.page-heading { margin-bottom: 20px; }
.heading-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ai-config-alert { margin-bottom: 16px; }
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
.prototype-preview { height: calc(94vh - 120px); min-height: 520px; background: var(--el-fill-color-light); }
.prototype-preview iframe { width: 100%; height: 100%; border: 1px solid var(--el-border-color); background: #fff; }
.inline-upload { display: inline-flex; vertical-align: middle; }
.ui-preview { display: flex; align-items: flex-start; justify-content: center; height: calc(94vh - 120px); min-height: 520px; overflow: auto; background: var(--el-fill-color-light); }
.ui-preview iframe { width: 100%; height: 100%; border: 1px solid var(--el-border-color); background: #fff; }
.ui-preview img { display: block; max-width: 100%; height: auto; background: #fff; box-shadow: 0 0 0 1px var(--el-border-color); }
.frontend-code-review { min-height: calc(94vh - 145px); }
.code-summary { display: flex; align-items: center; gap: 14px; min-height: 36px; color: var(--el-text-color-secondary); }
.code-tabs iframe { width: 100%; height: calc(94vh - 230px); min-height: 500px; border: 1px solid var(--el-border-color); background: #fff; }
.code-browser { display: grid; grid-template-columns: minmax(260px, 32%) 1fr; height: calc(94vh - 230px); min-height: 500px; border: 1px solid var(--el-border-color); }
.code-browser .el-menu { overflow: auto; border-right: 1px solid var(--el-border-color); }
.code-browser .el-menu-item { min-width: max-content; }
.code-content { min-width: 0; overflow: auto; background: #111827; color: #e5e7eb; }
.code-content > div { position: sticky; top: 0; padding: 10px 16px; background: #1f2937; color: #cbd5e1; }
.code-content pre { padding: 2px 16px 18px; margin: 0; font: 13px/1.65 Consolas, 'Courier New', monospace; white-space: pre; }
.logic-content { min-height: 240px; padding: 18px; margin: 0; border: 1px solid var(--el-border-color); background: var(--el-fill-color-light); font: 14px/1.7 Consolas, 'Microsoft YaHei', sans-serif; white-space: pre-wrap; }
@media (max-width: 900px) {
	.page-heading, .toolbar, .workspace-header { flex-direction: column; }
	.toolbar-actions { width: 100%; flex-wrap: wrap; }
	.stage-band { overflow-x: auto; }
	.stage-band :deep(.el-steps) { min-width: 760px; }
	.code-browser { grid-template-columns: 1fr; height: auto; }
	.code-browser .el-menu { max-height: 220px; border-right: 0; border-bottom: 1px solid var(--el-border-color); }
	.code-content { min-height: 420px; }
}
</style>
