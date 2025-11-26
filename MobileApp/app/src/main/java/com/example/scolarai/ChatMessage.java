package com.example.scolarai;

public class ChatMessage {
    private final String message;
    private final boolean isUser;   // true = user, false = AI
    private final boolean isFile;   // true if this message is a file attachment

    public ChatMessage(String message, boolean isUser, boolean isFile) {
        this.message = message;
        this.isUser = isUser;
        this.isFile = isFile;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public boolean isFile() {
        return isFile;
    }
}
