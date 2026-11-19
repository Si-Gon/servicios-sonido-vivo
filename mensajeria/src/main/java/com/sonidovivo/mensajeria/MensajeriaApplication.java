package com.sonidovivo.mensajeria;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Titulo y version que muestra Swagger (sin esto aparece "OpenAPI definition" v0).
@OpenAPIDefinition(info = @Info(
        title = "Sonido Vivo - API de Mensajería",
        version = "1.0",
        description = "Formulario de contacto y bandeja de mensajes del panel."))
@SpringBootApplication
public class MensajeriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MensajeriaApplication.class, args);
	}

}
