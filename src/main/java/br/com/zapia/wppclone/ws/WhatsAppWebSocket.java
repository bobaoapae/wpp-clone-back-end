package br.com.zapia.wppclone.ws;

import br.com.zapia.wppclone.authentication.JwtAuthenticationFilter;
import br.com.zapia.wppclone.authentication.JwtTokenProvider;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;

@Service
public class WhatsAppWebSocket extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String[] dataResponse = message.getPayload().split(",", 2);
        switch (dataResponse[0]) {
            case "token":
                try {
                    if (tokenProvider.validateTokenWs(dataResponse[1])) {
                        session.getAttributes().put("token", dataResponse[1]);
                        session.sendMessage(new TextMessage("token,valido"));
                        getWhatsAppClone().adicionarSession(session);
                    } else {
                        session.sendMessage(new TextMessage("token,invalido"));
                    }
                } catch (IOException e) {
                    logger.error("Token Ws", e);
                }
                break;
            default:
                try {
                    if (tokenProvider.validateTokenWs((String) session.getAttributes().get("token"))) {
                        getWhatsAppClone().processWebSocketMsg(session, dataResponse);
                    }
                } catch (Exception e) {
                    logger.error("WebSocket", e);
                }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
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
        try {
            if (tokenProvider.validateTokenWs((String) session.getAttributes().get("token"))) {
                getWhatsAppClone().removerSession(session);
            }
        } catch (Exception e) {
            logger.error("WebSocket ConnectionClosed", e);
        }
    }

    private WhatsAppClone getWhatsAppClone() {
        return applicationContext.getBean(WhatsAppClone.class);
    }

    @Override
    public boolean supportsPartialMessages() {
        return super.supportsPartialMessages();
    }
}
