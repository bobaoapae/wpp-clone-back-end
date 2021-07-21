package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractSendMessageHandler;
import br.com.zapia.wpp.api.model.payloads.SendMessageRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.GroupChat;
import br.com.zapia.wpp.client.docker.model.Message;
import br.com.zapia.wppclone.modelo.WhatsAppObjectWithIdProperty;
import br.com.zapia.wppclone.modelo.dto.WhatsAppObjectWithIdPropertyResponseDTO;
import br.com.zapia.wppclone.modelo.enums.WhatsAppObjectWithIdType;
import br.com.zapia.wppclone.servicos.UploadFileService;
import br.com.zapia.wppclone.servicos.WhatsAppObjectWithPropertyService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import br.com.zapia.wppclone.ws.WebSocketRequestSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Scope("usuario")
public class SendMessageHandler extends AbstractSendMessageHandler<WebSocketRequestSession> {

    private static final Logger logger = Logger.getLogger(SendMessageHandler.class.getName());

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;
    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private WhatsAppObjectWithPropertyService whatsAppObjectWithPropertyService;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, SendMessageRequest sendMessageRequest) throws JsonProcessingException {
        var usuario = webSocketRequestSession.getUsuario();
        return whatsAppClone.getWhatsAppClient().findChatById(sendMessageRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {

                var isSendOperatorNameEnabled = usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getEnviarNomeOperadores() && usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR");
                var textMsg = sendMessageRequest.getText();
                if (isSendOperatorNameEnabled) {
                    Message lastMessage = chat.getLastMsg();
                    var lastUserSendMessageProperty = whatsAppObjectWithPropertyService.buscarPropriedade(WhatsAppObjectWithIdType.CHAT, chat.getId(), "lastUserSendMessage");
                    var flagAppend = lastMessage == null || lastUserSendMessageProperty == null || !lastUserSendMessageProperty.getValue().equals(usuario.getUuid().toString());
                    if (flagAppend) {
                        if (sendMessageRequest.getFile() == null) {
                            textMsg = "*".concat(usuario.getNome()).concat(" diz:* ".concat(sendMessageRequest.getText()));
                        } else {
                            textMsg = "* Enviado por: ".concat(usuario.getNome()).concat("* \n".concat(sendMessageRequest.getText()));
                        }
                        if (lastUserSendMessageProperty == null) {
                            lastUserSendMessageProperty = new WhatsAppObjectWithIdProperty();
                            lastUserSendMessageProperty.setType(WhatsAppObjectWithIdType.CHAT);
                            lastUserSendMessageProperty.setWhatsAppId(chat.getId());
                            lastUserSendMessageProperty.setKey("lastUserSendMessage");
                            lastUserSendMessageProperty.setValue(usuario.getUuid().toString());
                            if (!whatsAppObjectWithPropertyService.salvar(lastUserSendMessageProperty)) {
                                logger.log(Level.SEVERE, "Fail on update lastUserSendMessage property of chat {" + chat.getId() + "} to value {" + usuario.getUuid().toString() + "}");
                            }
                        } else {
                            if (!whatsAppObjectWithPropertyService.alterarValor(lastUserSendMessageProperty.getUuid(), usuario.getUuid().toString())) {
                                logger.log(Level.SEVERE, "Fail on update lastUserSendMessage property of chat {" + chat.getId() + "} to value {" + usuario.getUuid().toString() + "}");
                            }

                            lastUserSendMessageProperty.setValue(usuario.getUuid().toString());
                        }
                        whatsAppClone.enviarEventoWpp(WhatsAppClone.TypeEventWebSocket.CHANGE_PROPERTY_CHAT, modelMapper.map(lastUserSendMessageProperty, WhatsAppObjectWithIdPropertyResponseDTO.class));
                    }
                }


                var msgBuilder = new SendMessageRequest.Builder(chat.getId());

                msgBuilder.withText(textMsg);

                if (!Strings.isNullOrEmpty(sendMessageRequest.getQuotedMsg())) {
                    msgBuilder.withQuotedMsg(sendMessageRequest.getQuotedMsg());
                }

                if (sendMessageRequest.getFile() != null) {
                    var file = uploadFileService.getAndRemoveFileUploaded(sendMessageRequest.getFile().getUuid());
                    if (file != null) {
                        var uuidUpload = whatsAppClone.getWhatsAppClient().uploadFile(file.getOriginalName(), file.getTempFile()).join();
                        msgBuilder.withFile(uuidUpload, fileBuilder -> fileBuilder.withCaption(sendMessageRequest.getText()).withForceDocument(sendMessageRequest.getFile().isForceDocument()));
                    }
                }

                if (sendMessageRequest.getLocation() != null) {
                    msgBuilder.withLocation(sendMessageRequest.getLocation().getLat(), sendMessageRequest.getLocation().getLng(), locationBuilder -> locationBuilder.withName(sendMessageRequest.getLocation().getName()));
                }

                if (chat instanceof GroupChat && sendMessageRequest.getMentionedContacts() != null) {
                    for (String mentionedContact : sendMessageRequest.getMentionedContacts()) {
                        msgBuilder.withMentionToContact(mentionedContact);
                    }
                }

                if (sendMessageRequest.getvCard() != null) {
                    msgBuilder.withVCard(sendMessageRequest.getvCard().getName(), sendMessageRequest.getvCard().getTelephone());
                }

                if (sendMessageRequest.getWebSite() != null) {
                    msgBuilder.withWebSite(sendMessageRequest.getWebSite());
                }

                if (sendMessageRequest.getButtons() != null) {
                    msgBuilder.withButtons(sendMessageRequest.getButtons().getTitle(), sendMessageRequest.getButtons().getFooter(), buttonsBuilder -> {
                        Arrays.stream(sendMessageRequest.getButtons().getButtons()).forEach(buttonsBuilder::withButton);
                    });
                }

                if (sendMessageRequest.getWhatsAppList() != null) {
                    msgBuilder.withList(listBuilder -> {
                        listBuilder
                                .withTitle(sendMessageRequest.getWhatsAppList().getTitle())
                                .withDescription(sendMessageRequest.getWhatsAppList().getDescription())
                                .withFooter(sendMessageRequest.getWhatsAppList().getFooter())
                                .withButtonText(sendMessageRequest.getWhatsAppList().getButtonText());
                        for (SendMessageRequest.Section section : sendMessageRequest.getWhatsAppList().getSections()) {
                            listBuilder.withSection(section.getTitle(), sectionBuilder -> {
                                for (SendMessageRequest.SectionItem row : section.getRows()) {
                                    sectionBuilder.withRow(row.getTitle(), row.getDescription()).build();
                                }
                            });
                        }
                    });
                }

                return chat.sendMessage(msgBuilder.build()).thenApply(message -> {
                    return new WebSocketResponse(org.eclipse.jetty.http.HttpStatus.OK_200, whatsAppClone.getWhatsAppSerializer().serializeMsg(message));
                });
            }
        });
    }
}
