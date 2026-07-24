package com.pig4cloud.pig.workflow.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.config.AiModelProperties;
import com.pig4cloud.pig.workflow.dto.AiModelStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiResponsesGateway implements AiModelGateway {

	private static final String API_KEY_ENV = "OPENAI_API_KEY";
	private final AiModelProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	@Autowired
	public OpenAiResponsesGateway(AiModelProperties properties, ObjectMapper objectMapper) {
		this(properties, objectMapper, configuredBuilder(properties));
	}

	OpenAiResponsesGateway(AiModelProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.restClient = builder.build();
	}

	private static RestClient.Builder configuredBuilder(AiModelProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.getReadTimeout());
		return RestClient.builder().requestFactory(requestFactory);
	}

	@Override
	public AiModelStatus status() {
		return new AiModelStatus(properties.isConfigured(), properties.getProvider(), properties.getModel(),
				properties.normalizedBaseUrl(), API_KEY_ENV);
	}

	@Override
	public AiModelStatus checkConnectivity() {
		JsonNode okSchema = objectMapper.createObjectNode().put("type", "boolean")
				.set("enum", objectMapper.createArrayNode().add(true));
		JsonNode schema = objectMapper.createObjectNode().put("type", "object")
				.set("properties", objectMapper.createObjectNode().set("ok", okSchema));
		((com.fasterxml.jackson.databind.node.ObjectNode) schema).set("required", objectMapper.createArrayNode().add("ok"));
		((com.fasterxml.jackson.databind.node.ObjectNode) schema).put("additionalProperties", false);
		generateStructured("connectivity_check", "Return the requested health result.",
				"Return {\"ok\":true} to confirm this model endpoint can generate a structured response.", schema);
		return status();
	}

	@Override
	public AiGeneration generateStructured(String operation, String instructions, String input, JsonNode schema) {
		return generateStructured(operation, instructions, input, List.of(), schema);
	}

	@Override
	public AiGeneration generateStructured(String operation, String instructions, String input, List<AiImage> images,
			JsonNode schema) {
		return generateStructured(operation, instructions, input, images, List.of(), schema);
	}

	@Override
	public AiGeneration generateStructured(String operation, String instructions, String input, List<AiImage> images,
			List<AiFile> files, JsonNode schema) {
		requireConfigured();
		Map<String, Object> payload = requestPayload(operation, instructions, boundedInput(input), images, files, schema);
		long startedAt = System.nanoTime();
		int attempts = Math.max(1, properties.getMaxAttempts());
		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				JsonNode response = restClient.post().uri(properties.responsesEndpoint())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.credential())
						.contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(JsonNode.class);
				AiGeneration result = parseResponse(response);
				long durationMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
				log.info("AI generation completed operation={} responseId={} model={} inputTokens={} outputTokens={} durationMs={}",
						operation, result.responseId(), result.model(), result.usage().inputTokens(),
						result.usage().outputTokens(), durationMs);
				return result;
			}
			catch (HttpStatusCodeException exception) {
				boolean retryable = exception.getStatusCode().value() == 429 || exception.getStatusCode().is5xxServerError();
				log.warn("AI request failed operation={} status={} attempt={} retryable={}", operation,
						exception.getStatusCode().value(), attempt, retryable);
				if (!retryable || attempt == attempts) {
					throw apiFailure(exception);
				}
				backoff(attempt);
			}
			catch (ResourceAccessException exception) {
				log.warn("AI request transport failure operation={} attempt={}", operation, attempt);
				if (attempt == attempts) {
					throw new CheckedException("AI 模型服务连接超时或不可达");
				}
				backoff(attempt);
			}
		}
		throw new CheckedException("AI 模型服务调用失败");
	}

	private Map<String, Object> requestPayload(String operation, String instructions, String input,
			List<AiImage> images, List<AiFile> files, JsonNode schema) {
		Map<String, Object> format = new LinkedHashMap<>();
		format.put("type", "json_schema");
		format.put("name", schemaName(operation));
		format.put("strict", true);
		format.put("schema", schema);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("model", properties.getModel());
		payload.put("instructions", instructions);
		payload.put("input", responseInput(input, images, files));
		payload.put("store", false);
		payload.put("max_output_tokens", properties.getMaxOutputTokens());
		payload.put("reasoning", Map.of("effort", properties.getReasoningEffort()));
		payload.put("text", Map.of("format", format));
		return payload;
	}

	private Object responseInput(String input, List<AiImage> images, List<AiFile> files) {
		if ((images == null || images.isEmpty()) && (files == null || files.isEmpty())) return input;
		List<Map<String, Object>> content = new ArrayList<>();
		if (files != null) {
			for (AiFile file : files) {
				Map<String, Object> part = new LinkedHashMap<>();
				part.put("type", "input_file");
				part.put("filename", file.filename());
				part.put("file_data", "data:" + file.mediaType() + ";base64,"
						+ Base64.getEncoder().encodeToString(file.bytes()));
				if ("application/pdf".equals(file.mediaType())) part.put("detail", "auto");
				content.add(part);
			}
		}
		for (AiImage image : images) {
			String dataUrl = "data:" + image.mediaType() + ";base64," + Base64.getEncoder().encodeToString(image.bytes());
			content.add(Map.of("type", "input_image", "image_url", dataUrl, "detail", "high"));
		}
		content.add(Map.of("type", "input_text", "text", input));
		return List.of(Map.of("role", "user", "content", content));
	}

	private AiGeneration parseResponse(JsonNode response) {
		if (response == null) throw new CheckedException("AI 模型服务返回了空响应");
		String responseId = response.path("id").asText();
		String status = response.path("status").asText();
		if (!"completed".equals(status)) {
			String message = response.path("error").path("message").asText("响应状态为 " + status);
			throw new CheckedException("AI 模型生成未完成: " + message);
		}
		String outputText = null;
		for (JsonNode item : response.path("output")) {
			if (!"message".equals(item.path("type").asText())) continue;
			for (JsonNode part : item.path("content")) {
				if ("refusal".equals(part.path("type").asText())) {
					throw new CheckedException("AI 模型拒绝了本次生成请求");
				}
				if ("output_text".equals(part.path("type").asText())) outputText = part.path("text").asText();
			}
		}
		if (!StringUtils.hasText(outputText)) throw new CheckedException("AI 模型未返回结构化内容");
		try {
			JsonNode usage = response.path("usage");
			return new AiGeneration(objectMapper.readTree(outputText), responseId, response.path("model").asText(),
					new AiUsage(usage.path("input_tokens").asLong(), usage.path("output_tokens").asLong(),
							usage.path("total_tokens").asLong()));
		}
		catch (JsonProcessingException exception) {
			throw new CheckedException("AI 模型返回的结构化内容无法解析");
		}
	}

	private CheckedException apiFailure(HttpStatusCodeException exception) {
		// Compatible providers may reflect request data in their error text. Only expose the status code.
		return new CheckedException("AI 模型服务调用失败（HTTP " + exception.getStatusCode().value() + "）");
	}

	private void requireConfigured() {
		if (!properties.isConfigured()) {
			throw new CheckedException("AI 模型服务未配置，请在后端设置 OPENAI_API_KEY 并重启服务");
		}
		if (!StringUtils.hasText(properties.getModel())) throw new CheckedException("AI 模型名称未配置");
	}

	private String boundedInput(String input) {
		String value = input == null ? "" : input;
		int limit = Math.max(1000, properties.getMaxInputChars());
		if (value.length() <= limit) return value;
		throw new CheckedException("AI 输入内容超过 " + limit + " 个字符，请精简资料或调整 OPENAI_MAX_INPUT_CHARS");
	}

	private String schemaName(String operation) {
		String name = operation == null ? "workflow_result" : operation.replaceAll("[^A-Za-z0-9_-]", "_");
		return name.length() > 64 ? name.substring(0, 64) : name;
	}

	private void backoff(int attempt) {
		try {
			Thread.sleep(Math.min(2000L, 250L * (1L << Math.min(attempt - 1, 3))));
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new CheckedException("AI 模型服务调用已中断");
		}
	}
}
