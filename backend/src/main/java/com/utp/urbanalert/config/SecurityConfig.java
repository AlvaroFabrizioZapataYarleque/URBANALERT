package com.utp.urbanalert.config;

import com.utp.urbanalert.security.CustomUserDetailsService;
import com.utp.urbanalert.security.JwtAuthenticationFilter;
import com.utp.urbanalert.security.ManejadorErroresSeguridad;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracion central de Spring Security.
 *
 * - Autenticacion STATELESS: no hay sesion en el servidor; cada solicitud
 *   debe traer su token JWT.
 * - La autorizacion fina (por rol/permiso) se declara en los controladores con
 *   @PreAuthorize y @Secured, habilitados con @EnableMethodSecurity.
 * - Aqui solo se define lo grueso: que rutas son publicas y que el resto
 *   exige estar autenticado.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final ManejadorErroresSeguridad manejadorErrores;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protege sesiones basadas en cookies; con tokens en cabecera (stateless) no aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        // Spring Security 6 tambien protege el despacho interno hacia /error; se permite
                        // para que un 400/404/405 no se convierta en un 401 enganoso.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/publico/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll() // solo desarrollo
                        .requestMatchers(new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name())).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // La consola de H2 se muestra dentro de frames del mismo origen.
                .headers(cabeceras -> cabeceras.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    /** Proveedor que valida usuario/contrasena contra la BD usando nuestro UserDetailsService. */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider();
        proveedor.setUserDetailsService(userDetailsService);
        proveedor.setPasswordEncoder(passwordEncoder());
        return proveedor;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    /** BCrypt: hash con sal incorporada y costo ajustable; es el estandar para contrasenas. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * El filtro JWT es un @Component, por lo que Spring Boot lo registraria tambien
     * como filtro de servlet global. Se desactiva ese registro para que solo se
     * ejecute dentro de la cadena de Spring Security.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> desactivarRegistroGlobalDelFiltroJwt(
            JwtAuthenticationFilter filtro) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesPermitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
