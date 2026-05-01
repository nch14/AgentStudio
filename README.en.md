# AgentStudio

[中文](./README.md)

AgentStudio is a personal AI Agent platform for managing task-capable agents, long-running conversations, scheduled automation, notifications, and agent workspace files. The public release includes a Spring Boot backend, a Umi/React frontend, and a Claude Code CLI runtime integration.

It is designed to run on a local machine or private server, helping an individual maintain a set of agents that can chat, execute tasks, and work on schedules.

## Features

- **Agent Management**: Create, enable, and configure agent providers, and maintain agent-local files.
- **Conversation Workspace**: Manage sessions with streaming replies, archiving, renaming, and deletion.
- **Task Execution**: Create tasks and track turns, progress, and execution results.
- **Automation Plans**: Schedule recurring tasks with cron expressions or a visual form builder.
- **Notification Center**: Use in-app notifications, with optional email and Bark push channels.
- **File Workspace**: View, create, and edit agent workspace files from the Web UI.
- **Claude Code Integration**: Run chat and task modes through Claude Code CLI and expose platform capabilities through MCP.

## Quick Start

Docker Compose is recommended for running the full application:

```bash
cd deploy
chmod +x deploy.sh
./deploy.sh build
./deploy.sh up
```

After startup, open `http://localhost:8685`.

For full deployment, configuration, and upgrade instructions, read the [deployment guide](./DEPLOY.en.md). For local development, see the development notes below.

## Project Structure

```text
AgentStudio/
├── server/             # Spring Boot multi-module backend
├── webui/              # Umi / React / TypeScript / Ant Design frontend
├── docker/             # Docker image build files
├── deploy/             # Docker Compose deployment scripts
├── DEPLOY.md           # 部署指南
├── DEPLOY.en.md        # Deployment guide
├── README.md           # 中文入口
└── README.en.md        # English entry
```

Server modules:

- `agents-common`: Common configuration, OSS, notifications, and utilities.
- `agents-domain`: Domain models, repository interfaces, and domain services.
- `agents-connect`: Agent provider SPI, MCP tools, and platform capability interfaces.
- `agents-claude-code`: Claude Code CLI process, prompts, workspace, and provider implementation.
- `agents-app`: HTTP APIs, application services, schedulers, and application entry point.

## Local Development

### Backend

Requirements:

- JDK 21
- Maven 3.9+

```bash
cd server
mvn clean package
mvn -pl agents-app spring-boot:run
```

The backend runs on port `8685`. Swagger UI is available at:

```text
http://localhost:8685/swagger-ui.html
```

### Frontend

Requirements:

- Node.js 22+
- npm

```bash
cd webui
npm install
npm run dev
```

The development server proxies `/api` requests to `http://localhost:8685`.

## Security

AgentStudio is designed as a personal local-use tool and does not include a multi-user authentication model by default. Do not expose it directly to the public internet. For public access, set up a reverse proxy with authentication, network access controls, and stricter Claude Code permission configuration.

If enabling high-privilege mode in Claude Code, only use it within trusted workspaces and avoid committing API keys, database credentials, logs, and agent workspace files to public repositories.

## License

This project is licensed under the Apache License 2.0.
