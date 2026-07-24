package com.pig4cloud.pig.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.ai.AiModelGateway;
import com.pig4cloud.pig.workflow.dto.GeneratedCodeFile;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Domain prompts and validation layered on the shared model gateway. */
@Service
@RequiredArgsConstructor
public class WorkflowAiGenerationService {

	public static final String GENERATOR = "AI_RESPONSES_V1";
	private static final int MAX_HTML_LENGTH = 2_000_000;
	private static final int MAX_CODE_LENGTH = 3_000_000;
	private static final Pattern EXTERNAL_HTML = Pattern.compile(
			"(?is)<(?:iframe|object|embed)\\b|\\b(?:src|href)\\s*=\\s*['\"]?(?:https?:)?//|"
					+ "(?:@import|url\\s*\\()\\s*['\"]?(?:https?:)?//|javascript\\s*:");

	private final AiModelGateway gateway;
	private final ObjectMapper objectMapper;

	public boolean isConfigured() {
		return gateway.status().configured();
	}

	public String extractMaterial(String filename, String extension, String mediaType, byte[] bytes) {
		List<AiModelGateway.AiImage> images = isImage(extension)
				? List.of(new AiModelGateway.AiImage(normalizeMediaType(mediaType, extension), bytes)) : List.of();
		List<AiModelGateway.AiFile> files = isImage(extension) ? List.of()
				: List.of(new AiModelGateway.AiFile(filename, normalizeMediaType(mediaType, extension), bytes));
		JsonNode result = gateway.generateStructured("material_extraction",
				"你是需求资料解析器。完整提取附件中与软件需求相关的正文、标题、列表、表格、流程、图示含义和约束。"
						+ "保持原始语义与层级，不补充附件中不存在的事实；无法辨认的内容明确标注。content 使用便于后续产品分析的 Markdown 文本。",
				"解析附件“" + filename + "”，输出完整的结构化文本内容。", images, files,
				objectSchema(Map.of("content", stringSchema()))).content();
		return requiredText(result, "content", 500_000);
	}

	public RequirementDraft analyzeRequirements(WorkflowProject project, List<String> documents) {
		Map<String, Object> context = new LinkedHashMap<>();
		context.put("projectName", project.getName());
		context.put("projectDescription", project.getDescription());
		context.put("techStack", project.getTechStack());
		context.put("documents", documents);
		JsonNode result = gateway.generateStructured("requirement_analysis",
				"你是资深产品经理。只依据用户资料提炼可审核的业务模块和功能点，不编造资料没有支持的业务。"
						+ "模块应按业务职责划分；功能点必须是具体用户能力；验收标准必须可验证；priority 只能是 HIGH、MEDIUM、LOW。",
				"分析以下项目资料并输出需求草稿。每个模块至少一个功能点，合并重复内容。\n" + json(context),
				requirementSchema()).content();
		List<AiDraftModule> modules = new ArrayList<>();
		Set<String> moduleNames = new HashSet<>();
		for (JsonNode moduleNode : result.path("modules")) {
			String moduleName = requiredText(moduleNode, "name", 80);
			if (!moduleNames.add(moduleName.toLowerCase(Locale.ROOT))) continue;
			List<AiDraftFeature> features = new ArrayList<>();
			Set<String> featureNames = new HashSet<>();
			for (JsonNode featureNode : moduleNode.path("features")) {
				String featureName = requiredText(featureNode, "name", 160);
				if (!featureNames.add(featureName.toLowerCase(Locale.ROOT))) continue;
				features.add(new AiDraftFeature(featureName, requiredText(featureNode, "description", 1000),
						requiredText(featureNode, "acceptanceCriteria", 2000), priority(featureNode.path("priority").asText())));
				if (features.size() >= 100) break;
			}
			if (!features.isEmpty()) {
				modules.add(new AiDraftModule(moduleName, requiredText(moduleNode, "description", 1000), features));
			}
			if (modules.size() >= 50) break;
		}
		if (modules.isEmpty()) throw new CheckedException("AI 未能从资料中提炼出可审核的功能点");
		return new RequirementDraft(modules);
	}

