package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wpp.client.docker.model.DriverState;
import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.payloads.SendMessageRequest;
import br.com.zapia.wppclone.servicos.UploadFileService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppRestController {

    @Autowired
    @Lazy
    private WhatsAppClone whatsAppClone;
    @Autowired
    private UsuarioPrincipalAutoWired usuarioPrincipalAutoWired;
    @Autowired
    private UploadFileService uploadFileService;
    private final ObjectMapper objectMapper;

    public WhatsAppRestController() {
        objectMapper = new ObjectMapper();
    }

    @GetMapping("/stats")
    @Async
    public CompletableFuture<ResponseEntity<?>> getStatus() {
        return whatsAppClone.getWhatsAppClient().getDriverState().thenCompose(driverState -> {
            var jsonData = objectMapper.createObjectNode();
            jsonData.put("status", driverState.name());
            if (driverState == DriverState.WAITING_QR_CODE_SCAN) {
                return whatsAppClone.getWhatsAppClient().getQrCode().thenApply(qrCode -> {
                    jsonData.put("qrCode", qrCode);
                    return ResponseEntity.ok(jsonData);
                });
            }
            return CompletableFuture.completedFuture(ResponseEntity.ok(jsonData));
        });
    }

    @GetMapping("/logout")
    @Async
    public CompletableFuture<ResponseEntity<?>> logout() {
        return whatsAppClone.getWhatsAppClient().logout().thenApply(aVoid -> ResponseEntity.ok().build());
    }

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(uploadFileService.addFileUploaded(file));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao fazer Upload", e);
        }
    }

    @PostMapping("/sendMessage")
    public CompletableFuture<ResponseEntity<?>> sendMessage(@RequestBody @Valid SendMessageRequest sendMessageRequest) {
        return whatsAppClone.getWhatsAppClient().getDriverState().thenCompose(driverState -> {
            if (driverState != DriverState.LOGGED)
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("WhatsApp not logged"));

            return whatsAppClone.getWhatsAppClient().findChatByNumber(sendMessageRequest.getChatNumber()).thenCompose(chat -> {
                var builder = new br.com.zapia.wpp.api.model.payloads.SendMessageRequest.Builder(chat.getId());
                builder.withText(sendMessageRequest.getMessage());
                CompletableFuture<String> futureUploadFile = null;
                if (sendMessageRequest.getFile() != null) {
                    var file = uploadFileService.getAndRemoveFileUploaded(sendMessageRequest.getFile());
                    if (file == null) {
                        return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("File not Found"));
                    }

                    futureUploadFile = whatsAppClone.getWhatsAppClient().uploadFile(sendMessageRequest.getFileName(), file.getTempFile());
                } else {
                    futureUploadFile = CompletableFuture.completedFuture("");
                }

                return futureUploadFile.thenCompose(s -> {
                    if (!s.isEmpty()) {
                        builder.withFile(s);
                    }
                    return whatsAppClone.getWhatsAppClient().sendMessage(builder.build());
                }).thenApply(message -> ResponseEntity.ok(message.getJsonNode()));
            });
        });
    }

}
