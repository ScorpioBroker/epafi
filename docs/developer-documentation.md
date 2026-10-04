# NGSI-LD4AI - Developer Documentation

This document explains how to call the MCP server programmatically and which tool APIs are available.

## 1) Integration model

`NGSI-LD4AI` is an MCP server. Programmatic access is done through MCP clients (for example SDKs or desktop clients) that can call tools exposed by this service.

Internally, those tool calls are mapped to NGSI-LD HTTP API calls against the configured broker (`ngsild.broker.baseurl`).

## 2) Startup for development

```bash
mvn quarkus:dev
```

Or package and run:

```bash
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## 3) Core tool API

### 3.1 Context discovery and query

- `getTypes()`
- `getEntities(...)`

`getEntities` supports common NGSI-LD query arguments, including:

- `id`, `type`, `idPattern`
- `attrs`, `q`
- `geometry`, `georel`, `coordinates`, `geoproperty`, `geometryProperty`
- `lang`, `scopeQ`, `datasetId`, `join`, `joinLevel`, `jsonKeys`, `omit`, `pick`

### 3.2 Entity lifecycle

- `createEntity(entity)`
- `updateEntity(entityFragment, entityId, updateExistingAttributes)`
- `deleteEntity(entityId)`
- `deleteAttribute(entityId, attrib, datasetId?, deleteAll?)`

### 3.3 Smart Data Models helpers

- `getAvailableDomains()`
- `getAvailableDatamodels(domainName)`
- `getDatamodel(repoName, dataModelName)`

### 3.4 Geospatial helper

- `getCoordinatesForAddress(address)`

### 3.5 Visualization and graphing helpers

The server also exposes chart/map/image tools such as:

- `createXYChart`, `createCategoryChart`, `createPieChart`, `createBubbleChart`
- `createDialChart`, `createRadarChart`, `createOHLCChart`, `createBoxChart`, `createHeatMapChart`
- `createMapImage`
- `generateEntityGraph`

## 4) End-to-end examples

### Example A: Query entities

1. Call `getTypes` to discover available entity types.
2. Call `getEntities` with a selected type:
   - `type = "AirQualityObserved"`
   - optional `q = "NO2>20"`

Expected result: JSON payload from `/ngsi-ld/v1/entities`.

### Example B: Create + update entity

1. Call `createEntity` with normalized NGSI-LD JSON (`id`, `type`, attributes, `@context`).
2. Call `updateEntity` with `entityId` and an attribute fragment.
3. Set `updateExistingAttributes=true` to overwrite existing values.

### Example C: Smart Data Models assisted generation

1. Call `getAvailableDomains`.
2. Call `getAvailableDatamodels(domainName)`.
3. Call `getDatamodel(repoName, dataModelName)` to retrieve schema JSON.
4. Build an entity matching the schema and call `createEntity`.

## 5) Configuration controls relevant to developers

- `ngsild.broker.baseurl`: target NGSI-LD broker
- `mcp.<tool>.description=deactivated`: disable tools
- `mcp.<tool>.<arg>`: refine argument instruction text passed to LLM clients

## 6) Error handling behavior

- For create/update/delete operations, success returns an empty string.
- On upstream broker errors, response body text is returned.
- Always validate empty-string success in client code.

## 7) Best practices

- Resolve types first with `getTypes` before generating queries.
- Include `@context` in entity payloads.
- Apply least privilege by disabling write tools when not required.

