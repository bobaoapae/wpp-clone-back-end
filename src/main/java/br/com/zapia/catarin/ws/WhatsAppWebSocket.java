package br.com.zapia.catarin.ws;

import br.com.zapia.catarin.authentication.JwtAuthenticationFilter;
import br.com.zapia.catarin.authentication.JwtTokenProvider;
import br.com.zapia.catarin.whatsApp.WhatsAppClone;
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
                        session.sendMessage(new TextMessage("token, valido"));
                        getCatarinWhatsApp().adicionarSession(session);
                    } else {
                        session.sendMessage(new TextMessage("token, invalido"));
                        session.close(CloseStatus.POLICY_VIOLATION);
                    }
                } catch (IOException e) {
                    logger.error("Token Ws", e);
                }
                break;
            default:
                try {
                    if (tokenProvider.validateTokenWs((String) session.getAttributes().get("token"))) {
                        getCatarinWhatsApp().processWebSocketMsg(session, dataResponse);
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
        super.afterConnectionClosed(session, status);
    }

    private WhatsAppClone getCatarinWhatsApp() {
        return applicationContext.getBean(WhatsAppClone.class);
    }

    @Override
    public boolean supportsPartialMessages() {
        return super.supportsPartialMessages();
    }
}
