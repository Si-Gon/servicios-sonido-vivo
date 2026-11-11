package com.sonidovivo.mensajeria.model;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Data 
@Entity 
@Table(name = "contacto")
public class Mensajeria {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;
    
    @Column (nullable = false, length = 150)
    private String correo;

    @Column (length = 80)
    private String region;

    @Column (length = 80)
    private String comuna;

    @Column (nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column (name = "estado",nullable = false, length = 20)
    private String estado = "PENDIENTE";
    
    //Vincula el atributo dJava con la columna fecha_creacion de MySQL.

    @Column (name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    //Asigna automaticamente la fecha y hora antes de insertar el contacto.

    @PrePersist 
    protected void asignarFechaCreacion(){
        this.fechaCreacion = LocalDateTime.now();
    }
}
