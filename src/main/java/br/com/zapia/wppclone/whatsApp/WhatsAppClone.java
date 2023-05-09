package br.com.zapia.wppclone.whatsApp;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.handlersWebSocket.IHandlerWebSocket;
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
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.utils.Util;
import br.com.zapia.wppclone.ws.WebSocketRequestSession;
import br.com.zapia.wppclone.ws.WebSocketSender;
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
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
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
    private WebSocketSender webSocketSender;
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
    @Value("${dockerEndPoint}")
    private String dockerEndPoint;
    @Value("${dockerImageName}")
    private String dockerImageName;
    @Value("${dockerUserName}")
    private String dockerUserName;
    @Value("${dockerPassword}")
    private String dockerPassword;
    @Value("${updateDockerImage}")
    private boolean autoUpdateDockerImage;
    private boolean forceShutdown;
    private ObjectMapper objectMapper;
    private Map<EventWebSocket, IHandlerWebSocket> handlers;
    private LocalDateTime lastTimeWithSessions;
    private LocalDateTime lastPingRemoteApi;
    private Usuario usuarioResponsavelInstancia;


    @PostConstruct
    public void init() throws Exception {
        try {
            if (!getUsuario().getUsuarioResponsavelPelaInstancia().isAtivo()) {
                throw new InstantiationException("Usuário Responsável Inativo");
            }
            objectMapper = new ObjectMapper();
            handlers = new ConcurrentHashMap<>();
            Reflections.collect().getSubTypesOf(IHandlerWebSocket.class).forEach(aClass -> {
                if (!Modifier.isAbstract(aClass.getModifiers())) {
                    try {
                        String className = aClass.getSimpleName();
                        className = Character.toLowerCase(className.charAt(0)) + className.substring(1);
                        handlers.put(aClass.getAnnotation(HandlerWebSocketEvent.class).event(), (IHandlerWebSocket) ap.getBean(className));
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Init Handlers", e);
                    }
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
                    logger.log(Level.SEVERE, "Wait on Connect", e);
                }
                whatsAppClient.addNewChatListener(chat -> {
                    whatsAppSerializer.serializeChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWebSocket.NEW_CHAT, jsonNodes);
                    });
                });
                whatsAppClient.addUpdateChatListener(chat -> {
                    whatsAppSerializer.serializeChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWebSocket.UPDATE_CHAT, jsonNodes);
                    });
                });
                whatsAppClient.addRemoveChatListener(chat -> {
                    whatsAppSerializer.serializeChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWebSocket.REMOVE_CHAT, jsonNodes);
                    });
                });
                whatsAppClient.addNewMessageListener(message -> {
                    whatsAppSerializer.serializeMsg(message).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWebSocket.NEW_MSG, jsonNodes);
                    });
                });
                whatsAppClient.addUpdateMessageListener(message -> {
                    whatsAppSerializer.serializeMsg(message).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWebSocket.UPDATE_MSG, jsonNodes);
                    });
                });
                whatsAppClient.addRemoveMessageListener(message -> {
                    whatsAppSerializer.serializeMsg(message).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TypeEventWebSocket.REMOVE_MSG, jsonNodes);
                    });
                });
            };
            onLowBaterry = (e) -> {
                enviarEventoWpp(TypeEventWebSocket.LOW_BATTERY, e);
            };
            onNeedQrCode = (e) -> {
                enviarEventoWpp(TypeEventWebSocket.NEED_QRCODE, e);
            };
            onChangeEstadoDriver = (e) -> {
                logger.log(Level.INFO, "WhatsAppClient Update State::" + e.name());
                enviarEventoWpp(TypeEventWebSocket.UPDATE_STATE, e.name());
            };
            onDisconnect = () -> {
                enviarEventoWpp(TypeEventWebSocket.DISCONNECT, "Falha ao Conectar ao Telefone");
            };
            usuarioResponsavelInstancia = getUsuario().getUsuarioResponsavelPelaInstancia();
            var dockerConfig = new DockerConfigBuilder(usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getUuid().toString() + "-" + usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getLogin(), dockerImageName, dockerEndPoint)
                    .withAutoUpdateBaseImage(autoUpdateDockerImage)
                    .withMaxMemoryMB(usuarioResponsavelInstancia.getMaxMemory())
                    .withDockerUserName(dockerUserName)
                    .withDockerPassword(dockerPassword)
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
                    .onWsDisconnect((code, reason, remote) -> {
                        whatsAppClient.start();
                        logger.log(Level.SEVERE, "RemoteWs Disconnect::" + reason);
                    })
                    .runnableFactory(runnable -> new UsuarioContextRunnable(runnable, usuarioResponsavelInstancia));
            whatsAppClient = builder.builder();
            whatsAppClient.start().thenAccept(aBoolean -> {
                ping();
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

    private void enviarParaWs(WebSocketSession ws, WsMessage message) {
        if (!(ws instanceof ConcurrentWebSocketSessionDecorator)) {
            ws = buscarWsDecorator(ws);
        }
        if (ws.isOpen()) {
            webSocketSender.sendToWs(ws, message).exceptionally(throwable -> {
                logger.log(Level.SEVERE, "EnviarParaWs", throwable);
                return null;
            });
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
    public void processWebSocketMsg(WebSocketSession session, WebSocketRequestSession webSocketRequest) {
        lastTimeWithSessions = LocalDateTime.now();
        WebSocketSession finalSession = buscarWsDecorator(session);
        try {
            processWebSocketResponse(webSocketRequest).thenAccept(webSocketResponse -> {
                try {
                    String dado;
                    if (webSocketResponse.getResponse() instanceof String) {
                        dado = (String) webSocketResponse.getResponse();
                    } else {
                        dado = objectMapper.writeValueAsString(webSocketResponse.getResponse());
                    }
                    int maxKb = 128 * 1024;
                    if (dado.getBytes().length >= maxKb) {
                        List<String> partials = Util.splitStringByByteLength(dado, maxKb);
                        for (int x = 0; x < partials.size(); x++) {
                            String data = partials.get(x);
                            WebSocketResponseFrame frame = new WebSocketResponseFrame(webSocketResponse.getStatus(), data);
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

    private CompletableFuture<WebSocketResponse> processWebSocketResponse(WebSocketRequestSession webSocketRequest) {
        try {
            IHandlerWebSocket handlerWebSocket = handlers.get(webSocketRequest.getWebSocketRequestPayLoad().getEvent());
            if (handlerWebSocket != null) {
                if (handlerWebSocket.getClassType().isAssignableFrom(String.class)) {
                    return handlerWebSocket.handle(webSocketRequest, webSocketRequest.getWebSocketRequestPayLoad().getPayload().toString());
                } else if (!handlerWebSocket.getClassType().isAssignableFrom(Void.class)) {
                    return handlerWebSocket.handle(webSocketRequest, objectMapper.readValue(webSocketRequest.getWebSocketRequestPayLoad().getPayload().toString(), handlerWebSocket.getClassType()));
                } else {
                    return handlerWebSocket.handle(webSocketRequest, null);
                }
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_IMPLEMENTED.value()));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Handle WebSocketRequest", e);
            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ExceptionUtils.getMessage(e)));
        }
    }

    public void enviarEventoWpp(TypeEventWebSocket typeEventWebSocket, Object dado) {
        getSessions().forEach(webSocketSession -> enviarEventoWpp(typeEventWebSocket, dado, webSocketSession));
    }

    @Async
    public void enviarEventoWpp(TypeEventWebSocket typeEventWebSocket, Object dado, WebSocketSession ws) {
        enviarParaWs(ws, new WsMessage(typeEventWebSocket.name().replace("_", "-"), dado));
    }

    @Async
    public CompletableFuture<Void> logout() {
        return whatsAppClient.logout().thenCompose(aBoolean -> {
            return whatsAppClient.stop();
        });
    }

    public void adicionarSession(WebSocketSession ws) {
        ws.setTextMessageSizeLimit(20 * 1024 * 1024);
        ws = new ConcurrentWebSocketSessionDecorator(ws, 60000, 60 * 1024 * 1024, ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP);
        sessions.add(ws);
        try {
            enviarEventoWpp(TypeEventWebSocket.UPDATE_STATE, whatsAppClient.getDriverState().get().toString(), ws);
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

    public void ping() {
        lastPingRemoteApi = LocalDateTime.now();
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void finalizarQuandoInativo() throws ExecutionException, InterruptedException {
        if ((lastPingRemoteApi == null || lastPingRemoteApi.plusMinutes(5).isBefore(LocalDateTime.now())) && (getSessions().isEmpty() && (lastTimeWithSessions == null || lastTimeWithSessions.plusMinutes(5).isBefore(LocalDateTime.now())) || whatsAppClient.getDriverState().get() == DriverState.WAITING_QR_CODE_SCAN)) {
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

    public enum TypeEventWebSocket {
        UPDATE_CHAT,
        REMOVE_CHAT,
        NEW_CHAT,
        NEW_MSG,
        UPDATE_MSG,
        REMOVE_MSG,
        LOW_BATTERY,
        NEED_QRCODE,
        UPDATE_STATE,
        DISCONNECT,
        ERROR,
        CHANGE_PROPERTY_CHAT,
        REMOVE_PROPERTY_CHAT,
        CHANGE_PROPERTY_MESSAGE
    }
}
