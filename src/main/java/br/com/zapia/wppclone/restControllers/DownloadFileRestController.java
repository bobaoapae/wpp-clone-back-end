package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.servicos.DownloadFileService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/downloadFile")
public class DownloadFileRestController {

    @Autowired
    private DownloadFileService downloadFileService;

    @GetMapping("/{key}")
    public ResponseEntity<?> downloadMedia(@PathVariable("key") String key) {
        try {
            File file = downloadFileService.getFileToDownload(key);
            Path path = Paths.get(file.getAbsolutePath());
            String contentType = new Tika().detect(file);
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("Filename", file.getName());
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Filename");
            downloadFileService.removeFileToDownload(key);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao Baixar", e);
        }
    }

}
