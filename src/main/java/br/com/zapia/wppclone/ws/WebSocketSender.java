package br.com.zapia.wppclone.ws;

import br.com.zapia.wppclone.authentication.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


@Service
public class WebSocketSender {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Async
    public CompletableFuture<Void> sendToWs(WebSocketSession ws, WsMessage  message){
        try {
            ws.sendMessage(new TextMessage(message.toString()));
        } catch (Exception e) {
            logger.error("EnviarParaWs", e);
            try {
                ws.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (IOException ex) {
                logger.error("CloseWs", ex);
            }
            throw new RuntimeException(e);
        }
        return null;
    }
}
