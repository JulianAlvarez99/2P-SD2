package com.exam.weather;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherService {

    private static final String SMN_URL = "https://ws.smn.gob.ar/map_items/weather";
    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String getWeatherXml(String targetProvince, String targetLocality) throws Exception {
        // 1. Fetch Data
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SMN_URL))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("External Service Error: " + response.statusCode());
        }

        // 2. Process Data
        return processJsonToXml(response.body(), targetProvince, targetLocality);
    }

    /**
     * Manually parses JSON and builds XML to avoid external dependencies
     * like Jackson/Gson in a basic exam setup.
     */
    private String processJsonToXml(String jsonArray, String targetProvince, String targetLocality) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<weatherData>\n");

        // Strip array brackets
        String cleaned = jsonArray.trim();
        if (cleaned.startsWith("[")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("]")) cleaned = cleaned.substring(0, cleaned.length() - 1);

        // Split by objects
        String[] objects = cleaned.split("\\},\\{");
        boolean found = false;

        for (String rawObj : objects) {
            String obj = rawObj;
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";

            String p = extractValue(obj, "province");
            String n = extractValue(obj, "name");

            if (p != null && p.equalsIgnoreCase(targetProvince)) {
                if (targetLocality == null || (n != null && n.equalsIgnoreCase(targetLocality))) {
                    xmlBuilder.append("  <location>\n");
                    xmlBuilder.append("    <name>").append(n).append("</name>\n");
                    xmlBuilder.append("    <province>").append(p).append("</province>\n");
                    xmlBuilder.append("    <temperature>").append(extractValue(obj, "temp")).append("</temperature>\n");
                    xmlBuilder.append("    <humidity>").append(extractValue(obj, "humidity")).append("</humidity>\n");
                    xmlBuilder.append("    <pressure>").append(extractValue(obj, "pressure")).append("</pressure>\n");
                    xmlBuilder.append("    <wind>").append(extractValue(obj, "wind_speed")).append("</wind>\n");
                    xmlBuilder.append("  </location>\n");
                    found = true;
                }
            }
        }

        xmlBuilder.append("</weatherData>");
        return found ? xmlBuilder.toString() : "";
    }

    private String extractValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\":\\s*\"?([^,\"}]+)\"?");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "N/A";
    }
}