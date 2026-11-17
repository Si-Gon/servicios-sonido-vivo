package com.sonidovivo.catalogo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Movimiento de stock que pide otro servicio o el panel.
public record MovimientoRequest(

        @NotBlank(message = "El tipo de movimiento es obligatorio")
        String tipo,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 40, message = "El motivo no puede superar los 40 caracteres")
        String motivo,

        @Size(max = 60, message = "La referencia no puede superar los 60 caracteres")
        String referencia
) {
}
