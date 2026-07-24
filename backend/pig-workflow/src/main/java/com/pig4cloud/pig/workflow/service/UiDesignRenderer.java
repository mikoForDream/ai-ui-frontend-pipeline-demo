package com.pig4cloud.pig.workflow.service;

import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import org.springframework.web.util.HtmlUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 生成可供 UI 审核的确定性高保真草稿，后续 AI 生成器复用同一产物契约。 */
@Component
public class UiDesignRenderer {

	public String render(WorkflowProject project, WorkflowModule module, List<WorkflowFeature> features) {
		StringBuilder rows = new StringBuilder();
		StringBuilder nav = new StringBuilder();
		for (int index = 0; index < features.size(); index++) {
			WorkflowFeature feature = features.get(index);
			String priority = feature.getPriority() == null ? "MEDIUM" : feature.getPriority();
			nav.append("<button class=\"nav-link").append(index == 0 ? " active" : "").append("\" data-index=\"")
				.append(index).append("\"><span class=\"nav-icon\">").append(index + 1).append("</span>")
				.append(escape(feature.getName())).append("</button>");
			rows.append("<tr data-row=\"").append(index).append("\"><td><span class=\"status-dot\"></span>")
				.append(escape(feature.getFeatureCode())).append("</td><td class=\"feature-name\">")
				.append(escape(feature.getName())).append("</td><td>").append(escape(feature.getAcceptanceCriteria()))
				.append("</td><td><span class=\"priority ").append(escape(priority.toLowerCase()))
				.append("\">").append(escape(priority)).append("</span></td><td><button class=\"row-action\">查看</button></td></tr>");
		}
		String firstName = features.isEmpty() ? "模块总览" : features.get(0).getName();
		return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"
				+ escape(module.getName()) + " UI 设计稿</title><style>"
				+ ":root{font-family:Inter,Arial,'Microsoft YaHei',sans-serif;color:#17202a;background:#f5f7f8}*{box-sizing:border-box}body{margin:0}.app{min-height:100vh;display:flex}.sidebar{width:236px;background:#202d35;color:#dbe4e8;padding:22px 14px;flex:none}.brand{font-size:12px;color:#93c5b2;letter-spacing:.4px;padding:0 12px}.product{font-size:18px;font-weight:700;color:#fff;padding:7px 12px 28px}.nav-label{font-size:11px;text-transform:uppercase;color:#82919a;padding:0 12px 8px}.nav-link{width:100%;display:flex;align-items:center;gap:9px;padding:10px 12px;border:0;border-radius:5px;background:transparent;color:#c8d1d5;text-align:left;cursor:pointer;margin:3px 0;font-size:13px}.nav-link.active{background:#2f6b58;color:#fff}.nav-icon{width:19px;height:19px;border:1px solid currentColor;border-radius:4px;display:inline-flex;align-items:center;justify-content:center;font-size:10px}.main{flex:1;min-width:0}.topbar{height:64px;background:#fff;border-bottom:1px solid #dce3e5;padding:0 30px;display:flex;align-items:center;justify-content:space-between}.crumb{font-size:13px;color:#738188}.top-actions{display:flex;align-items:center;gap:13px}.search{border:1px solid #d3dcdf;border-radius:4px;padding:8px 11px;width:180px;color:#56666d}.avatar{width:30px;height:30px;border-radius:50%;background:#d9ebe3;color:#2f6b58;display:grid;place-items:center;font-size:12px;font-weight:bold}.content{padding:28px 32px;max-width:1250px}.heading{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:22px}.heading h1{font-size:25px;margin:0 0 6px;color:#1a272e}.heading p{margin:0;color:#6d7c83;font-size:13px}.badge{background:#e7f0ed;color:#285d4c;border-radius:4px;padding:7px 11px;font-size:12px}.stats{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-bottom:20px}.stat{background:#fff;border:1px solid #dce3e5;border-radius:6px;padding:17px}.stat small{color:#7b888e;font-size:12px}.stat strong{display:block;font-size:24px;margin-top:7px;color:#24343b}.panel{background:#fff;border:1px solid #dce3e5;border-radius:6px;margin-bottom:20px}.panel-head{padding:17px 20px;border-bottom:1px solid #e8edef;display:flex;justify-content:space-between;align-items:center}.panel-head strong{font-size:14px}.panel-head span{font-size:12px;color:#829097}.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;min-width:680px}th,td{text-align:left;padding:14px 20px;border-bottom:1px solid #edf1f2;font-size:12px;color:#5e6d73}th{font-size:11px;color:#7e8c91;font-weight:600;text-transform:uppercase}td.feature-name{font-weight:600;color:#27363d}.status-dot{display:inline-block;width:7px;height:7px;border-radius:50%;background:#4a9b79;margin-right:8px}.priority{padding:4px 7px;border-radius:3px;font-size:11px}.priority.high{background:#fce8e5;color:#a64a3e}.priority.medium{background:#fff3d9;color:#97702b}.priority.low{background:#e7f0ed;color:#2f6b58}.row-action{border:1px solid #c8d4d7;background:#fff;color:#2f6b58;border-radius:3px;padding:5px 9px;cursor:pointer}.notice{padding:14px 20px;background:#f8faf9;border-top:1px solid #edf1f2;color:#66777d;font-size:12px}.toast{position:fixed;right:26px;bottom:24px;padding:11px 15px;background:#202d35;color:#fff;border-radius:4px;display:none;font-size:12px}@media(max-width:780px){.sidebar{width:190px}.topbar{padding:0 16px}.search{width:130px}.content{padding:22px 16px}.stats{grid-template-columns:1fr}.heading{gap:12px}}"
				+ "</style></head><body><div class=\"app\"><aside class=\"sidebar\"><div class=\"brand\">AI PRODUCT WORKFLOW</div><div class=\"product\">"
				+ escape(project.getName()) + "</div><div class=\"nav-label\">模块导航</div><nav>" + nav
				+ "</nav></aside><main class=\"main\"><header class=\"topbar\"><span class=\"crumb\">"
				+ escape(module.getModuleCode()) + " / " + escape(module.getName()) + " / UI 设计</span><div class=\"top-actions\"><input class=\"search\" placeholder=\"搜索模块内容\"><span class=\"avatar\">UI</span></div></header><section class=\"content\"><div class=\"heading\"><div><h1>"
				+ escape(firstName) + "</h1><p>基于已通过原型和产品规格生成的 UI 设计草稿</p></div><span class=\"badge\">设计草稿 · 待审核</span></div><div class=\"stats\"><div class=\"stat\"><small>模块功能点</small><strong>"
				+ features.size() + "</strong></div><div class=\"stat\"><small>设计状态</small><strong>待审核</strong></div><div class=\"stat\"><small>来源</small><strong>规则基线</strong></div></div><div class=\"panel\"><div class=\"panel-head\"><strong>功能清单</strong><span>"
				+ features.size() + " 项已映射</span></div><div class=\"table-wrap\"><table><thead><tr><th>编号</th><th>功能名称</th><th>验收标准</th><th>优先级</th><th>操作</th></tr></thead><tbody>"
				+ rows + "</tbody></table></div><div class=\"notice\">每个功能点都应在开发前完成 UI 状态、输入反馈和异常反馈确认。</div></div></section></main></div><div class=\"toast\" id=\"toast\">已打开功能详情</div><script>document.querySelectorAll('.nav-link').forEach(function(link){link.addEventListener('click',function(){document.querySelectorAll('.nav-link').forEach(function(item){item.classList.remove('active')});link.classList.add('active');var row=document.querySelector('[data-row=\"'+link.dataset.index+'\"]');if(row){row.scrollIntoView({behavior:'smooth',block:'center'})}})});document.querySelectorAll('.row-action').forEach(function(button){button.addEventListener('click',function(){var toast=document.getElementById('toast');toast.style.display='block';setTimeout(function(){toast.style.display='none'},1800)})});</script></body></html>";
	}

	private String escape(String value) {
		return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
	}

}
