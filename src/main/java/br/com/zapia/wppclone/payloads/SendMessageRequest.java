package br.com.zapia.wppclone.payloads;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class SendMessageRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9]+$", message = "Only numeric characters are allowed")
    private String chatNumber;
    private String message;
    private String file;
    private String fileName;

    public String getChatNumber() {
        return chatNumber;
    }

    public void setChatNumber(String chatNumber) {
        this.chatNumber = chatNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
