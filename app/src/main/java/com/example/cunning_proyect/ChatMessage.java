package com.example.cunning_proyect;

public class ChatMessage {

    private String sender;   // Nombre del usuario que envió el mensaje
    private String text;     // Contenido del mensaje
    private boolean isUser;  // true = mensaje propio (derecha), false = mensaje de otros (izquierda)

    // BUILDER
    public ChatMessage(String sender, String text, boolean isUser) {
        this.sender = sender;
        this.text = text;
        this.isUser = isUser;
    }

    // GETTERS
    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }
}