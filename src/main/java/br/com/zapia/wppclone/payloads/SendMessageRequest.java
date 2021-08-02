package br.com.zapia.wppclone.payloads;

public class SendMessageRequest {

    private String chatId;
    private String message;
    private String fileUUID;
    private String quotedMsg;

    public SendMessageRequest() {
        chatId = "";
        message = "";
        fileUUID = "";
        quotedMsg = "";
    }

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

    public String getFileUUID() {
        return fileUUID;
    }

    public void setFileUUID(String fileUUID) {
        this.fileUUID = fileUUID;
    }

    public String getQuotedMsg() {
        return quotedMsg;
    }

    public void setQuotedMsg(String quotedMsg) {
        this.quotedMsg = quotedMsg;
    }
}
