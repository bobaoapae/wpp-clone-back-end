package br.com.zapia.catarin.whatsApp.controle;

import br.com.zapia.catarin.whatsApp.modelo.ChatBotCatarinSpring;
import modelo.Chat;
import modelo.UserChat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.threadly.concurrent.collections.ConcurrentArrayList;

import java.util.List;

@Controller
@Scope("usuario")
public class ControleChatsAsync {


    private final List<ChatBotCatarinSpring> chats;
    @Autowired
    private ApplicationContext applicationContext;

    private ControleChatsAsync() {
        this.chats = new ConcurrentArrayList<>();
    }

    public ChatBotCatarinSpring addChat(Chat chat) {
        try {
            if (chat instanceof UserChat) {
                ChatBotCatarinSpring bean = applicationContext.getBean(ChatBotCatarinSpring.class);
                bean.inicializarChatBotCatarin(chat, true);
                chats.add(bean);
                return bean;
            }
        } catch (Exception ex) {
            chat.getDriver().onError(ex);
        }
        return null;
    }

    public ChatBotCatarinSpring getChatAsyncByChat(Chat chat) {
        for (ChatBotCatarinSpring chatt : chats) {
            if (chatt.getChatBotCatarin().getChat().equals(chat)) {
                return chatt;
            }
        }
        return addChat(chat);
    }

    public ChatBotCatarinSpring getChatAsyncByChat(String chatid) {
        for (ChatBotCatarinSpring chatt : chats) {
            if (chatt.getChatBotCatarin().getChat().getId().equals(chatid)) {
                return chatt;
            }
        }
        return null;
    }

    public List<ChatBotCatarinSpring> getChats() {
        return chats;
    }

    public void finalizar() {
        for (ChatBotCatarinSpring chatt : chats) {
            chatt.getChatBotCatarin().finalizar();
        }
    }
}
