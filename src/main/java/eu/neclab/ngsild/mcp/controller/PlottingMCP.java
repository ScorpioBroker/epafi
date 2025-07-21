package eu.neclab.ngsild.mcp.controller;

import jakarta.inject.Singleton;

import io.quarkiverse.mcp.server.Content;
import io.quarkiverse.mcp.server.ImageContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Base64.Encoder;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.Stroke;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.StyleBuilder;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.osm.OSMService;
import org.geotools.tile.util.TileLayer;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BubbleChart;
import org.knowm.xchart.BubbleChartBuilder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.BubbleSeries.BubbleSeriesRenderStyle;
import org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle;
import org.knowm.xchart.PieSeries.PieSeriesRenderStyle;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotools.geojson.geom.GeometryJSON;

@Singleton
public class PlottingMCP {

    Encoder base64Encoder = Base64.getEncoder();

    @Tool(name = "createXYChart", description = "Generate a XYChart with XChart and return as image content")
    public Content createXYChart(
            @ToolArg(name = "chart type", description = "the kind of XYChart. Valid values are: line, scatter, area, step, steparea, polygonarea") String chartType,
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "xAxisTitle", description = "X axis title") String xAxisTitle,
            @ToolArg(name = "yAxisTitle", description = "Y axis title") String yAxisTitle,
            @ToolArg(name = "xData", description = "Array of X data arrays. MUST be the same length as seriesNames and yData") double[][] xData,
            @ToolArg(name = "yData", description = "Array of Y data arrays. MUST be the same length as seriesNames and xData") double[][] yData,
            @ToolArg(name = "seriesNames", description = "Names for the datasets provided. . MUST be the same length as xData and yData") String[] seriesNames,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        XYChart chart = new XYChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .xAxisTitle(xAxisTitle)
                .yAxisTitle(yAxisTitle)
                .build();
        for (int i = 0; i < seriesNames.length; i++) {
            chart.addSeries(seriesNames[i], xData[i], yData[i]);
        }
        // Set render style based on chartType argument
        switch (chartType.toLowerCase()) {
            case "line":
                chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
                break;
            case "scatter":
                chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
                break;
            case "area":
                chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Area);
                break;
            case "step":
                chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Step);
                break;
            case "steparea":
                chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.StepArea);
                break;
            case "polygonarea":
                chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.PolygonArea);
                break;
            default:
                chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
                break;
        }

        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");

    }

    @Tool(name = "createCategoryChart", description = "Generate a CategoryChart (bar chart) with XChart and return as image content")
    public Content createCategoryChart(
            @ToolArg(name = "chart type", description = "the kind of CategoryChart. Valid values are: bar, scatter, steppedbar, stick, line, area") String chartType,
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "xAxisTitle", description = "X axis title") String xAxisTitle,
            @ToolArg(name = "yAxisTitle", description = "Y axis title") String yAxisTitle,
            @ToolArg(name = "categories", description = "Array of category labels") String[] categories,
            @ToolArg(name = "yData", description = "Array of Y data arrays. MUST be the same length as seriesNames") double[][] yData,
            @ToolArg(name = "seriesNames", description = "Names for the datasets provided. MUST be the same length as yData") String[] seriesNames,
            @ToolArg(name = "stacked", description = "Whether to stack the series (true/false)") boolean stacked,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .xAxisTitle(xAxisTitle)
                .yAxisTitle(yAxisTitle)
                .build();

        for (int i = 0; i < seriesNames.length; i++) {
            List<String> catList = Arrays.asList(categories);
            List<Double> yList = Arrays.stream(yData[i]).boxed().toList();
            chart.addSeries(seriesNames[i], catList, yList);
        }
        chart.getStyler().setStacked(stacked);
        // Set render style based on chartType argument
        switch (chartType.toLowerCase()) {
            case "bar":
                chart.getStyler().setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Bar);
                break;
            case "scatter":
                chart.getStyler().setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Scatter);
                break;
            case "steppedbar":
                chart.getStyler().setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.SteppedBar);
                break;
            case "stick":
                chart.getStyler().setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Stick);
                break;
            case "line":
                chart.getStyler().setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Line);
                break;
            case "area":
                chart.getStyler().setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Area);
                break;
            default:
                chart.getStyler().setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Bar);
                break;
        }

        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

    @Tool(name = "createPieChart", description = "Generate a PieChart (or Donut Chart) with XChart and return as image content")
    public Content createPieChart(
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "categories", description = "Array of category labels") String[] categories,
            @ToolArg(name = "values", description = "Array of values for each category") double[] values,
            @ToolArg(name = "donut", description = "Whether to render as a donut chart (true/false)") boolean donut,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        PieChart chart = new PieChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .build();

        for (int i = 0; i < categories.length; i++) {
            chart.addSeries(categories[i], values[i]);
        }

        if (donut) {
            chart.getStyler().setDefaultSeriesRenderStyle(PieSeriesRenderStyle.Donut);
        } else {
            chart.getStyler().setDefaultSeriesRenderStyle(PieSeriesRenderStyle.Pie);
        }

        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

    @Tool(name = "createBubbleChart", description = "Generate a BubbleChart with XChart and return as image content")
    public Content createBubbleChart(
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "xAxisTitle", description = "X axis title") String xAxisTitle,
            @ToolArg(name = "yAxisTitle", description = "Y axis title") String yAxisTitle,
            @ToolArg(name = "xData", description = "Array of X data arrays. MUST be the same length as seriesNames, yData, and bubbleSizes") double[][] xData,
            @ToolArg(name = "yData", description = "Array of Y data arrays. MUST be the same length as seriesNames, xData, and bubbleSizes") double[][] yData,
            @ToolArg(name = "bubbleSizes", description = "Array of bubble size arrays. MUST be the same length as seriesNames, xData, and yData") double[][] bubbleSizes,
            @ToolArg(name = "seriesNames", description = "Names for the datasets provided. MUST be the same length as xData, yData, and bubbleSizes") String[] seriesNames,
            @ToolArg(name = "bubbleShape", description = "Shape of the bubbles: 'round' or 'square'") String bubbleShape,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        // Use BubbleChart and BubbleChartBuilder
        BubbleChart chart = new BubbleChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .xAxisTitle(xAxisTitle)
                .yAxisTitle(yAxisTitle)
                .build();

        for (int i = 0; i < seriesNames.length; i++) {
            List<Double> xList = Arrays.stream(xData[i]).boxed().toList();
            List<Double> yList = Arrays.stream(yData[i]).boxed().toList();
            List<Double> sizeList = Arrays.stream(bubbleSizes[i]).boxed().toList();
            chart.addSeries(seriesNames[i], xList, yList, sizeList);
        }

        // Set bubble shape style
        // if ("square".equalsIgnoreCase(bubbleShape)) {
        // chart.getStyler().setBubbleSeriesRenderStyle(org.knowm.xchart.BubbleSeries.BubbleSeriesRenderStyle.Square);
        // } else {
        chart.getStyler().setDefaultSeriesRenderStyle(BubbleSeriesRenderStyle.Round);
        // }

        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

    @Tool(name = "createDialChart", description = "Generate a DialChart (gauge) with XChart and return as image content")
    public Content createDialChart(
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "values", description = "Array of values to display on the dial") double[] values,
            @ToolArg(name = "seriesNames", description = "Names for the values to display on the dial. Must be the same length as values") String[] seriesNames,
            @ToolArg(name = "tickValues", description = "Array of tick values for the dial") double[] tickValues,
            @ToolArg(name = "tickLabels", description = "Array of tick labels for the dial") String[] tickLabels,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        org.knowm.xchart.DialChart chart = new org.knowm.xchart.DialChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .build();
        // Add each value as a separate series with the provided series name
        for (int i = 0; i < values.length; i++) {
            String name = (seriesNames != null && seriesNames.length > i) ? seriesNames[i] : ("Value " + (i + 1));
            chart.addSeries(name, values[i]);
        }
        // Set custom ticks if provided
        if (tickValues != null && tickLabels != null && tickValues.length == tickLabels.length) {
            chart.getStyler().setAxisTickLabels(tickLabels);
            chart.getStyler().setAxisTickValues(tickValues);
        }
        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

    @Tool(name = "createRadarChart", description = "Generate a RadarChart with XChart and return as image content")
    public Content createRadarChart(
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "categories", description = "Array of category labels") String[] categories,
            @ToolArg(name = "yData", description = "Array of Y data arrays. MUST be the same length as seriesNames") double[][] yData,
            @ToolArg(name = "seriesNames", description = "Names for the datasets provided. MUST be the same length as yData") String[] seriesNames,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        org.knowm.xchart.RadarChart chart = new org.knowm.xchart.RadarChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .build();

        for (int i = 0; i < seriesNames.length; i++) {

            chart.addSeries(seriesNames[i], yData[i]);
        }

        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

    @Tool(name = "createOHLCChart", description = "Generate an OHLCChart (candlestick) with XChart and return as image content")
    public Content createOHLCChart(
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "xData", description = "Array of X data (labels or time)") String[] xData,
            @ToolArg(name = "open", description = "Open values") double[] open,
            @ToolArg(name = "high", description = "High values") double[] high,
            @ToolArg(name = "low", description = "Low values") double[] low,
            @ToolArg(name = "close", description = "Close values") double[] close,
            @ToolArg(name = "seriesName", description = "Name for the OHLC series") String seriesName,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        org.knowm.xchart.OHLCChart chart = new org.knowm.xchart.OHLCChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .build();

        List<String> xList = Arrays.asList(xData);
        List<Double> openList = Arrays.stream(open).boxed().toList();
        List<Double> highList = Arrays.stream(high).boxed().toList();
        List<Double> lowList = Arrays.stream(low).boxed().toList();
        List<Double> closeList = Arrays.stream(close).boxed().toList();

        chart.addSeries(seriesName, xList, openList, highList, lowList, closeList);

        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

    @Tool(name = "createBoxChart", description = "Generate a BoxChart with XChart and return as image content")
    public Content createBoxChart(
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "categories", description = "Array of category labels") String[] categories,
            @ToolArg(name = "yData", description = "Array of Y data arrays. MUST be the same length as categories") double[][] yData,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        org.knowm.xchart.BoxChart chart = new org.knowm.xchart.BoxChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .build();

        for (int i = 0; i < categories.length; i++) {
            List<Double> yList = Arrays.stream(yData[i]).boxed().toList();
            chart.addSeries(categories[i], yList);
        }

        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

    @Tool(name = "createHeatMapChart", description = "Generate a HeatMapChart with XChart and return as image content")
    public Content createHeatMapChart(
            @ToolArg(name = "title", description = "Chart title") String title,
            @ToolArg(name = "xDataSets", description = "Array of X data arrays (one per series)") int[][] xDataSets,
            @ToolArg(name = "yDataSets", description = "Array of Y data arrays (one per series)") int[][] yDataSets,
            @ToolArg(name = "heatData", description = "Array of heat values arrays (one per series)") int[][][] heatData,
            @ToolArg(name = "seriesNames", description = "Names for the datasets provided. MUST be the same length as xDataSets, yDataSets, and heatData") String[] seriesNames,
            @ToolArg(name = "width", description = "Chart width in pixels") int width,
            @ToolArg(name = "height", description = "Chart height in pixels") int height)
            throws IOException {
        org.knowm.xchart.HeatMapChart chart = new org.knowm.xchart.HeatMapChartBuilder()
                .width(width)
                .height(height)
                .title(title)
                .build();

        for (int i = 0; i < seriesNames.length; i++) {

            chart.addSeries(seriesNames[i], xDataSets[i], yDataSets[i], heatData[i]);
        }

        byte[] imageBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

    @Tool(name = "createMapImage", description = "Generates a map image visualizing NGSI-LD entities within a specified bounding box.")
    public Content createMapImage(
            @ToolArg(name = "minLon", description = "Minimum longitude (left/west)") double minLon,
            @ToolArg(name = "minLat", description = "Minimum latitude (bottom/south)") double minLat,
            @ToolArg(name = "maxLon", description = "Maximum longitude (right/east)") double maxLon,
            @ToolArg(name = "maxLat", description = "Maximum latitude (top/north)") double maxLat,
            @ToolArg(name = "width", description = "Image width in pixels") int width,
            @ToolArg(name = "height", description = "Image height in pixels") int height,
            @ToolArg(name = "ngsiEntitiesJson", description = "JSON array of NGSI-LD entities") String ngsiEntitiesJson)
            throws IOException, ServiceException {
        System.out.println("test");
        System.setProperty("org.geotools.tile.max.tiles", "512");
        MapContent map = new MapContent();
        map.setTitle("Map");
        String baseURL = "https://tile.openstreetmap.org/";
        TileService service = new OSMService("OSM", baseURL);

        ReferencedEnvelope bbox = new ReferencedEnvelope(
                minLon, maxLon, minLat, maxLat, DefaultGeographicCRS.WGS84);
        map.getViewport().setBounds(bbox);
        map.getViewport().setScreenArea(new java.awt.Rectangle(width, height));
        map.addLayer(new TileLayer(service));

        // --- Parse NGSI-LD entities and add as features ---
        ObjectMapper mapper = new ObjectMapper();

        GeometryJSON geometryJSON = new GeometryJSON();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("ngsi");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Geometry.class);
        b.add("id", String.class);
        SimpleFeatureType type = b.buildFeatureType();
        ListFeatureCollection collection = new ListFeatureCollection(type);
        ListFeatureCollection pointCollection = new ListFeatureCollection(type);

        JsonNode arr = mapper.readTree(ngsiEntitiesJson);
        for (JsonNode entity : arr) {
            // Example: location property as GeoJSON, name property as string
            JsonNode location = entity.get("location");
            JsonNode nameNode = entity.get("id");
            if (location != null && location.has("value")) {
                String geoJson = mapper.writeValueAsString(location.get("value"));
                Geometry geom = geometryJSON.read(new java.io.StringReader(geoJson));
                String name = nameNode.asText();
                SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
                fb.add(geom);
                fb.add(name);
                if (geom instanceof Point) {
                    pointCollection.add(fb.buildFeature(name));
                } else {
                    collection.add(fb.buildFeature(name));
                }
            }
        }
        if (!pointCollection.isEmpty()) {
            StyleBuilder sb = new StyleBuilder();
            // Create a big fat red dot style for points
            PointSymbolizer pointSymbolizer = sb.createPointSymbolizer();

            Graphic graphic = sb.createGraphic(
                    null, // external graphics
                    sb.createMark(org.geotools.styling.StyleBuilder.MARK_CIRCLE, java.awt.Color.RED, java.awt.Color.RED,
                            1.0),
                    null,
                    1.0,
                    1.0,
                    0.0);
            // Set the size to a large value (e.g., 24 pixels)
            graphic.setSize(sb.literalExpression(24));
            pointSymbolizer.setGraphic(graphic);

            map.addLayer(new FeatureLayer(new CollectionFeatureSource(pointCollection),
                    sb.createStyle(pointSymbolizer)));

        }
        if (!collection.isEmpty()) {
            StyleBuilder sb = new StyleBuilder();
            // Create a big fat red dot style for points
            // Create a thick red line style for non-point geometries
            Stroke stroke = sb.createStroke(java.awt.Color.RED, 4.0); // 4.0 pixel width, red color
            LineSymbolizer lineSymbolizer = sb.createLineSymbolizer(stroke);

            map.addLayer(new FeatureLayer(new CollectionFeatureSource(collection),
                    sb.createStyle(lineSymbolizer)));

        }

        // --- Render ---
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(map);

        java.awt.Graphics2D gr = image.createGraphics();
        gr.setPaint(java.awt.Color.WHITE);
        gr.fillRect(0, 0, width, height);

        renderer.paint(gr, new java.awt.Rectangle(width, height), bbox);

        gr.dispose();
        map.dispose();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        return new ImageContent(base64Encoder.encodeToString(imageBytes), "image/png");
    }

}
