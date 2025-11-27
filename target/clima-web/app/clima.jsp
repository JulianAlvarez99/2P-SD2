<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.exam.web.WeatherRestClient" %>
<%@ page import="com.exam.web.WeatherRestClient.LocationData" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>

<%
    // 1. Obtener parámetros
    String provincia = request.getParameter("provincia");
    String localidad = request.getParameter("localidad");

    // 2. Variables para la vista
    List<LocationData> dataList = new ArrayList<>();
    String mensajeError = null;

    // 3. Lógica de control
    if (provincia == null || provincia.trim().isEmpty()) {
        mensajeError = "Falta el parámetro 'provincia'. Agregue ?provincia=Nombre a la URL.";
    } else {
        try {
            // Instanciar el cliente Java (El mismo que usa el Servlet AJAX)
            WeatherRestClient client = new WeatherRestClient();

            // Llamar al backend (Puerto 8081)
            dataList = client.getLocations(provincia, localidad);

            if (dataList.isEmpty()) {
                mensajeError = "No se encontraron resultados para: " + provincia +
                        (localidad != null ? " - " + localidad : "") +
                        ". Verifique que el servidor Backend (8081) tenga datos.";
            }
        } catch (Exception e) {
            mensajeError = "Error de conexión con el Backend: " + e.getMessage();
            e.printStackTrace();
        }
    }
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Consulta Climática (Versión JSP)</title>
    <!-- Reutilizamos tu CSS moderno para que se vea bien -->
    <link rel="stylesheet" href="../styles.css">
    <style>
        /* Ajustes específicos para esta vista simple */
        body { display: block; padding: 40px; height: auto; }
        .card { max-width: 900px; margin: 0 auto; background: white; padding: 30px; border-radius: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); }
        .back-link { display: inline-block; margin-bottom: 20px; color: white; text-decoration: none; font-weight: bold; }
    </style>
</head>
<body>

<a href="../buscador.html" class="back-link">← Volver al Buscador Moderno</a>

<div class="card">
    <div class="header">
        <h1>Reporte JSP (Server-Side)</h1>
    </div>

    <%-- Bloque de Error --%>
    <% if (mensajeError != null) { %>
    <div style="background: #fed7d7; color: #c53030; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
        <strong>Atención:</strong> <%= mensajeError %>
    </div>
    <% } %>

    <%-- Bloque de Tabla de Resultados --%>
    <% if (!dataList.isEmpty()) { %>
    <div style="overflow-x: auto;">
        <table class="weather-table">
            <thead>
            <tr>
                <th>Localidad</th>
                <th>Provincia</th>
                <th>Temp (°C)</th>
                <th>Humedad (%)</th>
                <!-- Nuevas columnas agregadas -->
                <th>Presión (hPa)</th>
                <th>Viento (km/h)</th>
            </tr>
            </thead>
            <tbody>
            <% for (LocationData dato : dataList) { %>
            <tr>
                <td><%= dato.name %></td>
                <td><%= dato.province %></td>
                <td><%= dato.temp %></td>
                <td><%= dato.humidity %></td>
                <!-- Nuevos datos agregados -->
                <td><%= dato.pressure %></td>
                <td><%= dato.wind %></td>
            </tr>
            <% } %>
            </tbody>
        </table>
    </div>
    <p class="footer-note">Renderizado tradicional con Java Server Pages</p>
    <% } %>
</div>

</body>
</html>