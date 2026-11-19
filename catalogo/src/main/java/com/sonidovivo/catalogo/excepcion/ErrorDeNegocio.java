package com.sonidovivo.catalogo.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con el codigo HTTP que le corresponde.
 *
 * Los servicios lanzan este error y el manejador global arma la respuesta. Asi el mensaje se
 * escribe una sola vez, en el lugar donde se detecta el problema, y no se repite en cada
 * controlador un ResponseEntity con su Map.
 */
public class ErrorDeNegocio extends RuntimeException {

    private final HttpStatus estado;

    private ErrorDeNegocio(HttpStatus estado, String motivo) {
        super(motivo);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    /** 400: los datos enviados no sirven (campo vacio, codigo repetido, tipo mal escrito). */
    public static ErrorDeNegocio invalido(String motivo) {
        return new ErrorDeNegocio(HttpStatus.BAD_REQUEST, motivo);
    }

    /** 404: lo que se pidio no existe. */
    public static ErrorDeNegocio noEncontrado(String motivo) {
        return new ErrorDeNegocio(HttpStatus.NOT_FOUND, motivo);
    }

    /** 401: falta la credencial o no coincide (login). */
    public static ErrorDeNegocio noAutorizado(String motivo) {
        return new ErrorDeNegocio(HttpStatus.UNAUTHORIZED, motivo);
    }

    /** 403: la cuenta existe pero no puede usar el servicio (desactivada). */
    public static ErrorDeNegocio prohibido(String motivo) {
        return new ErrorDeNegocio(HttpStatus.FORBIDDEN, motivo);
    }

    /** 409: la operacion choca con el estado actual (categoria con productos, ultimo administrador). */
    public static ErrorDeNegocio conflicto(String motivo) {
        return new ErrorDeNegocio(HttpStatus.CONFLICT, motivo);
    }

    /** 422: los datos son validos pero la operacion no se puede completar (stock insuficiente). */
    public static ErrorDeNegocio sinStock(String motivo) {
        return new ErrorDeNegocio(HttpStatus.UNPROCESSABLE_ENTITY, motivo);
    }

    /** 503: depende de otro servicio que en este momento no responde. */
    public static ErrorDeNegocio noDisponible(String motivo) {
        return new ErrorDeNegocio(HttpStatus.SERVICE_UNAVAILABLE, motivo);
    }
}
