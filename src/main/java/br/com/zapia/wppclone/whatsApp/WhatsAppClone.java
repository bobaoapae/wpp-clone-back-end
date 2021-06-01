package br.com.zapia.wppclone.whatsApp;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.payloads.WebSocketRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponseFrame;
import br.com.zapia.wpp.client.docker.DockerConfigBuilder;
import br.com.zapia.wpp.client.docker.WhatsAppClient;
import br.com.zapia.wpp.client.docker.WhatsAppClientBuilder;
import br.com.zapia.wpp.client.docker.model.DriverState;
import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioContextCallable;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioContextRunnable;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.wppclone.handlersWebSocket.IHandlerWebSocketSpring;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.SendEmailService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.utils.Util;
import br.com.zapia.wppclone.ws.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
@Scope("usuario")
public class WhatsAppClone {

    @Autowired
    private UsuarioPrincipalAutoWired usuarioPrincipalAutoWired;
    @Autowired
    private WhatsAppSerializer whatsAppSerializer;
    @Autowired
    private ApplicationContext ap;
    @Autowired
    private WhatsAppCloneService whatsAppCloneService;
    @Autowired
    private SendEmailService sendEmailService;
    private List<WebSocketSession> sessions;
    private final Logger logger = Logger.getLogger(WhatsAppClone.class.getName());
    private WhatsAppClient whatsAppClient;
    private Consumer<String> onNeedQrCode;
    private Consumer<Integer> onLowBaterry;
    private Consumer<DriverState> onChangeEstadoDriver;
    private Runnable onConnect;
    private Runnable onDisconnect;
    @Value("${pathLogs}")
    private String pathLogs;
    @Value("${loginWhatsAppGeral}")
    private String loginWhatsAppGeral;
    private boolean forceShutdown;
    private boolean instanciaGeral;
    private ObjectMapper objectMapper;
    private Map<EventWebSocket, IHandlerWebSocketSpring> handlers;
    private LocalDateTime lastTimeWithSessions;
    private Usuario usuarioResponsavelInstancia;


