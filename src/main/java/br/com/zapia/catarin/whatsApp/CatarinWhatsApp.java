package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.utils.Util;
import br.com.zapia.catarin.whatsApp.controle.ControleChatsAsync;
import br.com.zapia.catarin.ws.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import driver.WebWhatsDriver;
import modelo.*;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

@Component
@Scope("singleton")
public class CatarinWhatsApp {

    @Autowired
    private ControleChatsAsync controleChatsAsync;
    @Autowired
    private SerializadorWhatsApp serializadorWhatsApp;
    @Lazy
    @Autowired
    private CatarinWhatsApp catarinWhatsApp;
    private List<WebSocketSession> sessions;
    private Logger logger;
    private StdSchedulerFactory schedulerFactory;
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBattery onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private ActionOnChangeEstadoDriver onChangeEstadoDriver;
    private Runnable onConnect;
    private Runnable onDisconnect;
    private Scheduler scheduler;
    private TelaWhatsApp telaWhatsApp;
    private List<Runnable> runAfterInit;
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
        sessions = Collections.synchronizedList(new ArrayList<>());
        runAfterInit = new ArrayList<>();
        onConnect = () -> {
            for (Chat chat : driver.getFunctions().getAllNewChats()) {
                controleChatsAsync.addChat(chat);
            }
            driver.getFunctions().addListennerToNewChat(chat -> controleChatsAsync.addChat(chat));
            driver.getFunctions().addListennerToNewChat(chat -> {
                try {
                    enviarEventoWpp(TipoEventoWpp.NEW_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "OnNewChat", e);
                }
            }, true);
            driver.getFunctions().addListennerToUpdateChat(chat -> {
                try {
                    enviarEventoWpp(TipoEventoWpp.CHAT_UPDATE, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "OnNewChat", e);
                }
            });
            driver.getFunctions().addListennerToNewMsg(new MessageObserverIncludeMe() {
                @Override
                public void onNewMsg(Message msg) {
                    try {
                        enviarEventoWpp(TipoEventoWpp.NEW_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(msg)));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "OnNewMsg", e);
                    }
                }

                @Override
                public void onNewStatusV3(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG_V3, msg.toJson());
                }
            });

            driver.getFunctions().addListennerToChangeMsg(new MessageObserverIncludeMe() {
                @Override
                public void onNewMsg(Message msg) {
                    try {
                        enviarEventoWpp(TipoEventoWpp.UPDATE_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(msg)));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "OnNewMsg", e);
                    }
                }

                @Override
                public void onNewStatusV3(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.UPDATE_MSG_V3, msg.toJson());
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
            enviarEventoWpp(TipoEventoWpp.UPDATE_ESTADO, e.name());
        };
        telaWhatsApp = new TelaWhatsApp();
        telaWhatsApp.setVisible(true);
        this.driver = new WebWhatsDriver(telaWhatsApp.getPanel(), pathCacheWebWhats, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, onChangeEstadoDriver);
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

    @Scheduled(fixedDelay = 6000L)
    public void enviarNotificacaoParaTestarConexao() {
        broadcastParaWs(new WsMessage("ping", System.currentTimeMillis()));
    }

    @Async("threadPoolTaskExecutor")
    public void broadcastParaWs(WsMessage message) {
        getSessions().stream().filter(webSocketSession -> webSocketSession.isOpen()).forEach(webSocketSession -> {
            try {
                catarinWhatsApp.enviarParaWs(webSocketSession, message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Async("threadPoolTaskExecutor")
    public void enviarParaWs(WebSocketSession ws, WsMessage message) throws IOException {
        if (ws.isOpen()) {
            ws.sendMessage(new TextMessage(message.toString()));
        }
    }

    public void adicionarSession(WebSocketSession ws) {
        synchronized (sessions) {
            sessions.add(ws);
        }
    }

    public void removerSession(WebSocketSession ws) {
        synchronized (sessions) {
            sessions.remove(ws);
        }
    }

    public List<WebSocketSession> getSessions() {
        return new ArrayList<>(sessions);
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

    @Async("threadPoolTaskExecutor")
    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado) {
        catarinWhatsApp.broadcastParaWs(new WsMessage(tipoEventoWpp.name().replace("_", "-"), dado));
        if (tipoEventoWpp == TipoEventoWpp.UPDATE_ESTADO && driver.getEstadoDriver() == EstadoDriver.LOGGED) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode dados = objectMapper.createObjectNode();
                dados.putObject("self").setAll(Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(driver.getFunctions().getMyChat())));
                ArrayNode chatsNode = objectMapper.createArrayNode();
                List<CompletableFuture<ArrayNode>> futures = new ArrayList<>();
                Collection<List<Chat>> partition = Util.partition(driver.getFunctions().getAllChats(), 5);
                partition.forEach(chats -> {
                    futures.add(serializadorWhatsApp.serializarChat(chats));
                });
                Util.pegarResultadosFutures(futures).forEach(chatsNode::addAll);
                dados.putArray("chats").addAll(chatsNode);
                catarinWhatsApp.enviarEventoWpp(TipoEventoWpp.INIT, new String(Base64.getEncoder().encode(zip(objectMapper.writeValueAsString(dados)))));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "SendInit", e);
            }
        }
        if (tipoEventoWpp == TipoEventoWpp.INIT) {
            runAfterInit.forEach(Runnable::run);
            runAfterInit.clear();
        }
    }

    public void runAfterInit(Runnable runnable) {
        runAfterInit.add(runnable);
    }

    public byte[] zip(final String str) {
        if ((str == null) || (str.length() == 0)) {
            throw new IllegalArgumentException("Cannot zip null or empty string");
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                gzipOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to zip content", e);
        }
    }

    public enum TipoEventoWpp {
        CHAT_UPDATE,
        NEW_CHAT,
        NEW_MSG,
        NEW_MSG_V3,
        UPDATE_MSG,
        UPDATE_MSG_V3,
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
