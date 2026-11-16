package com.sonidovivo.boletas.dto;

import com.sonidovivo.boletas.model.Pedido;
import com.sonidovivo.boletas.model.PedidoItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Boleta tal como la recibe el front.
public record PedidoResponse(
        Long id,
        Long usuarioId,
        String nombreCliente,
        String emailCliente,
        BigDecimal total,
        String estado,
        LocalDateTime fechaCreacion,
        List<ItemResponse> items
) {

    public record ItemResponse(
            Long productoId,
            String nombreProducto,
            BigDecimal precioUnitario,
            Integer cantidad,
            BigDecimal subtotal
    ) {
        public static ItemResponse desde(PedidoItem item) {
            return new ItemResponse(item.getProductoId(), item.getNombreProducto(),
                    item.getPrecioUnitario(), item.getCantidad(), item.getSubtotal());
        }
    }

    public static PedidoResponse desde(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuarioId(),
                pedido.getNombreCliente(),
                pedido.getEmailCliente(),
                pedido.getTotal(),
                pedido.getEstado(),
                pedido.getFechaCreacion(),
                pedido.getItems().stream().map(ItemResponse::desde).toList());
    }
}
