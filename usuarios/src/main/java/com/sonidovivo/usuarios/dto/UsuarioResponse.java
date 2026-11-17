package com.sonidovivo.usuarios.dto;

import com.sonidovivo.usuarios.model.Usuario;

// Lo que ve el front: nunca sale el hash ni la contrasena.
public record UsuarioResponse(
        Long id,
        String nombre,
        String rut,
        String email,
        String region,
        String comuna,
        String rol,
        Boolean activo
) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRut(),
                usuario.getEmail(),
                usuario.getRegion() == null ? "" : usuario.getRegion(),
                usuario.getComuna() == null ? "" : usuario.getComuna(),
                usuario.getRol().getNombre(),
                usuario.getActivo());
    }
}
