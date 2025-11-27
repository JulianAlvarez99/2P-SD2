package com.exam.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Actúa como Proxy para apis.datos.gob.ar
 * Rutas:
 * - /api/geo?type=provincias
 * - /api/geo?type=localidades&prov=Nombre
 */
@WebServlet(name = "GeoRefServlet", urlPatterns = {"/api/geo"})
public class GeoRefServlet extends HttpServlet {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String API_BASE = "https://apis.datos.gob.ar/georef/api";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        String prov = req.getParameter("prov");

        String targetUrl = "";

        // 1. Construir la URL externa según el tipo de pedido
        if ("provincias".equals(type)) {
            targetUrl = API_BASE + "/provincias?orden=nombre";
        } else if ("localidades".equals(type) && prov != null) {
            String encodedProv = URLEncoder.encode(prov, StandardCharsets.UTF_8);
            targetUrl = API_BASE + "/localidades?provincia=" + encodedProv + "&campos=nombre&max=1000&orden=nombre";
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parámetros inválidos");
            return;
        }

        try {
            // 2. Realizar la petición al Gobierno
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // 3. Configurar respuesta (Proxy Pass-Through)
            resp.setStatus(response.statusCode());
            resp.setContentType("application/json;charset=UTF-8");

            // 4. Copiar el stream de entrada (API) al stream de salida (Navegador)
            // Esto evita tener que parsear el JSON en Java. Lo pasamos directo.
            try (InputStream is = response.body(); OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error conectando con GeoRef");
            e.printStackTrace();
        }
    }
}