package com.sonidovivo.boletas.servicio;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Llamada al servicio de catalogo.
 *
 * Se usa RestClient, no Feign: es una llamada HTTP puntual, sin DTOs compartidos ni interfaces
 * generadas. La direccion no esta escrita en ningun archivo: se le pregunta a Eureka cual es la
 * instancia viva de "catalogo". Se evita @LoadBalanced a proposito, porque ese bean intercepta
 * tambien al cliente de Eureka y la instancia deja de registrarse.
 *
 * El descuento de la venta lo hace este servicio, no el navegador: la boleta y su efecto sobre el
 * stock son la misma operacion, asi que no puede quedar a cargo de quien compra. La devolucion
 * entra como una ENTRADA nueva del kardex, sin editar el movimiento original de la venta.
 */
@Service
public class CatalogoClient {

    private final DiscoveryClient descubridor;

    public CatalogoClient(DiscoveryClient descubridor) {
        this.descubridor = descubridor;
    }

    /**
     * Nombre y precio vigente del producto, leidos del catalogo.
     * El navegador manda que producto y cuanta cantidad, nunca a que precio.
     */
    public DatosDelProducto datosDelProducto(Long productoId) {
        RestClient catalogo = RestClient.create(direccionDelCatalogo());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> producto = catalogo.get()
                    .uri("/api/productos/{id}", productoId)
                    .retrieve()
                    .body(Map.class);

            if (producto == null) {
                throw new StockInsuficiente("El producto " + productoId + " no tiene datos en el catalogo.");
            }

            BigDecimal precio = new BigDecimal(String.valueOf(producto.get("precio")));
            Object descuento = producto.get("descuento");
            int porcentaje = descuento instanceof Number numero ? numero.intValue() : 0;

            return new DatosDelProducto(String.valueOf(producto.get("nombre")), precioVigente(precio, porcentaje));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new StockInsuficiente("El producto " + productoId + " no existe o no esta publicado.");
            }

            throw new CatalogoNoDisponible("El catalogo no devolvio el producto " + productoId
                    + " (" + e.getStatusCode().value() + ").");
        } catch (RuntimeException e) {
            if (e instanceof StockInsuficiente || e instanceof CatalogoNoDisponible) {
                throw e;
            }

            throw new CatalogoNoDisponible("No se pudo hablar con el servicio de catalogo: " + e.getMessage());
        }
    }

    // Misma regla que el sitio: se aplica el descuento y se redondea a la decena.
    private BigDecimal precioVigente(BigDecimal precio, int descuento) {
        if (descuento <= 0 || descuento >= 100) {
            return precio;
        }

        BigDecimal conDescuento = precio.multiply(BigDecimal.valueOf(100 - descuento))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return conDescuento.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).multiply(BigDecimal.TEN);
    }

    /** Lo que la boleta necesita saber de un producto. */
    public record DatosDelProducto(String nombre, BigDecimal precioVigente) {
    }

    /** Registra la salida de la venta. La referencia (el pedido) evita que se aplique dos veces. */
    public void descontarStock(Long productoId, int cantidad, Long pedidoId) {
        RestClient catalogo = RestClient.create(direccionDelCatalogo());

        try {
            catalogo.post()
                    .uri("/api/productos/{id}/movimientos", productoId)
                    .body(Map.of(
                            "tipo", "SALIDA",
                            "cantidad", cantidad,
                            "motivo", "VENTA",
                            "referencia", "PEDIDO-" + pedidoId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // 422 es stock insuficiente: es un problema del pedido, no del catalogo.
            if (e.getStatusCode().value() == 422) {
                throw new StockInsuficiente(motivo(e.getResponseBodyAsString()));
            }

            throw new CatalogoNoDisponible("El catalogo rechazo el descuento del producto "
                    + productoId + " (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            throw new CatalogoNoDisponible("No se pudo hablar con el servicio de catalogo: " + e.getMessage());
        }
    }

    /** Registra la entrada por cancelacion. La referencia evita que se aplique dos veces. */
    public void devolverStock(Long productoId, int cantidad, Long pedidoId) {
        RestClient catalogo = RestClient.create(direccionDelCatalogo());

        try {
            catalogo.post()
                    .uri("/api/productos/{id}/movimientos", productoId)
                    .body(Map.of(
                            "tipo", "ENTRADA",
                            "cantidad", cantidad,
                            "motivo", "CANCELACION",
                            "referencia", "PEDIDO-" + pedidoId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new CatalogoNoDisponible("El catalogo rechazo la devolucion del producto "
                    + productoId + " (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            throw new CatalogoNoDisponible("No se pudo hablar con el servicio de catalogo: " + e.getMessage());
        }
    }

    // El catalogo explica el rechazo en el campo "motivo"; si no se puede leer, se usa el cuerpo entero.
    private String motivo(String cuerpo) {
        try {
            String motivo = new ObjectMapper().readTree(cuerpo).path("motivo").asText();
            return motivo.isBlank() ? cuerpo : motivo;
        } catch (Exception e) {
            return cuerpo;
        }
    }

    // Primera instancia registrada de catalogo, con su host y puerto reales.
    private String direccionDelCatalogo() {
        List<ServiceInstance> instancias = descubridor.getInstances("catalogo");

        if (instancias.isEmpty()) {
            throw new CatalogoNoDisponible("El catalogo no esta registrado en Eureka.");
        }

        return instancias.get(0).getUri().toString();
    }

    /** El catalogo no tenia stock suficiente para la venta. */
    public static class StockInsuficiente extends RuntimeException {
        public StockInsuficiente(String mensaje) {
            super(mensaje);
        }
    }

    public static class CatalogoNoDisponible extends RuntimeException {
        public CatalogoNoDisponible(String mensaje) {
            super(mensaje);
        }
    }
}
