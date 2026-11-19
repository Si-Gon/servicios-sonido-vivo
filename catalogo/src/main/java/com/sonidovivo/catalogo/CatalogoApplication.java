package com.sonidovivo.catalogo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Titulo y version que muestra Swagger (sin esto aparece "OpenAPI definition" v0).
@OpenAPIDefinition(info = @Info(
        title = "Sonido Vivo - API de Catálogo",
        version = "1.0",
        description = "Productos, categorías y kardex de stock de la tienda."))
@SpringBootApplication
public class CatalogoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogoApplication.class, args);
	}

}
