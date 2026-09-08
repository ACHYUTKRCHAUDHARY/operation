package com.achyut.operation.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private final Path root;

    public FileStorageService(@Value("${app.storage.root:uploads}") String root) throws IOException {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public StoredFile store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");
        if (!ALLOWED.contains(file.getContentType())) throw new IllegalArgumentException("Unsupported file type");
        String safeCategory = category == null ? "misc" : category.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        String ext = extension(file.getOriginalFilename());
        String name = UUID.randomUUID() + ext;
        Path dir = root.resolve(safeCategory).normalize();
        if (!dir.startsWith(root)) throw new IllegalArgumentException("Invalid category");
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(name, safeCategory, "/files/" + safeCategory + "/" + name, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
    }

    public Path resolve(String category, String filename) {
        Path file = root.resolve(category).resolve(filename).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Invalid file path");
        return file;
    }

    private static String extension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx).toLowerCase(Locale.ROOT).replaceAll("[^.a-z0-9]", "");
    }

    public record StoredFile(String filename, String category, String url, long size, String contentType) {}
}
