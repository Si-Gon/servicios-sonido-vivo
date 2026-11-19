package com.sonidovivo.usuarios.dto;

import com.sonidovivo.usuarios.model.Usuario;

// Lo que devuelve el login: los datos de la cuenta y el token para las siguientes peticiones.
public record LoginResponse(String token, UsuarioResponse usuario) {

    public static LoginResponse desde(String token, Usuario usuario) {
        return new LoginResponse(token, UsuarioResponse.desde(usuario));
    }
}
