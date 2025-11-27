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
            // Instanciar el cliente Java que creamos
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
            e.printStackTrace(); // Imprime en la consola del servidor para debug
        }
    }
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Consulta Climática Distribuida</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f9; padding: 20px; }
        .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); max-width: 800px; margin: 0 auto; }
        h2 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background-color: #007bff; color: white; }
        tr:hover { background-color: #f1f1f1; }
        .alert { padding: 15px; margin-bottom: 20px; border: 1px solid transparent; border-radius: 4px; }
        .alert-danger { color: #721c24; background-color: #f8d7da; border-color: #f5c6cb; }
        .info { font-size: 0.9em; color: #666; margin-bottom: 15px; }
    </style>
</head>
<body>

<div class="card">
    <h2>Sistema de Clima Distribuido</h2>
    <div class="info">
        Backend: <strong>Puerto 8081</strong> | Frontend (Este): <strong>Puerto 8080</strong>
    </div>

    <%-- Bloque de Error --%>
    <% if (mensajeError != null) { %>
    <div class="alert alert-danger">
        <strong>Atención:</strong> <%= mensajeError %>
    </div>
    <% } %>

    <%-- Bloque de Tabla de Resultados --%>
    <% if (!dataList.isEmpty()) { %>
    <table>
        <thead>
        <tr>
            <th>Localidad</th>
            <th>Provincia</th>
            <th>Temp (°C)</th>
            <th>Humedad (%)</th>
        </tr>
        </thead>
        <tbody>
        <% for (LocationData dato : dataList) { %>
        <tr>
            <td><%= dato.name %></td>
            <td><%= dato.province %></td>
            <td><%= dato.temp %></td>
            <td><%= dato.humidity %></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>

</body>
</html>