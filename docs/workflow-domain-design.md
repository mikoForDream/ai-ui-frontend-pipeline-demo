# 工作流核心领域设计

## 第一阶段目标

第一阶段先完成确定性的执行闭环，不绑定某一家 AI 服务，也不引入外部 BPM 引擎：

1. 创建草稿流程定义。
2. 配置开始节点、中间节点和结束节点。
3. 发布前校验节点关系。
4. 根据已发布定义启动实例并生成首个任务。
5. 完成任务后创建下一节点任务；结束节点完成后关闭实例。
6. 任务失败时冻结实例，允许从失败节点重试。
7. 所有关键动作写入不可缺失的执行日志。

## 核心对象

- `wf_definition`：流程的稳定编码、版本和发布状态。
- `wf_node_definition`：节点类型、顺序、下一节点和扩展配置。
- `wf_instance`：一次真实业务执行，使用 `business_key` 保证幂等。
- `wf_task`：节点级执行单元，可用于 AI、人工或后端服务任务。
- `wf_execution_log`：实例和任务的审计轨迹、输入输出、错误和耗时。

节点的 `config_json` 将承载模型参数、提示词模板、人工处理人规则或服务调用配置。第一阶段只负责可靠流转，不在核心引擎内硬编码具体 AI 厂商。

## 状态流转

- 定义：`DRAFT -> PUBLISHED`
- 实例：`RUNNING -> COMPLETED`，或 `RUNNING -> FAILED -> RUNNING`
- 任务：`READY -> COMPLETED`，或 `READY/RUNNING -> FAILED -> READY`

## 首批接口

- `/workflow/definitions`：定义查询、新增、节点维护和发布。
- `/workflow/instances`：实例查询、执行详情和启动。
- `/workflow/tasks`：任务查询、完成、失败和重试。

当前引擎优先按 `source_node_key + action + priority` 查询 `wf_transition`，没有匹配规则时回退到 `next_node_key`。已支持 `COMPLETE`、`APPROVE`、`REJECT`、`RETURN`、`RETRY`、`CANCEL`、`SKIP` 和 `FAIL`；并行汇聚仍留到后续阶段。

## V2 领域增强

为支持 AI、UI、前端、后端与人工审核协同，新增以下模型：

- `wf_project`：关联项目、Git 仓库、技术栈和前后端目录。
- `wf_transition`：按任务动作表达通过、驳回、返回和默认流转路径。
- `wf_approval`：保存审核类型、审核人、决定、意见和操作幂等键。
- `wf_artifact`：保存需求、原型、UI、接口、代码等产物的稳定身份。
- `wf_artifact_version`：保存不可覆盖的产物版本、文件或 Git 引用。
- `wf_product_spec`：保存前后端共享的版本化产品规格。

`wf_definition` 和 `wf_instance` 增加可空的 `project_id`，旧数据及不属于项目的通用流程不受影响。`next_node_key` 暂时保留，后续动作路由优先查询 `wf_transition`，未配置时回退到原顺序路径。

Notion 继续承担规格和协作任务管理，Pig 数据库承担正式运行状态、审核决定和产物版本，避免两个系统同时成为状态真相源。

## 人工审核闭环

- `MANUAL_REVIEW` 或 `MANUAL` 节点创建任务时，同一事务自动创建一条 `PENDING` 审核记录。
- 节点 `config_json` 可配置 `candidateReviewerId` 或 `candidateRoleId`，用于限定候选用户或角色。
- `/workflow/approvals/page` 按实例、审核人、候选用户、候选角色和状态查询。
- `/workflow/approvals/{id}/claim` 以条件更新领取待办，避免多人同时领取。
- `/workflow/approvals/{id}/decisions` 提交 `APPROVE`、`REJECT` 或 `RETURN`；审核记录更新和流程推进处于同一数据库事务。
- `operationKey` 用于审核请求幂等：相同决定可安全重放，不同决定会被拒绝。
- 实例详情返回 `approvals`，与任务及执行日志共同组成审核时间线。

## V3 研发资料与功能点

