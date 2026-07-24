package com.pig4cloud.pig.workflow.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.config.AiModelProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class OpenAiResponsesGatewayTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void sendsBackendCredentialAndParsesStructuredResponse() {
		AiModelProperties properties = properties("test-secret");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(header("Authorization", "Bearer test-secret"))
				.andExpect(jsonPath("$.model").value("gpt-5.6"))
				.andExpect(jsonPath("$.store").value(false))
				.andExpect(jsonPath("$.text.format.type").value("json_schema"))
				.andRespond(withSuccess("""
						{"id":"resp_123","status":"completed","model":"gpt-5.6-sol","output":[{"type":"message","content":[{"type":"output_text","text":"{\\"ok\\":true}"}]}],"usage":{"input_tokens":12,"output_tokens":4,"total_tokens":16}}
						""", MediaType.APPLICATION_JSON));
		OpenAiResponsesGateway gateway = new OpenAiResponsesGateway(properties, objectMapper, builder);

		AiModelGateway.AiGeneration result = gateway.generateStructured("test", "instructions", "input",
				objectMapper.createObjectNode().put("type", "object"));

		assertTrue(result.content().path("ok").asBoolean());
		assertEquals("resp_123", result.responseId());
		assertEquals(16, result.usage().totalTokens());
		assertTrue(gateway.status().configured());
		assertFalse(gateway.status().toString().contains("test-secret"));
		server.verify();
	}

	@Test
	void failsClearlyWhenCredentialIsMissing() {
		OpenAiResponsesGateway gateway = new OpenAiResponsesGateway(properties(""), objectMapper, RestClient.builder());
		CheckedException exception = assertThrows(CheckedException.class, () -> gateway.generateStructured("test",
				"instructions", "input", objectMapper.createObjectNode().put("type", "object")));
		assertTrue(exception.getMessage().contains("OPENAI_API_KEY"));
	}

	@Test
	void sendsPdfAsBase64FileInput() {
		AiModelProperties properties = properties("test-secret");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(jsonPath("$.input[0].content[0].type").value("input_file"))
				.andExpect(jsonPath("$.input[0].content[0].filename").value("requirements.pdf"))
				.andExpect(jsonPath("$.input[0].content[0].file_data")
						.value("data:application/pdf;base64,AQID"))
				.andExpect(jsonPath("$.input[0].content[0].detail").value("auto"))
				.andRespond(withSuccess("""
						{"id":"resp_file","status":"completed","model":"gpt-5.6-sol","output":[{"type":"message","content":[{"type":"output_text","text":"{\\"content\\":\\"ok\\"}"}]}],"usage":{"input_tokens":20,"output_tokens":4,"total_tokens":24}}
						""", MediaType.APPLICATION_JSON));
		OpenAiResponsesGateway gateway = new OpenAiResponsesGateway(properties, objectMapper, builder);

		AiModelGateway.AiGeneration result = gateway.generateStructured("file_test", "instructions", "input",
				List.of(), List.of(new AiModelGateway.AiFile("requirements.pdf", "application/pdf",
						new byte[] { 1, 2, 3 })), objectMapper.createObjectNode().put("type", "object"));

		assertEquals("ok", result.content().path("content").asText());
		server.verify();
	}

	@Test
	void doesNotExposeProviderErrorBody() {
		AiModelProperties properties = properties("test-secret");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
						.body("{\"error\":{\"message\":\"reflected test-secret\"}}"));
		OpenAiResponsesGateway gateway = new OpenAiResponsesGateway(properties, objectMapper, builder);

		CheckedException exception = assertThrows(CheckedException.class, () -> gateway.generateStructured("test",
				"instructions", "input", objectMapper.createObjectNode().put("type", "object")));

		assertTrue(exception.getMessage().contains("HTTP 401"));
		assertFalse(exception.getMessage().contains("test-secret"));
		server.verify();
	}

	@Test
	void rejectsBaseUrlContainingCredentialsBeforeStatusDisclosure() {
		AiModelProperties properties = properties("test-secret");
		properties.setBaseUrl("https://user:password@example.com/v1");
		OpenAiResponsesGateway gateway = new OpenAiResponsesGateway(properties, objectMapper, RestClient.builder());

		IllegalStateException exception = assertThrows(IllegalStateException.class, gateway::status);

		assertFalse(exception.getMessage().contains("password"));
	}

	private AiModelProperties properties(String key) {
		AiModelProperties properties = new AiModelProperties();
		properties.setApiKey(key);
		return properties;
	}
}
