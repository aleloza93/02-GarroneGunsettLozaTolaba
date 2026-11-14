package com.tuti.daos.informes.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class InformeResolucionExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> manejarFalloPersistencia(
            DataAccessException exception,
            HttpServletRequest request
    ) {
        return crearRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo guardar la resolución",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> manejarNoEncontrado(
            RecursoNoEncontradoException exception,
            HttpServletRequest request
    ) {
        return crearRespuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConflictoOperacionException.class)
    public ResponseEntity<ApiErrorResponse> manejarConflicto(
            ConflictoOperacionException exception,
            HttpServletRequest request
    ) {
        return crearRespuesta(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarValidacion(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene campos inválidos",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> manejarFormatoInvalido(
            Exception exception,
            HttpServletRequest request
    ) {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "El cuerpo o los parámetros tienen un formato inválido",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> crearRespuesta(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
