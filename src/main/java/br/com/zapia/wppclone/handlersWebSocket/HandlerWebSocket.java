package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public abstract class HandlerWebSocket<T> implements IHandlerWebSocketSpring<T> {

    protected static Logger logger = Logger.getLogger(HandlerWebSocket.class.getName());

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;

    @Override
    public CompletableFuture<WebSocketResponse> handle(T o) throws Exception {
        throw new NotImplementedException();
    }
}
