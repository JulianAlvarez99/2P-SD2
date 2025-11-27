package com.exam.weather;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.concurrent.Executors;

public class WeatherServer {

    private static final int PORT = 8081;

    public static void main(String[] args) throws IOException {
        // 1. Crear servidor
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 2. Asignar el handler que usa el Servicio
        server.createContext("/clima", new WeatherHandler());

        // 3. Configurar hilos
        server.setExecutor(Executors.newFixedThreadPool(10));

        System.out.println("Servidor HTTP iniciado en puerto " + PORT);
        server.start();
    }

    static class WeatherHandler implements HttpHandler {

        // Instanciamos tu servicio separado
        private final WeatherService weatherService = new WeatherService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "<error>Method Not Allowed</error>");
                return;
            }

            // Parsing manual de query string
            String query = exchange.getRequestURI().getRawQuery();
            String province = null;
            String locality = null;

            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=");
                    if (kv.length == 2) {
                        String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        if ("provincia".equalsIgnoreCase(k)) province = v;
                        if ("localidad".equalsIgnoreCase(k)) locality = v;
                    }
                }
            }

            if (province == null) {
                sendResponse(exchange, 400, "<error>Falta provincia</error>");
                return;
            }

            try {
                // LLAMADA A TU SERVICIO (WeatherService.java)
                String xmlResponse = weatherService.getWeatherXml(province, locality);

                if (xmlResponse == null || xmlResponse.isEmpty()) {
                    sendResponse(exchange, 404, "<error>No encontrado</error>");
                } else {
                    sendResponse(exchange, 200, xmlResponse);
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "<error>Error Interno: " + e.getMessage() + "</error>");
            }
        }

        private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=utf-8");
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}