package com.sonidovivo.catalogo.controller;

import com.sonidovivo.catalogo.dto.MovimientoResponse;
import com.sonidovivo.catalogo.repository.MovimientoStockRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Historial completo del kardex para el panel: todos los productos, con filtros.
@RestController
@RequestMapping("/api/movimientos")
public class MovimientosController {

    private final MovimientoStockRepository movimientos;

    public MovimientosController(MovimientoStockRepository movimientos) {
        this.movimientos = movimientos;
    }

    @GetMapping
    public List<MovimientoResponse> historial(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String texto) {

        return movimientos.buscarConFiltros(productoId, limpiar(tipo), limpiar(motivo), limpiar(texto))
                .stream()
                .map(MovimientoResponse::desde)
                .toList();
    }

    // Un parametro vacio o en blanco se trata como "sin filtro".
    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
