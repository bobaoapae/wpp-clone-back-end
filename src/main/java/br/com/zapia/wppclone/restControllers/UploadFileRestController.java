package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.servicos.UploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/uploadFile")
public class UploadFileRestController {

    @Autowired
    private UploadFileService uploadFileService;

    @PostMapping
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(uploadFileService.addFileUploaded(file));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao fazer Upload", e);
        }
    }

}
