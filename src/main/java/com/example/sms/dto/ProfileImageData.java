package com.example.sms.dto;

import org.springframework.core.io.Resource;

/** Result of reading a profile image (never cached). */
public record ProfileImageData(Resource resource, String contentType, String fileName) {
}
