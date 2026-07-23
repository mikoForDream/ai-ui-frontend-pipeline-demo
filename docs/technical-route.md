# 技术路线

## 唯一路线

项目固定采用 Pig `v3.9.2` + Pig-UI `v3.9.2` 全栈框架。当前阶段不维护 React 版本，也不设计替代技术栈。

## 前端

- 框架：Vue 3.5
- 语言：TypeScript
- 构建：Vite 5
- UI：Element Plus
- 状态管理：Pinia
- 路由：Vue Router
- 请求：Axios，并复用 Pig-UI 既有请求封装、令牌刷新和错误处理
- 代码位置：`frontend/`

新增业务页面必须遵循 Pig-UI 的目录、菜单、权限、请求和组件约定，不得在仓库根目录创建独立前端应用。

## 后端

- 基线：Pig 3.9.2
- Java：17
- Spring Boot：3.5.7
- Spring Cloud：2025.0.0
- 权限：Spring Authorization Server / OAuth2
- 数据访问：复用 Pig 公共模块和既有 MyBatis-Plus 规范
- 代码位置：`backend/`

业务接口应优先在 Pig 后端实现，前端直接调用正式接口。只有外部依赖尚不可用时，才允许在测试范围内使用 fixture；测试 fixture 不得被描述为正式接口契约。

## 运行模式

仓库保留 Pig 的完整微服务源码。本地开发和工作流验证优先使用 `backend/pig-boot` 单体模式，降低多服务联调成本。需要验证微服务行为时，再启动注册中心、网关、认证和对应业务服务。

## 自动化工作流约束

每个 Notion 开发任务至少应提供：

- `scope`：`frontend`、`backend` 或 `fullstack`
- `module`：目标 Pig 业务模块
- `base_branch`：默认 `main`
- `target_branch`：功能分支
- 页面路径、菜单权限标识和接口路径
- 数据模型、数据库变更和验收条件
- 前端与后端验证命令

标准执行顺序：

```text
读取任务
→ 创建功能分支
→ 修改 Pig 后端与数据库
→ 修改 Pig-UI 页面和请求层
→ 运行前后端验证
→ 提交功能分支
→ 创建 PR
→ 人工评审后合并 main
```

## 当前迁移结论

- 原 React/Vitest 原型已从正式工作树移除。
- 原型历史仍可通过 Git 历史查看，但不再作为后续开发基线。
- 商品对比功能后续必须基于 `frontend/` 与 `backend/` 重新实现。

