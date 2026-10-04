# NGSI-LD4AI - Connect LLMs to NGSI-LD
![](https://badge.mcpx.dev?type=server 'MCP Server')
[![License: BSD-3-Clause](https://img.shields.io/badge/license-BSD%203%20Clause-blue.svg)](https://spdx.org/licenses/BSD-3-Clause.html)
![NGSI-LD badge](https://img.shields.io/badge/NGSI-LD-red.svg) 

Welcome to NGSI-LD4AI, a Model Context Protocol (MCP) server for NGSI-LD with Smart Data Models support.

NGSI-LD4AI provides access to any NGSI-LD compliant broker (for example [Scorpio Broker](https://github.com/ScorpioBroker/ScorpioBroker)) and guides the LLM to use NGSI-LD correctly.

Example conversation:
![Claude conversation](img/screenshot-claude.png)

## Attention

This server can let an LLM modify broker data, so data loss is possible. You can deactivate tools through configuration.

## Features

- Support for all transport modes (StdIO, HTTP-SSE, Streamable HTTP)
- Full NGSI-LD query interface via MCP (based on NGSI-LD v1.8.1)
- Entity type and attribute lookup for better request grounding
- Entity create and update with support for NGSI-LD attribute types
- Entity and attribute delete (including dataset IDs)
- Address-to-coordinates lookup via OSM (OpenStreetMap)
- Available Smart Data Model domains ([smartdatamodels.org](https://smartdatamodels.org/index.php/domains/))
- Available Smart Data Models in a domain
- Smart Data Model schema retrieval
- Feature-level deactivation through configuration

## Documentation

- [Quick Start](docs/quick-start.md)
- [Administration & Installation](docs/admin-installation.md)
- [Developer Documentation](docs/developer-documentation.md)
- [User Documentation (GUI)](docs/user-documentation.md)
- [Documentation Index](docs/README.md)

## Configuration

Please look at `src/main/resources/application.properties` for all configuration properties.

Tool description configuration follows this pattern:

- `mcp.<internal_tool_name>.description` for the tool description; set to `deactivated` to disable the tool
- `mcp.<internal_tool_name>.<tool_arg_name>` for tool argument descriptions

You can also set these properties through environment variables (upper case and `_` instead of `.`) or command line options.
For details, see [Quarkus Configuration Reference](https://quarkus.io/guides/config-reference).




## Building

NGSI-LD4AI is written using Quarkus and Quarkiverse and supports native building.
To disable one transport mode, remove or comment the related dependency in `pom.xml`.
Java build
```
mvn clean package 
```
Native build
```
mvn clean package -Pnative
```
## Claude Desktop config

For details, see the [MCP quickstart](https://modelcontextprotocol.io/quickstart/user).

In Claude Desktop, go to `File -> Settings -> Developer -> Edit Config`, then edit `claude_desktop_config.json` and add:
```
{
  "mcpServers": {
    "ngsild4ai": {
      "command": "java",
      "args": ["-jar", "<Your build folder>/quarkus-app/quarkus-run.jar"],
      "initializationTimeout": 10000
    }
  }
}
```
If you are using a native executable, call it directly in `command`:
```
{
  "mcpServers": {
    "ngsild4ai": {
      "command": "<path to ngsild4ai>/ngsild4ai.exe",
      "initializationTimeout": 10000
    }
  }
}
```

`initializationTimeout` is currently required because Claude does not yet handle `list_update` notifications.
## FAQ

Why aren't you using the @Tool and @ToolArgs annotations?

We used them initially and they are a great tool. At the time of development, they did not support linking to configuration parameters via expression language, and high configurability was an important aspect for this project.

## License

NGSI-LD4AI is licensed under [BSD-3-Clause](https://spdx.org/licenses/BSD-3-Clause.html).
For contributions this [contribution license](CONTRIBUTING.md) applies.

© 2026 NEC
