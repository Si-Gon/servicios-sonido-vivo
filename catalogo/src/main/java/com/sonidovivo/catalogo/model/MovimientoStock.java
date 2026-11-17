package com.sonidovivo.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

// Una linea del kardex. No se modifica ni se borra: es el historial del producto.
@Data
@Entity
@Table(name = "movimiento_stock")
public class MovimientoStock {

    public static final String VENTA = "VENTA";
    public static final String CANCELACION = "CANCELACION";
    public static final String AJUSTE = "AJUSTE";
    public static final String INICIAL = "INICIAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, length = 40)
    private String motivo;

    // Que lo origino: PEDIDO-12 en ventas y cancelaciones, nulo en los ajustes del panel.
    @Column(length = 60)
    private String referencia;

    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    // Solo para leer el producto en el historial: la columna la escribe productoId.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    private Producto producto;
}
