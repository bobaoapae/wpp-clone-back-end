package br.com.zapia.wppclone;

import java.io.File;

public class TemporaryFileHolder {
    private final File tempFile;
    private final String originalName;

    public TemporaryFileHolder(File tempFile, String originalName) {
        this.tempFile = tempFile;
        this.originalName = originalName;
    }

    public File getTempFile() {
        return tempFile;
    }

    public String getOriginalName() {
        return originalName;
    }
}
