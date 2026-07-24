# AI 全栈工作流项目

本项目统一采用 Pig 全栈技术路线，不再保留 React 技术路线或其他备选框架。

## 技术基线

- 后端：Pig `v3.9.2`
- 前端：Pig-UI `v3.9.2`
- 前端技术：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios
- 后端技术：Java 17、Spring Boot 3.5、Spring Cloud、Spring Authorization Server、MyBatis-Plus
- 基础设施：MySQL、Redis；微服务模式下使用 Nacos 与 Pig Gateway

## 目录

```text
backend/    Pig 完整后端源码
             └─ pig-workflow/ 工作流定义、实例、任务与执行日志模块
frontend/   Pig-UI 完整前端源码
infra/      本地基础设施与环境说明
docs/       技术路线和工作流约束
```

## 开发原则

1. 前端页面统一在 `frontend/` 内按 Pig-UI 规范开发。
2. 后端接口统一在 `backend/` 内复用 Pig 的认证、权限、数据访问和模块结构。
3. 本地开发优先使用 `backend/pig-boot` 单体模式；源码仍保留完整微服务能力。
4. 自动化任务必须提交到功能分支，通过验证和评审后再合并到 `main`。
5. 不使用 Mock 接口替代可以在 Pig 后端直接实现的正式接口。

## 当前工作流进度

工作流后端已经完成领域模型、动作路由与人工审核闭环：支持定义与节点配置、发布校验、实例启动、任务完成/失败/重试、审核通过/驳回/退回、候选用户或角色筛选、操作幂等和完整执行时间线。Pig-UI 已接入流程定义、流程实例和人工审核管理页面；流程定义页面提供节点画布、属性编辑、动作流转规则维护和发布校验入口，数据库脚本会注册对应菜单与管理员权限。

AI 研发流水线已经打通资料、需求、模块原型、UI 设计与前端开发阶段：研发项目工作台支持上传需求资料，文本、Markdown、Word 和 Excel 可直接抽取内容，PDF、图片和演示文稿会保留原文件并等待 AI 解析器；已解析资料可以生成按模块组织的功能点草稿，用户能够编辑、通过或驳回。全部功能点通过后会冻结 `REQ-V1` 产品规格，并可逐模块生成可交互 HTML 原型、在线预览、驳回重生成或审核通过。原型通过后，工作流可生成确定性的 UI 草稿，用户也可直接上传设计图介入。UI 通过后可按模块补充前端实现逻辑，生成版本化 Vue 3 代码包、在线运行预览、查看代码文件、下载 ZIP，并执行通过或驳回审核；所有模块前端代码通过后项目进入 `BACKEND_READY`。数据库脚本位于 `backend/db/workflow.sql`，领域说明位于 `docs/workflow-domain-design.md`。

当前真实回归路径为：`资料分析 -> 功能点审核 -> 冻结需求规格 -> 模块原型 V1 -> 驳回 -> 模块原型 V2 -> 全部通过 -> UI 草稿 V1 -> 驳回 -> UI 草稿 V2 -> 上传人工设计图 -> 全部通过 -> 前端代码 V1 -> 驳回 -> 前端代码 V2 -> 全部通过 -> BACKEND_READY`。当前前端代码生成器为明确标识的 `RULE_BASED_VUE3_V1`，不伪装外部 AI 或 Codex 调用；未来真实智能生成器继续复用相同的代码文件、产物版本和审核契约。本地回归脚本从 `PIG_ADMIN_PASSWORD`、`-Password` 或一次性 `-EncryptedPassword` 参数读取登录凭据，不在仓库中保存默认口令。

项目资料到前端代码审核的回归脚本为 `scripts/smoke-project-intake.ps1`，验证 `创建项目 -> 上传资料 -> 需求分析 -> 功能点审核 -> 需求规格冻结 -> 原型生成/预览/审核 -> UI 生成或上传/预览/审核 -> 前端逻辑填写 -> 代码生成/预览/下载/审核 -> 后端开发准备`。

本地开发环境可在项目根目录通过 `.\scripts\dev-start.ps1` 一键启动，并使用 `.\scripts\dev-status.ps1` 检查状态。详细说明见 [本地基础设施](infra/README.md)。

详细约束见 [技术路线](docs/technical-route.md)。
