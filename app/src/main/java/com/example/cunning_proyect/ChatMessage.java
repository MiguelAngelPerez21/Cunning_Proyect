package com.example.cunning_proyect;

public class ChatMessage {
    private String message;
    private String senderName;
    // En el futuro añadiremos timestamp, etc.

    public ChatMessage(String message, String senderName) {
        this.message = message;
        this.senderName = senderName;
    }
    public String getMessage() { return message; }
    public String getSenderName() { return senderName; }
}