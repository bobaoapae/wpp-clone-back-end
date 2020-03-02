package br.com.zapia.wppclone.whatsApp.controle;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.servicos.SendEmailService;
import br.com.zapia.wppclone.whatsApp.modelo.ChatBotWppCloneSpring;
import modelo.BindMsgListennerException;
import modelo.Chat;
import modelo.UserChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.threadly.concurrent.collections.ConcurrentArrayList;

import javax.annotation.PreDestroy;
import java.util.List;

@Controller
@Scope("usuario")
public class ControleChatsAsync {


    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<ChatBotWppCloneSpring> chats;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private SendEmailService sendEmailService;
    @Autowired
    private UsuarioPrincipalAutoWired usuarioPrincipal;

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
        } catch (BindMsgListennerException b) {
            try {
                sendEmailService.sendEmail("joao@zapia.com.br", "Driver API WhatsApp", "Ocorreu um erro ao realizar o bind do " +
                        "JavaListenner no evento de nova mensagem do chat: " + chat.getId() + " - Sessão: " + usuarioPrincipal.getUsuario().getLogin());
            } catch (Exception e) {
                logger.error("EnvioEmailErroBindMsg", e);
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
        return null;
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

    @PreDestroy
    public void finalizar() {
        for (ChatBotWppCloneSpring chatt : chats) {
            try {
                chatt.getChatBotWppClone().finalizar();
            } catch (Exception e) {
                logger.error("Finalizar Chat", e);
            }
        }
    }

    public void clearAllChats() {
        chats.clear();
    }
}