	public String generatePrototype(WorkflowProject project, WorkflowModule module, List<WorkflowFeature> features,
			String revisionComment) {
		return generateHtml("prototype_generation",
				"你是产品原型设计师。生成可直接在浏览器运行的单文件交互 HTML 原型，重点表达信息架构、任务流程、表单、状态和反馈，"
						+ "视觉保持低保真且清晰。必须响应式并覆盖全部功能点。只用内联 CSS/JavaScript，不引用外部资源。",
				moduleContext(project, module, features, null, revisionComment), List.of(), prototypeSchema());
	}

	public String generateUiDesign(WorkflowProject project, WorkflowModule module, List<WorkflowFeature> features,
			String approvedPrototypeHtml, String revisionComment) {
		return generateHtml("ui_design_generation",
				"你是企业级产品 UI 设计师。依据已审核原型生成可直接预览的高保真单文件 HTML。保持原型流程和功能完整，"
						+ "建立明确视觉层级、完整表单与空/加载/错误/成功状态，适配桌面和移动端。只用内联 CSS/JavaScript，不引用外部资源。",
				moduleContext(project, module, features, approvedPrototypeHtml, revisionComment), List.of(), htmlSchema());
	}

	public GeneratedFrontend generateFrontend(WorkflowProject project, WorkflowModule module,
			List<WorkflowFeature> features, String approvedUiHtml, AiModelGateway.AiImage uiImage,
			String revisionComment) {
		Map<String, Object> context = moduleContext(project, module, features, approvedUiHtml, revisionComment);
		context.put("frontendLogic", module.getFrontendLogic());
		context.put("target", "Vue 3 + TypeScript；代码需适配项目既有技术栈，并把 API 调用集中在 src/api 下");
		List<AiModelGateway.AiImage> images = uiImage == null ? List.of() : List.of(uiImage);
		JsonNode result = gateway.generateStructured("frontend_generation",
				"你是高级前端工程师。根据已审核 UI、功能点和用户补充逻辑生成可审查、可下载的 Vue 3 + TypeScript 模块代码。"
						+ "实现真实交互状态、校验、加载、错误反馈和 API 层，不得只输出静态示例。previewHtml 必须是忠实反映实现的独立单文件预览。"
						+ "所有文件路径必须是相对路径，禁止依赖未列出的远程资源。",
				"生成此前端模块。若附有设计图，必须按图中的布局、层级和控件实现。\n" + json(context), images,
				frontendSchema()).content();
		String previewHtml = validateHtml(requiredText(result, "previewHtml", MAX_HTML_LENGTH));
		List<GeneratedCodeFile> files = new ArrayList<>();
		Set<String> paths = new HashSet<>();
		int totalLength = 0;
		for (JsonNode fileNode : result.path("files")) {
			String path = requiredText(fileNode, "path", 300).replace('\\', '/');
			if (!isSafePath(path) || !paths.add(path.toLowerCase(Locale.ROOT))) {
				throw new CheckedException("AI 生成了不安全或重复的代码文件路径");
			}
			String content = requiredText(fileNode, "content", MAX_CODE_LENGTH);
			totalLength += content.length();
			if (totalLength > MAX_CODE_LENGTH) throw new CheckedException("AI 生成的前端代码总量超过限制");
			files.add(new GeneratedCodeFile(path, requiredText(fileNode, "language", 40), content));
			if (files.size() > 60) throw new CheckedException("AI 生成的前端代码文件数量超过限制");
		}
		if (files.isEmpty() || files.stream().noneMatch(file -> file.path().endsWith(".vue"))) {
			throw new CheckedException("AI 生成结果缺少 Vue 页面文件");
		}
		return new GeneratedFrontend(files, previewHtml);
	}

