package com.sonidovivo.usuarios.servicio;

import com.sonidovivo.usuarios.dto.UsuarioRequest;
import com.sonidovivo.usuarios.model.Rol;
import com.sonidovivo.usuarios.model.Usuario;
import com.sonidovivo.usuarios.repository.RolRepository;
import com.sonidovivo.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro, login y administracion de cuentas.
 *
 * Las reglas viven acá y no en el navegador: el front mostraba los mismos mensajes, pero los
 * validaba con la copia local. Ahora las cumple el servicio, incluso si alguien llama la API
 * directamente.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final BCryptPasswordEncoder encriptador = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    public List<Usuario> listar() {
        return usuarios.findAllByOrderByNombreAsc();
    }

    public Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new ErrorDeNegocio(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    @Transactional
    public Usuario registrar(UsuarioRequest datos, String rolPedido) {
        String rut = RutValidador.normalizar(datos.rut());
        String email = datos.email().trim().toLowerCase();

        if (!RutValidador.esValido(rut)) {
            throw new ErrorDeNegocio(HttpStatus.BAD_REQUEST, "El RUT ingresado no es válido.");
        }

        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ErrorDeNegocio(HttpStatus.BAD_REQUEST, "Ya existe una cuenta con ese correo.");
        }

        if (usuarios.existsByRut(rut)) {
            throw new ErrorDeNegocio(HttpStatus.BAD_REQUEST, "Ya existe una cuenta con ese RUT.");
        }

        String nombreRol = (rolPedido == null || rolPedido.isBlank()) ? Rol.CLIENTE : rolPedido.trim();
        Rol rol = roles.findByNombreIgnoreCase(nombreRol)
                .orElseThrow(() -> new ErrorDeNegocio(HttpStatus.BAD_REQUEST, "El rol no es válido."));

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setRut(rut);
        usuario.setEmail(email);
        usuario.setRegion(vacio(datos.region()) ? null : datos.region().trim());
        usuario.setComuna(vacio(datos.comuna()) ? null : datos.comuna().trim());
        usuario.setPasswordHash(encriptador.encode(datos.password()));
        usuario.setRol(rol);
        usuario.setActivo(datos.activo() == null || datos.activo());

        return usuarios.save(usuario);
    }

    /** Login: compara la contrasena con el hash y no devuelve nada si no coincide. */
    public Usuario autenticar(String email, String password) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new ErrorDeNegocio(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));

        if (!encriptador.matches(password, usuario.getPasswordHash())) {
            throw new ErrorDeNegocio(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ErrorDeNegocio(HttpStatus.FORBIDDEN, "La cuenta está desactivada.");
        }

        return usuario;
    }

    @Transactional
    public Usuario cambiarRol(Long id, String rolPedido) {
        Usuario usuario = buscar(id);

        Rol rol = roles.findByNombreIgnoreCase(rolPedido == null ? "" : rolPedido.trim())
                .orElseThrow(() -> new ErrorDeNegocio(HttpStatus.BAD_REQUEST, "El rol no es válido."));

        boolean dejaDeSerAdmin = Rol.ADMINISTRADOR.equalsIgnoreCase(usuario.getRol().getNombre())
                && !Rol.ADMINISTRADOR.equalsIgnoreCase(rol.getNombre());

        // La tienda no puede quedar sin administrador activo.
        if (dejaDeSerAdmin && Boolean.TRUE.equals(usuario.getActivo()) && administradoresActivos() <= 1) {
            throw new ErrorDeNegocio(HttpStatus.BAD_REQUEST, "No puedes quitar el rol al último administrador activo.");
        }

        usuario.setRol(rol);
        return usuarios.save(usuario);
    }

    @Transactional
    public Usuario cambiarEstado(Long id, boolean activo) {
        Usuario usuario = buscar(id);

        boolean seDesactiva = Boolean.TRUE.equals(usuario.getActivo()) && !activo;

        if (seDesactiva && Rol.ADMINISTRADOR.equalsIgnoreCase(usuario.getRol().getNombre())
                && administradoresActivos() <= 1) {
            throw new ErrorDeNegocio(HttpStatus.BAD_REQUEST, "No puedes desactivar al último administrador activo.");
        }

        usuario.setActivo(activo);
        return usuarios.save(usuario);
    }

    private long administradoresActivos() {
        return usuarios.countByRolNombreAndActivoTrue(Rol.ADMINISTRADOR);
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    // Error de negocio con su codigo HTTP: el controlador solo lo traduce a respuesta.
    public static class ErrorDeNegocio extends RuntimeException {
        private final HttpStatus estado;

        public ErrorDeNegocio(HttpStatus estado, String mensaje) {
            super(mensaje);
            this.estado = estado;
        }

        public HttpStatus getEstado() {
            return estado;
        }
    }
}
