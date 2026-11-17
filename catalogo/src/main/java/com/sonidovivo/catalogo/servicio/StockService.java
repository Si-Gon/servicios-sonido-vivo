package com.sonidovivo.catalogo.servicio;

import com.sonidovivo.catalogo.dto.MovimientoRequest;
import com.sonidovivo.catalogo.model.MovimientoStock;
import com.sonidovivo.catalogo.model.Producto;
import com.sonidovivo.catalogo.model.TipoMovimiento;
import com.sonidovivo.catalogo.repository.MovimientoStockRepository;
import com.sonidovivo.catalogo.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Todo cambio de stock pasa por aca: se actualiza el producto y queda la linea en el kardex,
// en la misma transaccion. Si algo falla, no queda ni el stock ni el movimiento a medias.
@Service
public class StockService {

    private final ProductoRepository productos;
    private final MovimientoStockRepository movimientos;

    public StockService(ProductoRepository productos, MovimientoStockRepository movimientos) {
        this.productos = productos;
        this.movimientos = movimientos;
    }

    /** Resultado de registrar: el movimiento y si ya estaba aplicado antes. */
    public record ResultadoMovimiento(MovimientoStock movimiento, boolean repetido) {
    }

    /**
     * Registra un movimiento sobre el stock actual (venta o cancelacion).
     *
     * Si trae referencia y ese movimiento ya existe (misma venta, misma cancelacion), no se vuelve
     * a aplicar: se devuelve el que ya estaba. Asi un reintento de red no descuenta dos veces.
     */
    @Transactional
    public ResultadoMovimiento registrar(Long productoId, MovimientoRequest datos) {
        String tipo = datos.tipo().trim().toUpperCase();

        if (!TipoMovimiento.ENTRADA.name().equals(tipo) && !TipoMovimiento.SALIDA.name().equals(tipo)) {
            throw new MovimientoInvalido("El tipo debe ser ENTRADA o SALIDA.");
        }

        String motivo = datos.motivo().trim().toUpperCase();
        String referencia = (datos.referencia() == null || datos.referencia().isBlank())
                ? null
                : datos.referencia().trim().toUpperCase();

        if (referencia != null) {
            Optional<MovimientoStock> yaAplicado =
                    movimientos.findByProductoIdAndMotivoAndReferencia(productoId, motivo, referencia);

            if (yaAplicado.isPresent()) {
                return new ResultadoMovimiento(yaAplicado.get(), true);
            }
        }

        // Una cancelacion solo devuelve lo que esa misma referencia vendio: si la venta nunca se
        // aplico (por ejemplo, el pago fallo a medias), no se inventa stock.
        if (MovimientoStock.CANCELACION.equals(motivo) && referencia != null
                && movimientos.findByProductoIdAndMotivoAndReferencia(productoId, MovimientoStock.VENTA, referencia).isEmpty()) {
            throw new SinVentaQueDevolver(productoId, referencia);
        }

        Producto producto = productos.findByIdForUpdate(productoId)
                .orElseThrow(() -> new ProductoNoEncontrado("El producto " + productoId + " no existe."));

        int cantidad = datos.cantidad();
        int stockAnterior = producto.getStock();
        int nuevoStock;

        if (TipoMovimiento.ENTRADA.name().equals(tipo)) {
            nuevoStock = stockAnterior + cantidad;
        } else {
            if (stockAnterior < cantidad) {
                throw new MovimientoInvalido("Stock insuficiente para \"" + producto.getNombre()
                        + "\": quedan " + stockAnterior + " y se piden " + cantidad + ".");
            }

            nuevoStock = stockAnterior - cantidad;
        }

        producto.setStock(nuevoStock);
        productos.save(producto);

        return new ResultadoMovimiento(
                guardarMovimiento(productoId, tipo, cantidad, motivo, referencia, nuevoStock), false);
    }

    /**
     * Deja el stock en un valor exacto (edicion manual del panel) y registra la diferencia.
     * Si el valor es el mismo, no escribe nada.
     */
    @Transactional
    public int fijarStock(Long productoId, int nuevoStock) {
        if (nuevoStock < 0) {
            throw new MovimientoInvalido("El stock debe ser un numero mayor o igual a cero.");
        }

        Producto producto = productos.findByIdForUpdate(productoId)
                .orElseThrow(() -> new ProductoNoEncontrado("El producto " + productoId + " no existe."));

        int diferencia = nuevoStock - producto.getStock();

        if (diferencia == 0) {
            return nuevoStock;
        }

        if (diferencia > 0) {
            darDeAlta(producto, diferencia, MovimientoStock.AJUSTE, null, nuevoStock);
        } else {
            darDeBaja(producto, -diferencia, MovimientoStock.AJUSTE, null, nuevoStock);
        }

        return nuevoStock;
    }

    // Primera carga de stock al crear un producto: queda como entrada inicial en el kardex.
    @Transactional
    public void registrarStockInicial(Long productoId, int cantidad) {
        if (cantidad <= 0) {
            return;
        }

        Producto producto = productos.findByIdForUpdate(productoId)
                .orElseThrow(() -> new ProductoNoEncontrado("El producto " + productoId + " no existe."));

        darDeAlta(producto, cantidad, MovimientoStock.INICIAL, null, cantidad);
    }

    private MovimientoStock darDeAlta(Producto producto, int cantidad, String motivo, String referencia, int nuevoStock) {
        producto.setStock(nuevoStock);
        productos.save(producto);

        return guardarMovimiento(producto.getId(), TipoMovimiento.ENTRADA.name(), cantidad, motivo, referencia, nuevoStock);
    }

    private MovimientoStock darDeBaja(Producto producto, int cantidad, String motivo, String referencia, int nuevoStock) {
        producto.setStock(nuevoStock);
        productos.save(producto);

        return guardarMovimiento(producto.getId(), TipoMovimiento.SALIDA.name(), cantidad, motivo, referencia, nuevoStock);
    }

    private MovimientoStock guardarMovimiento(Long productoId, String tipo, int cantidad,
                                              String motivo, String referencia, int stockResultante) {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setProductoId(productoId);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setMotivo(motivo);
        movimiento.setReferencia(referencia);
        movimiento.setStockResultante(stockResultante);

        return movimientos.save(movimiento);
    }

    public static class MovimientoInvalido extends RuntimeException {
        public MovimientoInvalido(String mensaje) {
            super(mensaje);
        }
    }

    // La venta no existe para esa referencia: la devolucion se omite, no es un error.
    public static class SinVentaQueDevolver extends RuntimeException {
        private final Long productoId;
        private final String referencia;

        public SinVentaQueDevolver(Long productoId, String referencia) {
            super("No hay una venta registrada con la referencia " + referencia
                    + " para el producto " + productoId + ": no hay nada que devolver.");
            this.productoId = productoId;
            this.referencia = referencia;
        }

        public Long getProductoId() {
            return productoId;
        }

        public String getReferencia() {
            return referencia;
        }
    }

    public static class ProductoNoEncontrado extends RuntimeException {
        public ProductoNoEncontrado(String mensaje) {
            super(mensaje);
        }
    }
}
