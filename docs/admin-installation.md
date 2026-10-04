# NGSI-LD4AI - Administration & Installation Guide

This guide describes how to deploy and configure instances of the MCP server in two supported operation styles:

1. Standalone using configuration files
2. Docker/Docker Compose using environment variables

## Runtime profile

- Component: Quarkus-based MCP server
- Default HTTP port: `9080`
- Required upstream dependency: one NGSI-LD broker endpoint (`ngsild.broker.baseurl`)

---

## 1) Standalone installation (configuration file based)

### 1.1 Prerequisites

- Java 17+
- Maven 3.9+
- Network access to the target NGSI-LD broker

### 1.2 Build

```bash
mvn clean package
```

### 1.3 Configure `application.properties`

Main file: `src/main/resources/application.properties`

At minimum, set:

```properties
quarkus.http.port=9080
ngsild.broker.baseurl=http://<broker-host>:<broker-port>
```

You can also tune or disable tool descriptions:

- `mcp.<tool-name>.description=deactivated` disables the tool
- `mcp.<tool-name>.<arg-name>=...` customizes argument-level instructions

Example:

```properties
mcp.query.description=deactivated
```

### 1.4 Start the service

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

### 1.5 Native executable (optional)

```bash
mvn clean package -Pnative
target\ngsild4ai-1.0.0-SNAPSHOT-runner.exe
```

---

## 2) Docker / Docker Compose installation (ENV-based)

The repository contains Dockerfiles under `src/main/docker`.

### 2.1 Build image (JVM example)

```bash
docker build -f src/main/docker/Dockerfile.jvm -t ngsild4ai:local .
```

### 2.2 Run with Docker and ENV injection

```bash
docker run --rm -p 9080:9080 ^
  -e NGSILD_BROKER_BASEURL=http://host.docker.internal:9090 ^
  -e QUARKUS_HTTP_PORT=9080 ^
  ngsild4ai:local
```

### 2.3 Example `docker-compose.yml`

```yaml
services:
  ngsild4ai:
    image: ngsild4ai:local
    container_name: ngsild4ai
    ports:
      - "9080:9080"
    environment:
      NGSILD_BROKER_BASEURL: "http://scorpio:9090"
      QUARKUS_HTTP_PORT: "9080"
      # Example optional tool control:
      # MCP_QUERY_DESCRIPTION: "deactivated"
    restart: unless-stopped
```

## 3) Configuration mapping rules

Quarkus property -> ENV conversion:

- Upper-case all characters
- Replace `.` with `_`

Examples:

- `ngsild.broker.baseurl` -> `NGSILD_BROKER_BASEURL`
- `quarkus.http.port` -> `QUARKUS_HTTP_PORT`
- `mcp.query.description` -> `MCP_QUERY_DESCRIPTION`

## 4) Operational checks

After startup, verify:

- The process starts without exceptions
- MCP client can list and call tools
- `getTypes` and `getEntities` return broker data

## 5) Security/production notes

- Use network policies/firewalls so the MCP server can only reach approved broker endpoints.
- Review enabled tools; disable write tools (`createEntity`, `updateEntity`, `deleteEntity`, `deleteAttribute`) when read-only behavior is required.
- Treat tool descriptions as policy controls and keep them under version control.

