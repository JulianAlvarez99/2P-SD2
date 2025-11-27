# Sistema Distribuido de Monitoreo Climático

Este proyecto implementa una arquitectura distribuida Cliente-Servidor para la consulta de datos meteorológicos en tiempo real, integrando múltiples tecnologías Java (SE y EE) y servicios externos.

La solución aborda progresivamente 4 desafíos técnicos:
- Servicios HTTP nativos
- Renderizado en Servidor (JSP)
- Arquitectura AJAX/API REST
- Comunicación Bidireccional (WebSockets)

## 1. Arquitectura del Sistema

El sistema se divide en dos nodos lógicos que deben ejecutarse simultáneamente:

### Nodo Backend (Puerto 8081)

Actúa como la fuente de verdad de los datos climáticos.
- Consume la API externa del Servicio Meteorológico Nacional (SMN).
- Procesa JSON crudo y expone una API interna en formato XML.
- Implementado con Java SE puro (sin frameworks externos).

### Nodo Frontend (Puerto 8080)

- Servidor de Aplicaciones Apache Tomcat 11.
- Actúa como BFF (Backend for Frontend) y cliente web.
- Consume el XML del Nodo Backend y la API de GeoRef (Datos Argentina).
- Maneja la interfaz de usuario, proxies y control de sesiones.

## 2. Puntos Resueltos del Examen

### Ejercicio 1: Servidor HTTP Nativo (Backend)

Implementación de un servidor HTTP multithreaded utilizando las librerías estándar de Java (`com.sun.net.httpserver`).

**Funcionalidad:**
- Intercepta peticiones GET.
- Consulta al SMN.
- Filtra por provincia/localidad.
- Transforma la respuesta a XML.

**Clases clave:**
- `WeatherServer.java`: entry point, configuración del socket 8081 y manejo de hilos.
- `WeatherService.java`: lógica de negocio, cliente HTTP y parser JSON-a-XML manual (Regex).

### Ejercicio 2: Cliente JSP (Server-Side Rendering)

Creación de un cliente web que consume el servicio del punto 1.

**Funcionalidad:**
- Una página JSP recibe parámetros.
- Invoca al backend XML.
- Parsear la respuesta y renderiza una tabla HTML completa en el servidor.

**Componentes:**
- `WeatherRestClient.java`: cliente HTTP robusto y parser DOM para XML.
- `clima.jsp`: vista que integra lógica Java para mostrar datos o errores.

### Ejercicio 3: Interfaz Dinámica (AJAX & Proxy)

Evolución hacia una SPA (Single Page Application) parcial para mejorar la UX.

**Funcionalidad:**
- Selectores en cascada (Provincia -> Localidad).
- Carga de tablas sin recargar la página.

**Componentes:**
- `GeoRefServlet.java`: proxy reverso que oculta la API del gobierno (`apis.datos.gob.ar`) para evitar problemas de CORS y centralizar el acceso.
- `WeatherAjaxServlet.java`: endpoint interno que devuelve fragmentos HTML de la tabla climática bajo demanda.
- `buscador.html`: interfaz moderna con JavaScript (Fetch API).

### Ejercicio 4: Control de Concurrencia (WebSockets)

Implementación de un semáforo distribuido para limitar el acceso al sistema.

**Funcionalidad:**
- Muestra el número de usuarios online en tiempo real.
- Bloquea el acceso si se supera el cupo máximo (`MAX_USERS`).

**Componentes:**
- `ConnectionCounterServer.java`: endpoint WebSocket (`/ws/contador`) que gestiona un `Set` sincronizado de sesiones. Implementa la lógica de rechazo (`BLOCKED`) y broadcast (`COUNT:N`).
- Frontend: lógica JS para manejar la conexión, mostrar pantalla de bloqueo (overlay) y reintentar automáticamente ("exponential backoff" simulado).

## 3. Estructura de Archivos

```text
/src/main
├── java/com/exam
│   ├── weather/                # --- NODO BACKEND ---
│   │   ├── WeatherServer.java  # Main Class (Solución Punto 1)
│   │   └── WeatherService.java # Lógica de datos
│   │
│   └── web/                    # --- NODO FRONTEND ---
│       ├── ConnectionCounterServer.java # WebSocket Endpoint
│       ├── GeoRefServlet.java           # Proxy API Geográfica
│       ├── WeatherAjaxServlet.java      # Renderizado parcial HTML
│       └── WeatherRestClient.java       # Cliente HTTP Java
│
└── webapp/
    ├── app/
    │   └── clima.jsp           # Solución Punto 2
    ├── styles.css              # Estilos Modernos
    └── buscador.html           # Solución Punto 3 y 4 (Interfaz Principal)
```

## 4. Tecnologías Utilizadas

- **Lenguaje:** Java 17+ (uso intensivo de `java.net.http.HttpClient`).
- **Servidor Web:** Apache Tomcat 11 (Jakarta EE 10).
- **Gestión de dependencias:** Maven.

**APIs Jakarta:**
- Jakarta Servlet 6.0
- Jakarta WebSocket 2.1
- Jakarta JSP 3.1

**Frontend:**
- HTML5
- CSS3 (Glassmorphism)
- JavaScript (ES6+)

**Formatos de datos:**
- JSON (externo)
- XML (interno)
- HTML (vista)

## 5. Instrucciones de Ejecución

Para levantar el sistema completo, se requieren dos terminales o configuraciones de ejecución paralelas.

### Paso 1: Iniciar el Nodo Backend

Este servidor provee los datos. Sin él, el frontend dará errores de conexión.

1. Navegar a la clase `com.exam.weather.WeatherServer`.
2. Ejecutar el método `main()`.

**Verificación:** la consola debe mostrar: `Servidor HTTP iniciado en puerto 8081`.

### Paso 2: Iniciar el Nodo Frontend

Este servidor provee la interfaz web.

1. Asegurarse de tener configurado Tomcat 11 en el IDE.
2. Ejecutar la configuración de Run **"Tomcat"** (artifact `clima-web:war exploded`).

**Verificación:** el navegador se abrirá o la consola mostrará logs de despliegue en puerto 8080.

### Paso 3: Acceso

- Interfaz principal: http://localhost:8080/app/buscador.html

> Nota: La ruta `/app` depende del *Application Context* configurado en el despliegue de Tomcat.

### Pruebas de Funcionalidad

- **Clima:** seleccionar una provincia y localidad para ver la tabla con temperatura, humedad, presión y viento.
- **WebSockets:** abrir la aplicación en múltiples pestañas. El contador "Online" debe incrementarse. Al superar el límite (configurado en Java), las nuevas pestañas mostrarán la pantalla de "Servidor Saturado".
