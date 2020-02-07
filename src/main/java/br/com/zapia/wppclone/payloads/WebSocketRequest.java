package br.com.zapia.wppclone.payloads;

import org.springframework.web.socket.WebSocketSession;

public class WebSocketRequest {

    private String tag;
    private WebSocketRequestPayLoad webSocketRequestPayLoad;
    private WebSocketSession webSocketSession;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public WebSocketRequestPayLoad getWebSocketRequestPayLoad() {
        return webSocketRequestPayLoad;
    }

    public void setWebSocketRequestPayLoad(WebSocketRequestPayLoad webSocketRequestPayLoad) {
        this.webSocketRequestPayLoad = webSocketRequestPayLoad;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }
}
