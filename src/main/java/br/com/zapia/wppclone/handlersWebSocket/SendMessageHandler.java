package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.payloads.SendMessageRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.Message;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.modelo.WhatsAppObjectWithIdProperty;
import br.com.zapia.wppclone.modelo.dto.WhatsAppObjectWithIdPropertyResponseDTO;
import br.com.zapia.wppclone.modelo.enums.WhatsAppObjectWithIdType;
import br.com.zapia.wppclone.servicos.UploadFileService;
import br.com.zapia.wppclone.servicos.WhatsAppObjectWithPropertyService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Component
@Scope("usuario")
@HandlerWebSocketEvent(event = EventWebSocket.SendMessage)
public class SendMessageHandler extends HandlerWebSocket<SendMessageRequest> {

    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private WhatsAppObjectWithPropertyService whatsAppObjectWithPropertyService;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, SendMessageRequest sendMessageRequest) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findChatById(sendMessageRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                Message lastMessage = chat.getLastMsg();
                var lastUserSendMessageProperty = whatsAppObjectWithPropertyService.buscarPropriedade(WhatsAppObjectWithIdType.CHAT, chat.getId(), "lastUserSendMessage");
                var flagAppend = lastMessage == null || lastUserSendMessageProperty == null || !lastUserSendMessageProperty.getValue().equals(usuario.getUuid().toString());
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
                if (flagAppend) {
                    if (lastUserSendMessageProperty == null) {
                        lastUserSendMessageProperty = new WhatsAppObjectWithIdProperty();
                        lastUserSendMessageProperty.setType(WhatsAppObjectWithIdType.CHAT);
                        lastUserSendMessageProperty.setWhatsAppId(chat.getId());
                        lastUserSendMessageProperty.setKey("lastUserSendMessage");
                        lastUserSendMessageProperty.setValue(usuario.getUuid().toString());
                        whatsAppObjectWithPropertyService.salvar(lastUserSendMessageProperty);
                    } else {
                        if (!whatsAppObjectWithPropertyService.alterarValor(lastUserSendMessageProperty.getUuid(), usuario.getUuid().toString())) {
                            logger.log(Level.SEVERE, "Fail on update lastUserSendMessage property of chat {" + chat.getId() + "} to value {" + usuario.getUuid().toString() + "}");
                        } else {
                            lastUserSendMessageProperty.setValue(usuario.getUuid().toString());
                        }
                    }
                    whatsAppClone.enviarEventoWpp(WhatsAppClone.TypeEventWebSocket.CHANGE_PROPERTY_CHAT, modelMapper.map(lastUserSendMessageProperty, WhatsAppObjectWithIdPropertyResponseDTO.class));
                }
                if (Strings.isNullOrEmpty(sendMessageRequest.getQuotedMsg())) {
                    if (Strings.isNullOrEmpty(sendMessageRequest.getFileUUID())) {
                        return chat.sendMessage(textMsg).thenApply(message -> {
                            return new WebSocketResponse(HttpStatus.OK.value(), whatsAppClone.getWhatsAppSerializer().serializeMsg(message).join());
                        });
                    } else {
                        File file = uploadFileService.getFileUploaded(sendMessageRequest.getFileUUID());
                        String fileName = file.getName().split("#")[0];
                        if (sendMessageRequest.getMessage().equalsIgnoreCase("sticker")) {
                            fileName += ".webp";
                        }
                        return chat.sendMessage(file, fileName, captionImage).thenApply(mediaMessage -> {
                            uploadFileService.removeFileUploaded(sendMessageRequest.getFileUUID());
                            return new WebSocketResponse(HttpStatus.OK.value(), whatsAppClone.getWhatsAppSerializer().serializeMsg(mediaMessage).join());

                        });
                    }
                } else {
                    return whatsAppClone.getWhatsAppClient().findMessage(sendMessageRequest.getQuotedMsg()).thenCompose(message -> {
                        if (message != null) {
                            if (Strings.isNullOrEmpty(sendMessageRequest.getFileUUID())) {
                                return message.reply(textMsg).thenApply(message1 -> {
                                    return new WebSocketResponse(HttpStatus.OK.value(), whatsAppClone.getWhatsAppSerializer().serializeMsg(message1).join());
                                });
                            } else {
                                File file = uploadFileService.getFileUploaded(sendMessageRequest.getFileUUID());
                                return message.reply(file, file.getName().split("#")[0], captionImage).thenApply(mediaMessage -> {
                                    uploadFileService.removeFileUploaded(sendMessageRequest.getFileUUID());
                                    return new WebSocketResponse(HttpStatus.OK.value(), whatsAppClone.getWhatsAppSerializer().serializeMsg(mediaMessage).join());
                                });
                            }
                        } else {
                            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value(), "Quoted Message"));
                        }
                    });
                }
            }
        });
    }

    @Override
    public Class<SendMessageRequest> getClassType() {
        return SendMessageRequest.class;
    }
}
