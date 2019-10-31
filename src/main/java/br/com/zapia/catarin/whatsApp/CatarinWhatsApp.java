package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.payloads.Notification;
import br.com.zapia.catarin.restControllers.WhatsAppRestController;
import br.com.zapia.catarin.whatsApp.controle.ControleChatsAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import driver.WebWhatsDriver;
import modelo.*;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Scope("singleton")
public class CatarinWhatsApp {

    @Autowired
    private WhatsAppRestController whatsAppRestController;
    @Autowired
    private ControleChatsAsync controleChatsAsync;
    private Logger logger;
    private StdSchedulerFactory schedulerFactory;
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBattery onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private ActionOnChangeEstadoDriver onChangeEstadoDriver;
    private Runnable onConnect;
    private Runnable onDisconnect;
    private ScheduledExecutorService executores = Executors.newScheduledThreadPool(5);
    private Scheduler scheduler;
    private TelaWhatsApp telaWhatsApp;
    @Value("${pathCacheWebWhats}")
    private String pathCacheWebWhats;
    @Value("${pathLogs}")
    private String pathLogs;
    @Value("${pathBinarios}")
    private String pathBinarios;


    @PostConstruct
    public void init() throws IOException {
        File file = new File(pathCacheWebWhats);
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathLogs);
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathBinarios);
        if (!file.exists()) {
            file.mkdir();
        }
        System.setProperty("jxbrowser.chromium.dir", pathBinarios);
        logger = Logger.getLogger(CatarinWhatsApp.class.getName());
        onConnect = () -> {
            for (Chat chat : driver.getFunctions().getAllNewChats()) {
                controleChatsAsync.addChat(chat);
            }
            driver.getFunctions().addListennerToNewChat(chat -> controleChatsAsync.addChat(chat));
            driver.getFunctions().addListennerToNewChat(chat -> {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    ObjectNode chatNode = (ObjectNode) objectMapper.readTree(chat.toJson());
                    ArrayNode msgsLists = objectMapper.createArrayNode();
                    chatNode.putObject("contact").setAll((ObjectNode) objectMapper.readTree(chat.getContact().toJson()));
                    chatNode.put("picture", chat.getContact().getThumb());
                    chatNode.put("type", chat.getJsObject().getProperty("kind").asString().getValue());
                    chatNode.put("noEarlierMsgs", chat.noEarlierMsgs());
                    for (Message message : chat.getAllMessages()) {
                        ObjectNode msgNode = (ObjectNode) objectMapper.readTree(message.toJson());
                        msgNode.putObject("sender").setAll((ObjectNode) objectMapper.readTree(message.getSender().toJson()));
                        msgsLists.add(msgNode);
                    }
                    chatNode.set("msgs", msgsLists);
                    enviarEventoWpp(TipoEventoWpp.NEW_CHAT, objectMapper.writeValueAsString(chatNode));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "OnNewChat", e);
                }
            }, true);
            driver.getFunctions().addListennerToNewMsg(new MessageObserverIncludeMe() {
                @Override
                public void onNewMsg(Message msg) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        ObjectNode msgNode = (ObjectNode) objectMapper.readTree(msg.toJson());
                        msgNode.putObject("sender").setAll((ObjectNode) objectMapper.readTree(msg.getSender().toJson()));
                        enviarEventoWpp(TipoEventoWpp.NEW_MSG, objectMapper.writeValueAsString(msgNode));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "OnNewMsg", e);
                    }
                }

                @Override
                public void onNewStatusV3(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG_V3, msg.toJson());
                }
            });
        };
        onLowBaterry = (e) -> {
            enviarEventoWpp(TipoEventoWpp.LOW_BATTERY, e + "");
        };
        onNeedQrCode = (e) -> {
            enviarEventoWpp(TipoEventoWpp.NEED_QRCODE, e);
        };
        onErrorInDriver = (e) -> {
            logger.log(Level.SEVERE, e.getMessage(), e);
        };
        onChangeEstadoDriver = (e) -> {
            System.out.println(e);
            enviarEventoWpp(TipoEventoWpp.UPDATE_ESTADO, e.name());
        };
        telaWhatsApp = new TelaWhatsApp();
        telaWhatsApp.setVisible(true);
        this.driver = new WebWhatsDriver(telaWhatsApp.getPanel(), pathCacheWebWhats, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, onChangeEstadoDriver);
        executores.scheduleWithFixedDelay(() -> {
            whatsAppRestController.enviarNotificacao(new Notification("none", "ok"));
        }, 0, 20, TimeUnit.SECONDS);
        schedulerFactory = new StdSchedulerFactory();
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName", "Catarin");
        properties.put("org.quartz.scheduler.instanceId", "AUTO");
        properties.put("org.quartz.threadPool.threadCount", "2");
        try {
            schedulerFactory.initialize(properties);
            scheduler = schedulerFactory.getScheduler();
            scheduler.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public WebWhatsDriver getDriver() {
        return driver;
    }

    public Logger getLogger() {
        return logger;
    }

    public void logout() {
        driver.getFunctions().logout();
    }

    public void enviarMesagemParaTecnico(String msg) {
        Chat c = driver.getFunctions().getChatByNumber("554491050665");
        String mensagem = "*Catarin:* " + msg;
        if (c != null) {
            c.sendMessage(mensagem);
            c.setArchive(true);
        }
    }

    @Async
    public CompletionStage<?> enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado) {
        whatsAppRestController.enviarNotificacao(new Notification(tipoEventoWpp.name().replace("_", "-"), dado));
        if (tipoEventoWpp == TipoEventoWpp.UPDATE_ESTADO && driver.getEstadoDriver() == EstadoDriver.LOGGED) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode dados = objectMapper.createObjectNode();
                Chat myChat = driver.getFunctions().getMyChat();
                ObjectNode myChatNode = (ObjectNode) objectMapper.readTree(myChat.toJson());
                myChatNode.putObject("contact").setAll((ObjectNode) objectMapper.readTree(myChat.getContact().toJson()));
                myChatNode.put("picture", myChat.getContact().getThumb());
                myChatNode.put("type", myChat.getJsObject().getProperty("kind").asString().getValue());
                myChatNode.put("noEarlierMsgs", myChat.noEarlierMsgs());
                dados.putObject("self").setAll(myChatNode);
                ArrayNode chatsNode = objectMapper.createArrayNode();
                List<Chat> allChats = driver.getFunctions().getAllChats();
                for (Chat chat : allChats) {
                    ObjectNode chatNode = (ObjectNode) objectMapper.readTree(chat.toJson());
                    ArrayNode msgsNode = objectMapper.createArrayNode();
                    for (Message message : chat.getAllMessages()) {
                        ObjectNode msgNode = (ObjectNode) objectMapper.readTree(message.toJson());
                        msgNode.putObject("sender").setAll((ObjectNode) objectMapper.readTree(message.getSender().toJson()));
                        msgsNode.add(msgNode);
                    }
                    chatNode.set("msgs", msgsNode);
                    chatNode.putObject("contact").setAll((ObjectNode) objectMapper.readTree(chat.getContact().toJson()));
                    chatNode.put("picture", chat.getContact().getThumb());
                    chatNode.put("type", chat.getJsObject().getProperty("kind").asString().getValue());
                    chatNode.put("noEarlierMsgs", chat.noEarlierMsgs());
                    chatsNode.add(chatNode);
                }
                dados.putArray("chats").addAll(chatsNode);
                enviarEventoWpp(TipoEventoWpp.INIT, objectMapper.writeValueAsString(dados));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "SendInit", e);
            }
        }
        return null;
    }

    public enum TipoEventoWpp {
        CHAT_UPDATE,
        NEW_CHAT,
        NEW_MSG,
        NEW_MSG_V3,
        LOW_BATTERY,
        NEED_QRCODE,
        UPDATE_ESTADO,
        INIT
    }

    private class TelaWhatsApp extends JFrame {

        private JPanel panel;

        public TelaWhatsApp() {
            this.setTitle("Catarin");
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            this.getContentPane().setLayout(new BorderLayout());
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            this.setMinimumSize(new Dimension(800, 600));
            this.setPreferredSize(new Dimension(800, 600));
            panel = new JPanel(new BorderLayout());
            this.add(panel);
            pack();
            this.setExtendedState(JFrame.ICONIFIED);
            this.setLocationRelativeTo(null);
        }

        public JPanel getPanel() {
            return panel;
        }
    }
}
