package br.com.zapia.catarin.restControllers;

import br.com.zapia.catarin.payloads.MediaMessageResponse;
import br.com.zapia.catarin.payloads.Notification;
import br.com.zapia.catarin.payloads.SendMessageRequest;
import br.com.zapia.catarin.whatsApp.CatarinWhatsApp;
import modelo.Chat;
import modelo.EstadoDriver;
import modelo.MediaMessage;
import modelo.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/whatsApp")
public class WhatsAppRestController {

    @Autowired
    private CatarinWhatsApp catarinWhatsApp;

    private final CopyOnWriteArrayList<SseEmitter> emittersWhatsApp = new CopyOnWriteArrayList<>();


    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_OPERADOR"})
    @GetMapping("/events")
    public SseEmitter eventos() {
        SseEmitter emitter = new SseEmitter(600000L);
        this.emittersWhatsApp.add(emitter);

        emitter.onCompletion(() -> this.emittersWhatsApp.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.emittersWhatsApp.remove(emitter);
        });
        emitter.onError(throwable -> {
            emitter.complete();
            this.emittersWhatsApp.remove(emitter);
        });
        catarinWhatsApp.enviarEventoWpp(CatarinWhatsApp.TipoEventoWpp.UPDATE_ESTADO, catarinWhatsApp.getDriver().getEstadoDriver().name());
        return emitter;
    }

    @Async
    public void enviarNotificacao(Notification notification) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter sseEmitter : this.emittersWhatsApp) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event();
                event.reconnectTime(500L);
                event.id(String.valueOf(event.hashCode()));
                event.name(notification.getType().toLowerCase());
                event.data(notification.getDado());
                sseEmitter.send(event);
            } catch (Exception e) {
                deadEmitters.add(sseEmitter);
            }
        }
        this.emittersWhatsApp.removeAll(deadEmitters);
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_OPERADOR"})
    @PostMapping("/sendMessage")
    public ResponseEntity<?> enviarMenssagem(@Valid @ModelAttribute SendMessageRequest sendMessageRequest) {
        if (sendMessageRequest.getChatId() == null || sendMessageRequest.getChatId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        int horaAtual = LocalTime.now().getHour();
        String saudacao = "";
        if (horaAtual >= 2 && horaAtual < 12) {
            saudacao = "Bom Dia";
        } else if (horaAtual >= 12 && horaAtual < 18) {
            saudacao = "Boa Tarde";
        } else {
            saudacao = "Boa Noite";
        }
        String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy"));
        String amanha = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy"));
        String msg = sendMessageRequest.getMessage().replaceAll("\\{estabelecimento}", "Catarin").replaceAll("\\{saudacao}", saudacao).replaceAll("\\{hoje}", hoje).replaceAll("\\{amanha}", amanha);
        if (catarinWhatsApp.getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Chat chat = catarinWhatsApp.getDriver().getFunctions().getChatById(sendMessageRequest.getChatId());
        if (chat != null) {
            if (sendMessageRequest.getMedia() == null || sendMessageRequest.getMedia().isEmpty()) {
                chat.sendMessage(msg);
            } else if (sendMessageRequest.getFileName() != null && !sendMessageRequest.getFileName().isEmpty()) {
                chat.sendFile(sendMessageRequest.getMedia(), sendMessageRequest.getFileName(), msg);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_OPERADOR"})
    @GetMapping("/mediaMessage/{id}")
    public ResponseEntity<?> mediaMessage(@PathVariable("id") String id) throws IOException {
        if (catarinWhatsApp.getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Message message = catarinWhatsApp.getDriver().getFunctions().getMessageById(id);
        if (message instanceof MediaMessage) {
            File file = ((MediaMessage) message).downloadMedia();
            String contentType = Files.probeContentType(file.toPath());
            byte[] data = Files.readAllBytes(file.toPath());
            String base64str = Base64.getEncoder().encodeToString(data);
            StringBuilder sb = new StringBuilder();
            sb.append("data:");
            sb.append(contentType);
            sb.append(";base64,");
            sb.append(base64str);
            String fileName = ((MediaMessage) message).getFileName();
            if (fileName.isEmpty()) {
                fileName = file.getName();
            }
            MediaMessageResponse mediaMessageResponse = new MediaMessageResponse(fileName, sb.toString());
            return ResponseEntity.ok(mediaMessageResponse);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_OPERADOR"})
    @PostMapping("/sendSeenChat/{id}")
    public ResponseEntity<?> marcarChatComoVisto(@PathVariable("id") String id) {
        if (catarinWhatsApp.getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Chat chat = catarinWhatsApp.getDriver().getFunctions().getChatById(id);
        if (chat == null) {
            return ResponseEntity.notFound().build();
        }
        chat.sendSeen(false);
        return ResponseEntity.ok().build();
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_OPERADOR"})
    @PostMapping("/loadEarly/{id}")
    public ResponseEntity<?> carregarMensagensAntigas(@PathVariable("id") String id) {
        if (catarinWhatsApp.getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Chat chat = catarinWhatsApp.getDriver().getFunctions().getChatById(id);
        if (chat == null) {
            return ResponseEntity.notFound().build();
        }
        chat.loadEarlierMsgs(null);
        return ResponseEntity.ok().build();
    }
}
