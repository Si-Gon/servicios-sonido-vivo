package com.sonidovivo.catalogo.dto;

import com.sonidovivo.catalogo.model.Producto;

import java.math.BigDecimal;

// Respuesta del producto: plana y con la categoria como nombre, igual que el tipo del front.
public record ProductoResponse(
        Long id,
        String codigo,
        Long categoriaId,
        String categoria,
        String nombre,
        String marca,
        String modelo,
        BigDecimal precio,
        Integer descuento,
        Integer stock,
        String descripcion,
        String imagen,
        boolean activo
) {

    public static ProductoResponse desde(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getNombre(),
                producto.getMarca(),
                producto.getModelo(),
                producto.getPrecio(),
                producto.getDescuento(),
                producto.getStock(),
                producto.getDescripcion(),
                producto.getImagen(),
                Boolean.TRUE.equals(producto.getActivo()));
    }
}
