package br.com.zapia.wppclone.payloads;

public class SendMessageRequest {

    private String chatId;
    private String message;
    private String media;
    private String fileName;
    private String quotedMsg;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getQuotedMsg() {
        return quotedMsg;
    }

    public void setQuotedMsg(String quotedMsg) {
        this.quotedMsg = quotedMsg;
    }
}
