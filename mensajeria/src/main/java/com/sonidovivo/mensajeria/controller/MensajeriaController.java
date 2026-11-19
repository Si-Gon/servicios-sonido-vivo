package com.sonidovivo.mensajeria.controller;

import com.sonidovivo.mensajeria.dto.ContactoRequest;
import com.sonidovivo.mensajeria.excepcion.ErrorDeNegocio;
import com.sonidovivo.mensajeria.model.Mensajeria;
import com.sonidovivo.mensajeria.repository.MensajeriaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Endpoints del formulario de contacto. El front llama a estos desde la pagina Contacto.
@RestController
@RequestMapping("/api/contacto")
public class MensajeriaController {

    // Estados que admite la bandeja.
    private static final List<String> ESTADOS = List.of("PENDIENTE", "RESPONDIDO");

    private final MensajeriaRepository repositorio;

    public MensajeriaController(MensajeriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    // Registrar un mensaje nuevo: lo que hace el formulario al enviar.
    @PostMapping
    public ResponseEntity<Mensajeria> crear(@Valid @RequestBody ContactoRequest datos) {
        Mensajeria mensaje = new Mensajeria();
        mensaje.setNombreCompleto(datos.nombreCompleto().trim());
        mensaje.setCorreo(datos.correo().trim().toLowerCase());
        mensaje.setRegion(datos.region());
        mensaje.setComuna(datos.comuna());
        mensaje.setMensaje(datos.mensaje().trim());
        mensaje.setEstado("PENDIENTE");

        Mensajeria guardado = repositorio.save(mensaje);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // Bandeja del administrador, del mas nuevo al mas antiguo.
    @GetMapping
    public List<Mensajeria> listar(@RequestParam(required = false) String estado) {
        if (estado == null || estado.isBlank()) {
            return repositorio.findAll();
        }
        return repositorio.findByEstadoOrderByFechaCreacionDesc(estado.toUpperCase());
    }

    // Un mensaje puntual, para la vista de detalle.
    @GetMapping("/{id}")
    public Mensajeria buscar(@PathVariable Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorDeNegocio.noEncontrado("El mensaje " + id + " no existe."));
    }

    // Marcar como respondido (o devolverlo a pendiente) desde la bandeja.
    @PatchMapping("/{id}/estado")
    public Mensajeria cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> cuerpo) {
        String estado = cuerpo.getOrDefault("estado", "RESPONDIDO").trim().toUpperCase();

        if (!ESTADOS.contains(estado)) {
            throw ErrorDeNegocio.invalido("El estado debe ser PENDIENTE o RESPONDIDO.");
        }

        Mensajeria mensaje = repositorio.findById(id)
                .orElseThrow(() -> ErrorDeNegocio.noEncontrado("El mensaje " + id + " no existe."));

        mensaje.setEstado(estado);
        return repositorio.save(mensaje);
    }

    // Borrar un mensaje (solo el administrador).
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!repositorio.existsById(id)) {
            throw ErrorDeNegocio.noEncontrado("El mensaje " + id + " no existe.");
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
