package com.example.sms.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /** Validates and saves the image, returns the generated file name. */
    String store(MultipartFile file, Long studentId);

    Resource load(String fileName);

    void delete(String fileName);

    /** image/png or image/jpeg, decided from the file extension we generated. */
    String contentTypeOf(String fileName);
}
