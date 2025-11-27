package com.exam.web;

import com.exam.web.WeatherRestClient.LocationData;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "WeatherAjaxServlet", urlPatterns = {"/api/weather-table"})
public class WeatherAjaxServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");

        String provincia = req.getParameter("provincia");
        String localidad = req.getParameter("localidad"); // Puede ser null o vacío

        if (provincia == null || provincia.isEmpty()) {
            resp.getWriter().write("<p class='error'>Debe seleccionar una provincia.</p>");
            return;
        }

        try (PrintWriter out = resp.getWriter()) {
            // 1. Usamos el cliente del Ejercicio 2
            WeatherRestClient client = new WeatherRestClient();
            List<LocationData> data = client.getLocations(provincia, localidad);

            // 2. Generamos el HTML (Server-Side Rendering del fragmento)
            if (data.isEmpty()) {
                out.write("<div class='alert alert-warning'>No se encontraron datos climáticos para esta zona en el SMN.</div>");
            } else {
                out.write("<table class='weather-table'>");
                out.write("<thead><tr><th>Localidad</th><th>Provincia</th><th>Temp (°C)</th><th>Humedad</th><th>Presión (hPa)</th><th>Viento(km/h)</th></tr></thead>");
                out.write("<tbody>");
                for (LocationData loc : data) {
                    out.write("<tr>");
                    out.write("<td>" + loc.name + "</td>");
                    out.write("<td>" + loc.province + "</td>");
                    out.write("<td>" + loc.temp + "</td>");
                    out.write("<td>" + loc.humidity + "</td>");
                    out.write("<td>" + loc.pressure + "</td>");
                    out.write("<td>" + loc.wind + "</td>");
                    out.write("</tr>");
                }
                out.write("</tbody></table>");
                out.write("<p class='footer-note'>Datos obtenidos del Nodo Backend (8081)</p>");
            }

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("<p class='error'>Error interno: " + e.getMessage() + "</p>");
        }
    }
}