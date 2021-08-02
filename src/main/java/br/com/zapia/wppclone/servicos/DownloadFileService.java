package br.com.zapia.wppclone.servicos;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DownloadFileService {

    private final Map<String, File> filesDownload;

    public DownloadFileService() {
        filesDownload = new ConcurrentHashMap<>();
    }

    public String addFileToFutureDownload(File file) {
        String key = UUID.randomUUID().toString();
        filesDownload.put(key, file);
        return key;
    }

    public File getFileToDownload(String key) {
        return filesDownload.getOrDefault(key, null);
    }

    public void removeFileToDownload(String key) {
        filesDownload.remove(key);
    }
}
