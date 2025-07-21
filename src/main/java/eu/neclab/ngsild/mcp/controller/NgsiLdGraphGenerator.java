package eu.neclab.ngsild.mcp.controller;

import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.alg.drawing.FRLayoutAlgorithm2D;
import org.jgrapht.alg.drawing.model.Box2D;
import org.jgrapht.alg.drawing.model.LayoutModel2D;
import org.jgrapht.alg.drawing.model.MapLayoutModel2D;
import org.jgrapht.alg.drawing.model.Point2D;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.mcp.server.ImageContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Singleton;

@Singleton
public class NgsiLdGraphGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Color scheme for different node types
    private static final Color ENTITY_COLOR = new Color(70, 130, 180); // Steel Blue
    private static final Color TYPE_COLOR = new Color(255, 165, 0); // Orange
    private static final Color ATTRIBUTE_COLOR = new Color(50, 205, 50); // Lime Green
    private static final Color RELATIONSHIP_COLOR = new Color(220, 20, 60); // Crimson
    private static final Color EDGE_COLOR = new Color(100, 100, 100); // Gray

    // Image dimensions
    private static final int IMAGE_WIDTH = 1200;
    private static final int IMAGE_HEIGHT = 800;
    private static final int NODE_SIZE = 40;
    private static final int MARGIN = 50;

    @Tool(description = "Generate a visual graph from NGSI-LD entities showing relationships, types, and attributes")
    public ImageContent generateEntityGraph(
            @ToolArg(description = "JSON string containing list of NGSI-LD entities") String ngsiLdEntitiesJson) {

        try {
            // Parse the JSON input
            JsonNode entitiesArray = objectMapper.readTree(ngsiLdEntitiesJson);

            if (!entitiesArray.isArray()) {
                throw new IllegalArgumentException("Input must be a JSON array of NGSI-LD entities");
            }

            // Create the graph
            Graph<GraphNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
            Map<String, GraphNode> nodeMap = new HashMap<>();

            // Process each entity
            for (JsonNode entity : entitiesArray) {
                processEntity(entity, graph, nodeMap);
            }

            // Generate image and return as ImageContent
            return generateGraphImage(graph);

        } catch (Exception e) {
            throw new RuntimeException("Error processing NGSI-LD entities: " + e.getMessage(), e);
        }
    }

    private void processEntity(JsonNode entity, Graph<GraphNode, DefaultEdge> graph,
            Map<String, GraphNode> nodeMap) {

        // Extract entity ID and type
        String entityId = entity.has("id") ? entity.get("id").asText() : "unknown";
        String entityType = entity.has("type") ? entity.get("type").asText() : "unknown";

        // Create entity node
        GraphNode entityNode = getOrCreateNode(entityId, NodeType.ENTITY, entityId, nodeMap);
        graph.addVertex(entityNode);

        // Create type node and connect to entity
        GraphNode typeNode = getOrCreateNode(entityType + "_type", NodeType.TYPE, entityType, nodeMap);
        graph.addVertex(typeNode);
        graph.addEdge(entityNode, typeNode);

        // Process attributes
        entity.fields().forEachRemaining(field -> {
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            // Skip standard NGSI-LD fields
            if (fieldName.equals("id") || fieldName.equals("type") || fieldName.equals("@context")) {
                return;
            }

            processAttribute(entityNode, fieldName, fieldValue, graph, nodeMap);
        });
    }

    private void processAttribute(GraphNode entityNode, String attributeName, JsonNode attributeValue,
            Graph<GraphNode, DefaultEdge> graph, Map<String, GraphNode> nodeMap) {

        if (attributeValue.isObject()) {
            // Check if it's a relationship
            if (attributeValue.has("type") && "Relationship".equals(attributeValue.get("type").asText())) {
                processRelationship(entityNode, attributeName, attributeValue, graph, nodeMap);
            } else {
                // Regular attribute
                processPropertyAttribute(entityNode, attributeName, attributeValue, graph, nodeMap);
            }
        } else {
            // Simple attribute
            String nodeId = entityNode.getId() + "_" + attributeName;
            String displayValue = attributeName + ": " + attributeValue.asText();
            GraphNode attrNode = getOrCreateNode(nodeId, NodeType.ATTRIBUTE, displayValue, nodeMap);
            graph.addVertex(attrNode);
            graph.addEdge(entityNode, attrNode);
        }
    }

    private void processRelationship(GraphNode entityNode, String relationName, JsonNode relationValue,
            Graph<GraphNode, DefaultEdge> graph, Map<String, GraphNode> nodeMap) {

        if (relationValue.has("object")) {
            String targetId = relationValue.get("object").asText();

            // Create or get target entity node
            GraphNode targetNode = getOrCreateNode(targetId, NodeType.ENTITY, targetId, nodeMap);
            graph.addVertex(targetNode);

            // Create relationship node
            String relNodeId = entityNode.getId() + "_" + relationName + "_" + targetId;
            GraphNode relationNode = getOrCreateNode(relNodeId, NodeType.RELATIONSHIP, relationName, nodeMap);
            graph.addVertex(relationNode);

            // Connect: source -> relationship -> target
            graph.addEdge(entityNode, relationNode);
            graph.addEdge(relationNode, targetNode);
        }
    }

    private void processPropertyAttribute(GraphNode entityNode, String attributeName, JsonNode attributeValue,
            Graph<GraphNode, DefaultEdge> graph, Map<String, GraphNode> nodeMap) {

        String nodeId = entityNode.getId() + "_" + attributeName;
        String displayValue = attributeName;

        if (attributeValue.has("value")) {
            displayValue += ": " + attributeValue.get("value").asText();
        }

        if (attributeValue.has("type")) {
            displayValue += " (" + attributeValue.get("type").asText() + ")";
        }

        GraphNode attrNode = getOrCreateNode(nodeId, NodeType.ATTRIBUTE, displayValue, nodeMap);
        graph.addVertex(attrNode);
        graph.addEdge(entityNode, attrNode);
    }

    private GraphNode getOrCreateNode(String id, NodeType type, String displayValue,
            Map<String, GraphNode> nodeMap) {
        return nodeMap.computeIfAbsent(id, k -> new GraphNode(id, type, displayValue));
    }

    private ImageContent generateGraphImage(Graph<GraphNode, DefaultEdge> graph) throws IOException {

        // Create BufferedImage

        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // Calculate layout using Fruchterman-Reingold algorithm
        LayoutModel2D<GraphNode> layoutModel = new MapLayoutModel2D<>(
                new Box2D(MARGIN, MARGIN, IMAGE_WIDTH - 2 * MARGIN, IMAGE_HEIGHT - 2 * MARGIN));

        FRLayoutAlgorithm2D<GraphNode, DefaultEdge> layoutAlgorithm = new FRLayoutAlgorithm2D<>();
        layoutAlgorithm.layout(graph, layoutModel);

        // Draw edges first
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(EDGE_COLOR);
        for (DefaultEdge edge : graph.edgeSet()) {
            GraphNode source = graph.getEdgeSource(edge);
            GraphNode target = graph.getEdgeTarget(edge);

            Point2D sourcePoint = layoutModel.get(source);
            Point2D targetPoint = layoutModel.get(target);

            g2d.drawLine((int) sourcePoint.getX(), (int) sourcePoint.getY(),
                    (int) targetPoint.getX(), (int) targetPoint.getY());
        }

        // Draw nodes
        FontMetrics fm = g2d.getFontMetrics();
        for (GraphNode node : graph.vertexSet()) {
            Point2D position = layoutModel.get(node);
            int x = (int) position.getX();
            int y = (int) position.getY();

            // Set node color based on type
            g2d.setColor(getNodeColor(node.getType()));

            // Draw node shape based on type
            drawNodeShape(g2d, node.getType(), x, y, NODE_SIZE);

            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
            drawNodeBorder(g2d, node.getType(), x, y, NODE_SIZE);

            // Draw label
            String label = truncateLabel(node.getDisplayValue(), 15);
            int labelWidth = fm.stringWidth(label);
            int labelHeight = fm.getHeight();

            g2d.setColor(Color.BLACK);
            g2d.drawString(label, x - labelWidth / 2, y + labelHeight / 4);
        }

        g2d.dispose();

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();

        // Create and return ImageContent

        return new ImageContent(Base64.getEncoder().encodeToString(imageBytes), "image/png");
    }

    private Color getNodeColor(NodeType type) {
        switch (type) {
            case ENTITY:
                return ENTITY_COLOR;
            case TYPE:
                return TYPE_COLOR;
            case ATTRIBUTE:
                return ATTRIBUTE_COLOR;
            case RELATIONSHIP:
                return RELATIONSHIP_COLOR;
            default:
                return Color.LIGHT_GRAY;
        }
    }

    private void drawNodeShape(Graphics2D g2d, NodeType type, int x, int y, int size) {
        int halfSize = size / 2;
        switch (type) {
            case ENTITY:
                g2d.fillOval(x - halfSize, y - halfSize, size, size);
                break;
            case TYPE:
                g2d.fillRect(x - halfSize, y - halfSize, size, size);
                break;
            case ATTRIBUTE:
                g2d.fillRoundRect(x - halfSize, y - halfSize, size, size, 10, 10);
                break;
            case RELATIONSHIP:
                // Draw diamond
                int[] xPoints = { x, x + halfSize, x, x - halfSize };
                int[] yPoints = { y - halfSize, y, y + halfSize, y };
                g2d.fillPolygon(xPoints, yPoints, 4);
                break;
        }
    }

    private void drawNodeBorder(Graphics2D g2d, NodeType type, int x, int y, int size) {
        int halfSize = size / 2;
        switch (type) {
            case ENTITY:
                g2d.drawOval(x - halfSize, y - halfSize, size, size);
                break;
            case TYPE:
                g2d.drawRect(x - halfSize, y - halfSize, size, size);
                break;
            case ATTRIBUTE:
                g2d.drawRoundRect(x - halfSize, y - halfSize, size, size, 10, 10);
                break;
            case RELATIONSHIP:
                // Draw diamond border
                int[] xPoints = { x, x + halfSize, x, x - halfSize };
                int[] yPoints = { y - halfSize, y, y + halfSize, y };
                g2d.drawPolygon(xPoints, yPoints, 4);
                break;
        }
    }

    private String truncateLabel(String label, int maxLength) {
        if (label.length() <= maxLength) {
            return label;
        }
        return label.substring(0, maxLength - 3) + "...";
    }

    // Inner classes
    public enum NodeType {
        ENTITY, TYPE, ATTRIBUTE, RELATIONSHIP
    }

    public static class GraphNode {
        private final String id;
        private final NodeType type;
        private final String displayValue;

        public GraphNode(String id, NodeType type, String displayValue) {
            this.id = id;
            this.type = type;
            this.displayValue = displayValue;
        }

        public String getId() {
            return id;
        }

        public NodeType getType() {
            return type;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            GraphNode graphNode = (GraphNode) obj;
            return Objects.equals(id, graphNode.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return displayValue;
        }
    }
}