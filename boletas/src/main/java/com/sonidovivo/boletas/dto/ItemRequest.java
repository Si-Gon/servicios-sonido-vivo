package com.sonidovivo.boletas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Una linea del carrito tal como llega al crear la boleta.
//
// Solo vienen el producto y la cantidad: el nombre y el precio los toma este servicio del catalogo,
// asi el total no depende de lo que diga el navegador.
public record ItemRequest(

        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad
) {
}
