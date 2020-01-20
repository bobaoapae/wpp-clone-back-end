package br.com.zapia.wppclone.payloads;

public class WebSocketRequestPayLoad {

    private String event;
    private Object payload;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public <K> K getPayload() {
        return (K) payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
