package com.sonidovivo.boletas.controller;

import com.sonidovivo.boletas.dto.PedidoRequest;
import com.sonidovivo.boletas.dto.PedidoResponse;
import com.sonidovivo.boletas.model.EstadoPedido;
import com.sonidovivo.boletas.model.Pedido;
import com.sonidovivo.boletas.model.PedidoItem;
import com.sonidovivo.boletas.repository.PedidoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Boletas: se emiten al pagar y las consulta el panel y el historial del cliente.
// El CORS lo maneja el gateway.
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository pedidos;

    public PedidoController(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    // Emite la boleta: el total y los subtotales los calcula el servicio, no el navegador.
    @PostMapping
    public ResponseEntity<?> emitir(@Valid @RequestBody PedidoRequest datos) {
        Pedido pedido = new Pedido();
        pedido.setUsuarioId(datos.usuarioId());
        pedido.setNombreCliente(datos.nombreCliente().trim());
        pedido.setEmailCliente(datos.emailCliente().trim().toLowerCase());
        pedido.setEstado(EstadoPedido.PENDIENTE.name());

        BigDecimal total = BigDecimal.ZERO;

        for (var linea : datos.items()) {
            PedidoItem item = new PedidoItem();
            item.setProductoId(linea.productoId());
            item.setNombreProducto(linea.nombreProducto().trim());
            item.setPrecioUnitario(linea.precioUnitario());
            item.setCantidad(linea.cantidad());

            BigDecimal subtotal = linea.precioUnitario().multiply(BigDecimal.valueOf(linea.cantidad()));
            item.setSubtotal(subtotal);
            total = total.add(subtotal);

            pedido.agregarItem(item);
        }

        pedido.setTotal(total);

        Pedido guardado = pedidos.save(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.desde(guardado));
    }

    // Listado para el panel; admite filtrar por usuario o por estado.
    @GetMapping
    public List<PedidoResponse> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String estado) {

        List<Pedido> encontrados;

        if (usuarioId != null) {
            encontrados = pedidos.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
        } else if (estado != null && !estado.isBlank()) {
            encontrados = pedidos.findByEstadoOrderByFechaCreacionDesc(estado.toUpperCase());
        } else {
            encontrados = pedidos.findAllByOrderByFechaCreacionDesc();
        }

        return encontrados.stream().map(PedidoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable Long id) {
        return pedidos.findById(id)
                .map(PedidoResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cambia el estado (en preparacion, despachado, entregado, cancelado).
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> cuerpo) {
        String nuevo = cuerpo.getOrDefault("estado", "").toUpperCase();

        boolean valido = Arrays.stream(EstadoPedido.values()).anyMatch(estado -> estado.name().equals(nuevo));

        if (!valido) {
            return ResponseEntity.badRequest()
                    .body(Map.of("motivo", "Estado invalido. Se acepta: " + Arrays.toString(EstadoPedido.values())));
        }

        return pedidos.findById(id).map(pedido -> {
            pedido.setEstado(nuevo);
            return ResponseEntity.ok(PedidoResponse.desde(pedidos.save(pedido)));
        }).orElse(ResponseEntity.notFound().build());
    }
}