	public GeneratedBackend generateBackend(WorkflowProject project, WorkflowModule module,
			List<WorkflowFeature> features, String revisionComment) {
		Map<String, Object> context = moduleContext(project, module, features, null, revisionComment);
		context.put("backendLogic", value(module.getBackendLogic()));
		context.put("target", "Java 17 + Spring Boot + MyBatis-Plus；代码需适配项目既有技术栈，并把接口、DTO、Service、Mapper 和校验分层");
		JsonNode result = gateway.generateStructured("backend_generation",
				"你是高级 Java 后端工程师。根据已审核功能点、用户补充后端逻辑和项目技术栈生成可审查、可下载的后端代码。"
						+ "实现真实的 REST 接口、参数校验、业务服务、持久化边界和明确错误处理，不要生成空壳或伪代码。"
						+ "所有文件路径必须是相对路径，禁止依赖未列出的远程资源；代码只使用标准 Java/Spring/MyBatis-Plus API。",
				"生成此前端模块对应的后端实现。输出接口摘要并给出完整源文件。\n" + json(context),
				backendSchema()).content();
		List<GeneratedCodeFile> files = new ArrayList<>();
		Set<String> paths = new HashSet<>();
		int totalLength = 0;
		for (JsonNode fileNode : result.path("files")) {
			String path = requiredText(fileNode, "path", 300).replace('\\', '/');
			if (!isSafePath(path) || !paths.add(path.toLowerCase(Locale.ROOT))) {
				throw new CheckedException("AI 生成了不安全或重复的后端代码文件路径");
			}
			String content = requiredText(fileNode, "content", MAX_CODE_LENGTH);
			totalLength += content.length();
			if (totalLength > MAX_CODE_LENGTH) throw new CheckedException("AI 生成的后端代码总量超过限制");
			files.add(new GeneratedCodeFile(path, requiredText(fileNode, "language", 40), content));
			if (files.size() > 80) throw new CheckedException("AI 生成的后端代码文件数量超过限制");
		}
		if (files.isEmpty() || files.stream().noneMatch(file -> file.path().endsWith(".java"))) {
			throw new CheckedException("AI 生成结果缺少 Java 源文件");
		}
		return new GeneratedBackend(requiredText(result, "apiSummary", 20_000), files);
	}

	private String generateHtml(String operation, String instructions, Map<String, Object> context,
			List<AiModelGateway.AiImage> images, JsonNode schema) {
		JsonNode result = gateway.generateStructured(operation, instructions,
				"生成完整 HTML 文档，不要输出 Markdown 代码围栏。\n" + json(context), images, schema).content();
		return validateHtml(requiredText(result, "html", MAX_HTML_LENGTH));
	}

	private Map<String, Object> moduleContext(WorkflowProject project, WorkflowModule module,
			List<WorkflowFeature> features, String approvedDesignHtml, String revisionComment) {
		Map<String, Object> context = new LinkedHashMap<>();
		context.put("project", Map.of("name", project.getName(), "description", value(project.getDescription()),
				"techStack", value(project.getTechStack())));
		context.put("module", Map.of("code", module.getModuleCode(), "name", module.getName(),
				"description", value(module.getDescription())));
		context.put("features", features.stream().map(feature -> Map.of("code", feature.getFeatureCode(),
				"name", feature.getName(), "description", value(feature.getDescription()), "acceptanceCriteria",
				value(feature.getAcceptanceCriteria()), "priority", priority(feature.getPriority()))).toList());
		if (StringUtils.hasText(approvedDesignHtml)) context.put("approvedDesignHtml", approvedDesignHtml);
		if (StringUtils.hasText(revisionComment)) context.put("reviewCommentToAddress", revisionComment);
		return context;
	}

	private JsonNode requirementSchema() {
		ObjectNode feature = objectSchema(Map.of("name", stringSchema(), "description", stringSchema(),
				"acceptanceCriteria", stringSchema(), "priority", enumSchema("HIGH", "MEDIUM", "LOW")));
		ObjectNode module = objectSchema(Map.of("name", stringSchema(), "description", stringSchema(),
				"features", arraySchema(feature, 1)));
		return objectSchema(Map.of("modules", arraySchema(module, 1)));
	}

	private JsonNode prototypeSchema() { return htmlSchema(); }

	private JsonNode htmlSchema() { return objectSchema(Map.of("html", stringSchema())); }

