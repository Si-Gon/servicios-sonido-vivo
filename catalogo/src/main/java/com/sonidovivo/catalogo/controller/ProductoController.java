package com.sonidovivo.catalogo.controller;

import com.sonidovivo.catalogo.dto.ProductoRequest;
import com.sonidovivo.catalogo.dto.ProductoResponse;
import com.sonidovivo.catalogo.model.Categoria;
import com.sonidovivo.catalogo.model.Producto;
import com.sonidovivo.catalogo.repository.CategoriaRepository;
import com.sonidovivo.catalogo.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Catalogo: consulta publica de productos y administracion desde el panel.
@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductoController {

    private static final int STOCK_CRITICO = 3;

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public ProductoController(ProductoRepository productos, CategoriaRepository categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    // Listado del catalogo. Admite los tres filtros que usa el front.
    @GetMapping
    public List<ProductoResponse> listar(
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false, defaultValue = "false") boolean criticos,
            @RequestParam(required = false, defaultValue = "3") int minimo) {

        List<Producto> encontrados;

        if (criticos) {
            encontrados = productos.findByStockLessThanEqualAndActivoTrueOrderByStockAsc(minimo);
        } else if (texto != null && !texto.isBlank() && categoria != null) {
            encontrados = productos.buscarPorTextoEnCategoria(categoria, texto.trim());
        } else if (texto != null && !texto.isBlank()) {
            encontrados = productos.buscarPorTexto(texto.trim());
        } else if (categoria != null) {
            encontrados = productos.findByCategoriaIdAndActivoTrueOrderByNombreAsc(categoria);
        } else {
            encontrados = productos.findByActivoTrueOrderByNombreAsc();
        }

        return encontrados.stream().map(ProductoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> buscar(@PathVariable Long id) {
        return productos.findByIdAndActivoTrue(id)
                .map(ProductoResponse::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ProductoRequest datos) {
        if (productos.existsByCodigo(datos.codigo().trim().toUpperCase())) {
            return ResponseEntity.badRequest().body(Map.of("motivo", "Ya existe un producto con ese codigo."));
        }

        Categoria categoria = categorias.findById(datos.categoriaId()).orElse(null);
        if (categoria == null) {
            return ResponseEntity.badRequest().body(Map.of("motivo", "La categoria no existe."));
        }

        Producto producto = new Producto();
        aplicar(producto, datos, categoria);

        Producto guardado = productos.save(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductoResponse.desde(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
        return productos.findById(id).map(producto -> {
            if (productos.existsByCodigoAndIdNot(datos.codigo().trim().toUpperCase(), id)) {
                return ResponseEntity.badRequest().body(Map.of("motivo", "Ya existe otro producto con ese codigo."));
            }

            Categoria categoria = categorias.findById(datos.categoriaId()).orElse(null);
            if (categoria == null) {
                return ResponseEntity.badRequest().body(Map.of("motivo", "La categoria no existe."));
            }

            aplicar(producto, datos, categoria);
            return ResponseEntity.ok(ProductoResponse.desde(productos.save(producto)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // El panel baja el stock al vender: es un cambio puntual, no una edicion completa.
    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> actualizarStock(@PathVariable Long id, @RequestBody Map<String, Integer> cuerpo) {
        Integer nuevo = cuerpo.get("stock");

        if (nuevo == null || nuevo < 0) {
            return ResponseEntity.badRequest().body(Map.of("motivo", "El stock debe ser un numero mayor o igual a cero."));
        }

        return productos.findById(id).map(producto -> {
            producto.setStock(nuevo);
            return ResponseEntity.ok(ProductoResponse.desde(productos.save(producto)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Baja logica: el producto deja de mostrarse pero la boleta vieja lo sigue nombrando.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        return productos.findById(id).map(producto -> {
            producto.setActivo(false);
            productos.save(producto);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private void aplicar(Producto producto, ProductoRequest datos, Categoria categoria) {
        producto.setCodigo(datos.codigo().trim().toUpperCase());
        producto.setCategoria(categoria);
        producto.setNombre(datos.nombre().trim());
        producto.setMarca(datos.marca().trim());
        producto.setModelo(datos.modelo().trim());
        producto.setPrecio(datos.precio());
        producto.setDescuento(datos.descuento());
        producto.setStock(datos.stock());
        producto.setDescripcion(datos.descripcion());
        producto.setImagen(datos.imagen());
        producto.setActivo(datos.activo() == null || datos.activo());
    }
}
