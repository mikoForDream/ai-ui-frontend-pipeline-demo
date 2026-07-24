package com.pig4cloud.pig.workflow.service;

import com.pig4cloud.pig.workflow.dto.GeneratedCodeFile;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 生成可审查的 Vue 3 前端代码草稿，未来 AI 生成器复用同一文件契约。 */
@Component
public class FrontendCodeRenderer {

	public RenderedFrontend render(WorkflowProject project, WorkflowModule module, List<WorkflowFeature> features,
			String logic) {
		String sourceCode = StringUtils.hasText(module.getModuleCode()) ? module.getModuleCode() : "module";
		String moduleCode = sourceCode.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-")
			.replaceAll("^-+|-+$", "");
		if (!StringUtils.hasText(moduleCode)) moduleCode = "module" + (module.getId() == null ? "" : "-" + module.getId());
		String route = "/workflow/" + moduleCode;
		String featureRows = features.stream().map(feature -> "\t\t{ code: '" + ts(feature.getFeatureCode())
				+ "', name: '" + ts(feature.getName()) + "', priority: '" + priority(feature.getPriority()) + "' },\n").reduce("", String::concat);
		String logicText = logic == null || logic.isBlank() ? "待补充：按功能点验收标准实现交互和状态反馈。" : logic.trim();
		List<GeneratedCodeFile> files = new ArrayList<>();
		files.add(new GeneratedCodeFile("src/views/workflow/" + moduleCode + "/index.vue", "vue", vueFile(project, module, features, logicText)));
		files.add(new GeneratedCodeFile("src/api/workflow/" + moduleCode + ".ts", "typescript", apiFile(route)));
		files.add(new GeneratedCodeFile("src/types/workflow/" + moduleCode + ".ts", "typescript", typeFile(featureRows)));
		files.add(new GeneratedCodeFile("docs/frontend/" + moduleCode + ".md", "markdown", documentation(module, features, logicText)));
		return new RenderedFrontend(files, preview(project, module, features, logicText));
	}

	private String vueFile(WorkflowProject project, WorkflowModule module, List<WorkflowFeature> features, String logic) {
		return "<template>\n  <section class=\"module-page\">\n    <header>\n      <p class=\"eyebrow\">"
				+ html(project.getName()) + " / " + html(module.getModuleCode()) + "</p>\n      <h1>" + html(module.getName())
				+ "</h1>\n      <p>基于已审核 UI 设计生成的 Vue 3 页面</p>\n    </header>\n    <div class=\"logic-note\">实现逻辑："
				+ html(logic) + "</div>\n    <el-table :data=\"features\" border>\n      <el-table-column prop=\"code\" label=\"编号\" />\n      <el-table-column prop=\"name\" label=\"功能点\" />\n      <el-table-column prop=\"priority\" label=\"优先级\" />\n    </el-table>\n  </section>\n</template>\n\n<script setup lang=\"ts\">\nimport { ref } from 'vue';\n\nconst features = ref([\n"
				+ features.stream().map(feature -> "  { code: '" + ts(feature.getFeatureCode()) + "', name: '" + ts(feature.getName())
				+ "', priority: '" + priority(feature.getPriority()) + "' },\n").reduce("", String::concat)
				+ "]);\n</script>\n\n<style scoped>\n.module-page{max-width:1100px;margin:0 auto;padding:32px;color:#1f2d33}.eyebrow{color:#2f6b58;font-size:12px}.module-page h1{font-size:28px;margin:8px 0}.logic-note{margin:20px 0;padding:14px 18px;background:#f2f7f4;border-left:4px solid #2f6b58;line-height:1.6}.module-page header p{color:#66767c}</style>\n";
	}

	private String apiFile(String route) {
		return "import request from '/@/utils/request';\n\nexport const listModuleRecords = (params?: Record<string, unknown>) =>\n  request({ url: '" + route + "', method: 'get', params });\n";
	}

	private String typeFile(String rows) {
		return "export interface ModuleFeature {\n  code: string;\n  name: string;\n  priority: 'HIGH' | 'MEDIUM' | 'LOW';\n}\n\nexport const initialModuleFeatures: ModuleFeature[] = [\n" + rows + "];\n";
	}

	private String documentation(WorkflowModule module, List<WorkflowFeature> features, String logic) {
		return "# " + module.getName() + " 前端实现\n\n生成器：RULE_BASED_VUE3_V1\n\n实现逻辑：" + logic + "\n\n功能点：\n"
				+ features.stream().map(feature -> "- " + feature.getFeatureCode() + "：" + feature.getName() + "\n").reduce("", String::concat);
	}

	private String preview(WorkflowProject project, WorkflowModule module, List<WorkflowFeature> features, String logic) {
		String cards = features.stream().map(feature -> "<article><span>" + html(feature.getPriority()) + "</span><h2>"
				+ html(feature.getName()) + "</h2><p>" + html(feature.getAcceptanceCriteria()) + "</p><button>模拟操作</button></article>").reduce("", String::concat);
		return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + html(module.getName()) + " 前端预览</title><style>body{margin:0;background:#f4f7f5;color:#1d2b31;font-family:Arial,'Microsoft YaHei',sans-serif}.page{max-width:1100px;margin:auto;padding:34px}.eyebrow{color:#2f6b58;font-size:12px}.page h1{margin:8px 0 6px;font-size:28px}.logic{padding:14px 18px;background:#eaf3ee;border-left:4px solid #2f6b58;margin:22px 0;line-height:1.6}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:14px}article{background:#fff;border:1px solid #d9e3dd;border-radius:6px;padding:18px}article span{font-size:11px;color:#2f6b58}article h2{font-size:17px;margin:10px 0}article p{color:#607078;line-height:1.6;min-height:48px}button{background:#2f6b58;color:#fff;border:0;padding:9px 12px;border-radius:4px;cursor:pointer}.feedback{color:#2f6b58;font-size:12px;margin-top:8px}</style></head><body><main class=\"page\"><div class=\"eyebrow\">" + html(project.getName()) + " / " + html(module.getModuleCode()) + "</div><h1>" + html(module.getName()) + " 前端预览</h1><div class=\"logic\">实现逻辑：" + html(logic) + "</div><section class=\"grid\">" + cards + "</section></main><script>document.querySelectorAll('button').forEach(function(button){button.addEventListener('click',function(){var node=document.createElement('div');node.className='feedback';node.textContent='交互反馈正常';button.after(node)})})</script></body></html>";
	}

	private String html(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name()); }
	private String ts(String value) {
		return (value == null ? "" : value).replace("\\", "\\\\").replace("'", "\\'")
			.replace("<", "\\x3C").replace(">", "\\x3E").replace("\r", "").replace("\n", " ");
	}
	private String priority(String value) {
		return "HIGH".equals(value) || "MEDIUM".equals(value) || "LOW".equals(value) ? value : "MEDIUM";
	}

	public record RenderedFrontend(List<GeneratedCodeFile> files, String previewHtml) {}
}
