package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/remoteManagement")
public class RemoteManagementController {

    @Lazy
    @Autowired
    private WhatsAppClone whatsAppClone;

    @Async
    @GetMapping("/stats")
    public CompletableFuture<ResponseEntity<?>> stats() {
        if (whatsAppClone.getWhatsAppClient() != null && whatsAppClone.getWhatsAppClient().isOpen()) {
            return whatsAppClone.getWhatsAppClient().getStats().thenApply(ResponseEntity::ok);
        }

        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("WhatsAppClient not initialized, try again in a few seconds"));
    }

    @GetMapping("/webSocketAddress")
    public ResponseEntity<?> webSocketAddress() {
        if (whatsAppClone.getWhatsAppClient() != null && whatsAppClone.getWhatsAppClient().isOpen()) {
            return ResponseEntity.ok(whatsAppClone.getWhatsAppClient().getRemoteEndPoint() + ":" + whatsAppClone.getWhatsAppClient().getRemotePort());
        }

        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("WhatsAppClient not initialized, try again in a few seconds");

    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        whatsAppClone.ping();
        return ResponseEntity.ok().build();
    }
}
