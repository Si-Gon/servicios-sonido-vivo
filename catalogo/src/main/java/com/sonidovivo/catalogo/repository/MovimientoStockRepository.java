package com.sonidovivo.catalogo.repository;

import com.sonidovivo.catalogo.model.MovimientoStock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    @EntityGraph(attributePaths = "producto")
    List<MovimientoStock> findByProductoIdOrderByCreadoEnDescIdDesc(Long productoId);

    // Sirve para no aplicar dos veces la misma venta o la misma cancelacion.
    Optional<MovimientoStock> findByProductoIdAndMotivoAndReferencia(Long productoId, String motivo, String referencia);

    /**
     * Historial completo con filtros opcionales: un parametro nulo significa "todos".
     * El texto busca en la referencia y tambien en el codigo y el nombre del producto.
     */
    @EntityGraph(attributePaths = "producto")
    @Query("""
           SELECT m FROM MovimientoStock m
           WHERE (:productoId IS NULL OR m.productoId = :productoId)
             AND (:tipo IS NULL OR m.tipo = :tipo)
             AND (:motivo IS NULL OR m.motivo = :motivo)
             AND (:texto IS NULL OR UPPER(m.referencia) LIKE UPPER(CONCAT('%', :texto, '%'))
                                OR UPPER(m.producto.nombre) LIKE UPPER(CONCAT('%', :texto, '%'))
                                OR UPPER(m.producto.codigo) LIKE UPPER(CONCAT('%', :texto, '%')))
           ORDER BY m.creadoEn DESC, m.id DESC
           """)
    List<MovimientoStock> buscarConFiltros(@Param("productoId") Long productoId,
                                           @Param("tipo") String tipo,
                                           @Param("motivo") String motivo,
                                           @Param("texto") String texto);
}
