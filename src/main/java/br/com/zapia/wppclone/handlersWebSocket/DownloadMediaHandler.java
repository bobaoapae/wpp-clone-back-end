package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.DownloadMediaResponse;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import modelo.MediaMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@HandlerWebSocketEvent(event = "downloadMedia")
public class DownloadMediaHandler extends HandlerWebSocket {
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
                        try {
                            String contentType = Files.probeContentType(file.toPath());
                            byte[] data = Files.readAllBytes(file.toPath());
                            String fileName = ((MediaMessage) msg).getFileName();
                            if (fileName.isEmpty()) {
                                fileName = file.getName();
                            }
                            String base64str = Base64.getEncoder().encodeToString(data);
                            StringBuilder sb = new StringBuilder();
                            sb.append("data:");
                            sb.append(contentType);
                            sb.append(";base64,");
                            sb.append(base64str);
                            return new WebSocketResponse(HttpStatus.OK, new DownloadMediaResponse(fileName, sb.toString()));
                        } catch (IOException e) {
                            whatsAppClone.getLogger().log(Level.SEVERE, "Convert to Base64", e);
                            return new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getMessage(e));
                        }
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
