package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import modelo.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Scope("usuario")
@Service
public class SerializadorWhatsApp {

    private ObjectMapper objectMapper;
    private LoadingCache<WhatsappObjectWithId, ObjectNode> cache;
    private LoadingCache<Chat, ObjectNode> cacheChats;
    private LoadingCache<Message, ObjectNode> cacheMsgs;
    @Lazy
    private final SerializadorWhatsApp serializadorWhatsApp;

    public SerializadorWhatsApp(SerializadorWhatsApp serializadorWhatsApp) {
        this.serializadorWhatsApp = serializadorWhatsApp;
    }

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build(
                        new CacheLoader<>() {
                            public ObjectNode load(WhatsappObjectWithId whatsappObject) throws IOException {
                                return (ObjectNode) objectMapper.readTree(whatsappObject.toJson());
                            }
                        });
        cacheChats = CacheBuilder.newBuilder()
                .maximumSize(200)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<>() {
                            public ObjectNode load(Chat chat) throws ExecutionException {
                                ObjectNode chatNode;
                                try {
                                    chatNode = cache.get(chat);
                                } catch (IllegalStateException e) {
                                    cache.invalidate(chat);
                                    chatNode = cache.get(chat);
                                }
                                chatNode.putObject("contact").setAll(cache.get(chat.getContact()));
                                chatNode.put("picture", chat.getContact().getThumb());
                                chatNode.put("type", chat.getJsObject().getProperty("kind").asString().getValue());
                                ((ObjectNode) chatNode.get("contact")).remove("type");
                                chatNode.remove(Arrays.asList("lastReceivedKey", "pendingMsgs"));
                                return chatNode;
                            }
                        });
        cacheMsgs = CacheBuilder.newBuilder()
                .maximumSize(2000)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<>() {
                            public ObjectNode load(Message message) throws ExecutionException {
                                ObjectNode msgNode;
                                try {
                                    msgNode = cache.get(message);
                                } catch (IllegalStateException e) {
                                    cache.invalidate(message);
                                    msgNode = cache.get(message);
                                }
                                if (message.getSender() != null && (message.getChat() instanceof GroupChat)) {
                                    msgNode.putObject("sender").setAll(cache.get(message.getSender()));
                                }
                                if (message instanceof MediaMessage) {
                                    msgNode.put("filename", ((MediaMessage) message).getFileName());
                                    if (message.getJsObject().hasProperty("pageCount") && message.getJsObject().getProperty("pageCount").isNumber()) {
                                        msgNode.put("pageCount", message.getJsObject().getProperty("pageCount").asNumber().getValue());
                                    }
                                }
                                msgNode.remove(Arrays.asList("self", "invis", "clientUrl", "directPath", "filehash", "uploadhash", "mediaKey", "mediaKeyTimestamp"));
                                return msgNode;
                            }
                        });
    }

    @Async
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) throws ExecutionException {
        ObjectNode chatNode;
        try {
            chatNode = cacheChats.get(chat);
        } catch (IllegalStateException e) {
            cacheChats.invalidate(chat);
            chatNode = cacheChats.get(chat);
        }
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
        ObjectNode objectNode;
        try {
            objectNode = cacheMsgs.get(message);
        } catch (IllegalStateException e) {
            cacheMsgs.invalidate(message);
            objectNode = cacheMsgs.get(message);
        }
        objectNode.put("ack", (Double) utils.Util.convertJSValue(message.getJsObject().getProperty("ack")));
        return CompletableFuture.completedFuture(objectNode);
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
}
