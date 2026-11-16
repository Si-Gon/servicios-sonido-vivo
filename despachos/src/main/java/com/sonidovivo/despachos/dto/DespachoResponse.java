package com.sonidovivo.despachos.dto;

import com.sonidovivo.despachos.model.Despacho;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DespachoResponse(
        Long id,
        Long pedidoId,
        String tipoEntrega,
        String nombre,
        String telefono,
        String region,
        String comuna,
        String direccion,
        String referencia,
        BigDecimal costoEnvio,
        LocalDateTime creadoEn
) {

    public static DespachoResponse desde(Despacho despacho) {
        return new DespachoResponse(
                despacho.getId(),
                despacho.getPedidoId(),
                despacho.getTipoEntrega(),
                despacho.getNombre(),
                despacho.getTelefono(),
                despacho.getRegion(),
                despacho.getComuna(),
                despacho.getDireccion(),
                despacho.getReferencia(),
                despacho.getCostoEnvio(),
                despacho.getCreadoEn());
    }
}
