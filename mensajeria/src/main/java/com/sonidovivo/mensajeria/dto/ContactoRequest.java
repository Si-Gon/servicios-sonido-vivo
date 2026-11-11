package com.sonidovivo.mensajeria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Lo que llega desde el formulario de contacto. No es la entidad: el id, el estado
// y la fecha los pone el servidor.
public record ContactoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo,

        @Size(max = 80, message = "La region no puede superar los 80 caracteres")
        String region,

        @Size(max = 80, message = "La comuna no puede superar los 80 caracteres")
        String comuna,

        @NotBlank(message = "El mensaje es obligatorio")
        String mensaje
) {
}
