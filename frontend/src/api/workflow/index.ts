import request from '/@/utils/request';

export interface PageQuery {
	current?: number;
	size?: number;
	[key: string]: unknown;
}

export interface WorkflowDefinition {
	id: string;
	projectId?: string;
	code: string;
	name: string;
	version: number;
	status: 'DRAFT' | 'PUBLISHED';
	description?: string;
	createTime?: string;
}

export interface WorkflowNodeDefinition {
	id: string;
	definitionId: string;
	nodeKey: string;
	nodeName: string;
	nodeType: 'AI' | 'SERVICE' | 'MANUAL_REVIEW' | 'DELIVERY';
	sortOrder: number;
	startNode: boolean;
	endNode: boolean;
	nextNodeKey?: string;
	configJson?: string;
}

export interface WorkflowTransition {
	id: string;
	definitionId: string;
	sourceNodeKey: string;
	targetNodeKey?: string;
	action: 'COMPLETE' | 'APPROVE' | 'REJECT' | 'RETURN' | 'RETRY' | 'CANCEL' | 'SKIP' | 'FAIL';
	conditionExpression?: string;
	priority: number;
	defaultTransition: boolean;
}

export interface WorkflowInstance {
	id: string;
	definitionId: string;
	businessKey: string;
	title: string;
	status: string;
	currentNodeKey?: string;
	startedBy?: string;
	startedAt?: string;
	finishedAt?: string;
	createTime?: string;
}

export interface WorkflowApproval {
	id: string;
	instanceId: string;
	taskId: string;
	approvalType: string;
	status: 'PENDING' | 'CLAIMED' | 'DECIDED';
	decision?: 'APPROVE' | 'REJECT' | 'RETURN';
	reviewerName?: string;
	comment?: string;
	reviewedAt?: string;
	createTime?: string;
}

export interface WorkflowProject {
	id: string;
	projectCode: string;
	name: string;
	description?: string;
	status: 'ACTIVE' | 'ARCHIVED';
	currentStage: string;
	repositoryUrl?: string;
	defaultBranch?: string;
	frontendPath?: string;
	backendPath?: string;
	techStack?: string;
	createTime?: string;
}

export interface WorkflowMaterial {
	id: string;
	originalName: string;
	contentType?: string;
	extension: string;
	fileSize: number;
	checksum: string;
	parseStatus: 'UPLOADED' | 'PARSED' | 'READY_FOR_AI' | 'FAILED';
	extractedLength: number;
	parseMessage?: string;
	createTime?: string;
}

export interface WorkflowModule {
	id: string;
	projectId: string;
	moduleCode: string;
	name: string;
	description?: string;
	sortOrder: number;
	status: 'REQUIREMENT_REVIEW' | 'REQUIREMENT_APPROVED' | 'PROTOTYPE_REVIEW' | 'PROTOTYPE_REVISION' | 'PROTOTYPE_APPROVED';
}

export interface ModulePrototype {
	artifactId: string;
	moduleId: string;
	versionId: string;
	versionNo: string;
	status: 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'RETURNED';
	generator: string;
	reviewComment?: string;
	createTime?: string;
}

export interface ModulePrototypeDetail extends ModulePrototype {
	html: string;
}

export interface WorkflowFeature {
	id: string;
	projectId: string;
	moduleId: string;
	featureCode: string;
	name: string;
	description?: string;
	acceptanceCriteria?: string;
	priority: 'HIGH' | 'MEDIUM' | 'LOW';
	status: 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED';
	reviewComment?: string;
	version: number;
}

export interface WorkflowProjectWorkspace {
	project: WorkflowProject;
	materials: WorkflowMaterial[];
	modules: WorkflowModule[];
	features: WorkflowFeature[];
	prototypes: ModulePrototype[];
	frozenSpecVersion?: string;
}

export const getDefinitionPage = (params: PageQuery) => request({ url: '/admin/workflow/definitions/page', method: 'get', params });

export const getDefinition = (id: string) => request({ url: `/admin/workflow/definitions/${id}`, method: 'get' });

export const getDefinitionNodes = (id: string) => request({ url: `/admin/workflow/definitions/${id}/nodes`, method: 'get' });

