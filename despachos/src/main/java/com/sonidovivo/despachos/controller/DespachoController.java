package com.sonidovivo.despachos.controller;

import com.sonidovivo.despachos.dto.DespachoRequest;
import com.sonidovivo.despachos.dto.DespachoResponse;
import com.sonidovivo.despachos.excepcion.ErrorDeNegocio;
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
        String tipo = tipoValido(datos.tipoEntrega());

        if (despachos.existsByPedidoId(datos.pedidoId())) {
            throw ErrorDeNegocio.conflicto("El pedido " + datos.pedidoId() + " ya tiene un despacho registrado.");
        }

        exigirDatosDeEntrega(tipo, datos);

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
    public DespachoResponse buscarPorPedido(@PathVariable Long pedidoId) {
        return despachos.findByPedidoId(pedidoId)
                .map(DespachoResponse::desde)
                .orElseThrow(() -> ErrorDeNegocio.noEncontrado("El pedido " + pedidoId + " no tiene despacho registrado."));
    }

    @GetMapping("/{id}")
    public DespachoResponse buscar(@PathVariable Long id) {
        return despachos.findById(id)
                .map(DespachoResponse::desde)
                .orElseThrow(() -> ErrorDeNegocio.noEncontrado("El despacho " + id + " no existe."));
    }

    // Corregir dirección o teléfono antes de despachar.
    @PutMapping("/{id}")
    public ResponseEntity<?> corregir(@PathVariable Long id, @Valid @RequestBody DespachoRequest datos) {
        String tipo = tipoValido(datos.tipoEntrega());
        exigirDatosDeEntrega(tipo, datos);

        Despacho despacho = despachos.findById(id)
                .orElseThrow(() -> ErrorDeNegocio.noEncontrado("El despacho " + id + " no existe."));

        aplicar(despacho, tipo, datos);
        return ResponseEntity.ok(DespachoResponse.desde(despachos.save(despacho)));
    }

    // El tipo de entrega solo puede ser uno de los dos que entiende la tabla.
    private String tipoValido(String tipo) {
        String limpio = tipo.trim().toUpperCase();

        if (!Despacho.DOMICILIO.equals(limpio) && !Despacho.RETIRO.equals(limpio)) {
            throw ErrorDeNegocio.invalido("El tipo de entrega debe ser DOMICILIO o RETIRO.");
        }

        return limpio;
    }

    // Un domicilio necesita region, comuna y direccion; un retiro no.
    private void exigirDatosDeEntrega(String tipo, DespachoRequest datos) {
        if (Despacho.DOMICILIO.equals(tipo)
                && (vacio(datos.region()) || vacio(datos.comuna()) || vacio(datos.direccion()))) {
            throw ErrorDeNegocio.invalido("Para entrega a domicilio se necesitan región, comuna y dirección.");
        }
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
