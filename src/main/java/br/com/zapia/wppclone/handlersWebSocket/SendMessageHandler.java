package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.SendMessageRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "sendMessage")
public class SendMessageHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SendMessageRequest sendMessageRequest = objectMapper.readValue((String) payload, SendMessageRequest.class);
        return whatsAppClone.getDriver().getFunctions().getChatById(sendMessageRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                return chat.getLastMsg().thenCompose(lastMessage -> {
                    boolean flagAppend = lastMessage == null || !usuario.getUuid().toString().equals(lastMessage.getCustomProperty("usuario").join());
                    String textMsg;
                    if (flagAppend && usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getEnviarNomeOperadores() && usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
                        textMsg = "*".concat(usuario.getNome()).concat(" diz:* ").concat(sendMessageRequest.getMessage());
                    } else {
                        textMsg = sendMessageRequest.getMessage();
                    }
                    String captionImage;
                    if (flagAppend && usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getEnviarNomeOperadores() && usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
                        captionImage = "*Enviado por: ".concat(usuario.getNome()).concat("*");
                    } else {
                        captionImage = sendMessageRequest.getMessage();
                    }
                    if (Strings.isNullOrEmpty(sendMessageRequest.getQuotedMsg())) {
                        if (Strings.isNullOrEmpty(sendMessageRequest.getMedia())) {
                            return chat.sendMessage(textMsg).thenCompose(message -> {
                                return message.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                                    return new WebSocketResponse(HttpStatus.OK, message.toJson());
                                });
                            });
                        } else if (!Strings.isNullOrEmpty(sendMessageRequest.getFileName())) {
                            return chat.sendFile(sendMessageRequest.getMedia(), sendMessageRequest.getFileName(), captionImage).thenCompose(mediaMessage -> {
                                return mediaMessage.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                                    return new WebSocketResponse(HttpStatus.OK, mediaMessage.toJson());
                                });
                            });
                        } else {
                            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST));
                        }
                    } else {
                        return whatsAppClone.getDriver().getFunctions().getMessageById(sendMessageRequest.getQuotedMsg()).thenCompose(message -> {
                            if (message != null) {
                                if (Strings.isNullOrEmpty(sendMessageRequest.getMedia())) {
                                    return message.replyMessage(textMsg).thenCompose(message1 -> {
                                        return message1.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                                            return new WebSocketResponse(HttpStatus.OK, message1.toJson());
                                        });
                                    });
                                } else if (!Strings.isNullOrEmpty(sendMessageRequest.getFileName())) {
                                    return message.replyMessageWithFile(sendMessageRequest.getMedia(), sendMessageRequest.getFileName(), captionImage).thenCompose(mediaMessage -> {
                                        return mediaMessage.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                                            return new WebSocketResponse(HttpStatus.OK, mediaMessage.toJson());
                                        });
                                    });
                                } else {
                                    return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST));
                                }
                            } else {
                                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND, "Quoted Message"));
                            }
                        });
                    }
                });
            }
        });
    }
}
