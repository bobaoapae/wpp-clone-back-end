package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.servicos.DownloadFileService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/downloadFile")
public class DownloadFileRestController {

    @Autowired
    private DownloadFileService downloadFileService;

    @GetMapping("/{key}")
    public ResponseEntity<StreamingResponseBody> downloadMedia(@PathVariable("key") String key) {
        try {
            File file = downloadFileService.getFileToDownload(key);
            if (file != null) {
                Path path = Paths.get(file.getAbsolutePath());
                StreamingResponseBody streamingResponseBody = outputStream -> outputStream.write(Files.readAllBytes(path));
                HttpHeaders headers = new HttpHeaders();
                String filenameUtf = URLEncoder.encode(file.getName().split("#")[0], StandardCharsets.UTF_8);
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filenameUtf + "; filename=" + filenameUtf);
                headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Filename");
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");
                headers.add("Filename", filenameUtf);
                headers.add("Content-Type", new Tika().detect(file));
                downloadFileService.removeFileToDownload(key);
                return new ResponseEntity(streamingResponseBody, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao Baixar", e);
        }
    }

}
