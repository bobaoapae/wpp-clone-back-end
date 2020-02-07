package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public abstract class HandlerWebSocket {

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;

    @Async
    abstract public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws Exception;
}
