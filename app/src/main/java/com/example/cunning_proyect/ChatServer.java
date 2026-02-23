package com.example.cunning_proyect;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Servidor iniciado en puerto " + PORT);

        while (true) {
            Socket socket = server.accept();
            System.out.println("Cliente conectado: " + socket);
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    // Reenviar mensaje a todos los clientes conectados (excepto al remitente opcional)
    static void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                try {
                    if (client != sender) { // opcional: no reenviar al mismo cliente
                        client.dos.writeUTF(message);
                        client.dos.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private DataOutputStream dos;
        private DataInputStream dis;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.dos = new DataOutputStream(socket.getOutputStream());
            this.dis = new DataInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = dis.readUTF(); // leer mensaje enviado por cliente
                    System.out.println("Recibido: " + message);
                    broadcast(message, this); // reenviar a los demás
                }
            } catch (IOException e) {
                System.out.println("Cliente desconectado: " + socket);
            } finally {
                try {
                    clients.remove(this);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
