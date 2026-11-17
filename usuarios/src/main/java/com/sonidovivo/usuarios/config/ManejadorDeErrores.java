package com.sonidovivo.usuarios.config;

import com.sonidovivo.usuarios.servicio.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Un solo lugar para que el front reciba {"motivo": "..."} en vez de un 500 sin explicación.
@RestControllerAdvice
public class ManejadorDeErrores {

    // Los @Valid también responden con motivo: el front muestra un mensaje, no un 400 mudo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(MethodArgumentNotValidException error) {
        String mensaje = error.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Revisa los datos enviados.");

        return ResponseEntity.badRequest().body(Map.of("motivo", mensaje));
    }

    @ExceptionHandler(UsuarioService.ErrorDeNegocio.class)
    public ResponseEntity<Map<String, String>> errorDeNegocio(UsuarioService.ErrorDeNegocio error) {
        return ResponseEntity.status(error.getEstado()).body(Map.of("motivo", error.getMessage()));
    }
}
