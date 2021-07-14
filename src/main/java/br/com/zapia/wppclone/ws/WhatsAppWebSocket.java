package br.com.zapia.wppclone.ws;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.authentication.JwtTokenProvider;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.OperadoresService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;

@Service
public class WhatsAppWebSocket extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebSocket.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private OperadoresService operadoresService;
    @Autowired
    private ObjectMapper mapper;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            try {
                WebSocketRequestSession webSocketRequest = mapper.readValue(message.getPayload(), WebSocketRequestSession.class);
                if (webSocketRequest.getWebSocketRequestPayLoad().getEvent() == EventWebSocket.Token) {
                    if (tokenProvider.validateTokenWs(webSocketRequest.getWebSocketRequestPayLoad().getPayload())) {
                        Usuario usuario = UsuarioScopedContext.getUsuario();
                        if (!usuario.getUsuarioResponsavelPelaInstancia().equals(usuario) && !usuario.getUsuarioResponsavelPelaInstancia().isAtivo()) {
                            session.sendMessage(new TextMessage(new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.FORBIDDEN.value(), "Usuário Principal Inativado")).toString()));
                        } else if (usuario.isAtivo()) {
                            session.getAttributes().put("token", webSocketRequest.getWebSocketRequestPayLoad().getPayload());
                            session.getAttributes().put("usuario", usuario);
                            if (usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
                                operadoresService.adicionarSessao(session);
                            }
                            session.sendMessage(new TextMessage(new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.OK.value())).toString()));
                            getWhatsAppClone().adicionarSession(session);
                        } else {
                            session.sendMessage(new TextMessage(new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.FORBIDDEN.value(), "Usuário Inativado")).toString()));
                        }
                    } else {
                        session.sendMessage(new TextMessage(new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.UNAUTHORIZED.value())).toString()));
                    }
                } else {
                    Object usuarioObj = session.getAttributes().get("usuario");
                    boolean result = false;
                    if (usuarioObj instanceof Usuario usuario && usuario.isAtivo()) {
                        UsuarioScopedContext.setUsuario(usuario);
                        result = true;
                    } else if (tokenProvider.validateTokenWs((String) session.getAttributes().get("token"))) {
                        result = true;
                    }
                    if (result) {
                        webSocketRequest.setUsuario((Usuario) session.getAttributes().get("usuario"));
                        getWhatsAppClone().processWebSocketMsg(session, webSocketRequest);
                    } else {
                        session.sendMessage(new TextMessage(new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.UNAUTHORIZED.value())).toString()));
                    }
                }
            } catch (JsonProcessingException e) {
                session.sendMessage(new TextMessage(new WsMessage("error", ExceptionUtils.getStackTrace(e)).toString()));
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
            Object usuarioObj = session.getAttributes().get("usuario");
            boolean result = false;
            if (usuarioObj instanceof Usuario usuario && usuario.isAtivo()) {
                UsuarioScopedContext.setUsuario((Usuario) usuarioObj);
                result = true;
            } else if (tokenProvider.validateTokenWs((String) session.getAttributes().get("token"))) {
                result = true;
                usuarioObj = UsuarioScopedContext.getUsuario();
            }
            if (result) {
                if (!status.equals(CloseStatus.GOING_AWAY)) {
                    if (((Usuario) usuarioObj).getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
                        operadoresService.removerSessao(session);
                    }
                    getWhatsAppClone().removerSession(session);
                }
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
