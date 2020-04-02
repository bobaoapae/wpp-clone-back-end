package br.com.zapia.wppclone.payloads;

public class ClearChatRequest {

    private String chatId;
    private boolean keepFavorites;

    public String getChatId() {
        return chatId;
    }

    public ClearChatRequest setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public boolean isKeepFavorites() {
        return keepFavorites;
    }

    public ClearChatRequest setKeepFavorites(boolean keepFavorites) {
        this.keepFavorites = keepFavorites;
        return this;
    }
}
