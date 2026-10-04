# NGSI-LD4AI - Quick Start

This quick start runs the MCP server locally and connects it to an NGSI-LD broker (for example, Scorpio Broker).

## Prerequisites

- Java 17+
- Maven 3.9+
- A reachable NGSI-LD broker endpoint (example: `http://localhost:9090`)
- Optional: Claude Desktop or another MCP client

## 1) Build

From the project root:

```bash
mvn clean package
```

The runnable artifact is:

- `target/quarkus-app/quarkus-run.jar`

## 2) Configure broker connection

Set the broker base URL so that the MCP server can forward NGSI-LD requests.

### Option A - Environment variable

```bash
# Linux/macOS
export NGSILD_BROKER_BASEURL=http://localhost:9090

# Windows (PowerShell)
$env:NGSILD_BROKER_BASEURL="http://localhost:9090"

# Windows (cmd)
set NGSILD_BROKER_BASEURL=http://localhost:9090
```

### Option B - `application.properties`

Edit `src/main/resources/application.properties`:

```properties
ngsild.broker.baseurl=http://localhost:9090
```

## 3) Start the MCP server

### Java mode

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

### Native mode (optional)

```bash
mvn clean package -Pnative
target\ngsild4ai-1.0.0-SNAPSHOT-runner.exe
```

## 4) Connect from Claude Desktop (StdIO)

In `claude_desktop_config.json`, add:

```json
{
  "mcpServers": {
    "ngsild4ai": {
      "command": "java",
      "args": ["-jar", "<absolute-path>\\target\\quarkus-app\\quarkus-run.jar"],
      "initializationTimeout": 10000
    }
  }
}
```

For native builds, replace `command` with the executable path and remove `args`.

## 5) Smoke test in the MCP client

Run these tools in sequence:

1. `getTypes`
2. `getEntities` with `type=<someType>` (or no arguments)
3. `createEntity` using a minimal NGSI-LD entity payload

If these return expected data, your setup is ready.

