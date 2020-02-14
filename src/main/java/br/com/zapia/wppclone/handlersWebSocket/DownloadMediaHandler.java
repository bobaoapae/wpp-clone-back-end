package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.servicos.DownloadMediaService;
import modelo.MediaMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "downloadMedia")
public class DownloadMediaHandler extends HandlerWebSocket {

    @Autowired
    private DownloadMediaService downloadMediaService;

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) {
        return whatsAppClone.getDriver().getFunctions().getMessageById((String) payload).thenCompose(msg -> {
            if (msg == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else if (msg instanceof MediaMessage) {
                return ((MediaMessage) msg).downloadMedia(5).thenCompose(file -> {
                    if (file == null) {
                        return ((MediaMessage) msg).downloadMedia(20);
                    } else {
                        return CompletableFuture.completedFuture(file);
                    }
                }).thenApply(file -> {
                    if (file != null) {
                        String key = downloadMediaService.addFileToFutureDownload(file);
                        return new WebSocketResponse(HttpStatus.OK, key);
                    } else {
                        return new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR, "file null");
                    }
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST));
            }
        });
    }
}
