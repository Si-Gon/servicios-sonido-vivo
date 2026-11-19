package com.sonidovivo.gateway.excepcion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Errores del propio gateway con el mismo formato que los servicios de negocio.
 *
 * Sin esto, cuando un servicio esta caido el gateway responde con su cuerpo por defecto, que no
 * trae "motivo", y el front no puede explicar que paso. Con el manejador, la pantalla dice que el
 * servicio no esta disponible en vez de un "error 503" pelado.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger registro = LoggerFactory.getLogger(ManejadorDeErrores.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> deEstado(ResponseStatusException error, ServerWebExchange intercambio) {
        HttpStatus estado = HttpStatus.resolve(error.getStatusCode().value());
        estado = estado == null ? HttpStatus.INTERNAL_SERVER_ERROR : estado;

        String motivo = estado == HttpStatus.SERVICE_UNAVAILABLE
                ? "El servicio solicitado no esta disponible en este momento. Reintenta en unos segundos."
                : "No se pudo completar la peticion.";

        return cuerpo(estado, motivo, intercambio);
    }

    // El servicio destino no acepta la conexion (esta abajo) o no contesta a tiempo.
    @ExceptionHandler({IOException.class, TimeoutException.class})
    public ResponseEntity<Map<String, Object>> deConexion(Exception error, ServerWebExchange intercambio) {
        registro.warn("Servicio no disponible para {}: {}", intercambio.getRequest().getURI().getPath(), error.getMessage());
        return cuerpo(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio solicitado no esta disponible en este momento. Reintenta en unos segundos.", intercambio);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception error, ServerWebExchange intercambio) {
        registro.error("Error inesperado en el gateway", error);
        return cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado en el gateway.", intercambio);
    }

    private ResponseEntity<Map<String, Object>> cuerpo(HttpStatus estado, String motivo, ServerWebExchange intercambio) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("momento", Instant.now().toString());
        respuesta.put("estado", estado.value());
        respuesta.put("motivo", motivo);
        respuesta.put("ruta", intercambio.getRequest().getURI().getPath());

        return ResponseEntity.status(estado).body(respuesta);
    }
}