    @PostConstruct
    public void init() throws Exception {
        try {
            if (!getUsuario().getUsuarioResponsavelPelaInstancia().isAtivo()) {
                throw new InstantiationException("Usuário Responsável Inativo");
            }
            instanciaGeral = getUsuario().getLogin().equals(loginWhatsAppGeral);
            objectMapper = new ObjectMapper();
            handlers = new ConcurrentHashMap<>();
            Constructor<Reflections> declaredConstructor = Reflections.class.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            declaredConstructor.newInstance().collect(getClass().getResourceAsStream("/META-INF/reflections/reflections.xml")).getTypesAnnotatedWith(HandlerWebSocketEvent.class).forEach(aClass -> {
                try {
                    String className = aClass.getSimpleName();
                    className = Character.toLowerCase(className.charAt(0)) + className.substring(1);
                    handlers.put(aClass.getAnnotation(HandlerWebSocketEvent.class).event(), (IHandlerWebSocketSpring) ap.getBean(className));
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Init Handlers", e);
                }
            });
            File file = new File(pathLogs + getUsuario().getUsuarioResponsavelPelaInstancia().getUuid());
            if (!file.exists()) {
                file.mkdirs();
            }
            sessions = new ArrayList<>();
            onConnect = () -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "OnConnect", e);
                }
                whatsAppClient.addNewChatListener(chat -> {
                    whatsAppSerializer.serializeChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWhatsApp.NEW_CHAT, jsonNodes);
                    });
                });
                whatsAppClient.addUpdateChatListener(chat -> {
                    whatsAppSerializer.serializeChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWhatsApp.UPDATE_CHAT, jsonNodes);
                    });
                });
                whatsAppClient.addRemoveChatListener(chat -> {
                    whatsAppSerializer.serializeChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWhatsApp.REMOVE_CHAT, jsonNodes);
                    });
                });
                whatsAppClient.addNewMessageListener(message -> {
                    whatsAppSerializer.serializeMsg(message).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWhatsApp.NEW_MSG, jsonNodes);
                    });
                });
                whatsAppClient.addUpdateMessageListener(message -> {
                    whatsAppSerializer.serializeMsg(message).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWhatsApp.UPDATE_MSG, jsonNodes);
                    });
                });
                whatsAppClient.addRemoveMessageListener(message -> {
                    whatsAppSerializer.serializeMsg(message).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWhatsApp.REMOVE_MSG, jsonNodes);
                    });
                });
            };
            onLowBaterry = (e) -> {
                enviarEventoWpp(TypeEventWhatsApp.LOW_BATTERY, e);
            };
            onNeedQrCode = (e) -> {
                enviarEventoWpp(TypeEventWhatsApp.NEED_QRCODE, e);
            };
            onChangeEstadoDriver = (e) -> {
                enviarEventoWpp(TypeEventWhatsApp.UPDATE_STATE, e.name());
            };
            onDisconnect = () -> {
                enviarEventoWpp(TypeEventWhatsApp.DISCONNECT, "Falha ao Conectar ao Telefone");
            };
            usuarioResponsavelInstancia = getUsuario().getUsuarioResponsavelPelaInstancia();
            var dockerConfig = new DockerConfigBuilder(usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getUuid().toString(), "docker.joaoiot.com.br")
                    .withAutoUpdateBaseImage(true)
                    .withMaxMemoryMB(800)
                    .build();
            WhatsAppClientBuilder builder = new WhatsAppClientBuilder(dockerConfig);
            builder
                    .onInit(onConnect)
                    .onUpdateDriverState(onChangeEstadoDriver)
                    .onNeedQrCode(onNeedQrCode)
                    .onLowBattery(onLowBaterry)
                    .onPhoneDisconnect(onDisconnect)
                    .onError(throwable -> {
                        logger.log(Level.SEVERE, "Error Driver WhatsApp " + usuarioResponsavelInstancia.getLogin(), throwable);
                    })
                    .callableFactory(callable -> {
                        return new UsuarioContextCallable(callable, usuarioResponsavelInstancia);
                    })
                    .runnableFactory(runnable -> new UsuarioContextRunnable(runnable, usuarioResponsavelInstancia));
            whatsAppClient = builder.builder();
            whatsAppClient.start().thenAccept(aBoolean -> {
                logger.log(Level.INFO, "WhatsAppClient Start::" + aBoolean);
            });
            whatsAppCloneService.adicionarInstancia(this);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Init", e);
            if (whatsAppClient != null) {
                whatsAppClient.stop().get();
            }
            throw e;
        }
    }

    @Async
    public void enviarParaWs(WebSocketSession ws, WsMessage message) {
        if (!(ws instanceof ConcurrentWebSocketSessionDecorator)) {
            ws = buscarWsDecorator(ws);
        }
        if (ws.isOpen()) {
            try {
                ws.sendMessage(new TextMessage(message.toString()));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "EnviarParaWs", e);
                try {
                    ws.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "CloseWs", ex);
                }
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
        lastTimeWithSessions = LocalDateTime.now();
        WebSocketSession finalSession = buscarWsDecorator(session);
        try {
            processWebSocketResponse((Usuario) session.getAttributes().get("usuario"), webSocketRequest).thenAccept(webSocketResponse -> {
                try {
                    String dado;
                    if (webSocketResponse.getResponse() instanceof String) {
                        dado = (String) webSocketResponse.getResponse();
                    } else {
                        dado = objectMapper.writeValueAsString(webSocketResponse.getResponse());
                    }
                    int maxKb = 1024 * 1024;
                    if (dado.getBytes().length >= maxKb) {
                        List<String> partials = Util.splitStringByByteLength(dado, maxKb);
                        for (int x = 0; x < partials.size(); x++) {
                            WebSocketResponseFrame frame = new WebSocketResponseFrame(webSocketResponse.getStatus(), partials.get(x));
                            frame.setFrameId(x + 1);
                            frame.setQtdFrames(partials.size());
                            enviarParaWs(finalSession, new WsMessage(webSocketRequest, frame));
                        }
                    } else {
                        enviarParaWs(finalSession, new WsMessage(webSocketRequest, webSocketResponse));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).exceptionally(throwable -> {
                logger.log(Level.SEVERE, "Process WebSocket Msg", throwable);
                enviarParaWs(finalSession, new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ExceptionUtils.getMessage(throwable))));
                return null;
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Process WebSocket Msg", e);
            enviarParaWs(finalSession, new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ExceptionUtils.getMessage(e))));
        }
    }

    private CompletableFuture<WebSocketResponse> processWebSocketResponse(Usuario usuario, WebSocketRequest webSocketRequest) {
        try {
            IHandlerWebSocketSpring handlerWebSocket = handlers.get(webSocketRequest.getWebSocketRequestPayLoad().getEvent());
            if (handlerWebSocket != null) {
                if (handlerWebSocket.getClassType().isAssignableFrom(String.class)) {
                    return handlerWebSocket.handle(usuario, webSocketRequest.getWebSocketRequestPayLoad().getPayload().toString());
                } else if (!handlerWebSocket.getClassType().isAssignableFrom(Void.class)) {
                    return handlerWebSocket.handle(usuario, objectMapper.readValue(webSocketRequest.getWebSocketRequestPayLoad().getPayload().toString(), handlerWebSocket.getClassType()));
                } else {
                    return handlerWebSocket.handle(usuario, null);
                }
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_IMPLEMENTED.value()));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Handle WebSocketRequest", e);
            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ExceptionUtils.getMessage(e)));
        }
    }

    public void enviarEventoWpp(TypeEventWhatsApp typeEventWhatsApp, Object dado) {
        getSessions().forEach(webSocketSession -> enviarEventoWpp(typeEventWhatsApp, dado, webSocketSession));
    }

    @Async
    public void enviarEventoWpp(TypeEventWhatsApp typeEventWhatsApp, Object dado, WebSocketSession ws) {
        enviarParaWs(ws, new WsMessage(typeEventWhatsApp.name().replace("_", "-"), dado));
    }

    @Async
    public CompletableFuture<Void> logout() {
        return whatsAppClient.logout().thenCompose(aBoolean -> {
            return whatsAppClient.stop();
        });
    }

    public void adicionarSession(WebSocketSession ws) {
        ws.setTextMessageSizeLimit(20 * 1024 * 1024);
        ws = new ConcurrentWebSocketSessionDecorator(ws, 60000, 40 * 1024 * 1024);
        sessions.add(ws);
        try {
            enviarEventoWpp(TypeEventWhatsApp.UPDATE_STATE, whatsAppClient.getDriverState().get().toString(), ws);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
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

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void finalizarQuandoInativo() throws ExecutionException, InterruptedException {
        if (!instanciaGeral && (getSessions().isEmpty() && (lastTimeWithSessions == null || lastTimeWithSessions.plusMinutes(5).isBefore(LocalDateTime.now())) || whatsAppClient.getDriverState().get() == DriverState.WAITING_QR_CODE_SCAN)) {
            logger.info("Finalizar Instancia Inativa: " + getUsuario().getUsuarioResponsavelPelaInstancia().getLogin());
            shutdown();
        }
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 0)
    public void finalizarSeForcado() {
        if (!getUsuario().getUsuarioResponsavelPelaInstancia().isAtivo() || forceShutdown) {
            forceShutdown = false;
            logger.info("Finalizar Instancia Forçada: " + getUsuario().getUsuarioResponsavelPelaInstancia().getLogin());
            shutdown();
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void checkPresence() throws ExecutionException, InterruptedException {
        if (getSessions().isEmpty()) {
            if (whatsAppClient.getDriverState().get() == DriverState.LOGGED) {
                whatsAppClient.sendPresenceUnavailable();
            }
        }
    }

    private void shutdown() {
        new Thread(() -> {
            UsuarioScopedContext.setUsuario(usuarioResponsavelInstancia);
            ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("whatsAppClone");
        }).start();
    }

    public void setForceShutdown(boolean forceShutdown) {
        this.forceShutdown = forceShutdown;
    }

    @PreDestroy
    public void preDestroy() {
        logger.info("Destroy WhatsAppClone");
        ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("whatsAppSerializer");
        for (WebSocketSession webSocketSession : getSessions()) {
            if (webSocketSession.isOpen()) {
                try {
                    webSocketSession.close(CloseStatus.GOING_AWAY);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Close Ws Session", e);
                }
            }
        }
        whatsAppClient.stop();
        whatsAppCloneService.removerInstancia(this);
    }

    public List<WebSocketSession> getSessions() {
        return sessions.stream().filter(WebSocketSession::isOpen).collect(Collectors.toUnmodifiableList());
    }

    public WhatsAppClient getWhatsAppClient() {
        return whatsAppClient;
    }

    public Logger getLogger() {
        return logger;
    }

    public WhatsAppSerializer getWhatsAppSerializer() {
        return whatsAppSerializer;
    }

    public Usuario getUsuario() {
        return usuarioPrincipalAutoWired.getUsuario();
    }

    public enum TypeEventWhatsApp {
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
        UPDATE_STATE,
        DISCONNECT,
        ERROR,
        CHAT_PICTURE
    }
}
