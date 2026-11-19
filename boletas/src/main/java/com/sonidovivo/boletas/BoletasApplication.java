package com.sonidovivo.boletas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Titulo y version que muestra Swagger (sin esto aparece "OpenAPI definition" v0).
@OpenAPIDefinition(info = @Info(
        title = "Sonido Vivo - API de Boletas",
        version = "1.0",
        description = "Emisión de la boleta de venta, consulta y cambio de estado."))
@SpringBootApplication
public class BoletasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoletasApplication.class, args);
    }

}
