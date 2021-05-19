package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.IHandlerWebSocket;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public interface IHandlerWebSocketSpring<T> extends IHandlerWebSocket<T> {
    @Async
    CompletableFuture<WebSocketResponse> handle(Usuario usuario, T o) throws Exception;
}
