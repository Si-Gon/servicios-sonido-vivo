package com.sonidovivo.boletas.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Boleta: el pedido con sus lineas. El cliente se copia aqui (nombre y correo) por si despues
// cambia en el servicio de usuarios.
@Data
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador del usuario en el servicio de usuarios: sin clave foranea, son bases distintas.
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "nombre_cliente", nullable = false, length = 120)
    private String nombreCliente;

    @Column(name = "email_cliente", nullable = false, length = 150)
    private String emailCliente;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    private String estado = EstadoPedido.PENDIENTE.name();

    // Clave del intento de pago: evita que un reintento cree una segunda boleta.
    @Column(name = "clave_idempotencia", length = 64, unique = true)
    private String claveIdempotencia;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PedidoItem> items = new ArrayList<>();

    // Deja la linea asociada al pedido en los dos sentidos.
    public void agregarItem(PedidoItem item) {
        item.setPedido(this);
        items.add(item);
    }
}
