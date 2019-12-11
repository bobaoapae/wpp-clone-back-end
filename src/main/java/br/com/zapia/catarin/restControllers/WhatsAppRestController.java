package br.com.zapia.catarin.restControllers;

import br.com.zapia.catarin.payloads.MediaMessageResponse;
import br.com.zapia.catarin.whatsApp.WhatsAppClone;
import modelo.EstadoDriver;
import modelo.MediaMessage;
import modelo.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

@Scope("usuario")
@RestController
@RequestMapping("/api/whatsApp")
public class WhatsAppRestController {

    @Lazy
    @Autowired
    private WhatsAppClone whatsAppClone;

    @GetMapping("/mediaMessage/{id}/{forceDownload}")
    public ResponseEntity<?> mediaMessage(@PathVariable("id") String id, @PathVariable("forceDownload") boolean forceDownload) throws IOException {
        if (whatsAppClone.getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Message message = whatsAppClone.getDriver().getFunctions().getMessageById(id);
        if (message instanceof MediaMessage) {
            File file = ((MediaMessage) message).downloadMedia(5);
            if (file == null) {
                file = ((MediaMessage) message).downloadMedia(20);
            }
            if (file == null) {
                return ResponseEntity.notFound().build();
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
}
