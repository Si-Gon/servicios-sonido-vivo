package com.sonidovivo.usuarios.repository;

import com.sonidovivo.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // El rol se trae en la misma consulta: sin esto el listado hace una consulta por fila.
    @EntityGraph(attributePaths = "rol")
    List<Usuario> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByRut(String rut);

    boolean existsByRutAndIdNot(String rut, Long id);

    // Para no dejar la tienda sin ningun administrador activo.
    long countByRolNombreAndActivoTrue(String rol);
}
