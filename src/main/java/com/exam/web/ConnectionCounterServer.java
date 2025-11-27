package com.exam.web;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint("/ws/contador")
public class ConnectionCounterServer {

    // Usamos un Set sincronizado
    private static final Set<Session> activeSessions = Collections.synchronizedSet(new HashSet<>());

    // Aumentamos a 10 para evitar bloqueos falsos durante pruebas
    // Cambia esto a 2 o 3 solo cuando quieras probar el bloqueo final
    private static final int MAX_USERS = 3;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        System.out.println("--- Intento de conexión: " + session.getId() + " ---");

        synchronized (activeSessions) {
            int currentSize = activeSessions.size();
            System.out.println("Usuarios actuales antes de conectar: " + currentSize);

            if (currentSize >= MAX_USERS) {
                System.out.println(">>> RECHAZADO: Cupo lleno (" + currentSize + "/" + MAX_USERS + ")");
                session.getBasicRemote().sendText("BLOCKED");
                session.close();
            } else {
                activeSessions.add(session);
                System.out.println(">>> ACEPTADO. Nuevo total: " + activeSessions.size());
                broadcast("COUNT:" + activeSessions.size());
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        boolean removed = activeSessions.remove(session);
        if (removed) {
            System.out.println("Usuario desconectado (" + session.getId() + "). Restantes: " + activeSessions.size());
            broadcast("COUNT:" + activeSessions.size());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        activeSessions.remove(session);
        System.err.println("Error en socket: " + throwable.getMessage());
    }

    private static void broadcast(String message) {
        synchronized (activeSessions) {
            for (Session s : activeSessions) {
                if (s.isOpen()) {
                    try {
                        s.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        // Ignorar errores de envío
                    }
                }
            }
        }
    }
}