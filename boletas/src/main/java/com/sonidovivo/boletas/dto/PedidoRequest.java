package com.sonidovivo.boletas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Datos para emitir una boleta. El total no se recibe: lo calcula el servicio.
public record PedidoRequest(

        @NotNull(message = "El usuario es obligatorio")
        Long usuarioId,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 120, message = "El nombre del cliente no puede superar los 120 caracteres")
        String nombreCliente,

        @NotBlank(message = "El correo del cliente es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String emailCliente,

        @NotEmpty(message = "La boleta necesita al menos un producto")
        List<@Valid ItemRequest> items,

        // Opcional: la manda el checkout para que un reintento no emita dos boletas.
        @Size(max = 64, message = "La clave de idempotencia no puede superar los 64 caracteres")
        String claveIdempotencia
) {
}
