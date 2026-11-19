package com.sonidovivo.usuarios.seguridad;

import com.sonidovivo.usuarios.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Emite el token JWT del login.
 *
 * El token lleva el identificador y el rol del usuario, va firmado con HS256 y una clave compartida
 * con el gateway (que es quien lo valida). Asi ningun servicio de negocio recibe peticiones sin
 * pasar por el control de acceso del borde.
 */
@Service
public class TokenService {

    private final SecretKey clave;
    private final long horasVigencia;

    public TokenService(@Value("${seguridad.jwt.secreto}") String secreto,
                        @Value("${seguridad.jwt.horas:8}") long horasVigencia) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.horasVigencia = horasVigencia;
    }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("nombre", usuario.getNombre())
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(horasVigencia, ChronoUnit.HOURS)))
                .signWith(clave)
                .compact();
    }
}
