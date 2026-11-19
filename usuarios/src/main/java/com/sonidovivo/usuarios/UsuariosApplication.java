package com.sonidovivo.usuarios;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Titulo y version que muestra Swagger (sin esto aparece "OpenAPI definition" v0).
@OpenAPIDefinition(info = @Info(
        title = "Sonido Vivo - API de Usuarios",
        version = "1.0",
        description = "Registro, login con token JWT, roles y estado de las cuentas."))
@SpringBootApplication
public class UsuariosApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsuariosApplication.class, args);
	}

}
