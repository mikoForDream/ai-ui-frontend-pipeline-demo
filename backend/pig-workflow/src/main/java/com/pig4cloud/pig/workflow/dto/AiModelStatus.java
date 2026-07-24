package com.pig4cloud.pig.workflow.dto;

public record AiModelStatus(boolean configured, String provider, String model, String baseUrl,
		String apiKeyEnvironmentVariable) {
}
