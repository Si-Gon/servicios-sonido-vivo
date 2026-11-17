package com.sonidovivo.catalogo.repository;

import com.sonidovivo.catalogo.model.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByProductoIdOrderByCreadoEnDescIdDesc(Long productoId);

    // Sirve para no aplicar dos veces la misma venta o la misma cancelacion.
    Optional<MovimientoStock> findByProductoIdAndMotivoAndReferencia(Long productoId, String motivo, String referencia);
}
