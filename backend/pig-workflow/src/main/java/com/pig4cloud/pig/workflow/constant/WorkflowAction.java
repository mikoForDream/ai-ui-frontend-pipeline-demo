package com.pig4cloud.pig.workflow.constant;

import java.util.Set;

/**
 * 支持的任务动作。
 */
public final class WorkflowAction {

	public static final String COMPLETE = "COMPLETE";
	public static final String APPROVE = "APPROVE";
	public static final String REJECT = "REJECT";
	public static final String RETURN = "RETURN";
	public static final String RETRY = "RETRY";
	public static final String CANCEL = "CANCEL";
	public static final String SKIP = "SKIP";
	public static final String FAIL = "FAIL";

	private static final Set<String> SUPPORTED = Set.of(COMPLETE, APPROVE, REJECT, RETURN, RETRY, CANCEL, SKIP,
			FAIL);

	public static boolean isSupported(String action) {
		return SUPPORTED.contains(action);
	}

	private WorkflowAction() {
	}

}
