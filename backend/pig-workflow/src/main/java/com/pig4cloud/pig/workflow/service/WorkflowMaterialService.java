package com.pig4cloud.pig.workflow.service;

import cn.hutool.core.util.IdUtil;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.common.file.core.FileTemplate;
import com.pig4cloud.pig.workflow.entity.WorkflowMaterial;
import com.pig4cloud.pig.workflow.mapper.WorkflowMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 项目资料存储与确定性文本抽取。 */
@Service
@RequiredArgsConstructor
public class WorkflowMaterialService {

	private static final String BUCKET = "workflow-materials";
	private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
	private static final int MAX_EXTRACTED_CHARS = 500_000;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "md", "csv", "json", "yaml", "yml",
			"doc", "docx", "xls", "xlsx", "pdf", "png", "jpg", "jpeg", "ppt", "pptx");
	private static final Set<String> PLAIN_TEXT_EXTENSIONS = Set.of("txt", "md", "csv", "json", "yaml", "yml");

	private final FileTemplate fileTemplate;
	private final WorkflowProjectService projectService;
	private final WorkflowMaterialMapper materialMapper;

	@Transactional(rollbackFor = Exception.class)
	public WorkflowMaterial upload(Long projectId, MultipartFile file) {
		projectService.requireProject(projectId);
		if (file == null || file.isEmpty()) {
			throw new CheckedException("请选择要上传的资料");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new CheckedException("单个资料不能超过 50MB");
		}
		String originalName = safeOriginalName(file.getOriginalFilename());
		String extension = extension(originalName);
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new CheckedException("不支持该文件类型: " + extension);
		}

		String objectName = projectId + "/" + IdUtil.simpleUUID() + "." + extension;
		String checksum;
		try (InputStream input = file.getInputStream()) {
			byte[] bytes = input.readAllBytes();
			checksum = DigestUtils.md5DigestAsHex(bytes);
			fileTemplate.putObject(BUCKET, objectName, new java.io.ByteArrayInputStream(bytes), file.getContentType());
		}
		catch (Exception exception) {
			throw new CheckedException("资料保存失败: " + exception.getMessage());
		}

		WorkflowMaterial material = new WorkflowMaterial();
		material.setProjectId(projectId);
		material.setOriginalName(originalName);
		material.setObjectName(objectName);
		material.setBucketName(BUCKET);
		material.setContentType(file.getContentType());
		material.setExtension(extension);
		material.setFileSize(file.getSize());
		material.setChecksum(checksum);
		material.setParseStatus("UPLOADED");
		materialMapper.insert(material);
		return parse(material.getId());
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowMaterial parse(Long materialId) {
		WorkflowMaterial material = requireMaterial(materialId);
		try (InputStream input = (InputStream) fileTemplate.getObject(material.getBucketName(), material.getObjectName())) {
			String text = extract(input, material.getExtension());
			if (text == null) {
				material.setParseStatus("READY_FOR_AI");
				material.setExtractedText(null);
				material.setParseMessage("文件已保存，等待多模态或专用文档解析器处理");
			}
			else {
				material.setParseStatus("PARSED");
				material.setExtractedText(limit(text));
				material.setParseMessage("已抽取 " + material.getExtractedText().length() + " 个字符");
			}
		}
		catch (Exception exception) {
			material.setParseStatus("FAILED");
			material.setParseMessage(limitMessage(exception.getMessage()));
		}
		materialMapper.updateById(material);
		return material;
	}

	public InputStream open(Long materialId) {
		WorkflowMaterial material = requireMaterial(materialId);
		return (InputStream) fileTemplate.getObject(material.getBucketName(), material.getObjectName());
	}

	public WorkflowMaterial requireMaterial(Long materialId) {
		WorkflowMaterial material = materialMapper.selectById(materialId);
		if (material == null) {
			throw new CheckedException("项目资料不存在");
		}
		return material;
	}

	private String extract(InputStream input, String extension) throws IOException {
		if (PLAIN_TEXT_EXTENSIONS.contains(extension)) {
			return extractPlainText(input);
		}
		if ("docx".equals(extension)) {
			return extractDocx(input);
		}
		if ("xlsx".equals(extension) || "xls".equals(extension)) {
			return extractWorkbook(input);
		}
		return null;
	}

	private String extractPlainText(InputStream input) throws IOException {
		StringBuilder text = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null && text.length() < MAX_EXTRACTED_CHARS) {
				text.append(line).append('\n');
			}
		}
		return text.toString();
	}

	private String extractDocx(InputStream input) throws IOException {
		StringBuilder text = new StringBuilder();
		try (XWPFDocument document = new XWPFDocument(input)) {
			for (XWPFParagraph paragraph : document.getParagraphs()) {
				if (paragraph.getNumID() != null) {
					text.append("- ");
				}
				text.append(paragraph.getText()).append('\n');
				if (text.length() >= MAX_EXTRACTED_CHARS) {
					break;
				}
			}
		}
		return text.toString();
	}

	private String extractWorkbook(InputStream input) throws IOException {
		StringBuilder text = new StringBuilder();
		DataFormatter formatter = new DataFormatter(Locale.CHINA);
		try (Workbook workbook = WorkbookFactory.create(input)) {
			for (Sheet sheet : workbook) {
				text.append("模块: ").append(sheet.getSheetName()).append('\n');
				for (Row row : sheet) {
					List<String> values = new ArrayList<>();
					for (Cell cell : row) {
						String value = formatter.formatCellValue(cell).trim();
						if (StringUtils.hasText(value)) {
							values.add(value);
						}
					}
					if (!values.isEmpty() && !isSpreadsheetHeader(values.get(0))) {
						text.append("- ").append(String.join(" - ", values)).append('\n');
					}
					if (text.length() >= MAX_EXTRACTED_CHARS) {
						return limit(text.toString());
					}
				}
			}
		}
		return text.toString();
	}

	private boolean isSpreadsheetHeader(String value) {
		String normalized = value.replace(" ", "").toLowerCase(Locale.ROOT);
		return Set.of("功能点", "功能名称", "名称", "feature", "featurename").contains(normalized);
	}

	private String safeOriginalName(String originalName) {
		if (!StringUtils.hasText(originalName)) {
			throw new CheckedException("文件名不能为空");
		}
		String normalized = originalName.replace('\\', '/');
		String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		if (!StringUtils.hasText(name) || name.contains("..")) {
			throw new CheckedException("文件名不合法");
		}
		return name;
	}

	private String extension(String name) {
		int separator = name.lastIndexOf('.');
		return separator < 0 ? "" : name.substring(separator + 1).toLowerCase(Locale.ROOT);
	}

	private String limit(String value) {
		return value.length() <= MAX_EXTRACTED_CHARS ? value : value.substring(0, MAX_EXTRACTED_CHARS);
	}

	private String limitMessage(String value) {
		if (!StringUtils.hasText(value)) {
			return "资料解析失败";
		}
		return value.length() <= 500 ? value : value.substring(0, 500);
	}

}
