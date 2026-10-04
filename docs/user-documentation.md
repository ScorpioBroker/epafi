# NGSI-LD4AI - User Documentation (GUI Usage)

This document describes how end users interact with the MCP server through GUI-based MCP clients.

## 1) Supported GUI clients

Typical options:

- Claude Desktop (recommended for everyday use)
- MCP Inspector (recommended for testing and tool-level debugging)
- Other MCP-compatible GUI clients

## 2) Using Claude Desktop

### 2.1 Add server configuration

Open Claude Desktop configuration (`claude_desktop_config.json`) and add:

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

For a native executable:

- Use `command` = full path to executable
- Remove `args`

### 2.2 Start and verify

1. Restart Claude Desktop.
2. Open a new chat.
3. Ask the agent to call `getTypes`.
4. Confirm the returned types come from your broker.

## 3) Suggested user workflow

1. Discover available data:
   - run `getTypes`
   - run `getEntities`
2. Create or update context data:
   - run `createEntity` or `updateEntity`
3. Validate outcome:
   - rerun `getEntities` with filters

## 4) Common prompts for GUI users

- "List all available NGSI-LD entity types."
- "Show entities of type Vehicle in area X."
- "Create a new AirQualityObserved entity from this sensor data."
- "Update NO2 value for entity `<entity-id>`."
- "Draw a chart of the queried values."

## 5) Safety and governance guidance

- The server can modify broker data if write tools are enabled.
- For read-only usage, ask an administrator to disable write tools:
  - `createEntity`
  - `updateEntity`
  - `deleteEntity`
  - `deleteAttribute`
- Always review generated payloads before execution in production environments.

## 6) Troubleshooting

- No tools visible:
  - Check server process is running.
  - Check config path and JSON syntax.
- Empty query results:
  - Verify `ngsild.broker.baseurl`.
  - Check broker data and tenant/context settings.
- Timeouts:
  - Keep `initializationTimeout` at or above `10000`.
  - Check connectivity between MCP server and broker.

