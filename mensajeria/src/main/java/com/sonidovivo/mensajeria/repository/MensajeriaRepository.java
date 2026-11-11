package com.sonidovivo.mensajeria.repository;

import com.sonidovivo.mensajeria.model.Mensajeria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Acceso a la tabla contacto. JpaRepository ya trae guardar, listar, buscar, borrar.
public interface MensajeriaRepository extends JpaRepository<Mensajeria, Long> {

    // Los mensajes que aun no se responden, para la bandeja del administrador.
    List<Mensajeria> findByEstadoOrderByFechaCreacionDesc(String estado);
}
