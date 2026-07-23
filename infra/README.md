# 本地基础设施

当前本地开发基线已经验证通过：

- Node.js `18.20.8`、npm `10.8.2`
- JDK `17.0.19`、Maven `3.9.16`（便携版位于被忽略的 `.tools/`）
- MySQL：`127.0.0.1:33306`
- Redis：`127.0.0.1:36379`
- Pig Boot：`http://127.0.0.1:9999/admin`
- Pig UI：`http://127.0.0.1:8888`

Pig 后端自带数据库脚本和 Docker Compose 配置：

- 数据库脚本：`../backend/db/`
- Docker Compose：`../backend/docker-compose.yml`

## 启动顺序

1. 启动 Docker Desktop。
2. 在 `backend/` 下启动 `pig-mysql` 和 `pig-redis`。
3. 使用 `mvn -Pboot -DskipTests package` 构建后端。
4. 运行 `backend/pig-boot/target/pig-boot.jar`。
5. 在 `frontend/` 下运行 `npm install` 和 `npm run dev`。

后端健康检查地址：`http://127.0.0.1:9999/admin/actuator/health`。前端开发代理下的健康检查地址：`http://127.0.0.1:8888/api/admin/actuator/health`。

本地开发连接参数已写入 `backend/pig-boot/src/main/resources/application-dev.yml`，也可以使用 `MYSQL_HOST`、`MYSQL_PORT`、`REDIS_HOST` 和 `REDIS_PORT` 环境变量覆盖。

禁止把真实密码、令牌、私钥或生产连接信息提交到仓库。环境差异应通过本地配置或受控的环境变量管理。
