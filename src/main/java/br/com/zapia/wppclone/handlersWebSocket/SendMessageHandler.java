package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.SendMessageRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "sendMessage")
public class SendMessageHandler implements HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(WhatsAppClone whatsAppClone, Object payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SendMessageRequest sendMessageRequest = objectMapper.readValue((String) payload, SendMessageRequest.class);
        return whatsAppClone.getDriver().getFunctions().getChatById(sendMessageRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                if (Strings.isNullOrEmpty(sendMessageRequest.getQuotedMsg())) {
                    if (Strings.isNullOrEmpty(sendMessageRequest.getMedia())) {
                        return chat.sendMessage(sendMessageRequest.getMessage()).thenApply(jsValue -> {
                            return new WebSocketResponse(HttpStatus.OK);
                        });
                    } else if (!Strings.isNullOrEmpty(sendMessageRequest.getFileName())) {
                        return chat.sendFile(sendMessageRequest.getMedia(), sendMessageRequest.getFileName(), sendMessageRequest.getMessage()).thenApply(jsValue -> {
                            return new WebSocketResponse(HttpStatus.OK);
                        });
                    } else {
                        return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST));
                    }
                } else {
                    return whatsAppClone.getDriver().getFunctions().getMessageById(sendMessageRequest.getQuotedMsg()).thenCompose(message -> {
                        if (message != null) {
                            if (Strings.isNullOrEmpty(sendMessageRequest.getMedia())) {
                                return message.replyMessage(sendMessageRequest.getMessage()).thenApply(jsValue -> {
                                    return new WebSocketResponse(HttpStatus.OK);
                                });
                            } else if (!Strings.isNullOrEmpty(sendMessageRequest.getFileName())) {
                                return message.replyMessageWithFile(sendMessageRequest.getMedia(), sendMessageRequest.getFileName(), sendMessageRequest.getMessage()).thenApply(jsValue -> {
                                    return new WebSocketResponse(HttpStatus.OK);
                                });
                            } else {
                                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST));
                            }
                        } else {
                            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND, "Quoted Message"));
                        }
                    });
                }
            }
        });
    }
}
