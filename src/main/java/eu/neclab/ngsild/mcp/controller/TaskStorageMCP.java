package eu.neclab.ngsild.mcp.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Singleton;

@Singleton
public class TaskStorageMCP {

    private static final Logger logger = LoggerFactory.getLogger(TaskStorageMCP.class);
    Map<String, String> id2Plan = Maps.newHashMap();

    @Tool(description = "Allows to store task plans for the LLM in any format that the LLM choses to")
    public String storePlan(@ToolArg(description = "A task plan") String taskPlan,
            @ToolArg(description = "A unique session id for the current conversation") String id) {
        logger.info("id: " + id);
        logger.info("task plan: " + taskPlan);
        id2Plan.put(id, taskPlan);
        return "";
    }

    @Tool(description = "Retrieves a stored task plan for the LLM in any format that the LLM choses to. Returns an empty string if no plan is active.")
    public String retrievePlan(
            @ToolArg(description = "A unique session id for the current conversation") String id) {
        String taskPlan = id2Plan.get(id);
        logger.info("id: " + id);
        if (taskPlan == null) {
            return "";
        }
        logger.info("task plan: " + taskPlan);
        return taskPlan;
    }

    @Tool(description = "Removes a stored task plan for the LLM.")
    public String deletePlan(
            @ToolArg(description = "A unique session id for the current conversation") String id) {
        id2Plan.remove(id);
        return "";
    }
}
