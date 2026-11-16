package com.sonidovivo.despachos.repository;

import com.sonidovivo.despachos.model.Despacho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    List<Despacho> findAllByOrderByCreadoEnDesc();

    Optional<Despacho> findByPedidoId(Long pedidoId);

    boolean existsByPedidoId(Long pedidoId);

    List<Despacho> findByTipoEntregaOrderByCreadoEnDesc(String tipoEntrega);
}
