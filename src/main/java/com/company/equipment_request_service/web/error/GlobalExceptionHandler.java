package com.company.equipment_request_service.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice


public class GlobalExceptionHandler {

    @ExceptionHandler(RequestNotFoundException.class)

    public ResponseEntity<ErrorResponse> handleNotFound(RequestNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)  // HTTP 404
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleBadId(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // HTTP 400
                .body(new ErrorResponse("INVALID_ID_FORMAT",
                        "Идентификатор должен быть целым числом: '" + ex.getValue() + "'"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Невалидные данные");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // HTTP 400
                .body(new ErrorResponse("VALIDATION_ERROR", msg));
    }
}