package com.sonidovivo.catalogo.dto;

import com.sonidovivo.catalogo.model.MovimientoStock;

import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        Long productoId,
        String codigo,
        String producto,
        String tipo,
        Integer cantidad,
        String motivo,
        String referencia,
        Integer stockResultante,
        LocalDateTime creadoEn
) {

    public static MovimientoResponse desde(MovimientoStock movimiento) {
        String codigo = movimiento.getProducto() == null ? null : movimiento.getProducto().getCodigo();
        String nombre = movimiento.getProducto() == null ? null : movimiento.getProducto().getNombre();

        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getProductoId(),
                codigo,
                nombre,
                movimiento.getTipo(),
                movimiento.getCantidad(),
                movimiento.getMotivo(),
                movimiento.getReferencia(),
                movimiento.getStockResultante(),
                movimiento.getCreadoEn());
    }
}
