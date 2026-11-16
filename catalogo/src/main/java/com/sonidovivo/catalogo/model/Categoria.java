package com.sonidovivo.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

// Categoria del catalogo: agrupa los productos.
@Data
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String nombre;

    // La base pone la fecha: la entidad solo la lee.
    @Column(name = "creado_en", insertable = false, updatable = false)
    private java.time.LocalDateTime creadoEn;
}
