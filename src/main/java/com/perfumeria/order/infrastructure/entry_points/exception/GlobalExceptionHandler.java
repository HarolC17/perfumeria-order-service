package com.perfumeria.order.infrastructure.entry_points.exception;

import com.perfumeria.order.domain.exception.*;
import com.perfumeria.order.infrastructure.entry_points.dto.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =============================
    // 400 - BAD REQUEST (Validaciones)
    // =============================
    @ExceptionHandler({
            CampoObligatorioException.class,
            CarritoVacioException.class,
            TotalInvalidoException.class,
            EstadoInvalidoException.class,
            StockInsuficienteException.class
    })
    public ResponseEntity<ResponseDTO> handleBadRequest(RuntimeException ex) {
        ResponseDTO response = new ResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // =============================
    // 404 - NOT FOUND
    // =============================
    @ExceptionHandler({
            UsuarioNoEncontradoException.class,
            PedidoNoEncontradoException.class,
            PagoNoEncontradoException.class
    })
    public ResponseEntity<ResponseDTO> handleNotFound(RuntimeException ex) {
        ResponseDTO response = new ResponseDTO(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // =============================
    // 400 - Bean Validation
    // =============================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO> handleValidationException(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(". "));

        ResponseDTO response = new ResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                errores
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // =============================
    // 500 - INTERNAL SERVER ERROR
    // =============================
    @ExceptionHandler({
            PedidoPersistenciaException.class,
            PagoPersistenciaException.class,
            ErrorDeComunicacionException.class
    })
    public ResponseEntity<ResponseDTO> handleInternalError(RuntimeException ex) {
        ResponseDTO response = new ResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // =============================
    // 500 - Excepción General
    // =============================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleGeneral(Exception ex) {
        ResponseDTO response = new ResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor: " + ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}