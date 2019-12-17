package br.com.zapia.wppclone.payloads;

public class DeleteMessageRequest {

    private String msgId;
    private boolean fromAll;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public boolean isFromAll() {
        return fromAll;
    }

    public void setFromAll(boolean fromAll) {
        this.fromAll = fromAll;
    }
}
