package br.com.zapia.wppclone.whatsApp;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.handlersWebSocket.HandlerWebSocket;
import br.com.zapia.wppclone.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wppclone.payloads.WebSocketRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.utils.Util;
import br.com.zapia.wppclone.whatsApp.controle.ControleChatsAsync;
import br.com.zapia.wppclone.ws.WsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import driver.WebWhatsDriver;
import modelo.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
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
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    @Value("${headLess}")
    private boolean headLess;
    @Value("${forceBeta}")
    private boolean forceBeta;
    private ObjectMapper objectMapper;
    private Map<String, HandlerWebSocket> handlers;


    @PostConstruct
    public void init() throws IOException {
        objectMapper = new ObjectMapper();
        handlers = new ConcurrentHashMap<>();
        new Reflections("br.com.zapia.wppclone.handlersWebSocket").getTypesAnnotatedWith(HandlerWebSocketEvent.class).forEach(aClass -> {
            try {
                handlers.put(aClass.getAnnotation(HandlerWebSocketEvent.class).event(), (HandlerWebSocket) aClass.getDeclaredConstructor().newInstance());
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                logger.log(Level.SEVERE, "Init Handlers", e);
            }
        });
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
            driver.getFunctions().getAllChats(true).thenAccept(chats -> chats.forEach(controleChatsAsync::addChat));
            driver.getFunctions().addChatListenner(c -> controleChatsAsync.addChat(c), EventType.ADD, false);
            driver.getFunctions().addChatListenner(chat -> {
                enviarEventoWpp(TipoEventoWpp.NEW_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            }, EventType.ADD);
            driver.getFunctions().addChatListenner(chat -> {
                enviarEventoWpp(TipoEventoWpp.UPDATE_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            }, EventType.CHANGE, "unreadCount", "pin", "presenceType", "shouldAppearInList");
            driver.getFunctions().addChatListenner(chat -> {
                enviarEventoWpp(TipoEventoWpp.REMOVE_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            }, EventType.REMOVE);
            driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                @Override
                public void run(Message m) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(m)));
                }
            }, EventType.ADD);
            driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                @Override
                public void run(Message m) {
                    enviarEventoWpp(TipoEventoWpp.REMOVE_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(m)));
                }
            }, EventType.REMOVE);
            driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                @Override
                public void run(Message m) {
                    enviarEventoWpp(TipoEventoWpp.UPDATE_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(m)));
                }
            }, EventType.CHANGE, "ack", "isRevoked", "oldId");
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
        if (!headLess) {
            telaWhatsApp = new TelaWhatsApp();
            telaWhatsApp.setVisible(true);
            this.driver = webWhatsDriverSpring.initialize(telaWhatsApp.getPanel(), pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUuid(), forceBeta, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, onChangeEstadoDriver);
        } else {
            this.driver = webWhatsDriverSpring.initialize(pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUuid(), forceBeta, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, onChangeEstadoDriver);
        }
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

    private void enviarParaWs(WebSocketSession ws, WsMessage message) {
        if (!(ws instanceof ConcurrentWebSocketSessionDecorator)) {
            ws = buscarWsDecorator(ws);
        }
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

    private WebSocketSession buscarWsDecorator(WebSocketSession ws) {
        return sessions.stream().filter(webSocketSession -> {
            ConcurrentWebSocketSessionDecorator concurrentWebSocketSessionDecorator = (ConcurrentWebSocketSessionDecorator) webSocketSession;
            return concurrentWebSocketSessionDecorator.getDelegate().equals(ws);
        }).findFirst().orElse(ws);
    }

    @Async
    public void processWebSocketMsg(WebSocketSession session, WebSocketRequest webSocketRequest) {
        if (driver.getEstadoDriver() != EstadoDriver.LOGGED) {
            enviarParaWs(session, new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.FAILED_DEPENDENCY, "WhatsApp Not Logged")));
        } else {
            try {
                processWebSocketResponse(webSocketRequest).thenAccept(webSocketResponse -> {
                    enviarParaWs(session, new WsMessage(webSocketRequest, webSocketResponse));
                });
            } catch (Exception e) {
                enviarEventoWpp(TipoEventoWpp.ERROR, e);
            }
        }
    }

    private CompletableFuture<WebSocketResponse> processWebSocketResponse(WebSocketRequest webSocketRequest) {
        try {
            HandlerWebSocket handlerWebSocket = handlers.get(webSocketRequest.getWebSocketRequestPayLoad().getEvent());
            logger.info("WebSocket Event: " + webSocketRequest.getWebSocketRequestPayLoad().getEvent());
            if (handlerWebSocket != null) {
                return handlerWebSocket.handle(this, webSocketRequest.getWebSocketRequestPayLoad().getPayload());
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_IMPLEMENTED));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Handle WebSocketRequest", e);
            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getMessage(e)));
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
            whatsAppClone.sendInit(ws);
        }
    }

    @Async
    public void sendInit(WebSocketSession ws) {
        driver.getFunctions().getMyChat().thenCompose(chat -> {
            if (chat != null) {
                ObjectNode dados = objectMapper.createObjectNode();
                return serializadorWhatsApp.serializarChat(chat, true).thenAccept(jsonNodes -> {
                    dados.putObject("self").setAll(jsonNodes);
                }).thenCompose(aVoid -> driver.getFunctions().isBusiness()).thenCompose(value -> {
                    if (value) {
                        return driver.getFunctions().getStoreObjectByName("QuickReply").thenAccept(jsObject -> {
                            try {
                                dados.putArray("quickReplys").addAll((ArrayNode) objectMapper.readTree(jsObject.toJSONString()));
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Serialize QuickReplys", e);
                            }
                        });
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                }).thenCompose(aVoid -> {
                    return serializadorWhatsApp.serializarAllContacts().thenAccept(jsonNodes -> {
                        dados.putArray("contacts").addAll(jsonNodes);
                    }).thenCompose(aVoid1 -> serializadorWhatsApp.serializarAllChats()).thenAccept(jsonNodes -> {
                        dados.putArray("chats").addAll(jsonNodes);
                    });
                }).thenApply(aVoid -> {
                    try {
                        return objectMapper.writeValueAsString(dados);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                driver.reiniciar();
                return CompletableFuture.completedFuture("My Chat null");
            }
        }).thenAccept(s -> {
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.INIT, s, ws);
        });
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
        return driver.getFunctions().logout();
    }

    public void adicionarSession(WebSocketSession ws) {
        ws.setTextMessageSizeLimit(100 * 1024 * 1024);
        ws = new ConcurrentWebSocketSessionDecorator(ws, 60000, 10 * 1024 * 1024);
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

    public SerializadorWhatsApp getSerializadorWhatsApp() {
        return serializadorWhatsApp;
    }

    public enum TipoEventoWpp {
        UPDATE_CHAT,
        REMOVE_CHAT,
        NEW_CHAT,
        NEW_MSG,
        NEW_MSG_V3,
        UPDATE_MSG,
        REMOVE_MSG,
        REMOVE_MSG_V3,
        UPDATE_MSG_V3,
        LOW_BATTERY,
        NEED_QRCODE,
        UPDATE_ESTADO,
        ERROR,
        CHAT_PICTURE,
        INIT
    }

    private class TelaWhatsApp extends JFrame {

        private JPanel panel;

        public TelaWhatsApp() {
            this.setTitle(usuarioPrincipalAutoWired.getUsuario().getNome() + " - " + usuarioPrincipalAutoWired.getUsuario().getLogin());
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            this.getContentPane().setLayout(new BorderLayout());
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
