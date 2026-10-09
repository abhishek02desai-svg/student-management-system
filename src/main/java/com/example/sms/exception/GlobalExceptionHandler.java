package com.example.sms.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        log.error(
                "Resource not found. Path: {}, Message: {}",
                request.getRequestURI(),
                exception.getMessage()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .errors(null)
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }


    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException exception,
            HttpServletRequest request) {

        log.error(
                "Duplicate resource. Path: {}, Message: {}",
                request.getRequestURI(),
                exception.getMessage()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .errors(null)
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }


    // @RequestBody validation -> MethodArgumentNotValidException, form-data validation -> BindException
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(
            Exception exception,
            HttpServletRequest request) {

        BindingResult bindingResult = (exception instanceof MethodArgumentNotValidException manve)
                ? manve.getBindingResult()
                : ((BindException) exception).getBindingResult();

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));

        log.warn("Validation failed. Path: {}, Errors: {}", request.getRequestURI(), fieldErrors);

        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }


    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException exception,
            HttpServletRequest request) {

        log.warn("Bad request. Path: {}, Message: {}", request.getRequestURI(), exception.getMessage());

        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request, null);
    }


    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException exception,
            HttpServletRequest request) {

        log.warn("Conflict. Path: {}, Message: {}", request.getRequestURI(), exception.getMessage());

        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException exception,
            HttpServletRequest request) {

        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, null);
    }


    // thrown by @PreAuthorize when the logged-in user is not allowed
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request) {

        log.warn("Access denied. Path: {}", request.getRequestURI());

        return build(HttpStatus.FORBIDDEN, "You do not have permission to do this", request, null);
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {

        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large. Maximum allowed size is 2 MB",
                request, null);
    }


    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(
            FileStorageException exception,
            HttpServletRequest request) {

        log.error("File storage error. Path: {}", request.getRequestURI(), exception);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "File could not be processed", request, null);
    }


    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> errors) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
