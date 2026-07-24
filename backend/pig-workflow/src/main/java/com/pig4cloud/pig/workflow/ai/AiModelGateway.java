package com.pig4cloud.pig.workflow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.pig4cloud.pig.workflow.dto.AiModelStatus;

import java.util.List;

public interface AiModelGateway {

	AiModelStatus status();

	AiGeneration generateStructured(String operation, String instructions, String input, JsonNode schema);

	AiGeneration generateStructured(String operation, String instructions, String input, List<AiImage> images,
			JsonNode schema);

	AiGeneration generateStructured(String operation, String instructions, String input, List<AiImage> images,
			List<AiFile> files, JsonNode schema);

	AiModelStatus checkConnectivity();

	record AiImage(String mediaType, byte[] bytes) { }

	record AiFile(String filename, String mediaType, byte[] bytes) { }

	record AiUsage(long inputTokens, long outputTokens, long totalTokens) { }

	record AiGeneration(JsonNode content, String responseId, String model, AiUsage usage) { }
}
