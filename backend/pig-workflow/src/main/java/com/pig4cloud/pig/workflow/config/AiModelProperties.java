package com.pig4cloud.pig.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;

/** Backend-only AI model service configuration. */
@Component
@ConfigurationProperties(prefix = "workflow.ai")
public class AiModelProperties {

	private String provider = "OPENAI";
	private String baseUrl = "https://api.openai.com/v1";
	private String model = "gpt-5.6";
	private String apiKey;
	private String reasoningEffort = "medium";
	private int maxOutputTokens = 32000;
	private int maxInputChars = 200000;
	private int maxAttempts = 3;
	private Duration connectTimeout = Duration.ofSeconds(10);
	private Duration readTimeout = Duration.ofMinutes(3);

	public boolean isConfigured() {
		return StringUtils.hasText(apiKey);
	}

	public URI responsesEndpoint() {
		return URI.create(normalizedBaseUrl() + "/responses");
	}

	public String normalizedBaseUrl() {
		if (!StringUtils.hasText(baseUrl)) {
			throw new IllegalStateException("workflow.ai.base-url 不能为空");
		}
		String normalized = baseUrl.trim().replaceAll("/+$", "");
		try {
			URI uri = URI.create(normalized);
			if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
					|| uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
					|| uri.getFragment() != null) {
				throw new IllegalArgumentException();
			}
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("workflow.ai.base-url 必须是合法且不含凭据的 HTTP(S) 服务地址");
		}
		return normalized;
	}

	/** Deliberately not exposed as a JavaBean getter. */
	public String credential() {
		return apiKey;
	}

	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }
	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
	public String getModel() { return model; }
	public void setModel(String model) { this.model = model; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }
	public String getReasoningEffort() { return reasoningEffort; }
	public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
	public int getMaxOutputTokens() { return maxOutputTokens; }
	public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
	public int getMaxInputChars() { return maxInputChars; }
	public void setMaxInputChars(int maxInputChars) { this.maxInputChars = maxInputChars; }
	public int getMaxAttempts() { return maxAttempts; }
	public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
	public Duration getConnectTimeout() { return connectTimeout; }
	public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
	public Duration getReadTimeout() { return readTimeout; }
	public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
}
