package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.MediaMessage;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.DownloadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "downloadMedia")
public class DownloadMediaHandler extends HandlerWebSocket<String> {

    @Autowired
    private DownloadFileService downloadFileService;

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, String msgId) {
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

    @Override
    public Class<String> getClassType() {
        return String.class;
    }
}
