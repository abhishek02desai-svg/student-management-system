package com.example.sms.service.impl;

import com.example.sms.exception.BadRequestException;
import com.example.sms.exception.FileStorageException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB

    private final Path root;

    public FileStorageServiceImpl(@Value("${app.file.upload-dir:uploads/profile-images}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory: " + root, e);
        }
        log.info("Profile images will be stored in {}", root);
    }

    @Override
    public String store(MultipartFile file, Long studentId) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a non-empty image file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image is too large. Maximum allowed size is 2 MB");
        }

        // We look at the real file bytes, not the file name / Content-Type sent by the client
        String extension;
        try (InputStream in = file.getInputStream()) {
            extension = detectExtension(in.readNBytes(8));
        } catch (IOException e) {
            throw new FileStorageException("Could not read the uploaded file", e);
        }
        if (extension == null) {
            throw new BadRequestException("Only JPEG and PNG images are allowed");
        }

        // Generated name => the client's file name is never used (no path traversal)
        String fileName = "student-" + studentId + "-" + UUID.randomUUID() + "." + extension;
        Path target = root.resolve(fileName).normalize();

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Could not store the uploaded file", e);
        }

        log.info("Stored profile image {}", fileName);
        return fileName;
    }

    @Override
    public Resource load(String fileName) {
        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root) || !Files.exists(file)) {
            throw new ResourceNotFoundException("Profile image file not found");
        }
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String fileName) {
        if (fileName == null) {
            return;
        }
        try {
            Path file = root.resolve(fileName).normalize();
            if (file.startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            // not fatal: the DB is already updated, just leave a note
            log.warn("Could not delete file {}: {}", fileName, e.getMessage());
        }
    }

    @Override
    public String contentTypeOf(String fileName) {
        return fileName != null && fileName.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private String detectExtension(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return "png";
        }
        return null;
    }
}