- `wf_material` 保存项目原始资料的对象存储位置、MD5 校验值、解析状态和抽取文本。
- `wf_module` 保存从需求资料中拆分出的研发模块及其需求审核状态。
- `wf_feature` 保存可独立编辑和审核的功能点、验收标准、优先级与版本。
- 项目从 `MATERIAL_COLLECTION` 进入 `FEATURE_REVIEW`；所有功能点通过后进入 `PROTOTYPE_READY`。
- 当前确定性解析器处理文本、Markdown、Word 和 Excel。PDF、图片及演示文稿标记为 `READY_FOR_AI`，不伪装成已解析资料。
- 当前需求草稿抽取器为 `RULE_BASED_V1`，用于先打通可靠闭环；后续 AI 执行器必须输出相同的模块与功能点结构，并继续经过人工审核。

## V4 模块原型与审核

- 首次生成模块原型前，将全部已审核模块和功能点冻结为 `wf_product_spec` 的 `REQ-V1` 版本。
- 每个模块对应一个 `PROTOTYPE` 类型的 `wf_artifact`，`module_id` 明确产物归属；每次生成新增 `wf_artifact_version`，不覆盖旧版本。
- 当前生成器为 `RULE_BASED_HTML_V1`，输出带模块导航、业务输入、操作反馈和验收标准的交互 HTML；需求文本进入原型前执行 HTML 转义，前端在沙箱 iframe 中预览。
- 原型版本支持 `PENDING_REVIEW -> APPROVED/REJECTED/RETURNED`，驳回或退回必须填写意见；只有当前版本可以审核。
- 被驳回模块进入 `PROTOTYPE_REVISION` 并可生成下一版本；已通过模块不可直接覆盖。
- 所有模块原型通过后，项目从 `PROTOTYPE_REVIEW` 推进到 `UI_READY`。下一阶段允许 AI 基于已通过原型生成 UI，也允许用户上传设计图介入。

## V5 模块 UI 设计与审核

- 原型已通过的模块可以生成 UI 设计草稿，也可以由用户上传 PNG、JPG、JPEG 或 WebP 设计图；上传文件最大 20MB，并存入对象存储而不是数据库。
- 当前确定性生成器为 `RULE_BASED_UI_V1`，用于打通可靠审核闭环，不伪装外部 AI 调用。后续 AI 生成器复用 `UI_DESIGN` 产物及版本契约。
- 每个模块使用稳定的 `wf_artifact` 身份，每次生成或上传新增 `wf_artifact_version`。已通过版本不可覆盖，只有当前待审版本可以审核。
- HTML 草稿在前端沙箱 iframe 中预览；上传图片通过带认证的内容接口读取。两种来源均支持通过、驳回和退回，驳回或退回必须填写意见。
- 模块状态依次为 `PROTOTYPE_APPROVED -> UI_REVIEW -> UI_APPROVED/UI_REVISION`；全部模块 UI 通过后，项目进入 `FRONTEND_READY`。

## V6 模块前端开发与代码审核

- UI 设计已通过的模块可填写最多 5000 字的前端实现逻辑，补充页面状态、交互规则、校验和异常反馈；不填写时按已审核功能点及验收标准生成。
- 当前确定性生成器为 `RULE_BASED_VUE3_V1`，输出 Vue 页面、TypeScript API、类型文件、实现文档和独立 HTML 运行预览。它只负责打通可靠闭环，不伪装真实 AI 或 Codex 调用。
- 每次生成新增 `FRONTEND_CODE` 类型产物版本，记录对应的 UI 设计版本 ID 和生成时的实现逻辑快照，不覆盖历史版本。未来真实智能生成器复用相同的文件列表、产物版本和审核契约。
- 工作台可在线运行预览、逐文件查看代码及下载 ZIP；下载前拒绝绝对路径、空路径和目录穿越路径，用户输入进入 HTML、Vue 或 TypeScript 字符串前进行对应转义。
- 只有当前 `PENDING_REVIEW` 版本可以审核；驳回或退回必须填写意见，被驳回模块可修改实现逻辑并生成下一版本，已通过版本不可覆盖。
- 模块状态依次为 `UI_APPROVED -> FRONTEND_REVIEW -> FRONTEND_APPROVED/FRONTEND_REVISION`；全部模块前端代码通过后，项目从 `FRONTEND_REVIEW` 推进到 `BACKEND_READY`，等待后端开发阶段。