	private JsonNode frontendSchema() {
		ObjectNode file = objectSchema(Map.of("path", stringSchema(), "language", stringSchema(), "content", stringSchema()));
		return objectSchema(Map.of("previewHtml", stringSchema(), "files", arraySchema(file, 1)));
	}

	private JsonNode backendSchema() {
		ObjectNode file = objectSchema(Map.of("path", stringSchema(), "language", stringSchema(), "content", stringSchema()));
		return objectSchema(Map.of("apiSummary", stringSchema(), "files", arraySchema(file, 1)));
	}

	private ObjectNode objectSchema(Map<String, JsonNode> properties) {
		ObjectNode node = objectMapper.createObjectNode().put("type", "object").put("additionalProperties", false);
		ObjectNode propertyNode = objectMapper.createObjectNode();
		ArrayNode required = objectMapper.createArrayNode();
		properties.forEach((name, schema) -> { propertyNode.set(name, schema); required.add(name); });
		node.set("properties", propertyNode);
		node.set("required", required);
		return node;
	}

	private ObjectNode arraySchema(JsonNode items, int minimum) {
		return objectMapper.createObjectNode().put("type", "array").put("minItems", minimum).set("items", items);
	}

	private ObjectNode stringSchema() { return objectMapper.createObjectNode().put("type", "string"); }

	private ObjectNode enumSchema(String... values) {
		ObjectNode node = stringSchema();
		ArrayNode enums = objectMapper.createArrayNode();
		for (String value : values) enums.add(value);
		node.set("enum", enums);
		return node;
	}

	private String validateHtml(String html) {
		String value = html.trim();
		if (!(value.regionMatches(true, 0, "<!doctype html", 0, 14) || value.regionMatches(true, 0, "<html", 0, 5))) {
			throw new CheckedException("AI 生成结果不是完整 HTML 文档");
		}
		if (EXTERNAL_HTML.matcher(value).find()) {
			throw new CheckedException("AI 生成的 HTML 包含外部资源或不允许的嵌入内容");
		}
		return value;
	}

	private boolean isSafePath(String path) {
		if (!StringUtils.hasText(path) || path.startsWith("/") || path.matches("^[A-Za-z]:.*")) return false;
		return List.of(path.split("/", -1)).stream().noneMatch(part -> part.isBlank() || ".".equals(part) || "..".equals(part));
	}

	private String requiredText(JsonNode node, String field, int maxLength) {
		String value = node.path(field).asText().trim();
		if (!StringUtils.hasText(value)) throw new CheckedException("AI 结构化结果缺少字段: " + field);
		if (value.length() > maxLength) throw new CheckedException("AI 结构化结果字段过长: " + field);
		return value;
	}

	private String priority(String value) {
		return Set.of("HIGH", "MEDIUM", "LOW").contains(value) ? value : "MEDIUM";
	}

	private boolean isImage(String extension) {
		return Set.of("png", "jpg", "jpeg", "webp").contains(extension);
	}

	private String normalizeMediaType(String mediaType, String extension) {
		if (StringUtils.hasText(mediaType) && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(mediaType)) return mediaType;
		return switch (extension) {
			case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
			case "png" -> MediaType.IMAGE_PNG_VALUE;
			case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
			case "ppt" -> "application/vnd.ms-powerpoint";
			case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
			case "doc" -> "application/msword";
			default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
		};
	}

	private String value(String value) { return value == null ? "" : value; }

	private String json(Object value) {
		try { return objectMapper.writeValueAsString(value); }
		catch (JsonProcessingException exception) { throw new CheckedException("构造 AI 生成上下文失败"); }
	}

	public record AiDraftFeature(String name, String description, String acceptanceCriteria, String priority) { }
	public record AiDraftModule(String name, String description, List<AiDraftFeature> features) { }
	public record RequirementDraft(List<AiDraftModule> modules) { }
	public record GeneratedFrontend(List<GeneratedCodeFile> files, String previewHtml) { }
	public record GeneratedBackend(String apiSummary, List<GeneratedCodeFile> files) { }
}
