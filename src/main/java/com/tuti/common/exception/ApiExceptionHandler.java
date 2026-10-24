package com.tuti.common.exception;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException ex,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<ProblemDetail> conflicto(ConflictoNegocioException ex,
            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        ResponseEntity<ProblemDetail> respuesta = error(HttpStatus.BAD_REQUEST,
                "Hay campos faltantes o invalidos.", request);
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorCampo(e.getField(), e.getDefaultMessage())).toList();
        respuesta.getBody().setProperty("errores", errores);
        return respuesta;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> formato(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Formato de solicitud invalido.", request);
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String detalle,
            HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setProperty("_links", java.util.Map.of("self",
                java.util.Map.of("href", request.getRequestURI())));
        return ResponseEntity.status(status).body(problema);
    }

    public record ErrorCampo(String campo, String mensaje) {}
}

