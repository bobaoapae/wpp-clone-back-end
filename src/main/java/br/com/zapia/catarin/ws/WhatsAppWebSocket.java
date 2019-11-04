package br.com.zapia.catarin.ws;

import br.com.zapia.catarin.whatsApp.CatarinWhatsApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Service
public class WhatsAppWebSocket extends BinaryWebSocketHandler {

    @Lazy
    @Autowired
    private CatarinWhatsApp catarinWhatsApp;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        super.handleTextMessage(session, message);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        catarinWhatsApp.adicionarSession(session);
        catarinWhatsApp.enviarEventoWpp(CatarinWhatsApp.TipoEventoWpp.UPDATE_ESTADO, catarinWhatsApp.getDriver().getEstadoDriver().name());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        catarinWhatsApp.removerSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return super.supportsPartialMessages();
    }
}
