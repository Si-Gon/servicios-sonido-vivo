package com.sonidovivo.despachos.controller;

import com.sonidovivo.despachos.dto.DespachoRequest;
import com.sonidovivo.despachos.dto.DespachoResponse;
import com.sonidovivo.despachos.model.Despacho;
import com.sonidovivo.despachos.repository.DespachoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Despachos: los datos de entrega de un pedido. El estado del pedido lo maneja el servicio de
// boletas; aca solo se registra y corrige la entrega. El CORS vive en el gateway.
@RestController
@RequestMapping("/api/despachos")
public class DespachoController {

    private final DespachoRepository despachos;

    public DespachoController(DespachoRepository despachos) {
        this.despachos = despachos;
    }

    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody DespachoRequest datos) {
        String tipo = datos.tipoEntrega().trim().toUpperCase();

        if (!Despacho.DOMICILIO.equals(tipo) && !Despacho.RETIRO.equals(tipo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("motivo", "El tipo de entrega debe ser DOMICILIO o RETIRO."));
        }

        if (despachos.existsByPedidoId(datos.pedidoId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("motivo", "Ese pedido ya tiene un despacho registrado."));
        }

        String falta = validarDatos(tipo, datos);
        if (falta != null) {
            return ResponseEntity.badRequest().body(Map.of("motivo", falta));
        }

        Despacho despacho = new Despacho();
        aplicar(despacho, tipo, datos);

        Despacho guardado = despachos.save(despacho);
        return ResponseEntity.status(HttpStatus.CREATED).body(DespachoResponse.desde(guardado));
    }

    @GetMapping
    public List<DespachoResponse> listar(@RequestParam(required = false) String tipoEntrega) {
        List<Despacho> encontrados = (tipoEntrega == null || tipoEntrega.isBlank())
                ? despachos.findAllByOrderByCreadoEnDesc()
                : despachos.findByTipoEntregaOrderByCreadoEnDesc(tipoEntrega.toUpperCase());

        return encontrados.stream().map(DespachoResponse::desde).toList();
    }

    // El despacho de un pedido puntual: lo usa la pantalla de confirmación.
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<DespachoResponse> buscarPorPedido(@PathVariable Long pedidoId) {
        return despachos.findByPedidoId(pedidoId)
                .map(DespachoResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DespachoResponse> buscar(@PathVariable Long id) {
        return despachos.findById(id)
                .map(DespachoResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Corregir dirección o teléfono antes de despachar.
    @PutMapping("/{id}")
    public ResponseEntity<?> corregir(@PathVariable Long id, @Valid @RequestBody DespachoRequest datos) {
        String tipo = datos.tipoEntrega().trim().toUpperCase();

        if (!Despacho.DOMICILIO.equals(tipo) && !Despacho.RETIRO.equals(tipo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("motivo", "El tipo de entrega debe ser DOMICILIO o RETIRO."));
        }

        String falta = validarDatos(tipo, datos);
        if (falta != null) {
            return ResponseEntity.badRequest().body(Map.of("motivo", falta));
        }

        return despachos.findById(id).map(despacho -> {
            aplicar(despacho, tipo, datos);
            return ResponseEntity.ok(DespachoResponse.desde(despachos.save(despacho)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Un domicilio necesita region, comuna y direccion; un retiro no.
    private String validarDatos(String tipo, DespachoRequest datos) {
        if (Despacho.DOMICILIO.equals(tipo)) {
            if (vacio(datos.region()) || vacio(datos.comuna()) || vacio(datos.direccion())) {
                return "Para entrega a domicilio se necesitan región, comuna y dirección.";
            }
        }

        return null;
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private void aplicar(Despacho despacho, String tipo, DespachoRequest datos) {
        despacho.setPedidoId(datos.pedidoId());
        despacho.setTipoEntrega(tipo);
        despacho.setNombre(datos.nombre().trim());
        despacho.setTelefono(datos.telefono().trim());
        despacho.setReferencia(datos.referencia());

        // El retiro no tiene costo de envio ni direccion.
        if (Despacho.RETIRO.equals(tipo)) {
            despacho.setRegion(null);
            despacho.setComuna(null);
            despacho.setDireccion(null);
            despacho.setCostoEnvio(BigDecimal.ZERO);
        } else {
            despacho.setRegion(datos.region().trim());
            despacho.setComuna(datos.comuna().trim());
            despacho.setDireccion(datos.direccion().trim());
            despacho.setCostoEnvio(datos.costoEnvio() == null ? BigDecimal.ZERO : datos.costoEnvio());
        }
    }
}
