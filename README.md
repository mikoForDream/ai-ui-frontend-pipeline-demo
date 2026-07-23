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

工作流后端已经完成领域模型、动作路由与人工审核闭环：支持定义与节点配置、发布校验、实例启动、任务完成/失败/重试、审核通过/驳回/退回、候选用户或角色筛选、操作幂等和完整执行时间线。Pig-UI 已接入流程定义、流程实例和人工审核管理页面；流程定义页面提供节点画布、属性编辑、动作流转规则维护和发布校验入口，数据库脚本会注册对应菜单与管理员权限。数据库脚本位于 `backend/db/workflow.sql`，领域说明位于 `docs/workflow-domain-design.md`。

当前真实回归路径为：`生成 -> 审核驳回 -> 返工 -> 再次审核通过 -> 交付`。本地回归脚本从 `PIG_ADMIN_PASSWORD` 环境变量或 `-Password` 参数读取管理员密码，不在仓库中保存默认口令。

本地开发环境可在项目根目录通过 `.\scripts\dev-start.ps1` 一键启动，并使用 `.\scripts\dev-status.ps1` 检查状态。详细说明见 [本地基础设施](infra/README.md)。

详细约束见 [技术路线](docs/technical-route.md)。
