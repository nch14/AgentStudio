# AgentStudio

[English](./README.en.md)

AgentStudio 是一个面向个人使用的 AI Agent 平台，用来集中管理可执行任务的 Agent、长期对话、自动化计划、通知和 Agent 工作区文件。当前公开版包含 Spring Boot 服务端、Umi/React 前端，以及 Claude Code CLI 运行时集成。

项目适合在本机或私有服务器上运行，帮助个人维护一组可聊天、可执行任务、可按计划工作的 AI Agent。

## 功能概览

- **Agent 管理**：创建、启用和配置 Agent Provider，维护 Agent 的本地文件。
- **对话工作台**：管理会话列表，支持流式回复、归档、重命名和删除。
- **任务执行**：创建任务，跟踪任务回合、进度和执行结果。
- **自动化计划**：通过 cron 表达式或可视化表单配置周期任务。
- **通知中心**：支持站内通知，并可配置邮件和 Bark 推送。
- **文件工作区**：在 Web UI 中查看、新建和编辑 Agent 工作区文件。
- **Claude Code 集成**：通过 Claude Code CLI 运行聊天和任务模式，并通过 MCP 暴露平台能力。

## 快速开始

推荐使用 Docker Compose 运行完整应用：

```bash
cd deploy
chmod +x deploy.sh
./deploy.sh build
./deploy.sh up
```

启动后访问 `http://localhost:8685`。

完整部署、配置和升级说明请阅读 [部署指南](./DEPLOY.md)。如果只做本地开发，可以直接阅读下方的开发说明。

## 项目结构

```text
AgentStudio/
├── server/             # Spring Boot 多 Module 服务端
├── webui/              # Umi / React / TypeScript / Ant Design 前端
├── docker/             # Docker 镜像构建文件
├── deploy/             # Docker Compose 部署脚本
├── DEPLOY.md           # 部署指南
├── DEPLOY.en.md        # Deployment guide
├── README.md           # 中文入口
└── README.en.md        # English entry
```

服务端模块：

- `agents-common`：通用配置、OSS、通知和工具类。
- `agents-domain`：领域模型、仓储接口和领域服务。
- `agents-connect`：Agent Provider SPI、MCP 工具和平台能力接口。
- `agents-claude-code`：Claude Code CLI 进程、提示词、工作区和 Provider 实现。
- `agents-app`：HTTP API、应用服务、调度器和启动入口。

## 本地开发

### 服务端

要求：

- JDK 21
- Maven 3.9+

```bash
cd server
mvn clean package
mvn -pl agents-app spring-boot:run
```

服务端默认端口为 `8685`，Swagger UI 地址为：

```text
http://localhost:8685/swagger-ui.html
```

### 前端

要求：

- Node.js 22+
- npm

```bash
cd webui
npm install
npm run dev
```

开发环境会将 `/api` 请求代理到 `http://localhost:8685`。

## 安全说明

AgentStudio 当前定位为个人本地工具，默认没有多用户鉴权模型。不要直接暴露到公网环境；如需公网访问，请先加反向代理鉴权、网络访问控制和更严格的 Claude Code 权限配置。

如果启用 Claude Code 的高权限模式，请只在可信工作区使用，并避免把 API key、数据库、日志和 Agent 工作区文件提交到公开仓库。

## License

本项目使用 Apache License 2.0。
