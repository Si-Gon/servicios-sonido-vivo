package com.sonidovivo.boletas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Una linea del carrito tal como llega al crear la boleta.
public record ItemRequest(

        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 150, message = "El nombre del producto no puede superar los 150 caracteres")
        String nombreProducto,

        @NotNull(message = "El precio unitario es obligatorio")
        @PositiveOrZero(message = "El precio unitario no puede ser negativo")
        BigDecimal precioUnitario,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad
) {
}
