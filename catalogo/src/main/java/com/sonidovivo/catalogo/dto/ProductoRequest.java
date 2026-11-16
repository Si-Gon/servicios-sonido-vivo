package com.sonidovivo.catalogo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Lo que llega al crear o editar un producto. El id y las fechas los pone el servidor.
public record ProductoRequest(

        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 20, message = "El codigo no puede superar los 20 caracteres")
        String codigo,

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 60, message = "La marca no puede superar los 60 caracteres")
        String marca,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 60, message = "El modelo no puede superar los 60 caracteres")
        String modelo,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        BigDecimal precio,

        @Min(value = 0, message = "El descuento no puede ser negativo")
        @Max(value = 100, message = "El descuento no puede superar el 100")
        Integer descuento,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
        String descripcion,

        @Size(max = 160, message = "El nombre del archivo de imagen no puede superar los 160 caracteres")
        String imagen,

        Boolean activo
) {
}
