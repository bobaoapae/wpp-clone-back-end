package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractDownloadMediaHandler;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.MediaMessage;
import br.com.zapia.wppclone.servicos.DownloadFileService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import br.com.zapia.wppclone.ws.WebSocketRequestSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
public class DownloadMediaHandler extends AbstractDownloadMediaHandler<WebSocketRequestSession> {

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;
    @Autowired
    private DownloadFileService downloadFileService;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, String msgId) {
        return whatsAppClone.getWhatsAppClient().findMessage(msgId).thenCompose(msg -> {
            if (msg == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else if (msg instanceof MediaMessage mediaMessage) {
                return mediaMessage.download().thenApply(file -> {
                    if (file != null) {
                        String key = downloadFileService.addFileToFutureDownload(file);
                        return new WebSocketResponse(HttpStatus.OK.value(), key);
                    } else {
                        return new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Donwload Failed");
                    }
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST.value()));
            }
        });
    }
}
