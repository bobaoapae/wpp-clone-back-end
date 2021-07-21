package br.com.zapia.wppclone.servicos;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadFileService {

    private final Map<String, File> filesUploaded;

    public UploadFileService() {
        filesUploaded = new ConcurrentHashMap<>();
    }

    public String addFileUploaded(MultipartFile multipartFile) throws IOException {
        String key = UUID.randomUUID().toString();

        String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        File file = File.createTempFile(multipartFile.getOriginalFilename() + "#", ".tmp");

        Files.copy(multipartFile.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        filesUploaded.put(key, file);

        return key;
    }

    public File getAndRemoveFileUploaded(String key) {
        return filesUploaded.remove(key);
    }

}
