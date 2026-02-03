package com.example.cunning_proyect;

public class ChatMessage {
    String text;
    boolean isUser; // true = usuario (derecha), false = bot (izquierda)

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
}