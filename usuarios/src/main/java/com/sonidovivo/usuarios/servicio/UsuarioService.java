package com.sonidovivo.usuarios.servicio;

import com.sonidovivo.usuarios.dto.UsuarioRequest;
import com.sonidovivo.usuarios.excepcion.ErrorDeNegocio;
import com.sonidovivo.usuarios.model.Rol;
import com.sonidovivo.usuarios.model.Usuario;
import com.sonidovivo.usuarios.repository.RolRepository;
import com.sonidovivo.usuarios.repository.UsuarioRepository;
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
        return usuarios.findById(id).orElseThrow(() -> ErrorDeNegocio.noEncontrado("Usuario no encontrado."));
    }

    @Transactional
    public Usuario registrar(UsuarioRequest datos, String rolPedido) {
        String rut = RutValidador.normalizar(datos.rut());
        String email = datos.email().trim().toLowerCase();

        if (!RutValidador.esValido(rut)) {
            throw ErrorDeNegocio.invalido("El RUT ingresado no es válido.");
        }

        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ErrorDeNegocio.invalido("Ya existe una cuenta con ese correo.");
        }

        if (usuarios.existsByRut(rut)) {
            throw ErrorDeNegocio.invalido("Ya existe una cuenta con ese RUT.");
        }

        String nombreRol = (rolPedido == null || rolPedido.isBlank()) ? Rol.CLIENTE : rolPedido.trim();
        Rol rol = roles.findByNombreIgnoreCase(nombreRol)
                .orElseThrow(() -> ErrorDeNegocio.invalido("El rol no es válido."));

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
                .orElseThrow(() -> ErrorDeNegocio.noAutorizado("Correo o contraseña incorrectos."));

        if (!encriptador.matches(password, usuario.getPasswordHash())) {
            throw ErrorDeNegocio.noAutorizado("Correo o contraseña incorrectos.");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw ErrorDeNegocio.prohibido("La cuenta está desactivada.");
        }

        return usuario;
    }

    @Transactional
    public Usuario cambiarRol(Long id, String rolPedido) {
        Usuario usuario = buscar(id);

        Rol rol = roles.findByNombreIgnoreCase(rolPedido == null ? "" : rolPedido.trim())
                .orElseThrow(() -> ErrorDeNegocio.invalido("El rol no es válido."));

        boolean dejaDeSerAdmin = Rol.ADMINISTRADOR.equalsIgnoreCase(usuario.getRol().getNombre())
                && !Rol.ADMINISTRADOR.equalsIgnoreCase(rol.getNombre());

        // La tienda no puede quedar sin administrador activo.
        if (dejaDeSerAdmin && Boolean.TRUE.equals(usuario.getActivo()) && administradoresActivos() <= 1) {
            throw ErrorDeNegocio.invalido("No puedes quitar el rol al último administrador activo.");
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
            throw ErrorDeNegocio.invalido("No puedes desactivar al último administrador activo.");
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
}
