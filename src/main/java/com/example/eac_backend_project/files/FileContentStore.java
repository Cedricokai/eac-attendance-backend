package com.example.eac_backend_project.files;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Immutable, opaque content keys. Never resolve client filenames as filesystem paths. */
@Component
public class FileContentStore {
    private final Path root;
    public FileContentStore(@Value("${app.files.storage-root:./uploads/file-suite}") String root) { this.root=Path.of(root).toAbsolutePath().normalize(); }
    public record Stored(String key,String checksum,long size) {}
    public Stored write(InputStream stream,long limit) throws IOException {
        Files.createDirectories(root);
        String key=UUID.randomUUID().toString(); Path path=path(key); long size=0;
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try (OutputStream out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)) {
                byte[] buffer=new byte[65536]; int read;
                while ((read=stream.read(buffer))!=-1) {
                    size+=read;
                    if(size>limit) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"File exceeds upload or storage limit");
                    digest.update(buffer,0,read); out.write(buffer,0,read);
                }
            }
            return new Stored(key,HexFormat.of().formatHex(digest.digest()),size);
        } catch (IOException|RuntimeException|NoSuchAlgorithmException e) { Files.deleteIfExists(path); if(e instanceof IOException io) throw io; if(e instanceof RuntimeException runtime) throw runtime; throw new IllegalStateException(e); }
    }
    public Path path(String key) {
        if(key==null || !key.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("Invalid storage key");
        Path path=root.resolve(key).normalize(); if(!path.startsWith(root)) throw new IllegalArgumentException("Invalid storage path"); return path;
    }
    public void remove(String key) { try { Files.deleteIfExists(path(key)); } catch(IOException e) { throw new IllegalStateException("Failed to remove content",e); } }
}
