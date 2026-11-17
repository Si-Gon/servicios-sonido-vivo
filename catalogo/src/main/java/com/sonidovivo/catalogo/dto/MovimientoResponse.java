package com.sonidovivo.catalogo.dto;

import com.sonidovivo.catalogo.model.MovimientoStock;

import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        Long productoId,
        String tipo,
        Integer cantidad,
        String motivo,
        String referencia,
        Integer stockResultante,
        LocalDateTime creadoEn
) {

    public static MovimientoResponse desde(MovimientoStock movimiento) {
        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getProductoId(),
                movimiento.getTipo(),
                movimiento.getCantidad(),
                movimiento.getMotivo(),
                movimiento.getReferencia(),
                movimiento.getStockResultante(),
                movimiento.getCreadoEn());
    }
}
