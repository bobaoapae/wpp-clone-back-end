package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import modelo.Chat;
import modelo.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SerializadorWhatsApp {

    private ObjectMapper objectMapper;
    @Lazy
    @Autowired
    private SerializadorWhatsApp serializadorWhatsApp;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) throws IOException {
        ObjectNode chatNode = (ObjectNode) objectMapper.readTree(chat.toJson());
        ArrayNode msgsNode = objectMapper.createArrayNode();
        List<CompletableFuture<ObjectNode>> futures = new ArrayList<>();
        Collection<List<Message>> partition = Util.partition(chat.getAllMessages(), 5);
        partition.forEach(messages -> {
            messages.forEach(message -> {
                try {
                    futures.add(serializadorWhatsApp.serializarMsg(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
        Util.pegarResultadosFutures(futures).forEach(msgsNode::add);
        chatNode.set("msgs", msgsNode);
        chatNode.putObject("contact").setAll((ObjectNode) objectMapper.readTree(chat.getContact().toJson()));
        chatNode.put("picture", chat.getContact().getThumb());
        chatNode.put("type", chat.getJsObject().getProperty("kind").asString().getValue());
        chatNode.put("noEarlierMsgs", chat.noEarlierMsgs());
        return CompletableFuture.completedFuture(chatNode);
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<ObjectNode> serializarMsg(Message message) throws IOException {
        ObjectNode msgNode = (ObjectNode) objectMapper.readTree(message.toJson());
        if (message.getSender() != null) {
            msgNode.putObject("sender").setAll((ObjectNode) objectMapper.readTree(message.getSender().toJson()));
        }
        if (message.getChat() != null) {
            msgNode.put("unreadCount", message.getChat().getJsObject().getProperty("unreadCount").asNumber().getValue());
        }
        return CompletableFuture.completedFuture(msgNode);
    }
}
