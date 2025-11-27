package com.exam.web;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente que consume el servicio XML del puerto 8081.
 */
public class WeatherRestClient {

    private static final String BACKEND_URL = "http://localhost:8081/clima";
    private final HttpClient httpClient;

    public WeatherRestClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<LocationData> getLocations(String provincia, String localidad) {
        List<LocationData> list = new ArrayList<>();
        try {
            // 1. Construir URL hacia el Backend (Nodo A)
            StringBuilder urlBuilder = new StringBuilder(BACKEND_URL);
            urlBuilder.append("?provincia=").append(URLEncoder.encode(provincia, StandardCharsets.UTF_8));

            if (localidad != null && !localidad.isEmpty()) {
                urlBuilder.append("&localidad=").append(URLEncoder.encode(localidad, StandardCharsets.UTF_8));
            }

            // 2. Ejecutar Petición
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 3. Parsear el XML recibido
                list = parseXmlResponse(response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<LocationData> parseXmlResponse(String xml) throws Exception {
        List<LocationData> result = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        NodeList nodes = doc.getElementsByTagName("location");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                String name = getTagValue("name", elem);
                String prov = getTagValue("province", elem);
                String temp = getTagValue("temperature", elem); // XML tag was <temperature> or <temp>? Adjusted to step 1 logic
                String hum = getTagValue("humidity", elem);
                String pres = getTagValue("pressure", elem);
                String wind = getTagValue("wind", elem);
                // Fallback si el tag varia (en el paso 1 a veces usamos 'temp' o 'temperature')
                if (temp.isEmpty()) temp = getTagValue("temp", elem);

                result.add(new LocationData(name, prov, temp, hum, pres, wind));
            }
        }
        return result;
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return "";
    }

    // Clase interna simple (DTO) para transportar datos al JSP
    public static class LocationData {
        public String name;
        public String province;
        public String temp;
        public String humidity;
        public String pressure;
        public String wind;


        public LocationData(String name, String province, String temp, String humidity, String pressure, String wind) {
            this.name = name;
            this.province = province;
            this.temp = temp;
            this.humidity = humidity;
            this.pressure = pressure;
            this.wind = wind;
        }
    }
}