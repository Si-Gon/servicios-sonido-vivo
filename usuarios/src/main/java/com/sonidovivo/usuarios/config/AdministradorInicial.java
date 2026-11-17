package com.sonidovivo.usuarios.config;

import com.sonidovivo.usuarios.model.Rol;
import com.sonidovivo.usuarios.model.Usuario;
import com.sonidovivo.usuarios.repository.RolRepository;
import com.sonidovivo.usuarios.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Si la base esta vacia, deja un administrador para poder entrar al panel.
 *
 * El front traia ese usuario escrito en el codigo (admin@sonidovivo.cl / admin123); ahora se crea
 * una sola vez, con la contrasena cifrada y solo cuando no hay ninguna cuenta.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final String password;

    public AdministradorInicial(UsuarioRepository usuarios,
                                RolRepository roles,
                                @Value("${usuarios.admin.password:admin123}") String password) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }

        Rol administrador = roles.findByNombreIgnoreCase(Rol.ADMINISTRADOR).orElse(null);

        if (administrador == null) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNombre("Administrador");
        usuario.setRut("12345678-5");
        usuario.setEmail("admin@sonidovivo.cl");
        usuario.setRegion(null);
        usuario.setComuna(null);
        usuario.setPasswordHash(new BCryptPasswordEncoder().encode(password));
        usuario.setRol(administrador);
        usuario.setActivo(true);

        usuarios.save(usuario);
    }
}
