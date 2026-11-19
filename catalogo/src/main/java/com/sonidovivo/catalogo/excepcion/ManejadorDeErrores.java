package com.sonidovivo.catalogo.excepcion;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

/**
 * Un solo formato de error para todo el servicio.
 *
 * El front lee el campo "motivo": si cada controlador respondiera a su manera (o si Spring devolviera
 * su cuerpo por defecto en las validaciones), la pantalla no podria mostrar la causa real.
 *
 * Formato: {"momento", "estado", "motivo", "ruta"}
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger registro = LoggerFactory.getLogger(ManejadorDeErrores.class);

    @ExceptionHandler(ErrorDeNegocio.class)
    public ResponseEntity<Map<String, Object>> deNegocio(ErrorDeNegocio error, HttpServletRequest peticion) {
        return cuerpo(error.getEstado(), error.getMessage(), peticion);
    }

    // Datos de entrada que no cumplen las anotaciones del DTO (@NotBlank, @Email, @Size...).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> deValidacion(MethodArgumentNotValidException error, HttpServletRequest peticion) {
        String motivo = error.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Los datos enviados no son validos.");

        return cuerpo(HttpStatus.BAD_REQUEST, motivo, peticion);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> deCuerpoIlegible(HttpMessageNotReadableException error, HttpServletRequest peticion) {
        return cuerpo(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es valido o esta vacio.", peticion);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> deTipoEquivocado(MethodArgumentTypeMismatchException error, HttpServletRequest peticion) {
        return cuerpo(HttpStatus.BAD_REQUEST, "El valor de '" + error.getName() + "' no tiene el formato esperado.", peticion);
    }

    // Choque contra una restriccion de la base (por ejemplo un codigo duplicado).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> deBaseDeDatos(DataIntegrityViolationException error, HttpServletRequest peticion) {
        registro.warn("Restriccion de base de datos en {}: {}", peticion.getRequestURI(), error.getMostSpecificCause().getMessage());
        return cuerpo(HttpStatus.CONFLICT, "La operacion choca con un dato que ya existe.", peticion);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception error, HttpServletRequest peticion) {
        registro.error("Error inesperado en {}", peticion.getRequestURI(), error);
        return cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado en el servicio.", peticion);
    }

    private ResponseEntity<Map<String, Object>> cuerpo(HttpStatus estado, String motivo, HttpServletRequest peticion) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("momento", Instant.now().toString());
        respuesta.put("estado", estado.value());
        respuesta.put("motivo", motivo);
        respuesta.put("ruta", peticion.getRequestURI());

        return ResponseEntity.status(estado).body(respuesta);
    }
}
