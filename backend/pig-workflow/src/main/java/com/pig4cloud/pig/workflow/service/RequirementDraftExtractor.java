package com.pig4cloud.pig.workflow.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在 AI 执行器接入前提供确定性的需求草稿抽取，结果必须经过人工审核。
 */
@Component
public class RequirementDraftExtractor {

	private static final Pattern MODULE_PREFIX = Pattern.compile("^(?:模块|module)\\s*[:：-]\\s*(.{2,60})$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern MODULE_HEADING = Pattern.compile(
			"^(?:#{1,6}\\s*)?(?:\\d+[.、)]\\s*)?(.{2,40}(?:模块|中心|管理))[:：]?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern FEATURE_LINE = Pattern.compile("^(?:[-*+•]|\\d+[.、)])\\s*(.{2,200})$");

	public List<DraftModule> extract(List<String> documents) {
		Map<String, Set<String>> grouped = new LinkedHashMap<>();
		String currentModule = "核心业务";
		grouped.put(currentModule, new LinkedHashSet<>());

		for (String document : documents) {
			if (!StringUtils.hasText(document)) {
				continue;
			}
			for (String rawLine : document.split("\\R")) {
				String line = rawLine.trim();
				if (!StringUtils.hasText(line)) {
					continue;
				}
				Matcher moduleMatcher = MODULE_PREFIX.matcher(line);
				Matcher headingMatcher = MODULE_HEADING.matcher(line);
				if (moduleMatcher.matches() || headingMatcher.matches()) {
					currentModule = clean(moduleMatcher.matches() ? moduleMatcher.group(1) : headingMatcher.group(1));
					grouped.computeIfAbsent(currentModule, ignored -> new LinkedHashSet<>());
					continue;
				}
				Matcher featureMatcher = FEATURE_LINE.matcher(line);
				if (featureMatcher.matches()) {
					String feature = clean(featureMatcher.group(1));
					if (feature.length() >= 2) {
						grouped.computeIfAbsent(currentModule, ignored -> new LinkedHashSet<>()).add(feature);
					}
				}
			}
		}

		if (grouped.values().stream().allMatch(Set::isEmpty)) {
			throw new IllegalArgumentException("资料中没有识别到列表型功能点，请使用编号或项目符号列出主要功能");
		}
		List<DraftModule> result = new ArrayList<>();
		grouped.forEach((name, features) -> {
			if (!features.isEmpty()) {
				result.add(new DraftModule(name, features.stream().limit(100).toList()));
			}
		});
		return result;
	}

	private String clean(String value) {
		return value.replaceFirst("[。；;：:]$", "").trim();
	}

	public String analyzerName() {
		return "RULE_BASED_V1".toUpperCase(Locale.ROOT);
	}

	public record DraftModule(String name, List<String> features) {
	}

}
