package com.exam.web;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@ServerEndpoint("/ws/contador")
public class ConnectionCounterServer {

    // Usuarios activos usando la aplicación
    private static final Set<Session> activeSessions = Collections.synchronizedSet(new HashSet<>());

    // Usuarios esperando turno (Cola FIFO - First In First Out)
    private static final Queue<Session> waitQueue = new ConcurrentLinkedQueue<>();

    private static final int MAX_USERS = 3;
    private static final Object lock = new Object(); // Objeto para sincronizar cambios de estado

    @OnOpen
    public void onOpen(Session session) throws IOException {
        System.out.println("Nueva conexión: " + session.getId());

        synchronized (lock) {
            if (activeSessions.size() < MAX_USERS) {
                // Caso 1: Hay lugar, entra directo
                activateUser(session);
            } else {
                // Caso 2: Lleno, va a la cola
                waitQueue.add(session);
                int position = waitQueue.size();
                System.out.println("Usuario " + session.getId() + " en cola. Posición: " + position);
                sendMessage(session, "WAITING:" + position);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        synchronized (lock) {
            if (activeSessions.contains(session)) {
                // Se fue un usuario activo
                activeSessions.remove(session);
                System.out.println("Usuario activo desconectado. Cupos: " + activeSessions.size() + "/" + MAX_USERS);
                broadcastActiveCount(); // Actualizar contador a los activos

                // Promover al siguiente de la cola si hay alguien esperando
                promoteNextUser();
            } else {
                // Se fue un usuario que estaba esperando (se cansó de esperar)
                if (waitQueue.remove(session)) {
                    System.out.println("Usuario abandonó la cola.");
                    notifyQueuePositions(); // Re-avisar posiciones a los que quedan
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Manejo básico: cerrar sesión limpia el estado en onClose
        try { session.close(); } catch (IOException e) {}
    }

    // --- Métodos Auxiliares ---

    private void activateUser(Session session) {
        activeSessions.add(session);
        sendMessage(session, "ACCEPTED"); // Avisar al frontend que desbloquee
        broadcastActiveCount();
    }

    private void promoteNextUser() {
        // Si hay lugar y hay gente esperando
        if (activeSessions.size() < MAX_USERS && !waitQueue.isEmpty()) {
            Session nextSession = waitQueue.poll(); // Sacar al primero
            if (nextSession != null && nextSession.isOpen()) {
                System.out.println("Promoviendo usuario de cola: " + nextSession.getId());
                activateUser(nextSession);
                notifyQueuePositions(); // Avisar a los restantes que avanzaron un lugar
            }
        }
    }

    // Notificar a todos los activos cuántos somos
    private void broadcastActiveCount() {
        String msg = "COUNT:" + activeSessions.size();
        for (Session s : activeSessions) {
            sendMessage(s, msg);
        }
    }

    // Notificar a cada usuario en espera su nueva posición
    private void notifyQueuePositions() {
        int pos = 1;
        for (Session s : waitQueue) {
            sendMessage(s, "WAITING:" + pos);
            pos++;
        }
    }

    private void sendMessage(Session s, String msg) {
        if (s.isOpen()) {
            try {
                s.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}