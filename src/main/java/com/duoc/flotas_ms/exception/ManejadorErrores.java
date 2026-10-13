package com.duoc.flotas_ms.exception;

import com.duoc.flotas_ms.dto.RespuestaError;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger logger = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaError> manejarEstado(ResponseStatusException error) {
        String mensaje = error.getReason();
        if (mensaje == null) {
            mensaje = "No se pudo procesar la solicitud";
        }
        RespuestaError respuesta = new RespuestaError(error.getStatusCode().value(),
                mensaje, new ArrayList<>());
        return new ResponseEntity<>(respuesta, error.getHeaders(), error.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException error) {
        List<String> detalles = new ArrayList<>();
        for (FieldError campo : error.getBindingResult().getFieldErrors()) {
            detalles.add(campo.getField() + ": " + campo.getDefaultMessage());
        }
        RespuestaError respuesta = new RespuestaError(400, "Datos de entrada invalidos", detalles);
        return ResponseEntity.badRequest().body(respuesta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> manejarJson(HttpMessageNotReadableException error) {
        RespuestaError respuesta = new RespuestaError(400,
                "El cuerpo debe contener un JSON valido con los campos esperados", new ArrayList<>());
        return ResponseEntity.badRequest().body(respuesta);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> manejarParametro(MethodArgumentTypeMismatchException error) {
        RespuestaError respuesta = new RespuestaError(400,
                "El parametro de la solicitud tiene un formato invalido", new ArrayList<>());
        return ResponseEntity.badRequest().body(respuesta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> manejarOtrosErrores(Exception error) {
        if (error instanceof ErrorResponse) {
            ErrorResponse errorHttp = (ErrorResponse) error;
            int estado = errorHttp.getStatusCode().value();
            String mensaje = "No se pudo procesar la solicitud";
            if (estado == 404) {
                mensaje = "Recurso no encontrado";
            } else if (estado == 405) {
                mensaje = "Metodo HTTP no permitido";
            } else if (estado == 415) {
                mensaje = "El tipo de contenido no es compatible";
            }
            RespuestaError respuesta = new RespuestaError(estado, mensaje, new ArrayList<>());
            return new ResponseEntity<>(respuesta, errorHttp.getHeaders(), errorHttp.getStatusCode());
        }

        logger.error("Error inesperado al procesar la solicitud", error);
        RespuestaError respuesta = new RespuestaError(500,
                "Error interno del servidor", new ArrayList<>());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }
}
