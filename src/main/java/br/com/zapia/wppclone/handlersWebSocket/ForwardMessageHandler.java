package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.payloads.ForwardMessagesRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.Chat;
import br.com.zapia.wpp.client.docker.model.Message;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
@HandlerWebSocketEvent(event = EventWebSocket.ForwardMessage)
public class ForwardMessageHandler extends HandlerWebSocket<ForwardMessagesRequest> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, ForwardMessagesRequest forwardMessagesRequest) throws JsonProcessingException {
        List<Chat> chats = new ArrayList<>();
        List<Message> msgs = new ArrayList<>();
        List<CompletableFuture<Message>> futuresMessage = new ArrayList<>();
        List<CompletableFuture<Chat>> futuresChat = new ArrayList<>();
        for (String idMsg : forwardMessagesRequest.getIdsMsgs()) {
            futuresMessage.add(whatsAppClone.getWhatsAppClient().findMessage(idMsg));
        }
        for (String idChat : forwardMessagesRequest.getIdsChats()) {
            futuresChat.add(whatsAppClone.getWhatsAppClient().findChatById(idChat));
        }
        return CompletableFuture.allOf(futuresMessage.toArray(new CompletableFuture[0])).thenCompose(aVoid -> {
            return CompletableFuture.allOf(futuresChat.toArray(new CompletableFuture[0])).thenAccept(aVoid1 -> {
                for (CompletableFuture<Message> messageCompletableFuture : futuresMessage) {
                    Message msg = messageCompletableFuture.join();
                    if (msg != null) {
                        msgs.add(msg);
                    }
                }
                for (CompletableFuture<Chat> chatCompletableFuture : futuresChat) {
                    Chat chat = chatCompletableFuture.join();
                    if (chat != null) {
                        chats.add(chat);
                    }
                }
            });
        }).thenCompose(aVoid -> {
            return whatsAppClone.getWhatsAppClient().findChatById(msgs.get(0).getContact().getId()).thenCompose(chat -> {
                if (chats.size() > 0 && msgs.size() > 0) {
                    return chat.forwardMessages(chats.toArray(Chat[]::new), msgs.toArray(Message[]::new)).thenApply(jsValue -> {
                        return new WebSocketResponse(HttpStatus.OK.value());
                    });
                } else {
                    return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST.value()));
                }
            });
        });
    }

    @Override
    public Class<ForwardMessagesRequest> getClassType() {
        return ForwardMessagesRequest.class;
    }
}
