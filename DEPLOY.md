# AgentStudio 部署指南

[English](./DEPLOY.en.md) | [返回 README](./README.md)

本文面向希望直接运行 AgentStudio 的使用者，覆盖 Docker Compose 部署、运行目录、常用配置和运维命令。项目介绍和本地开发说明请查看 [README.md](./README.md)。

## 前置要求

- Docker 20.10 或更高版本
- Docker Compose 2.0 或更高版本
- 可访问 npm、Maven Central、NodeSource、Claude Code 安装源等公共网络资源

## 快速部署

进入部署目录：

```bash
cd deploy
chmod +x deploy.sh
```

构建镜像：

```bash
./deploy.sh build
```

启动服务：

```bash
./deploy.sh up
```

启动后访问：

```text
http://localhost:8685
```

## 运行目录

所有运行时数据、配置、Agent 工作区都集中在 `AGENT_STUDIO_HOME`，默认值为 `~/.agent-studio/`，与项目源码完全分离。首次执行 `./deploy.sh up`（或 `./deploy.sh init`）时脚本会创建以下结构：

```text
~/.agent-studio/
├── config/                  # Spring Boot 外置配置
│   └── application-prod.yml # 可编辑的 prod profile 配置覆盖文件
├── claude/                  # Claude Code 配置目录
│   ├── settings.json        # API Key、权限和 Claude Code 配置
│   ├── skills/              # 自定义技能定义
│   └── agents/              # 子 Agent 定义
├── agent-workspace/         # Agent 工作区
├── data/                    # SQLite 数据库文件
└── logs/                    # 应用日志
```

如需使用其他位置（例如多实例隔离或共享盘），在执行脚本前导出环境变量：

```bash
export AGENT_STUDIO_HOME=/path/to/your/agent-studio
./deploy.sh up
```

`deploy.sh` 会自动把当前 `AGENT_STUDIO_HOME` 写入 `deploy/.env`，供后续 `docker compose ...` 命令读取。该 `.env` 已被 `.gitignore` 排除，不会被提交。

这些目录会包含 API key、数据库、日志和用户工作区内容，**请勿打包或分享整个 `~/.agent-studio/`**。

## Claude Code 配置

编辑 `~/.agent-studio/claude/settings.json`，填入 Anthropic API key 和权限配置：

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

如果使用自定义 API 代理，将 `endpoint` 改为你的代理地址。该文件位于 `AGENT_STUDIO_HOME` 内，与仓库分离，请妥善保管，避免将其复制进任何会被发布或分享的位置。

## 应用配置

容器以 `prod` profile 启动，会自动加载外置的 `~/.agent-studio/config/application-prod.yml`。该文件用于覆盖镜像内置的默认配置，只需写入要覆盖的字段即可。完整默认配置位于：

```text
server/agents-app/src/main/resources/application.yml
```

`./deploy.sh up`（或 `init`）首次执行时会生成示例 `application-prod.yml` 模板，包含必填的初始化 token 占位符——请在首次启动前编辑该文件。

常用配置示例：

```yaml
agents:
  workspace-path: /home/agents/agent-workspace
  claude-code:
    cli-path: claude
    mcp-server-url: http://localhost:8685/sse
```

## 鉴权与初始化 Token

AgentStudio 使用 `agents.auth.initial-token` 作为首次创建管理员账号的初始化令牌，**该值必须由部署者自行设置**。镜像内置默认值为空，未设置时服务可正常启动，但不会自动初始化 API token，所有需要鉴权的 API 都无法调用。

请在 `~/.agent-studio/config/application-prod.yml` 中设置：

```yaml
agents:
  auth:
    initial-token: "<请替换为足够强的随机字符串>"
    allow-http-init: false
```

可使用如下命令生成随机 token：

```bash
openssl rand -hex 32
```

首次完成管理员账号初始化后，建议轮换或清空该 token，避免长期保留在配置中。

镜像默认 `agents.auth.allow-http-init=true` 仅为方便本地起步；生产部署请在 `application-prod.yml` 中显式置为 `false`，并通过反向代理强制 HTTPS。

## 数据库

默认使用 SQLite，数据库文件位于：

```text
~/.agent-studio/data/agents.db
```

如需切换为 MySQL，在 `~/.agent-studio/config/application-prod.yml` 中覆盖数据源：

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-host:3306/agents?serverTimezone=UTC
    username: your-username
    password: your-password
```

## 通知配置

下列片段可追加到 `~/.agent-studio/config/application-prod.yml` 中启用对应通道。

邮件通知示例：

```yaml
spring:
  mail:
    host: smtp.example.com
    username: your-email@example.com
    password: your-password
    port: 465
    protocol: smtps
```

Bark 推送示例：

```yaml
app.bark:
  server-url: https://api.day.app
```

OSS 对象存储示例：

```yaml
oss:
  url: http://your-minio-host:9000
  public-url: http://your-public-minio-host:9000
  username: your-access-key
  password: your-secret-key
  bucket: agents
```

## 时区

容器默认使用镜像中的时区配置。需要调整时区时，可以在启动前设置宿主机环境变量：

```bash
export TZ=Asia/Shanghai
./deploy.sh up
```

## 常用命令

| 命令 | 说明 |
|------|------|
| `./deploy.sh init` | 初始化目录和示例配置，不启动服务 |
| `./deploy.sh build` | 构建 Docker 镜像 |
| `./deploy.sh up` | 初始化并启动服务 |
| `./deploy.sh down` | 停止服务 |
| `./deploy.sh restart` | 重启服务 |
| `./deploy.sh logs` | 实时查看日志 |

## 升级

拉取新代码后重新构建并重启：

```bash
cd deploy
./deploy.sh build
./deploy.sh up
```

升级前建议备份 `~/.agent-studio/data/`、`~/.agent-studio/config/` 和 `~/.agent-studio/claude/`。

## 安全建议

- 部署前必须在 `~/.agent-studio/config/application-prod.yml` 中设置 `agents.auth.initial-token`，使用足够强的随机值，并在首次初始化完成后轮换或清空。
- 不要将 AgentStudio 直接暴露到公网。
- 如果需要公网访问，请先配置反向代理鉴权、HTTPS 和网络访问控制。
- `~/.agent-studio/` 内含 API Key、数据库、日志、Agent 工作区，请勿整体打包或上传到公开位置；同样不要将 `deploy/.env` 复制到他处。
- Claude Code 高权限模式只应在可信工作区使用。
