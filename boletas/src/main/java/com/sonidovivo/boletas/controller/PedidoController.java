package com.sonidovivo.boletas.controller;

import com.sonidovivo.boletas.dto.PedidoRequest;
import com.sonidovivo.boletas.dto.PedidoResponse;
import com.sonidovivo.boletas.model.EstadoPedido;
import com.sonidovivo.boletas.model.Pedido;
import com.sonidovivo.boletas.model.PedidoItem;
import com.sonidovivo.boletas.repository.PedidoRepository;
import com.sonidovivo.boletas.servicio.CatalogoClient;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final CatalogoClient catalogo;

    public PedidoController(PedidoRepository pedidos, CatalogoClient catalogo) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
    }

    // Emite la boleta: el total y los subtotales los calcula el servicio, no el navegador.
    @PostMapping
    public ResponseEntity<?> emitir(@Valid @RequestBody PedidoRequest datos) {
        String clave = (datos.claveIdempotencia() == null || datos.claveIdempotencia().isBlank())
                ? null
                : datos.claveIdempotencia().trim();

        // Si el navegador reintenta el mismo intento de pago, se devuelve la boleta que ya existe.
        if (clave != null) {
            Pedido yaEmitida = pedidos.findByClaveIdempotencia(clave).orElse(null);

            if (yaEmitida != null) {
                return ResponseEntity.ok(PedidoResponse.desde(yaEmitida));
            }
        }

        Pedido pedido = new Pedido();
        pedido.setClaveIdempotencia(clave);
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

        try {
            Pedido guardado = pedidos.save(pedido);
            return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.desde(guardado));
        } catch (DataIntegrityViolationException e) {
            // Dos peticiones a la vez con la misma clave: gana la primera, esta devuelve esa boleta.
            if (clave == null) {
                throw e;
            }

            return pedidos.findByClaveIdempotencia(clave)
                    .map(PedidoResponse::desde)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> e);
        }
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
            // Cancelar devuelve el stock al catalogo como una entrada nueva; no se reescribe la venta.
            if (EstadoPedido.CANCELADO.name().equals(nuevo)) {
                if (EstadoPedido.CANCELADO.name().equals(pedido.getEstado())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("motivo", "El pedido ya estaba cancelado."));
                }

                if (EstadoPedido.DESPACHADO.name().equals(pedido.getEstado())
                        || EstadoPedido.ENTREGADO.name().equals(pedido.getEstado())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("motivo", "El pedido ya salio: no se puede cancelar."));
                }

                try {
                    for (PedidoItem item : pedido.getItems()) {
                        catalogo.devolverStock(item.getProductoId(), item.getCantidad(), pedido.getId());
                    }
                } catch (CatalogoClient.CatalogoNoDisponible e) {
                    // Sin la devolucion no se cancela: es preferible repetir la operacion a dejarla a medias.
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(Map.of("motivo", e.getMessage()));
                }
            }

            pedido.setEstado(nuevo);
            return ResponseEntity.ok(PedidoResponse.desde(pedidos.save(pedido)));
        }).orElse(ResponseEntity.notFound().build());
    }
}
