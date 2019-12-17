package br.com.zapia.wppclone.payloads;

public class WebSocketResponse {

    private int status;
    private Object response;

    public WebSocketResponse(int status) {
        this(status, null);
    }

    public WebSocketResponse(int status, Object response) {
        this.status = status;
        this.response = response;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }
}
