package com.ltdd.streamapp.gdrive.exception;

import com.ltdd.streamapp.gdrive.payload.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;


@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Handle general exceptions using the INTERNAL_SERVER_ERROR enum
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                ErrorCode.INTERNAL_SERVER_ERROR.getStatus(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, null, ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
    }

    // Handle runtime exceptions using the BAD_REQUEST enum
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeExceptions(RuntimeException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                ErrorCode.BAD_REQUEST.getStatus(),
                ErrorCode.BAD_REQUEST.getMessage(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, null, ErrorCode.BAD_REQUEST.getStatus());
    }

    // Override handling of validation errors (Spring 6 uses HttpStatusCode)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        StringBuilder errorMessage = new StringBuilder("Validation failed for: ");
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errorMessage.append(error.getField())
                        .append(" (")
                        .append(error.getDefaultMessage())
                        .append(") ")
        );
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                ErrorCode.VALIDATION_ERROR.getStatus(),
                ErrorCode.VALIDATION_ERROR.getMessage(),
                errorMessage.toString(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, headers, status);
    }
}