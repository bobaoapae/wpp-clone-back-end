package br.com.zapia.catarin.restControllers;

import br.com.zapia.catarin.payloads.MediaMessageResponse;
import br.com.zapia.catarin.payloads.SendMessageRequest;
import br.com.zapia.catarin.utils.Util;
import br.com.zapia.catarin.whatsApp.CatarinWhatsApp;
import br.com.zapia.catarin.whatsApp.SerializadorWhatsApp;
import modelo.Chat;
import modelo.EstadoDriver;
import modelo.MediaMessage;
import modelo.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@RestController
@RequestMapping("/api/whatsApp")
public class WhatsAppRestController {

    @Lazy
    @Autowired
    private CatarinWhatsApp catarinWhatsApp;
    @Autowired
    private SerializadorWhatsApp serializadorWhatsApp;

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
    @GetMapping("/mediaMessage/{id}/{forceDownload}")
    public ResponseEntity<?> mediaMessage(@PathVariable("id") String id, @PathVariable("forceDownload") boolean forceDownload) throws IOException {
        if (catarinWhatsApp.getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Message message = catarinWhatsApp.getDriver().getFunctions().getMessageById(id);
        if (message instanceof MediaMessage) {
            File file = ((MediaMessage) message).downloadMedia(5);
            if (file == null) {
                file = ((MediaMessage) message).downloadMedia(20);
            }
            String contentType = Files.probeContentType(file.toPath());
            byte[] data = Files.readAllBytes(file.toPath());
            String fileName = ((MediaMessage) message).getFileName();
            if (fileName.isEmpty()) {
                fileName = file.getName();
            }
            if (!forceDownload) {
                String base64str = Base64.getEncoder().encodeToString(data);
                StringBuilder sb = new StringBuilder();
                sb.append("data:");
                sb.append(contentType);
                sb.append(";base64,");
                sb.append(base64str);
                MediaMessageResponse mediaMessageResponse = new MediaMessageResponse(fileName, sb.toString());
                return ResponseEntity.ok(mediaMessageResponse);
            } else {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-disposition", "attachment;filename=\"" + fileName + "\"");
                ResponseEntity<byte[]> responseEntity = new ResponseEntity(data, headers, HttpStatus.OK);
                return responseEntity;
            }
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
        chat.loadEarlierMsgs(() -> {
            try {
                catarinWhatsApp.enviarEventoWpp(CatarinWhatsApp.TipoEventoWpp.CHAT_UPDATE, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return ResponseEntity.ok().build();
    }
}
