package com.sonidovivo.boletas.controller;

import com.sonidovivo.boletas.dto.PedidoRequest;
import com.sonidovivo.boletas.excepcion.ErrorDeNegocio;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
            // El nombre y el precio salen del catalogo, no del navegador: la boleta no puede
            // depender de lo que diga quien compra.
            var delCatalogo = catalogo.datosDelProducto(linea.productoId());

            PedidoItem item = new PedidoItem();
            item.setProductoId(linea.productoId());
            item.setNombreProducto(delCatalogo.nombre());
            item.setPrecioUnitario(delCatalogo.precioVigente());
            item.setCantidad(linea.cantidad());

            BigDecimal subtotal = delCatalogo.precioVigente().multiply(BigDecimal.valueOf(linea.cantidad()));
            item.setSubtotal(subtotal);
            total = total.add(subtotal);

            pedido.agregarItem(item);
        }

        pedido.setTotal(total);

        Pedido guardado;

        try {
            guardado = pedidos.save(pedido);
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

        // La venta y su efecto sobre el stock son una sola operacion, y la ejecuta el servicio:
        // el navegador no descuenta nada. Si una linea no alcanza, se anula la boleta completa.
        try {
            for (PedidoItem item : guardado.getItems()) {
                catalogo.descontarStock(item.getProductoId(), item.getCantidad(), guardado.getId());
            }
        } catch (RuntimeException e) {
            anular(guardado);

            if (e instanceof CatalogoClient.StockInsuficiente sinStock) {
                throw ErrorDeNegocio.sinStock(sinStock.getMessage());
            }

            throw ErrorDeNegocio.noDisponible("No se pudo descontar el stock de la boleta "
                    + guardado.getId() + ": " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.desde(guardado));
    }

    // Compensacion: la venta no se concreto, asi que se devuelve lo que alcanzo a descontarse y se
    // borra la boleta. No se deja una boleta cancelada: al cliente no se le vendio nada, y manana
    // no tiene por que aparecer en sus compras.
    private void anular(Pedido pedido) {
        for (PedidoItem item : pedido.getItems()) {
            try {
                catalogo.devolverStock(item.getProductoId(), item.getCantidad(), pedido.getId());
            } catch (RuntimeException ignorado) {
                // Si ni la devolucion responde, la boleta se borra igual: el kardex queda con la
                // salida y su referencia, y se puede corregir desde el panel.
            }
        }

        pedidos.delete(pedido);
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

    // "Mis compras" del sitio: el gateway pone en X-Usuario-Id el id que viene dentro del token,
    // asi que cada cliente ve solo sus pedidos y no puede pedir los de otra persona.
    @GetMapping("/mios")
    public List<PedidoResponse> mios(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        if (usuarioId == null) {
            throw ErrorDeNegocio.noAutorizado("Debes iniciar sesion para ver tus compras.");
        }

        return pedidos.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(PedidoResponse::desde)
                .toList();
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-Usuario-Rol", required = false) String rol) {

        Pedido pedido = pedidos.findById(id)
                .orElseThrow(() -> ErrorDeNegocio.noEncontrado("La boleta " + id + " no existe."));

        // El gateway entrega el id y el rol que vienen dentro del token. Sin cabeceras es una
        // llamada interna entre servicios, que no pasa por el borde.
        boolean esAdmin = "Administrador".equalsIgnoreCase(rol);
        boolean esDueno = usuarioId != null && usuarioId.equals(pedido.getUsuarioId());

        if (rol != null && !esAdmin && !esDueno) {
            throw ErrorDeNegocio.prohibido("La boleta " + id + " no es tuya.");
        }

        return PedidoResponse.desde(pedido);
    }

    // Cambia el estado (en preparacion, despachado, entregado, cancelado).
    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> cuerpo) {
        String nuevo = cuerpo.getOrDefault("estado", "").trim().toUpperCase();

        boolean valido = Arrays.stream(EstadoPedido.values()).anyMatch(estado -> estado.name().equals(nuevo));

        if (!valido) {
            throw ErrorDeNegocio.invalido("Estado inválido. Se acepta: " + Arrays.toString(EstadoPedido.values()));
        }

        Pedido pedido = pedidos.findById(id)
                .orElseThrow(() -> ErrorDeNegocio.noEncontrado("La boleta " + id + " no existe."));

        // Cancelar devuelve el stock al catalogo como una entrada nueva; no se reescribe la venta.
        if (EstadoPedido.CANCELADO.name().equals(nuevo)) {
            if (EstadoPedido.CANCELADO.name().equals(pedido.getEstado())) {
                throw ErrorDeNegocio.conflicto("La boleta " + id + " ya estaba cancelada.");
            }

            if (EstadoPedido.DESPACHADO.name().equals(pedido.getEstado())
                    || EstadoPedido.ENTREGADO.name().equals(pedido.getEstado())) {
                throw ErrorDeNegocio.conflicto("El pedido " + id + " ya salió: no se puede cancelar.");
            }

            try {
                for (PedidoItem item : pedido.getItems()) {
                    catalogo.devolverStock(item.getProductoId(), item.getCantidad(), pedido.getId());
                }
            } catch (CatalogoClient.CatalogoNoDisponible e) {
                // Sin la devolucion no se cancela: es preferible repetir la operacion a dejarla a medias.
                throw ErrorDeNegocio.noDisponible(e.getMessage());
            }
        }

        pedido.setEstado(nuevo);
        return PedidoResponse.desde(pedidos.save(pedido));
    }
}