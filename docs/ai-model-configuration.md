# AI 模型服务配置

工作流的资料分析、原型生成、UI 生成和前端代码生成统一使用后端 Responses API 网关。API Key 不经过浏览器，不写入数据库，也不应提交到 Git。

## 必需配置

在启动 `pig-boot` 的进程环境中注入：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `OPENAI_API_KEY` | 无 | 必填，只在后端进程中读取 |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | OpenAI 或兼容 Responses API 的服务根地址 |
| `OPENAI_MODEL` | `gpt-5.6` | 生成模型 |
| `OPENAI_REASONING_EFFORT` | `medium` | 推理强度 |

本地开发可在当前 PowerShell 进程中临时设置环境变量后运行 `scripts/dev-start.ps1`。生产环境应使用部署平台的 Secret、容器 Secret 或专用密钥管理服务注入，避免写入 YAML、脚本、启动参数和日志。

配置变更后必须重启后端。登录系统进入“研发项目”后，页面顶部会显示配置状态；“检测连接”会执行一次最小结构化生成，因此会产生少量 Token 消耗。

## 可选限制

`OPENAI_MAX_OUTPUT_TOKENS`、`OPENAI_MAX_INPUT_CHARS`、`OPENAI_MAX_ATTEMPTS`、`OPENAI_CONNECT_TIMEOUT` 和 `OPENAI_READ_TIMEOUT` 可用于限制单次输出、输入、重试与超时。

网关只记录操作名、响应 ID、模型、Token 用量和耗时，不记录 API Key、输入资料或生成内容。
