package br.com.zapia.wppclone.payloads;

import org.springframework.http.HttpStatus;

public class WebSocketResponseFrame extends WebSocketResponse {

    private int frameId;
    private int qtdFrames;

    public WebSocketResponseFrame(int status) {
        super(status);
    }

    public WebSocketResponseFrame(HttpStatus httpStatus) {
        super(httpStatus);
    }

    public WebSocketResponseFrame(int status, Object response) {
        super(status, response);
    }

    public WebSocketResponseFrame(HttpStatus httpStatus, Object response) {
        super(httpStatus, response);
    }

    public int getFrameId() {
        return frameId;
    }

    public void setFrameId(int frameId) {
        this.frameId = frameId;
    }

    public int getQtdFrames() {
        return qtdFrames;
    }

    public void setQtdFrames(int qtdFrames) {
        this.qtdFrames = qtdFrames;
    }
}
