package com.utp.urbanalert.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que se ejecuta UNA vez por solicitud, antes de la autenticacion por
 * formulario de Spring Security:
 *  1. Lee la cabecera "Authorization: Bearer <token>".
 *  2. Valida el token y carga al usuario desde la base de datos.
 *  3. Si todo es correcto, deja al usuario autenticado en el SecurityContext.
 *
 * Si el token falta o es invalido NO responde error aqui: simplemente no
 * autentica, y es Spring Security quien devuelve 401 si el endpoint lo exige.
 * Asi los endpoints publicos siguen funcionando aunque llegue un token vencido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.startsWith(PREFIJO_BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecera.substring(PREFIJO_BEARER.length());
            try {
                String username = jwtService.extraerUsername(token);
                UserDetails usuario = userDetailsService.loadUserByUsername(username);

                if (usuario.isEnabled() && jwtService.esTokenValido(token, usuario)) {
                    UsernamePasswordAuthenticationToken autenticacion =
                            new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                log.debug("Token rechazado: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
