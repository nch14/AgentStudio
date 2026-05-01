# AgentStudio Deployment Guide

[中文](./DEPLOY.md) | [Back to README](./README.en.md)

This guide is for users who want to run AgentStudio directly. It covers Docker Compose deployment, runtime directories, common configuration, and operations commands. For project overview and local development notes, see [README.en.md](./README.en.md).

## Prerequisites

- Docker 20.10 or later
- Docker Compose 2.0 or later
- Network access to public resources such as npm, Maven Central, NodeSource, and the Claude Code installer

## Quick Deployment

Enter the deployment directory:

```bash
cd deploy
chmod +x deploy.sh
```

Build images:

```bash
./deploy.sh build
```

Start the service:

```bash
./deploy.sh up
```

After startup, open:

```text
http://localhost:8685
```

## Runtime Directories

All runtime data, config, and Agent workspace files live under `AGENT_STUDIO_HOME`, which defaults to `~/.agent-studio/` and is fully separated from the project source tree. The first `./deploy.sh up` (or `./deploy.sh init`) creates the following layout:

```text
~/.agent-studio/
├── config/                  # Spring Boot externalized config
│   └── application-prod.yml # Editable prod-profile config overrides
├── claude/                  # Claude Code config directory
│   ├── settings.json        # API key, permissions, and Claude Code config
│   ├── skills/              # Custom skill definitions
│   └── agents/              # Sub-agent definitions
├── agent-workspace/         # Agent workspace
├── data/                    # SQLite database files
└── logs/                    # Application logs
```

To use a different location (e.g. for multi-instance isolation or a shared volume), export the environment variable before running the script:

```bash
export AGENT_STUDIO_HOME=/path/to/your/agent-studio
./deploy.sh up
```

`deploy.sh` writes the resolved `AGENT_STUDIO_HOME` to `deploy/.env`, which is consumed by subsequent `docker compose ...` commands. That `.env` file is ignored by `.gitignore` and is never committed.

These directories will contain API keys, databases, logs, and user workspace files — **do not archive or share `~/.agent-studio/` as a whole.**

## Claude Code Configuration

Edit `~/.agent-studio/claude/settings.json` to add your Anthropic API key and permissions:

```json
{
  "apiKey": "sk-ant-xxx",
  "api": {
    "endpoint": "https://api.anthropic.com"
  },
  "permissions": {
    "allow": [],
    "deny": []
  }
}
```

If you use a custom API proxy, replace `endpoint` with your proxy URL. The file lives inside `AGENT_STUDIO_HOME`, separate from the source tree — keep it private and avoid copying it into anywhere that may be published or shared.

## Application Configuration

The container starts with the `prod` Spring profile and automatically loads `~/.agent-studio/config/application-prod.yml`. This file overrides the defaults bundled inside the image — only include the keys you want to change. The full default config is located at:

```text
server/agents-app/src/main/resources/application.yml
```

The first run of `./deploy.sh up` (or `init`) generates a starter `application-prod.yml` template containing the required initial-token placeholder. **Edit this file before exposing the service.**

Common configuration example:

```yaml
agents:
  workspace-path: /home/agents/agent-workspace
  claude-code:
    cli-path: claude
    mcp-server-url: http://localhost:8685/sse
```

## Authentication and Initial Token

AgentStudio uses `agents.auth.initial-token` as the bootstrap token for creating the first administrator account. **This value must be supplied by the deployer.** The image ships with an empty default; when it is empty, the service still starts but no API token is provisioned, so any authenticated API call will fail.

Set it in `~/.agent-studio/config/application-prod.yml`:

```yaml
agents:
  auth:
    initial-token: "<replace with a strong random string>"
    allow-http-init: false
```

Generate a random token with:

```bash
openssl rand -hex 32
```

After the first administrator account is created, rotate or clear the token so it does not stay in your configuration long-term.

The image default `agents.auth.allow-http-init=true` exists only for ease of local bootstrap. For production deployments, set it explicitly to `false` in `application-prod.yml` and enforce HTTPS via a reverse proxy.

## Database

SQLite is used by default. The database file is stored at:

```text
~/.agent-studio/data/agents.db
```

To switch to MySQL, override the datasource in `~/.agent-studio/config/application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-host:3306/agents?serverTimezone=UTC
    username: your-username
    password: your-password
```

## Notifications

The snippets below can be appended to `~/.agent-studio/config/application-prod.yml` to enable each channel.

Email notification example:

```yaml
spring:
  mail:
    host: smtp.example.com
    username: your-email@example.com
    password: your-password
    port: 465
    protocol: smtps
```

Bark push example:

```yaml
app.bark:
  server-url: https://api.day.app
```

OSS object storage example:

```yaml
oss:
  url: http://your-minio-host:9000
  public-url: http://your-public-minio-host:9000
  username: your-access-key
  password: your-secret-key
  bucket: agents
```

## Timezone

The container uses the timezone configured in the image by default. To override it, set the host environment variable before startup:

```bash
export TZ=America/New_York
./deploy.sh up
```

## Commands

| Command | Description |
|---------|-------------|
| `./deploy.sh init` | Initialize directories and example configs without starting |
| `./deploy.sh build` | Build Docker images |
| `./deploy.sh up` | Initialize and start the service |
| `./deploy.sh down` | Stop the service |
| `./deploy.sh restart` | Restart the service |
| `./deploy.sh logs` | Follow live logs |

## Upgrade

After pulling new code, rebuild and restart:

```bash
cd deploy
./deploy.sh build
./deploy.sh up
```

Before upgrading, back up `~/.agent-studio/data/`, `~/.agent-studio/config/`, and `~/.agent-studio/claude/`.

## Security Recommendations

- Before deploying, set `agents.auth.initial-token` in `~/.agent-studio/config/application-prod.yml` to a strong random value, and rotate or clear it after the first bootstrap.
- Do not expose AgentStudio directly to the public internet.
- If public access is required, configure reverse-proxy authentication, HTTPS, and network access controls first.
- `~/.agent-studio/` holds API keys, the database, logs, and the Agent workspace — do not archive or upload it as a whole, and do not copy `deploy/.env` elsewhere.
- Claude Code high-privilege mode should only be used in trusted workspaces.
