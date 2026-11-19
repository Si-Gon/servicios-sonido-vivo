package com.sonidovivo.despachos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Titulo y version que muestra Swagger (sin esto aparece "OpenAPI definition" v0).
@OpenAPIDefinition(info = @Info(
        title = "Sonido Vivo - API de Despachos",
        version = "1.0",
        description = "Datos de entrega de cada pedido: domicilio o retiro en tienda."))
@SpringBootApplication
public class DespachosApplication {

    public static void main(String[] args) {
        SpringApplication.run(DespachosApplication.class, args);
    }

}
