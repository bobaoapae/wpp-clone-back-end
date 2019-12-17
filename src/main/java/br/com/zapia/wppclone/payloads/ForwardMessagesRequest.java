package br.com.zapia.wppclone.payloads;

public class ForwardMessagesRequest {

    private String[] idsChats;
    private String[] idsMsgs;

    public String[] getIdsChats() {
        return idsChats;
    }

    public void setIdsChats(String[] idsChats) {
        this.idsChats = idsChats;
    }

    public String[] getIdsMsgs() {
        return idsMsgs;
    }

    public void setIdsMsgs(String[] idsMsgs) {
        this.idsMsgs = idsMsgs;
    }
}
