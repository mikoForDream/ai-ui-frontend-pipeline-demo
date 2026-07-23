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
