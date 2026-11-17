package com.sonidovivo.boletas.repository;

import com.sonidovivo.boletas.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = "items")
    List<Pedido> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = "items")
    List<Pedido> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @EntityGraph(attributePaths = "items")
    List<Pedido> findByEstadoOrderByFechaCreacionDesc(String estado);

    @EntityGraph(attributePaths = "items")
    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);

    @EntityGraph(attributePaths = "items")
    Optional<Pedido> findByClaveIdempotencia(String claveIdempotencia);
}
