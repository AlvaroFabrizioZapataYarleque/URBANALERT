package com.utp.urbanalert.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Genera y valida tokens JWT firmados con HMAC-SHA256.
 *
 * El token contiene: subject (username), roles, fecha de emision y de
 * expiracion. La firma garantiza que no fue alterado; la expiracion limita
 * el tiempo de uso de un token robado.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${app.jwt.secret}") String secretBase64,
                      @Value("${app.jwt.expiration-ms}") long expiracionMs) {
        // Falla al arrancar si el secreto tiene menos de 256 bits (WeakKeyException).
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(UserDetails usuario) {
        List<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith("ROLE_"))
                .sorted()
                .toList();
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("roles", roles)
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /** Devuelve el username del token. Lanza JwtException si esta vencido, alterado o mal formado. */
    public String extraerUsername(String token) {
        return extraerClaims(token).getSubject();
    }

    /** El token es valido si pertenece al usuario indicado, esta firmado correctamente y no ha vencido. */
    public boolean esTokenValido(String token, UserDetails usuario) {
        try {
            return extraerUsername(token).equals(usuario.getUsername());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpiracionSegundos() {
        return expiracionMs / 1000;
    }

    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
