package com.sonidovivo.gateway.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Verifica la firma y la vigencia del token que emitio el servicio de usuarios. */
@Component
public class ValidadorDeToken {

    private final SecretKey clave;

    public ValidadorDeToken(@Value("${seguridad.jwt.secreto}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    /** Devuelve los datos del usuario si el token es valido y no expiro. */
    public Optional<Datos> validar(String cabecera) {
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            return Optional.empty();
        }

        try {
            Claims datos = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(cabecera.substring(7))
                    .getPayload();

            return Optional.of(new Datos(datos.getSubject(), String.valueOf(datos.get("rol")), String.valueOf(datos.get("nombre"))));
        } catch (JwtException | IllegalArgumentException e) {
            // Firma invalida, token vencido o mal formado: se trata como no autenticado.
            return Optional.empty();
        }
    }

    public record Datos(String id, String rol, String nombre) {
    }
}
