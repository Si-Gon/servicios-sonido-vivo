package com.sonidovivo.despachos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Datos de entrega de un pedido. Con RETIRO no se exige dirección; con DOMICILIO, sí.
public record DespachoRequest(

        @NotNull(message = "El pedido es obligatorio")
        Long pedidoId,

        @NotBlank(message = "El tipo de entrega es obligatorio")
        String tipoEntrega,

        @NotBlank(message = "El nombre de quien recibe es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,

        @Size(max = 80, message = "La región no puede superar los 80 caracteres")
        String region,

        @Size(max = 80, message = "La comuna no puede superar los 80 caracteres")
        String comuna,

        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
        String direccion,

        @Size(max = 200, message = "La referencia no puede superar los 200 caracteres")
        String referencia,

        @PositiveOrZero(message = "El costo de envío no puede ser negativo")
        BigDecimal costoEnvio
) {
}