export const saveDefinitionNode = (definitionId: string, data: Partial<WorkflowNodeDefinition>) =>
	request({ url: `/admin/workflow/definitions/${definitionId}/nodes`, method: 'post', data });

export const deleteDefinitionNode = (nodeId: string) => request({ url: `/admin/workflow/definitions/nodes/${nodeId}`, method: 'delete' });

export const getDefinitionTransitions = (id: string) => request({ url: `/admin/workflow/definitions/${id}/transitions`, method: 'get' });

export const saveDefinitionTransition = (definitionId: string, data: Partial<WorkflowTransition>) =>
	request({ url: `/admin/workflow/definitions/${definitionId}/transitions`, method: 'post', data });

export const deleteDefinitionTransition = (transitionId: string) =>
	request({ url: `/admin/workflow/definitions/transitions/${transitionId}`, method: 'delete' });

export const createDefinition = (data: Partial<WorkflowDefinition>) => request({ url: '/admin/workflow/definitions', method: 'post', data });

export const publishDefinition = (id: string) => request({ url: `/admin/workflow/definitions/${id}/publish`, method: 'post' });

export const getInstancePage = (params: PageQuery) => request({ url: '/admin/workflow/instances/page', method: 'get', params });

export const getInstanceDetail = (id: string) => request({ url: `/admin/workflow/instances/${id}`, method: 'get' });

export const startInstance = (data: { definitionId: string; businessKey: string; title: string; inputJson?: string }) =>
	request({ url: '/admin/workflow/instances/start', method: 'post', data });

export const getApprovalPage = (params: PageQuery) => request({ url: '/admin/workflow/approvals/page', method: 'get', params });

export const claimApproval = (id: string) => request({ url: `/admin/workflow/approvals/${id}/claim`, method: 'post' });

export const decideApproval = (
	id: string,
	data: { decision: 'APPROVE' | 'REJECT' | 'RETURN'; operationKey: string; comment?: string; outputJson?: string }
) => request({ url: `/admin/workflow/approvals/${id}/decisions`, method: 'post', data });

export const getProjectPage = (params: PageQuery) => request({ url: '/admin/workflow/projects/page', method: 'get', params });

export const getProjectWorkspace = (id: string) => request({ url: `/admin/workflow/projects/${id}/workspace`, method: 'get' });

export const createProject = (data: Partial<WorkflowProject>) => request({ url: '/admin/workflow/projects', method: 'post', data });

export const updateProject = (id: string, data: Partial<WorkflowProject>) =>
	request({ url: `/admin/workflow/projects/${id}`, method: 'put', data });

export const uploadProjectMaterial = (projectId: string, data: FormData) =>
	request({
		url: `/admin/workflow/projects/${projectId}/materials`,
		method: 'post',
		data,
		headers: { 'Content-Type': 'multipart/form-data' },
		timeout: 120000,
	});

export const parseProjectMaterial = (id: string) => request({ url: `/admin/workflow/materials/${id}/parse`, method: 'post' });

export const analyzeProjectMaterials = (projectId: string) =>
	request({ url: `/admin/workflow/projects/${projectId}/analysis`, method: 'post', timeout: 120000 });

export const updateProjectFeature = (id: string, data: Partial<WorkflowFeature>) =>
	request({ url: `/admin/workflow/features/${id}`, method: 'put', data });

export const reviewProjectFeature = (id: string, data: { action: 'APPROVE' | 'REJECT' | 'RETURN'; comment?: string }) =>
	request({ url: `/admin/workflow/features/${id}/reviews`, method: 'post', data });

export const generateModulePrototype = (moduleId: string) =>
	request({ url: `/admin/workflow/modules/${moduleId}/prototypes`, method: 'post', timeout: 120000 });

export const getModulePrototype = (versionId: string) =>
	request({ url: `/admin/workflow/prototypes/${versionId}`, method: 'get' });

export const reviewModulePrototype = (versionId: string, data: { action: 'APPROVE' | 'REJECT' | 'RETURN'; comment?: string }) =>
	request({ url: `/admin/workflow/prototypes/${versionId}/reviews`, method: 'post', data });
