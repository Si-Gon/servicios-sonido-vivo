package com.sonidovivo.catalogo.controller;

import com.sonidovivo.catalogo.dto.CategoriaRequest;
import com.sonidovivo.catalogo.model.Categoria;
import com.sonidovivo.catalogo.repository.CategoriaRepository;
import com.sonidovivo.catalogo.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Categorias: las usa el filtro del catalogo y el panel de administracion.
// El CORS lo maneja el gateway: si el servicio tambien lo agrega, la respuesta
// viaja con dos Access-Control-Allow-Origin y el navegador la rechaza.
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public CategoriaController(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    // Cada categoria viaja con cuantos productos activos tiene: lo muestra el panel.
    @GetMapping
    public List<Map<String, Object>> listar() {
        return categorias.findAllByOrderByNombreAsc().stream()
                .map(categoria -> Map.<String, Object>of(
                        "id", categoria.getId(),
                        "nombre", categoria.getNombre(),
                        "productos", productos.countByCategoriaId(categoria.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> buscar(@PathVariable Long id) {
        return categorias.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CategoriaRequest datos) {
        String nombre = datos.nombre().trim();

        if (categorias.existsByNombreIgnoreCase(nombre)) {
            return ResponseEntity.badRequest().body(Map.of("motivo", "Esa categoria ya existe."));
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return ResponseEntity.status(HttpStatus.CREATED).body(categorias.save(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> renombrar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest datos) {
        String nombre = datos.nombre().trim();

        return categorias.findById(id).map(categoria -> {
            if (categorias.existsByNombreIgnoreCase(nombre)
                    && !categoria.getNombre().equalsIgnoreCase(nombre)) {
                return ResponseEntity.badRequest().body(Map.of("motivo", "Esa categoria ya existe."));
            }

            categoria.setNombre(nombre);
            return ResponseEntity.ok(categorias.save(categoria));
        }).orElse(ResponseEntity.notFound().build());
    }

    // No se borra una categoria con productos: dejaria productos sin clasificar.
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        return categorias.findById(id).map(categoria -> {
            long cuantos = productos.countByCategoriaId(id);

            if (cuantos > 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("motivo", "La categoria tiene " + cuantos + " productos: no se puede borrar."));
            }

            categorias.delete(categoria);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
