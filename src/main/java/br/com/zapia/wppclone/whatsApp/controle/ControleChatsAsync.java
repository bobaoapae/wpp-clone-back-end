package br.com.zapia.wppclone.whatsApp.controle;

import br.com.zapia.wppclone.whatsApp.modelo.ChatBotWppCloneSpring;
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


    private final List<ChatBotWppCloneSpring> chats;
    @Autowired
    private ApplicationContext applicationContext;

    private ControleChatsAsync() {
        this.chats = new ConcurrentArrayList<>();
    }

    public ChatBotWppCloneSpring addChat(Chat chat) {
        try {
            if (chat instanceof UserChat) {
                ChatBotWppCloneSpring bean = applicationContext.getBean(ChatBotWppCloneSpring.class);
                bean.inicializarChatBotWppClone(chat);
                chats.add(bean);
                return bean;
            }
        } catch (Exception ex) {
            chat.getDriver().onError(ex);
        }
        return null;
    }

    public ChatBotWppCloneSpring getChatAsyncByChat(Chat chat) {
        for (ChatBotWppCloneSpring chatt : chats) {
            if (chatt.getChatBotWppClone().getChat().equals(chat)) {
                return chatt;
            }
        }
        return addChat(chat);
    }

    public ChatBotWppCloneSpring getChatAsyncByChat(String chatid) {
        for (ChatBotWppCloneSpring chatt : chats) {
            if (chatt.getChatBotWppClone().getChat().getId().equals(chatid)) {
                return chatt;
            }
        }
        return null;
    }

    public List<ChatBotWppCloneSpring> getChats() {
        return chats;
    }

    public void finalizar() {
        for (ChatBotWppCloneSpring chatt : chats) {
            chatt.getChatBotWppClone().finalizar();
        }
    }
}
