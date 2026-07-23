package com.pig4cloud.pig.workflow.constant;

/**
 * 工作流状态常量。
 */
public final class WorkflowStatus {

	public static final String DEFINITION_DRAFT = "DRAFT";

	public static final String DEFINITION_PUBLISHED = "PUBLISHED";

	public static final String INSTANCE_RUNNING = "RUNNING";

	public static final String INSTANCE_COMPLETED = "COMPLETED";

	public static final String INSTANCE_FAILED = "FAILED";

	public static final String INSTANCE_CANCELLED = "CANCELLED";

	public static final String TASK_READY = "READY";

	public static final String TASK_RUNNING = "RUNNING";

	public static final String TASK_COMPLETED = "COMPLETED";

	public static final String TASK_FAILED = "FAILED";

	public static final String TASK_REJECTED = "REJECTED";

	public static final String TASK_RETURNED = "RETURNED";

	public static final String TASK_SKIPPED = "SKIPPED";

	public static final String TASK_CANCELLED = "CANCELLED";

	private WorkflowStatus() {
	}

}
