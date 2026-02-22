package com.example.cunning_proyect;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    private static final int PORT = 12345;

    // Ahora guardamos ClientHandler en vez de PrintWriter
    private static List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor activo en el puerto " + PORT);

        while (true) {

            Socket clientSocket = serverSocket.accept();
            System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

            ClientHandler clientHandler = new ClientHandler(clientSocket);
            clients.add(clientHandler);

            new Thread(clientHandler).start();
        }
    }

    static class ClientHandler implements Runnable {

        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            try {
                reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                writer = new PrintWriter(
                        socket.getOutputStream(), true);

                // 🔵 Notificar que alguien entra
                broadcast("🔵 Un usuario se ha unido al chat", this);

                String message;

                while ((message = reader.readLine()) != null) {

                    System.out.println("Mensaje recibido: " + message);

                    // 🔥 Reenviar a todos MENOS al que lo envió
                    broadcast(message, this);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {
                    clients.remove(this);

                    // 🔴 Notificar salida
                    broadcast("🔴 Un usuario ha salido del chat", this);

                    socket.close();
                    System.out.println("Cliente desconectado");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 🔥 Broadcast correcto
        private void broadcast(String message, ClientHandler sender) {

            synchronized (clients) {

                for (ClientHandler client : clients) {

                    // NO reenviar al mismo cliente
                    if (client != sender) {
                        client.sendMessage(message);
                    }
                }
            }
        }

        public void sendMessage(String message) {
            writer.println(message);
            writer.flush();
        }
    }
}