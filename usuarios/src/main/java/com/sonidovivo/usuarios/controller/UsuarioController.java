package com.sonidovivo.usuarios.controller;

import com.sonidovivo.usuarios.dto.LoginRequest;
import com.sonidovivo.usuarios.dto.UsuarioRequest;
import com.sonidovivo.usuarios.dto.LoginResponse;
import com.sonidovivo.usuarios.dto.UsuarioResponse;
import com.sonidovivo.usuarios.excepcion.ErrorDeNegocio;
import com.sonidovivo.usuarios.seguridad.TokenService;
import com.sonidovivo.usuarios.servicio.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Cuentas de la tienda: registro, login y administración. El CORS lo pone el gateway.
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;
    private final TokenService tokens;

    public UsuarioController(UsuarioService usuarios, TokenService tokens) {
        this.usuarios = usuarios;
        this.tokens = tokens;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarios.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return UsuarioResponse.desde(usuarios.buscar(id));
    }

    // Registro del sitio y alta desde el panel usan el mismo endpoint: cambia si viene el rol.
    @PostMapping
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UsuarioResponse.desde(usuarios.registrar(datos, datos.rol())));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest datos) {
        var usuario = usuarios.autenticar(datos.email(), datos.password());
        return LoginResponse.desde(tokens.emitir(usuario), usuario);
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @RequestBody Map<String, String> cuerpo) {
        return UsuarioResponse.desde(usuarios.cambiarRol(id, cuerpo.get("rol")));
    }

    @PatchMapping("/{id}/activo")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @RequestBody Map<String, Boolean> cuerpo) {
        Boolean activo = cuerpo.get("activo");

        if (activo == null) {
            throw ErrorDeNegocio.invalido("Falta indicar si la cuenta queda activa.");
        }

        return UsuarioResponse.desde(usuarios.cambiarEstado(id, activo));
    }
}
