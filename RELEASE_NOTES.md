# Release Notes

## v0.1.0 — AgentStudio 首个开源版本

AgentStudio 是一个面向个人使用的 AI Agent 平台，用来集中管理可执行任务的 Agent、长期对话、自动化计划、通知和 Agent 工作区文件。

本版本包含 Spring Boot 服务端、Umi/React 前端，以及 Claude Code CLI 运行时集成，适合在本机或私有服务器运行。

### 功能概览

- **Agent 管理**：创建、启用和配置 Agent Provider，维护 Agent 的本地文件
- **对话工作台**：管理会话列表，支持流式回复、归档、重命名和删除
- **任务执行**：创建任务，跟踪任务回合、进度和执行结果
- **自动化计划**：通过 cron 表达式或可视化表单配置周期任务
- **通知中心**：站内通知，可选邮件和 Bark 推送
- **文件工作区**：在 Web UI 中查看、新建和编辑 Agent 工作区文件
- **Claude Code 集成**：通过 Claude Code CLI 运行聊天与任务模式，并通过 MCP 暴露平台能力

### 技术栈

- **服务端**：Java 21、Spring Boot 3.5.6、Spring AI、Spring Data JPA，默认使用 SQLite
- **前端**：Umi、React、TypeScript、Ant Design
- **部署**：Docker / Docker Compose

### 快速开始

```bash
cd deploy
chmod +x deploy.sh
./deploy.sh init                                    # 在 ~/.agent-studio/ 下生成默认配置
# 编辑 ~/.agent-studio/config/application-prod.yml，
# 把 agents.auth.initial-token 替换为足够强的随机值（参考 openssl rand -hex 32）
./deploy.sh build
./deploy.sh up
```

启动后访问 `http://localhost:8685`。完整说明见 [DEPLOY.md](./DEPLOY.md)。

### 安全说明

本版本定位为个人本地工具，**默认无多用户鉴权**。请不要直接暴露到公网；如需公网访问，请先配置反向代理鉴权、HTTPS 与网络访问控制，并谨慎使用 Claude Code 的高权限模式。

### License

Apache License 2.0

---

## v0.1.0 — Initial open-source release of AgentStudio

AgentStudio is a personal AI Agent platform for managing task-capable agents, long-running conversations, scheduled automation, notifications, and agent workspace files.

This release includes a Spring Boot backend, a Umi/React frontend, and a Claude Code CLI runtime integration. Designed to run locally or on a private server.

### Features

- **Agent management**: create, enable, and configure agent providers; manage agent-local files
- **Conversation workbench**: streaming replies, archive, rename, delete
- **Task execution**: create tasks, track turns, progress, and results
- **Automation plans**: schedule periodic work via cron or a visual form
- **Notifications**: in-app messages with optional email and Bark push
- **File workspace**: view, create, and edit agent workspace files in the web UI
- **Claude Code integration**: chat and task mode powered by the Claude Code CLI, exposing platform capabilities over MCP

### Tech stack

- **Backend**: Java 21, Spring Boot 3.5.6, Spring AI, Spring Data JPA, SQLite (default)
- **Frontend**: Umi, React, TypeScript, Ant Design
- **Deploy**: Docker / Docker Compose

### Quick start

```bash
cd deploy
chmod +x deploy.sh
./deploy.sh init                                    # creates default config under ~/.agent-studio/
# Edit ~/.agent-studio/config/application-prod.yml and replace
# agents.auth.initial-token with a strong random value (see: openssl rand -hex 32)
./deploy.sh build
./deploy.sh up
```

Then open `http://localhost:8685`. See [DEPLOY.en.md](./DEPLOY.en.md) for the full guide.

### Security notice

This release is intended as a personal local tool and ships **without multi-user authentication**. Do not expose it directly to the public Internet — put a reverse proxy with auth, HTTPS, and network ACLs in front of it first, and be careful when enabling Claude Code's high-privilege mode.

### License

Apache License 2.0
