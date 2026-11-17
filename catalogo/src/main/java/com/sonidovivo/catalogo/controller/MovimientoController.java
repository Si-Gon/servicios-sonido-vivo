package com.sonidovivo.catalogo.controller;

import com.sonidovivo.catalogo.dto.MovimientoRequest;
import com.sonidovivo.catalogo.dto.MovimientoResponse;
import com.sonidovivo.catalogo.repository.MovimientoStockRepository;
import com.sonidovivo.catalogo.servicio.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Kardex del producto: la venta descuenta con una SALIDA y la cancelacion devuelve con una ENTRADA
// nueva, sin tocar el movimiento original.
@RestController
@RequestMapping("/api/productos/{productoId}/movimientos")
public class MovimientoController {

    private final StockService stock;
    private final MovimientoStockRepository movimientos;

    public MovimientoController(StockService stock, MovimientoStockRepository movimientos) {
        this.stock = stock;
        this.movimientos = movimientos;
    }

    @PostMapping
    public ResponseEntity<?> registrar(@PathVariable Long productoId, @Valid @RequestBody MovimientoRequest datos) {
        try {
            var resultado = stock.registrar(productoId, datos);

            // Si ya estaba aplicado (mismo motivo y referencia), no es un error: se informa igual.
            HttpStatus codigo = resultado.repetido() ? HttpStatus.OK : HttpStatus.CREATED;

            return ResponseEntity.status(codigo)
                    .body(Map.of(
                            "movimiento", MovimientoResponse.desde(resultado.movimiento()),
                            "repetido", resultado.repetido()));
        } catch (StockService.SinVentaQueDevolver e) {
            return ResponseEntity.ok(Map.of("omitido", true, "motivo", e.getMessage()));
        } catch (StockService.MovimientoInvalido e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("motivo", e.getMessage()));
        } catch (StockService.ProductoNoEncontrado e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public List<MovimientoResponse> historial(@PathVariable Long productoId) {
        return movimientos.findByProductoIdOrderByCreadoEnDescIdDesc(productoId)
                .stream()
                .map(MovimientoResponse::desde)
                .toList();
    }
}
