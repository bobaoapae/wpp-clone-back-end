package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import modelo.Chat;
import modelo.Message;
import modelo.WhatsappObject;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Scope("usuario")
@Service
public class SerializadorWhatsApp {

    private ObjectMapper objectMapper;
    private Logger log = Logger.getLogger(SerializadorWhatsApp.class.getName());

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) {
        ObjectNode chatNode = Objects.requireNonNull(converterParaObjectNode(chat));
        chatNode.put("picture", chat.getContact().getThumb());
        return CompletableFuture.completedFuture(chatNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarChat(List<Chat> chats) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        chats.forEach(chat -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarChat(chat)));
            } catch (Exception e) {
                log.log(Level.SEVERE, "SerializarChat", e);
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async
    public CompletableFuture<ObjectNode> serializarMsg(Message message) {
        ObjectNode msgNode = Objects.requireNonNull(converterParaObjectNode(message));
        return CompletableFuture.completedFuture(msgNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarMsg(List<Message> messages) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        messages.forEach(message -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarMsg(message)));
            } catch (Exception e) {
                log.log(Level.SEVERE, "SerializarMsg", e);
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    private ObjectNode converterParaObjectNode(WhatsappObject whatsappObject) {
        try {
            return (ObjectNode) objectMapper.readTree(utils.Util.callFunction(whatsappObject.getJsObject(), "toJSON").asObject().toJSONString());
        } catch (IOException e) {
            log.log(Level.SEVERE, "ConverterParaObjectNode", e);
            return null;
        }
    }
}
