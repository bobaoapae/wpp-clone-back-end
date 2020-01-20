package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public interface HandlerWebSocket {
    @Async
    CompletableFuture<WebSocketResponse> handle(WhatsAppClone whatsAppClone, Object payload) throws Exception;
}
