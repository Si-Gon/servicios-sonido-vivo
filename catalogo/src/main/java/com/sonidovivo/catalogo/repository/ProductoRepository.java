package com.sonidovivo.catalogo.repository;

import com.sonidovivo.catalogo.model.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // La categoria se trae en la misma consulta: sin esto serian 51 consultas para el listado.
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByCategoriaIdAndActivoTrueOrderByNombreAsc(Long categoriaId);

    // Stock en o bajo el minimo: la consulta de productos criticos del panel.
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByStockLessThanEqualAndActivoTrueOrderByStockAsc(Integer stock);

    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findByIdAndActivoTrue(Long id);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    long countByCategoriaId(Long categoriaId);

    @EntityGraph(attributePaths = "categoria")
    @Query("""
           SELECT p FROM Producto p
           WHERE p.activo = true
             AND (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.marca) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.modelo) LIKE LOWER(CONCAT('%', :texto, '%')))
           ORDER BY p.nombre
           """)
    List<Producto> buscarPorTexto(@Param("texto") String texto);

    @EntityGraph(attributePaths = "categoria")
    @Query("""
           SELECT p FROM Producto p
           WHERE p.activo = true AND p.categoria.id = :categoriaId
             AND (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.marca) LIKE LOWER(CONCAT('%', :texto, '%')))
           ORDER BY p.nombre
           """)
    List<Producto> buscarPorTextoEnCategoria(@Param("categoriaId") Long categoriaId, @Param("texto") String texto);

    // Bloquea la fila mientras se calcula el nuevo stock: dos ventas a la vez no pueden leer el mismo valor.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
