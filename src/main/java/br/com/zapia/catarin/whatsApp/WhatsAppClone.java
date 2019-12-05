package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.catarin.utils.Util;
import br.com.zapia.catarin.whatsApp.controle.ControleChatsAsync;
import br.com.zapia.catarin.ws.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import driver.WebWhatsDriver;
import modelo.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.threadly.concurrent.collections.ConcurrentArrayList;

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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@Component
@Scope("usuario")
public class WhatsAppClone {

    @Autowired
    private UsuarioPrincipalAutoWired usuarioPrincipalAutoWired;
    @Autowired
    private ControleChatsAsync controleChatsAsync;
    @Autowired
    private SerializadorWhatsApp serializadorWhatsApp;
    @Lazy
    @Autowired
    private WhatsAppClone whatsAppClone;
    @Autowired
    private WebWhatsDriverSpring webWhatsDriverSpring;
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
    @Value("${pathCacheWebWhats}")
    private String pathCacheWebWhats;
    @Value("${pathLogs}")
    private String pathLogs;
    @Value("${pathBinarios}")
    private String pathBinarios;


    @PostConstruct
    public void init() throws IOException {
        File file = new File(pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUuid());
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathLogs + usuarioPrincipalAutoWired.getUsuario().getUuid());
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathBinarios);
        if (!file.exists()) {
            file.mkdir();
        }
        System.setProperty("jxbrowser.chromium.dir", pathBinarios);
        logger = Logger.getLogger(WhatsAppClone.class.getName());
        sessions = new ConcurrentArrayList<>();
        onConnect = () -> {
            for (Chat chat : driver.getFunctions().getAllNewChats()) {
                controleChatsAsync.addChat(chat);
            }
            driver.getFunctions().addListennerToNewChat(chat -> controleChatsAsync.addChat(chat));
            driver.getFunctions().addListennerToNewChat(chat -> {
                try {
                    enviarEventoWpp(TipoEventoWpp.NEW_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
                } catch (ExecutionException e) {
                    logger.log(Level.SEVERE, "OnNewChat", e);
                }
            }, true);
            driver.getFunctions().addListennerToUpdateChat(chat -> {
                try {
                    enviarEventoWpp(TipoEventoWpp.CHAT_UPDATE, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
                } catch (ExecutionException e) {
                    logger.log(Level.SEVERE, "OnUpdateChat", e);
                }
            });
            driver.getFunctions().addListennerToNewMsg(new MessageObserverIncludeMe() {
                @Override
                public void onNewMsg(Message msg) {
                    try {
                        enviarEventoWpp(TipoEventoWpp.NEW_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(msg)));
                    } catch (ExecutionException e) {
                        logger.log(Level.SEVERE, "OnUpdateChat", e);
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
                    } catch (ExecutionException e) {
                        logger.log(Level.SEVERE, "OnUpdateMsg", e);
                    }
                }

                @Override
                public void onNewStatusV3(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.UPDATE_MSG_V3, msg.toJson());
                }
            });
        };
        onLowBaterry = (e) -> {
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.LOW_BATTERY, e + "");
        };
        onNeedQrCode = (e) -> {
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.NEED_QRCODE, e);
        };
        onErrorInDriver = (e) -> {
            logger.log(Level.SEVERE, e.getMessage(), e);
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.ERROR, ExceptionUtils.getStackTrace(e));
        };
        onChangeEstadoDriver = (e) -> {
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.UPDATE_ESTADO, e.name());
        };
        telaWhatsApp = new TelaWhatsApp();
        telaWhatsApp.setVisible(true);
        this.driver = webWhatsDriverSpring.initialize(telaWhatsApp.getPanel(), pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUuid(), onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, onChangeEstadoDriver);
        schedulerFactory = new StdSchedulerFactory();
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName", "WppWebClone" + usuarioPrincipalAutoWired.getUsuario().getUuid());
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

    @Async
    public void broadcastParaWs(WsMessage message) {
        getSessions().stream().filter(WebSocketSession::isOpen).forEach(webSocketSession -> {
            whatsAppClone.enviarParaWs(webSocketSession, message);
        });
    }

    @Async
    public void enviarParaWs(WebSocketSession ws, WsMessage message) {
        if (ws.isOpen()) {
            try {
                ws.sendMessage(new TextMessage(message.toString()));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "EnviarParaWs", e);
            }
        } else {
            removerSession(ws);
        }
    }

    @Async
    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado) {
        getSessions().forEach(webSocketSession -> whatsAppClone.enviarEventoWpp(tipoEventoWpp, dado, webSocketSession));
    }

    @Async
    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado, WebSocketSession ws) {
        whatsAppClone.enviarParaWs(ws, new WsMessage(tipoEventoWpp.name().replace("_", "-"), dado));
        if (tipoEventoWpp == TipoEventoWpp.UPDATE_ESTADO && driver.getEstadoDriver() == EstadoDriver.LOGGED) {
            sendInit(ws);
        }
    }

    @Async
    public void sendInit(WebSocketSession ws) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode dados = objectMapper.createObjectNode();
            Chat myChat = driver.getFunctions().getMyChat();
            if (myChat == null) {
                driver.reiniciar();
            } else {
                dados.putObject("self").setAll(Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(myChat)));
                ArrayNode chatsNode = objectMapper.createArrayNode();
                List<CompletableFuture<ArrayNode>> futures = new ArrayList<>();
                List<Chat> allChats = driver.getFunctions().getAllChats();
                int partitionSize = allChats.size() < Runtime.getRuntime().availableProcessors() ? allChats.size() : allChats.size() / Runtime.getRuntime().availableProcessors();
                Collection<List<Chat>> partition = Util.partition(allChats, partitionSize);
                partition.forEach(chats -> futures.add(serializadorWhatsApp.serializarChat(chats)));
                Util.pegarResultadosFutures(futures).forEach(chatsNode::addAll);
                dados.putArray("chats").addAll(chatsNode);
                whatsAppClone.enviarEventoWpp(TipoEventoWpp.INIT, objectMapper.writeValueAsString(dados), ws);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "SendInit", e);
        }
    }

    public void enviarMesagemParaTecnico(String msg) {
        Chat c = driver.getFunctions().getChatByNumber("554491050665");
        String mensagem = "*Catarin:* " + msg;
        if (c != null) {
            c.sendMessage(mensagem);
            c.setArchive(true);
        }
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

    @Async
    public CompletableFuture<Void> logout() {
        driver.getFunctions().logout();
        return CompletableFuture.completedFuture(null);
    }

    public void adicionarSession(WebSocketSession ws) {
        ws = new ConcurrentWebSocketSessionDecorator(ws, 15000, 10 * 1024 * 1024);
        sessions.add(ws);
        whatsAppClone.enviarEventoWpp(WhatsAppClone.TipoEventoWpp.UPDATE_ESTADO, whatsAppClone.getDriver().getEstadoDriver().name(), ws);
    }

    public void removerSession(WebSocketSession ws) {
        if (!(ws instanceof ConcurrentWebSocketSessionDecorator)) {
            sessions.removeAll(sessions.stream().filter(webSocketSession -> {
                ConcurrentWebSocketSessionDecorator concurrentWebSocketSessionDecorator = (ConcurrentWebSocketSessionDecorator) webSocketSession;
                return concurrentWebSocketSessionDecorator.getDelegate().equals(ws);
            }).collect(Collectors.toList()));
        } else {
            sessions.remove(ws);
        }
    }

    public List<WebSocketSession> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public WebWhatsDriver getDriver() {
        return driver;
    }

    public Logger getLogger() {
        return logger;
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
        ERROR,
        INIT
    }

    private class TelaWhatsApp extends JFrame {

        private JPanel panel;

        public TelaWhatsApp() {
            this.setTitle(usuarioPrincipalAutoWired.getUsuario().getNome() + " - " + usuarioPrincipalAutoWired.getUsuario().getLogin());
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
