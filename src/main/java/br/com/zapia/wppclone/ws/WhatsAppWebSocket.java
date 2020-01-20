package br.com.zapia.wppclone.ws;

import br.com.zapia.wppclone.authentication.JwtAuthenticationFilter;
import br.com.zapia.wppclone.authentication.JwtTokenProvider;
import br.com.zapia.wppclone.payloads.WebSocketRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.threadly.util.ExceptionUtils;

import java.io.IOException;

@Service
public class WhatsAppWebSocket extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            try {
                WebSocketRequest webSocketRequest = mapper.readValue(message.getPayload(), WebSocketRequest.class);
                if (webSocketRequest.getWebSocketRequestPayLoad().getEvent().equalsIgnoreCase("token")) {
                    if (tokenProvider.validateTokenWs(webSocketRequest.getWebSocketRequestPayLoad().getPayload())) {
                        session.getAttributes().put("token", webSocketRequest.getWebSocketRequestPayLoad().getPayload());
                        session.sendMessage(new TextMessage(new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.OK)).toString()));
                        getWhatsAppClone().adicionarSession(session);
                    } else {
                        session.sendMessage(new TextMessage(new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.UNAUTHORIZED)).toString()));
                    }
                } else {
                    if (tokenProvider.validateTokenWs((String) session.getAttributes().get("token"))) {
                        getWhatsAppClone().processWebSocketMsg(session, webSocketRequest);
                    }
                }
            } catch (JsonProcessingException e) {
                session.sendMessage(new TextMessage(new WsMessage("error", ExceptionUtils.stackToString(e)).toString()));
                throw e;
            }
        } catch (IOException e) {
            logger.error("handleTextMessage", e);
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
