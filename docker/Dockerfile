# syntax=docker/dockerfile:1

# Stage 1: Build frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /app/webui
COPY webui/package.json ./
RUN npm install
COPY webui/ .
RUN npm run build

# Stage 2: Build backend
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app/server
COPY server/pom.xml .
COPY server/agents-common agents-common
COPY server/agents-domain agents-domain
COPY server/agents-connect agents-connect
COPY server/agents-claude-code agents-claude-code
COPY server/agents-app agents-app
RUN mvn clean package -DskipTests

# Stage 3: Runtime on base image
FROM agents-base:latest AS runtime
COPY --from=backend-builder /app/server/agents-app/target/agents-app-*.jar ${APP_DIR}/app.jar
COPY --from=frontend-builder /app/webui/dist ${APP_DIR}/static/frontend
RUN mkdir -p ${APP_DIR}/data && \
    chown -R ${APP_USER}:${APP_USER} ${APP_DIR}/data

ENV SPRING_DATASOURCE_URL=jdbc:sqlite:${APP_DIR}/data/agents.db
EXPOSE 8685
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
