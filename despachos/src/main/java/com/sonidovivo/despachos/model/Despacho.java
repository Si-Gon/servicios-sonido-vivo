package com.sonidovivo.despachos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Entrega de un pedido. pedido_id identifica la boleta pero no es clave foranea: la boleta vive
// en el servicio de boletas, con su propia base.
@Data
@Entity
@Table(name = "despacho")
public class Despacho {

    public static final String DOMICILIO = "DOMICILIO";
    public static final String RETIRO = "RETIRO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false, unique = true)
    private Long pedidoId;

    @Column(name = "tipo_entrega", nullable = false, length = 20)
    private String tipoEntrega = DOMICILIO;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 80)
    private String region;

    @Column(length = 80)
    private String comuna;

    @Column(length = 200)
    private String direccion;

    @Column(length = 200)
    private String referencia;

    @Column(name = "costo_envio", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
