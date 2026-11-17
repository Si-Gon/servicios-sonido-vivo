package com.sonidovivo.boletas.servicio;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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
 * La devolucion entra al catalogo como una ENTRADA nueva del kardex, sin editar el movimiento
 * original de la venta.
 */
@Service
public class CatalogoClient {

    private final DiscoveryClient descubridor;

    public CatalogoClient(DiscoveryClient descubridor) {
        this.descubridor = descubridor;
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

    // Primera instancia registrada de catalogo, con su host y puerto reales.
    private String direccionDelCatalogo() {
        List<ServiceInstance> instancias = descubridor.getInstances("catalogo");

        if (instancias.isEmpty()) {
            throw new CatalogoNoDisponible("El catalogo no esta registrado en Eureka.");
        }

        return instancias.get(0).getUri().toString();
    }

    public static class CatalogoNoDisponible extends RuntimeException {
        public CatalogoNoDisponible(String mensaje) {
            super(mensaje);
        }
    }
}
