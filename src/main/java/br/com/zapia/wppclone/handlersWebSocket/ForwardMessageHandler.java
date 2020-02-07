package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.ForwardMessagesRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import modelo.Chat;
import modelo.Message;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "forwardMessage")
public class ForwardMessageHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ForwardMessagesRequest forwardMessagesRequest = objectMapper.readValue((String) payload, ForwardMessagesRequest.class);
        List<Chat> chats = new ArrayList<>();
        List<Message> msgs = new ArrayList<>();
        List<CompletableFuture<Message>> futuresMessage = new ArrayList<>();
        List<CompletableFuture<Chat>> futuresChat = new ArrayList<>();
        for (String idMsg : forwardMessagesRequest.getIdsMsgs()) {
            futuresMessage.add(whatsAppClone.getDriver().getFunctions().getMessageById(idMsg));
        }
        for (String idChat : forwardMessagesRequest.getIdsChats()) {
            futuresChat.add(whatsAppClone.getDriver().getFunctions().getChatById(idChat));
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
            if (chats.size() > 0 && msgs.size() > 0) {
                return msgs.get(0).getChat().forwardMessage(msgs.toArray(Message[]::new), chats.toArray(Chat[]::new)).thenApply(jsValue -> {
                    return new WebSocketResponse(HttpStatus.OK);
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST));
            }
        });
    }
}
