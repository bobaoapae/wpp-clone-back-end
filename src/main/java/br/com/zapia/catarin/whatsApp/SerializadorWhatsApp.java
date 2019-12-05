package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import modelo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Scope("usuario")
@Service
public class SerializadorWhatsApp {

    private ObjectMapper objectMapper;
    private Logger log = Logger.getLogger(SerializadorWhatsApp.class.getName());
    @Lazy
    @Autowired
    private SerializadorWhatsApp serializadorWhatsApp;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) throws ExecutionException {
        ObjectNode chatNode = converterParaObjectNode(chat);
        Objects.requireNonNull(chatNode).putObject("contact").setAll(Objects.requireNonNull(converterParaObjectNode(chat.getContact())));
        chatNode.put("picture", chat.getContact().getThumb());
        chatNode.put("type", chat.getJsObject().getProperty("kind").asString().getValue());
        ((ObjectNode) chatNode.get("contact")).remove("type");
        chatNode.remove(Arrays.asList("lastReceivedKey", "pendingMsgs"));
        ArrayNode arrayNode = objectMapper.createArrayNode();
        List<Message> allMessages = chat.getAllMessages();
        int partitionSize = allMessages.size() < Runtime.getRuntime().availableProcessors() ? allMessages.size() : allMessages.size() / Runtime.getRuntime().availableProcessors();
        Collection<List<Message>> partition = Util.partition(allMessages, partitionSize);
        List<CompletableFuture<ArrayNode>> futures = new ArrayList<>();
        partition.forEach(messages -> {
            futures.add(serializadorWhatsApp.serializarMsg(messages));
        });
        Util.pegarResultadosFutures(futures).forEach(arrayNode::addAll);
        chatNode.set("msgs", arrayNode);
        chatNode.put("noEarlierMsgs", chat.noEarlierMsgs());
        chatNode.put("isVisible", chat.isVisible());
        chatNode.put("unreadCount", (Double) utils.Util.convertJSValue(chat.getJsObject().getProperty("unreadCount")));
        return CompletableFuture.completedFuture(chatNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarChat(List<Chat> chats) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        chats.forEach(chat -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarChat(chat)));
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async
    public CompletableFuture<ObjectNode> serializarMsg(Message message) throws ExecutionException {
        ObjectNode msgNode = Objects.requireNonNull(converterParaObjectNode(message));
        if (message.getSender() != null && (message.getChat() instanceof GroupChat)) {
            msgNode.putObject("sender").setAll(Objects.requireNonNull(converterParaObjectNode(message.getSender())));
        }
        if (message instanceof MediaMessage) {
            msgNode.put("filename", ((MediaMessage) message).getFileName());
            if (message.getJsObject().hasProperty("pageCount") && message.getJsObject().getProperty("pageCount").isNumber()) {
                msgNode.put("pageCount", message.getJsObject().getProperty("pageCount").asNumber().getValue());
            }
        }
        msgNode.remove(Arrays.asList("self", "invis", "clientUrl", "directPath", "filehash", "uploadhash", "mediaKey", "mediaKeyTimestamp"));
        return CompletableFuture.completedFuture(msgNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarMsg(List<Message> messages) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        messages.forEach(message -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarMsg(message)));
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    private ObjectNode converterParaObjectNode(WhatsappObject whatsappObject) {
        try {
            return (ObjectNode) objectMapper.readTree(whatsappObject.toJson());
        } catch (IOException e) {
            log.log(Level.SEVERE, "ConverterParaObjectNode", e);
            return null;
        }
    }
}
