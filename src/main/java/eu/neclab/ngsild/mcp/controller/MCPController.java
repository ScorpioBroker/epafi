package eu.neclab.ngsild.mcp.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class MCPController {

	private static final Logger logger = LoggerFactory.getLogger(MCPController.class);

	private static final String DEACTIVATED = "deactivated";

	@ConfigProperty(name = "ngsild.broker.baseurl", defaultValue = "http://localhost:9090")
	String baseUrl;

	@ConfigProperty(name = "mcp.types.description")
	String mcpTypesDescription;

	@ConfigProperty(name = "mcp.query.description")
	String mcpQueryDescription;

	@ConfigProperty(name = "mcp.query.attrs")
	String mcpQueryAttrsDescription;

	@ConfigProperty(name = "mcp.query.coordinates")
	String mcpQueryCoordinatesDescription;

	@ConfigProperty(name = "mcp.query.csf")
	String mcpQueryCsfDescription;

	@ConfigProperty(name = "mcp.query.datasetId")
	String mcpQueryDatasetIdDescription;

	@ConfigProperty(name = "mcp.query.geometry")
	String mcpQueryGeometryDescription;

	@ConfigProperty(name = "mcp.query.geometryProperty")
	String mcpQueryGeometryPropertyDescription;

	@ConfigProperty(name = "mcp.query.geoproperty")
	String mcpQueryGeopropertyDescription;

	@ConfigProperty(name = "mcp.query.georel")
	String mcpQueryGeorelDescription;

	@ConfigProperty(name = "mcp.query.id")
	String mcpQueryIdDescription;

	@ConfigProperty(name = "mcp.query.idPattern")
	String mcpQueryIdPatternDescription;

	@ConfigProperty(name = "mcp.query.join")
	String mcpQueryJoinDescription;

	@ConfigProperty(name = "mcp.query.joinLevel")
	String mcpQueryJoinLevelDescription;

	@ConfigProperty(name = "mcp.query.jsonKeys")
	String mcpQueryJsonKeysDescription;

	@ConfigProperty(name = "mcp.query.lang")
	String mcpQueryLangDescription;

	@ConfigProperty(name = "mcp.query.omit")
	String mcpQueryOmitDescription;

	@ConfigProperty(name = "mcp.query.pick")
	String mcpQueryPickDescription;

	@ConfigProperty(name = "mcp.query.q")
	String mcpQueryQDescription;

	@ConfigProperty(name = "mcp.query.scopeQ")
	String mcpQueryScopeQDescription;

	@ConfigProperty(name = "mcp.query.type")
	String mcpQueryTypeDescription;

	// Temporal query configuration properties
	@ConfigProperty(name = "mcp.temporal.query.description")
	String mcpTemporalQueryDescription;

	@ConfigProperty(name = "mcp.temporal.query.timerel")
	String mcpTemporalTimerelDescription;

	@ConfigProperty(name = "mcp.temporal.query.timeAt")
	String mcpTemporalTimeAtDescription;

	@ConfigProperty(name = "mcp.temporal.query.endTimeAt")
	String mcpTemporalEndTimeAtDescription;

	@ConfigProperty(name = "mcp.temporal.query.timeproperty")
	String mcpTemporalTimepropertyDescription;

	@ConfigProperty(name = "mcp.temporal.query.aggrMethods")
	String mcpTemporalAggrMethodsDescription;

	@ConfigProperty(name = "mcp.temporal.query.aggrPeriodDuration")
	String mcpTemporalAggrPeriodDurationDescription;

	@ConfigProperty(name = "mcp.temporal.query.lastN")
	String mcpTemporalLastNDescription;

	// Create entity properties
	@ConfigProperty(name = "mcp.create.description")
	String mcpCreateDescription;

	@ConfigProperty(name = "mcp.create.entity")
	String mcpCreateEntityDescription;

	// Update entity properties
	@ConfigProperty(name = "mcp.update.description")
	String mcpUpdateDescription;

	@ConfigProperty(name = "mcp.update.entity")
	String mcpUpdateEntityDescription;

	@ConfigProperty(name = "mcp.update.entityid")
	String mcpUpdateEntityIdDescription;

	@ConfigProperty(name = "mcp.update.updateexistngattrs")
	String mcpUpdateExistingAttrsDescription;

	// Delete entity properties
	@ConfigProperty(name = "mcp.delete.description")
	String mcpDeleteDescription;

	@ConfigProperty(name = "mcp.delete.entityid")
	String mcpDeleteEntityIdDescription;

	// Delete attribute properties
	@ConfigProperty(name = "mcp.deleteattrib.description")
	String mcpDeleteAttribDescription;

	@ConfigProperty(name = "mcp.deleteattrib.entityid")
	String mcpDeleteAttribEntityIdDescription;

	@ConfigProperty(name = "mcp.deleteattrib.attrib")
	String mcpDeleteAttribAttribDescription;

	@ConfigProperty(name = "mcp.deleteattrib.datasetid")
	String mcpDeleteAttribDatasetIdDescription;

	@ConfigProperty(name = "mcp.deleteattrib.deleteall")
	String mcpDeleteAttribDeleteAllDescription;

	// SDM Domains properties
	@ConfigProperty(name = "mcp.sdm.domains.description")
	String mcpSdmDomainsDescription;

	// SDM Data models properties
	@ConfigProperty(name = "mcp.sdm.datamodels.description")
	String mcpSdmDatamodelsDescription;

	@ConfigProperty(name = "mcp.sdm.datamodels.domain")
	String mcpSdmDatamodelsDomainDescription;

	// SDM Data model properties
	@ConfigProperty(name = "mcp.sdm.datamodel.description")
	String mcpSdmDatamodelDescription;

	@ConfigProperty(name = "mcp.sdm.datamodel.reponame")
	String mcpSdmDatamodelReponameDescription;

	@ConfigProperty(name = "mcp.sdm.datamodel.datamodelname")
	String mcpSdmDatamodelDatamodelnameDescription;

	// Geotools properties
	@ConfigProperty(name = "mcp.geotools.address2coordinates.description")
	String mcpGeotoolsAddress2coordinatesDescription;

	@ConfigProperty(name = "mcp.geotools.address2coordinates.address")
	String mcpGeotoolsAddress2coordinatesAddressDescription;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	Vertx vertx;

	@Inject
	ToolManager toolManager;

	WebClient webClient;

	@Startup
	void setup() {
		logger.info("Init");
		webClient = WebClient.create(vertx);

		registerTools();

		logger.info("Init done");

	}

	private void registerTools() {
		registerGetTypesTool();
		registerGetEntitesTool();
		registerCreateEntityTool();
		registerUpdateEntityTool();
		registerDeleteEntityTool();
		registerDeleteAttributeTool();
		registerGetAvailableDomainsTool();
		registerGetAvailableDatamodelsTool();
		registerGetDatamodelTool();
		registerGetCoordinatesForAddressTool();
	}

	private void registerGetTemporalEntitiesTool() {
		if (mcpTemporalQueryDescription != null && !mcpTemporalQueryDescription.equals(DEACTIVATED)) {
			toolManager.newTool("getTemporalEntities").setDescription(mcpTemporalQueryDescription)
					.addArgument("attrs", mcpQueryAttrsDescription, false, String.class)
					.addArgument("coordinates", mcpQueryCoordinatesDescription, false, String.class)
					.addArgument("csf", mcpQueryCsfDescription, false, String.class)
					.addArgument("datasetId", mcpQueryDatasetIdDescription, false, String.class)
					.addArgument("geometry", mcpQueryGeometryDescription, false, String.class)
					.addArgument("geometryProperty", mcpQueryGeometryPropertyDescription, false, String.class)
					.addArgument("geoproperty", mcpQueryGeopropertyDescription, false, String.class)
					.addArgument("georel", mcpQueryGeorelDescription, false, String.class)
					.addArgument("id", mcpQueryIdDescription, false, String.class)
					.addArgument("idPattern", mcpQueryIdPatternDescription, false, String.class)
					.addArgument("join", mcpQueryJoinDescription, false, String.class)
					.addArgument("joinLevel", mcpQueryJoinLevelDescription, false, Integer.class)
					.addArgument("jsonKeys", mcpQueryJsonKeysDescription, false, String.class)
					.addArgument("lang", mcpQueryLangDescription, false, String.class)
					.addArgument("omit", mcpQueryOmitDescription, false, String.class)
					.addArgument("pick", mcpQueryPickDescription, false, String.class)
					.addArgument("q", mcpQueryQDescription, false, String.class)
					.addArgument("scopeQ", mcpQueryScopeQDescription, false, String.class)
					.addArgument("type", mcpQueryTypeDescription, false, String.class)
					// Temporal query parameters
					.addArgument("timerel", mcpTemporalTimerelDescription, false, String.class)
					.addArgument("timeAt", mcpTemporalTimeAtDescription, false, String.class)
					.addArgument("endTimeAt", mcpTemporalEndTimeAtDescription, false, String.class)
					.addArgument("timeproperty", mcpTemporalTimepropertyDescription, false, String.class)
					.addArgument("aggrMethods", mcpTemporalAggrMethodsDescription, false, String.class)
					.addArgument("aggrPeriodDuration", mcpTemporalAggrPeriodDurationDescription, false, String.class)
					.addArgument("lastN", mcpTemporalLastNDescription, false, String.class)
					.setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String attrs = (String) parameters.get("attrs");
						String coordinates = (String) parameters.get("coordinates");
						String csf = (String) parameters.get("csf");
						String datasetId = (String) parameters.get("datasetId");
						String geometry = (String) parameters.get("geometry");
						String geometryProperty = (String) parameters.get("geometryProperty");
						String geoproperty = (String) parameters.get("geoproperty");
						String georel = (String) parameters.get("georel");
						String id = (String) parameters.get("id");
						String idPattern = (String) parameters.get("idPattern");
						String join = (String) parameters.get("join");
						Integer joinLevel = (Integer) parameters.get("joinLevel");
						String jsonKeys = (String) parameters.get("jsonKeys");
						String lang = (String) parameters.get("lang");
						String omit = (String) parameters.get("omit");
						String pick = (String) parameters.get("pick");
						String q = (String) parameters.get("q");
						String scopeQ = (String) parameters.get("scopeQ");
						String type = (String) parameters.get("type");
						// Temporal parameters
						String timerel = (String) parameters.get("timerel");
						String timeAt = (String) parameters.get("timeAt");
						String endTimeAt = (String) parameters.get("endTimeAt");
						String timeproperty = (String) parameters.get("timeproperty");
						String aggrMethods = (String) parameters.get("aggrMethods");
						String aggrPeriodDuration = (String) parameters.get("aggrPeriodDuration");
						String lastN = (String) parameters.get("lastN");

						return getTemporalEntities(attrs, coordinates, csf, datasetId, geometry, geometryProperty,
								geoproperty, georel, id, idPattern, join, joinLevel, jsonKeys, lang, omit, pick,
								q, scopeQ, type, timerel, timeAt, endTimeAt, timeproperty, aggrMethods,
								aggrPeriodDuration, lastN)
								.onItem().transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	private void registerGetEntitesTool() {
		if (mcpQueryDescription != null && !mcpQueryDescription.equals(DEACTIVATED)) {
			toolManager.newTool("getEntities").setDescription(mcpQueryDescription)
					.addArgument("attrs", mcpQueryAttrsDescription, false, String.class)
					.addArgument("coordinates", mcpQueryCoordinatesDescription, false, String.class)
					.addArgument("csf", mcpQueryCsfDescription, false, String.class)
					.addArgument("datasetId", mcpQueryDatasetIdDescription, false, String.class)
					.addArgument("geometry", mcpQueryGeometryDescription, false, String.class)
					.addArgument("geometryProperty", mcpQueryGeometryPropertyDescription, false, String.class)
					.addArgument("geoproperty", mcpQueryGeopropertyDescription, false, String.class)
					.addArgument("georel", mcpQueryGeorelDescription, false, String.class)
					.addArgument("id", mcpQueryIdDescription, false, String.class)
					.addArgument("idPattern", mcpQueryIdPatternDescription, false, String.class)
					.addArgument("join", mcpQueryJoinDescription, false, String.class)
					.addArgument("joinLevel", mcpQueryJoinLevelDescription, false, Integer.class)
					.addArgument("jsonKeys", mcpQueryJsonKeysDescription, false, String.class)
					.addArgument("lang", mcpQueryLangDescription, false, String.class)
					.addArgument("omit", mcpQueryOmitDescription, false, String.class)
					.addArgument("pick", mcpQueryPickDescription, false, String.class)
					.addArgument("q", mcpQueryQDescription, false, String.class)
					.addArgument("scopeQ", mcpQueryScopeQDescription, false, String.class)
					.addArgument("type", mcpQueryTypeDescription, false, String.class).setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String attrs = (String) parameters.get("attrs");
						String coordinates = (String) parameters.get("coordinates");
						String csf = (String) parameters.get("csf");
						String datasetId = (String) parameters.get("datasetId");
						String geometry = (String) parameters.get("geometry");
						String geometryProperty = (String) parameters.get("geometryProperty");
						String geoproperty = (String) parameters.get("geoproperty");
						String georel = (String) parameters.get("georel");
						String id = (String) parameters.get("id");
						String idPattern = (String) parameters.get("idPattern");
						String join = (String) parameters.get("join");
						Integer joinLevel = (Integer) parameters.get("joinLevel");
						String jsonKeys = (String) parameters.get("jsonKeys");
						String lang = (String) parameters.get("lang");
						String omit = (String) parameters.get("omit");
						String pick = (String) parameters.get("pick");
						String q = (String) parameters.get("q");
						String scopeQ = (String) parameters.get("scopeQ");
						String type = (String) parameters.get("type");

						return getEntities(attrs, coordinates, csf, datasetId, geometry, geometryProperty, geoproperty,
								georel, id, idPattern, join, joinLevel, jsonKeys, lang, omit, pick, q, scopeQ, type)
								.onItem().transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	public Uni<String> getTemporalEntities(String attrs, String coordinates, String csf, String datasetId,
			String geometry, String geometryProperty, String geoproperty, String georel, String id,
			String idPattern, String join, Integer joinLevel, String jsonKeys, String lang, String omit,
			String pick, String q, String scopeQ, String type, String timerel, String timeAt, String endTimeAt,
			String timeproperty, String aggrMethods, String aggrPeriodDuration, String lastN) {

		HttpRequest<Buffer> tmp = webClient.getAbs(baseUrl + "/ngsi-ld/v1/temporal/entities")
				.putHeader("Accept", "application/json");

		// Original entity query parameters
		if (id != null) {
			tmp.addQueryParam("id", id);
		}
		if (type != null) {
			tmp.addQueryParam("type", type);
		}
		if (idPattern != null) {
			tmp.addQueryParam("idPattern", idPattern);
		}
		if (attrs != null) {
			tmp.addQueryParam("attrs", attrs);
		}
		if (q != null) {
			tmp.addQueryParam("q", q);
		}
		if (geometry != null) {
			tmp.addQueryParam("geometry", geometry);
		}
		if (georel != null) {
			tmp.addQueryParam("georel", georel);
		}
		if (coordinates != null) {
			tmp.addQueryParam("coordinates", coordinates);
		}
		if (geoproperty != null) {
			tmp.addQueryParam("geoproperty", geoproperty);
		}
		if (geometryProperty != null) {
			tmp.addQueryParam("geometryProperty", geometryProperty);
		}
		if (lang != null) {
			tmp.addQueryParam("lang", lang);
		}
		if (scopeQ != null) {
			tmp.addQueryParam("scopeQ", scopeQ);
		}
		if (csf != null) {
			tmp.addQueryParam("csf", csf);
		}
		if (datasetId != null) {
			tmp.addQueryParam("datasetId", datasetId);
		}
		if (join != null) {
			tmp.addQueryParam("join", join);
		}
		if (joinLevel != null) {
			tmp.addQueryParam("joinLevel", joinLevel.toString());
		}
		if (jsonKeys != null) {
			tmp.addQueryParam("jsonKeys", jsonKeys);
		}
		if (omit != null) {
			tmp.addQueryParam("omit", omit);
		}
		if (pick != null) {
			tmp.addQueryParam("pick", pick);
		}

		// Temporal query parameters
		if (timerel != null) {
			tmp.addQueryParam("timerel", timerel);
		}
		if (timeAt != null) {
			tmp.addQueryParam("timeAt", timeAt);
		}
		if (endTimeAt != null) {
			tmp.addQueryParam("endTimeAt", endTimeAt);
		}
		if (timeproperty != null) {
			tmp.addQueryParam("timeproperty", timeproperty);
		}
		if (aggrMethods != null) {
			tmp.addQueryParam("aggrMethods", aggrMethods);
		}
		if (aggrPeriodDuration != null) {
			tmp.addQueryParam("aggrPeriodDuration", aggrPeriodDuration);
		}
		if (lastN != null) {
			tmp.addQueryParam("lastN", lastN);
		}

		tmp.addQueryParam("limit", "1000");
		return tmp.send().onItem().transform(resp -> resp.bodyAsString());
	}

	private void registerGetTypesTool() {
		if (mcpTypesDescription != null && !mcpTypesDescription.equals(DEACTIVATED)) {
			toolManager.newTool("getTypes").setDescription(mcpTypesDescription).setAsyncHandler(ta -> {
				return getTypes().onItem().transform(t -> ToolResponse.success(t));
			}).register();
		}
	}

	private void registerCreateEntityTool() {
		if (mcpCreateDescription != null && !mcpCreateDescription.equals(DEACTIVATED)) {
			toolManager.newTool("createEntity").setDescription(mcpCreateDescription)
					.addArgument("entity", mcpCreateEntityDescription, true, String.class).setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String entity = (String) parameters.get("entity");

						return createEntity(entity).onItem().transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	private void registerUpdateEntityTool() {
		if (mcpUpdateDescription != null && !mcpUpdateDescription.equals(DEACTIVATED)) {
			toolManager.newTool("updateEntity").setDescription(mcpUpdateDescription)
					.addArgument("entityFragment", mcpUpdateEntityDescription, true, String.class)
					.addArgument("entityId", mcpUpdateEntityIdDescription, true, String.class)
					.addArgument("updateExistingAttributes", mcpUpdateExistingAttrsDescription, true, Boolean.class)
					.setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String entityFragment = (String) parameters.get("entityFragment");
						String entityId = (String) parameters.get("entityId");
						Boolean updateExistingAttributes = (Boolean) parameters.get("updateExistingAttributes");

						return updateEntity(entityFragment, entityId,
								updateExistingAttributes != null ? updateExistingAttributes : false).onItem()
								.transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	private void registerDeleteEntityTool() {
		if (mcpDeleteDescription != null && !mcpDeleteDescription.equals(DEACTIVATED)) {
			toolManager.newTool("deleteEntity").setDescription(mcpDeleteDescription)
					.addArgument("entityId", "The entity ID of the entity to be deleted", true, String.class)
					.setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String entityId = (String) parameters.get("entityId");

						return deleteEntity(entityId).onItem().transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	private void registerDeleteAttributeTool() {
		if (mcpDeleteAttribDescription != null && !mcpDeleteAttribDescription.equals(DEACTIVATED)) {
			toolManager.newTool("deleteAttribute").setDescription(mcpDeleteAttribDescription)
					.addArgument("entityId", mcpDeleteAttribEntityIdDescription, true, String.class)
					.addArgument("attrib", mcpDeleteAttribAttribDescription, true, String.class)
					.addArgument("datasetId", mcpDeleteAttribDatasetIdDescription, false, String.class)
					.addArgument("deleteAll", mcpDeleteAttribDeleteAllDescription, false, String.class)
					.setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String entityId = (String) parameters.get("entityId");
						String attrib = (String) parameters.get("attrib");
						String datasetId = (String) parameters.get("datasetId");
						String deleteAll = (String) parameters.get("deleteAll");

						return deleteAttribute(entityId, attrib, datasetId, deleteAll).onItem()
								.transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	private void registerGetAvailableDomainsTool() {
		if (mcpSdmDomainsDescription != null && !mcpSdmDomainsDescription.equals(DEACTIVATED)) {
			toolManager.newTool("getAvailableDomains").setDescription(mcpSdmDomainsDescription).setAsyncHandler(ta -> {
				return getAvailableDomains().onItem().transform(t -> ToolResponse.success(t));
			}).register();
		}
	}

	private void registerGetAvailableDatamodelsTool() {
		if (mcpSdmDatamodelsDescription != null && !mcpSdmDatamodelsDescription.equals(DEACTIVATED)) {
			toolManager.newTool("getAvailableDatamodels").setDescription(mcpSdmDatamodelsDescription)
					.addArgument("domainName", mcpSdmDatamodelsDomainDescription, true, String.class)
					.setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String domainName = (String) parameters.get("domainName");

						return getAvailableDatamodels(domainName).onItem().transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	private void registerGetDatamodelTool() {
		if (mcpSdmDatamodelDescription != null && !mcpSdmDatamodelDescription.equals(DEACTIVATED)) {
			toolManager.newTool("getDatamodel").setDescription(mcpSdmDatamodelDescription)
					.addArgument("repoName", mcpSdmDatamodelReponameDescription, true, String.class)
					.addArgument("dataModelName", mcpSdmDatamodelDatamodelnameDescription, true, String.class)
					.setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String repoName = (String) parameters.get("repoName");
						String dataModelName = (String) parameters.get("dataModelName");

						return getDatamodel(repoName, dataModelName).onItem().transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	private void registerGetCoordinatesForAddressTool() {
		if (mcpGeotoolsAddress2coordinatesDescription != null
				&& !mcpGeotoolsAddress2coordinatesDescription.equals(DEACTIVATED)) {
			toolManager.newTool("getCoordinatesForAddress").setDescription(mcpGeotoolsAddress2coordinatesDescription)
					.addArgument("address", mcpGeotoolsAddress2coordinatesAddressDescription, true, String.class)
					.setAsyncHandler(ta -> {
						Map<String, Object> parameters = ta.args();
						String address = (String) parameters.get("address");

						return getCoordinatesForAddress(address).onItem().transform(t -> ToolResponse.success(t));
					}).register();
		}
	}

	public Uni<String> getEntities(String attrs, String coordinates, String csf, String datasetId, String geometry,
			String geometryProperty, String geoproperty, String georel, String id, String idPattern, String join,
			Integer joinLevel, String jsonKeys, String lang, String omit, String pick, String q, String scopeQ,
			String type) {
		HttpRequest<Buffer> tmp = webClient.getAbs(baseUrl + "/ngsi-ld/v1/entities").putHeader("Accept",
				"application/json");
		if (id != null) {
			tmp.addQueryParam("id", id);
		}
		if (type != null) {
			tmp.addQueryParam("type", type);
		}
		if (idPattern != null) {
			tmp.addQueryParam("idPattern", idPattern);
		}
		if (attrs != null) {
			tmp.addQueryParam("attrs", attrs);
		}
		if (q != null) {
			tmp.addQueryParam("q", q);
		}
		if (geometry != null) {
			tmp.addQueryParam("geometry", geometry);
		}
		if (georel != null) {
			tmp.addQueryParam("georel", georel);
		}
		if (coordinates != null) {
			tmp.addQueryParam("coordinates", coordinates);
		}
		if (geoproperty != null) {
			tmp.addQueryParam("geoproperty", geoproperty);
		}
		if (geometryProperty != null) {
			tmp.addQueryParam("geometryProperty", geometryProperty);
		}
		if (lang != null) {
			tmp.addQueryParam("lang", lang);
		}
		if (scopeQ != null) {
			tmp.addQueryParam("scopeQ", scopeQ);
		}
		tmp.addQueryParam("limit", "1000");
		return tmp.send().onItem().transform(resp -> resp.bodyAsString());

	}

	public Uni<String> getTypes() {
		return webClient.getAbs(baseUrl + "/ngsi-ld/v1/types").putHeader("Accept", "application/json")
				.addQueryParam("details", "true")
				.addQueryParam("bbox", "true").send().onItem().transform(resp -> resp.bodyAsString());

	}

	public Uni<String> createEntity(String entity) {
		return webClient.postAbs(baseUrl + "/ngsi-ld/v1/entities").putHeader("Content-Type", "application/ld+json")
				.sendBuffer(Buffer.buffer(entity)).onItem().transform(resp -> {
					if (resp.statusCode() == 201) {
						return "";
					}
					return resp.bodyAsString();
				});

	}

	public Uni<String> updateEntity(String entityFragment, String entityId, boolean updateExistingAttributes) {
		HttpRequest<Buffer> tmp = webClient.postAbs(baseUrl + "/ngsi-ld/v1/entities/" + entityId + "/attrs")
				.putHeader("Content-Type", "application/ld+json");
		if (!updateExistingAttributes) {
			tmp.addQueryParam("options", "noOverwrite");
		}
		return tmp.sendBuffer(Buffer.buffer(entityFragment)).onItem().transform(resp -> {
			if (resp.statusCode() == 204) {
				return "";
			}
			return resp.bodyAsString();
		});
	}

	public Uni<String> deleteEntity(String entityId) {
		return webClient.deleteAbs(baseUrl + "/ngsi-ld/v1/entities/" + entityId).send().onItem().transform(resp -> {
			if (resp.statusCode() == 204) {
				return "";
			}
			return resp.bodyAsString();
		});
	}

	public Uni<String> deleteAttribute(String entityId, String attrib, String datasetId, String deleteAll) {
		HttpRequest<Buffer> tmp = webClient
				.deleteAbs(baseUrl + "/ngsi-ld/v1/entities/" + entityId + "/attrs/" + attrib);
		if (datasetId != null) {
			tmp.addQueryParam("datasetId", datasetId);
		}
		if (deleteAll != null) {
			tmp.addQueryParam("deleteAll", deleteAll);
		}
		return tmp.send().onItem().transform(resp -> {
			if (resp.statusCode() == 204) {
				return "";
			}
			return resp.bodyAsString();
		});
	}

	public Uni<String> getAvailableDomains() {
		return webClient.getAbs(
				"https://raw.githubusercontent.com/smart-data-models/data-models/refs/heads/master/specs/AllSubjects/official_list_data_models.json")
				.send().onItem().transform(resp -> {
					Set<String> result = Sets.newHashSet();
					resp.bodyAsJsonObject().getJsonArray("officialList").forEach(entry -> {
						List<String> domains = ((JsonObject) entry).getJsonArray("domains").getList();
						result.addAll(domains);
					});
					return String.join(",", result);
				});
	}

	public Uni<String> getAvailableDatamodels(String domainName) {
		return webClient.getAbs(
				"https://raw.githubusercontent.com/smart-data-models/data-models/refs/heads/master/specs/AllSubjects/official_list_data_models.json")
				.send().onItem().transformToUni(resp -> {
					List<Map<String, Object>> result = Lists.newArrayList();
					resp.bodyAsJsonObject().getJsonArray("officialList").forEach(entry -> {
						List<String> domains = ((JsonObject) entry).getJsonArray("domains").getList();
						if (domains.contains(domainName)) {
							Map<String, Object> tmp = ((JsonObject) entry).getMap();
							tmp.put("@context", "https://raw.githubusercontent.com/smart-data-models/"
									+ tmp.get("repoName") + "/refs/heads/master/context.jsonld");
							result.add(tmp);
						}
					});

					try {
						return Uni.createFrom().item(objectMapper.writeValueAsString(result));
					} catch (JsonProcessingException e) {
						return Uni.createFrom().failure(new ToolCallException(e));
					}
				});

	}

	public Uni<String> getDatamodel(String repoName, String dataModelName) {
		return webClient.getAbs("https://raw.githubusercontent.com/smart-data-models/" + repoName
				+ "/refs/heads/master/" + dataModelName + "/schema.json").send().onItem()
				.transform(resp -> resp.bodyAsString());
	}

	public Uni<String> getCoordinatesForAddress(String address) {
		try {
			logger.info("https://nominatim.openstreetmap.org/search?q=" + address + "&format=json&addressdetails=1");
			return webClient
					.getAbs("https://nominatim.openstreetmap.org/search?q=" + address + "&format=json&addressdetails=1")
					.send().onItem().transform(t -> {

						List<Map<String, Object>> osmResult = t.bodyAsJsonArray().getList();
						logger.info(t.bodyAsString());
						if (osmResult.size() > 0) {
							Object bbox = osmResult.get(0).get("boundingbox");
							if (bbox != null && bbox instanceof List<?> l) {
								String minLat = (String) l.get(0);
								String maxLat = (String) l.get(1);
								String minLon = (String) l.get(2);
								String maxLon = (String) l.get(3);
								StringBuilder result = new StringBuilder("[[[");
								result.append(minLon);
								result.append(',');
								result.append(minLat);
								result.append("][");

								result.append(maxLon);
								result.append(',');
								result.append(minLat);
								result.append("][");

								result.append(maxLon);
								result.append(',');
								result.append(maxLat);
								result.append("][");

								result.append(minLon);
								result.append(',');
								result.append(maxLat);
								result.append("][");

								result.append(minLon);
								result.append(',');
								result.append(minLat);

								result.append("]]]");
								logger.info(result.toString());
								return result.toString();
							}
							Object lon = osmResult.get(0).get("lon");
							Object lat = osmResult.get(0).get("lat");
							if (lat != null && lon != null) {
								return '[' + lon.toString() + ',' + lat.toString() + ']';
							}
						}
						return "";
					});
		} catch (Exception e) {
			throw new ToolCallException(e);
		}
	}

}
