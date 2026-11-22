package com.sonidovivo.gateway.seguridad;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Control de acceso del borde.
 *
 * El gateway es el unico punto por el que entra el front, asi que aca se valida el token JWT y se
 * exige el rol antes de derivar la peticion al servicio. Los servicios de negocio no tienen que
 * repetir esta validacion: si alguien los llama directo, no pasa por el borde.
 *
 * Tres niveles: publico (catalogo, registro y login), autenticado (comprar, ver un pedido, perfil)
 * y solo administrador (gestion de productos, usuarios, pedidos, mensajes y movimientos).
 */
@Component
public class FiltroDeAutorizacion implements GlobalFilter, Ordered {

    private final ValidadorDeToken validador;

    public FiltroDeAutorizacion(ValidadorDeToken validador) {
        this.validador = validador;
    }

    // Rutas que no piden token: catalogo publico, registro, login y formulario de contacto.
    private static final List<String> PUBLICAS_POST = List.of(
            "/api/usuarios", "/api/usuarios/login", "/api/contacto");

    private static final List<String> PUBLICAS_GET = List.of(
            "/api/productos", "/api/categorias");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest peticion = exchange.getRequest();
        String ruta = peticion.getURI().getPath();
        String metodo = peticion.getMethod().name();

        // El preflight de CORS lo responde la configuracion del gateway, no hace falta token.
        if ("OPTIONS".equals(metodo) || esPublica(metodo, ruta)) {
            return chain.filter(exchange);
        }

        var datos = validador.validar(peticion.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        if (datos.isEmpty()) {
            return rechazar(exchange, HttpStatus.UNAUTHORIZED, "Falta el token o no es valido.");
        }

        if (esSoloAdministrador(metodo, ruta) && !"Administrador".equalsIgnoreCase(datos.get().rol())) {
            return rechazar(exchange, HttpStatus.FORBIDDEN, "Esta operacion requiere rol de administrador.");
        }

        // Se propaga quien hizo la peticion, para que el servicio lo pueda usar o registrar.
        ServerHttpRequest conIdentidad = peticion.mutate()
                .header("X-Usuario-Id", datos.get().id())
                .header("X-Usuario-Rol", datos.get().rol())
                .build();

        return chain.filter(exchange.mutate().request(conIdentidad).build());
    }

    private boolean esPublica(String metodo, String ruta) {
        if ("GET".equals(metodo)) {
            // El kardex (historial de stock) es del panel, no del sitio publico, aunque cuelgue
            // de la ruta de un producto.
            if (ruta.startsWith("/api/movimientos") || ruta.contains("/movimientos")) {
                return false;
            }

            return PUBLICAS_GET.stream().anyMatch(ruta::startsWith);
        }

        if ("POST".equals(metodo)) {
            return PUBLICAS_POST.stream().anyMatch(ruta::startsWith);
        }

        return false;
    }

    private boolean esSoloAdministrador(String metodo, String ruta) {
        // Usuarios: el listado y los cambios de rol o estado son del panel; ver una cuenta es perfil.
        if (ruta.startsWith("/api/usuarios")) {
            if ("GET".equals(metodo)) {
                return ruta.equals("/api/usuarios") || ruta.equals("/api/usuarios/");
            }

            return !"/api/usuarios/login".equals(ruta) && !"POST".equals(metodo);
        }

        // Catalogo: leer es publico, escribir y ver el historial es del panel.
        if (ruta.startsWith("/api/productos") || ruta.startsWith("/api/categorias") || ruta.startsWith("/api/movimientos")) {
            // Excepcion: al comprar, el cliente registra la SALIDA de stock de su venta. Basta con
            // tener sesion; el catalogo revisa que un cliente solo pueda registrar ventas.
            if ("POST".equals(metodo) && ruta.matches("/api/productos/\\d+/movimientos/?")) {
                return false;
            }

            if (!"GET".equals(metodo)) {
                return true;
            }

            // El kardex (historial de stock) es informacion del panel, aunque cuelgue de un producto.
            return ruta.startsWith("/api/movimientos") || ruta.contains("/movimientos");
        }

        // Mensajeria: el POST es publico, la bandeja es del panel.
        if (ruta.startsWith("/api/contacto")) {
            return !"POST".equals(metodo);
        }

        // Pedidos: el cliente puede emitir y ver el suyo; listar y cambiar estado es del panel.
        if (ruta.startsWith("/api/pedidos")) {
            if ("GET".equals(metodo)) {
                return ruta.equals("/api/pedidos") || ruta.equals("/api/pedidos/");
            }

            return "PATCH".equals(metodo) || "DELETE".equals(metodo);
        }

        // Despachos: el cliente registra la entrega y ve la de su pedido para el comprobante;
        // el listado completo y la correccion de datos son del panel.
        if (ruta.startsWith("/api/despachos")) {
            if ("GET".equals(metodo)) {
                return ruta.equals("/api/despachos") || ruta.equals("/api/despachos/");
            }

            return "PUT".equals(metodo) || "DELETE".equals(metodo);
        }

        return false;
    }

    private Mono<Void> rechazar(ServerWebExchange exchange, HttpStatus estado, String motivo) {
        var respuesta = exchange.getResponse();
        respuesta.setStatusCode(estado);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] cuerpo = ("{\"motivo\":\"" + motivo + "\"}").getBytes(StandardCharsets.UTF_8);
        var buffer = respuesta.bufferFactory().wrap(cuerpo);

        return respuesta.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Antes de las rutas, para no derivar una peticion que no esta autorizada.
        return -100;
    }
}