package com.utp.urbanalert.security;

import com.utp.urbanalert.model.NombreRol;
import com.utp.urbanalert.model.Permiso;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias puras del servicio JWT (no levantan Spring). */
class JwtServiceTest {

    private static final String SECRETO = "YECOmuHnj/qYG2bj4W8PnDgSSdwwwV5T104IzaosssWKP5EpqDJiTD3aHnIXSqDN";

    private final JwtService jwtService = new JwtService(SECRETO, 60_000);
    private final UsuarioPrincipal maria = principal("maria");
    private final UsuarioPrincipal jose = principal("jose");

    private static UsuarioPrincipal principal(String username) {
        return new UsuarioPrincipal(1L, username, "hash", true, 10L, null,
                Set.of(NombreRol.CIUDADANO), Set.of(Permiso.INCIDENCIA_VER));
    }

    @Test
    void generarToken_debeTenerElUsernameComoSubject() {
        String token = jwtService.generarToken(maria);

        assertThat(token.split("\\.")).hasSize(3); // cabecera.payload.firma
        assertThat(jwtService.extraerUsername(token)).isEqualTo("maria");
    }

    @Test
    void esTokenValido_conElMismoUsuario_debeSerTrue() {
        String token = jwtService.generarToken(maria);

        assertThat(jwtService.esTokenValido(token, maria)).isTrue();
    }

    @Test
    void esTokenValido_conOtroUsuario_debeSerFalse() {
        String tokenDeMaria = jwtService.generarToken(maria);

        assertThat(jwtService.esTokenValido(tokenDeMaria, jose)).isFalse();
    }

    @Test
    void tokenExpirado_debeSerInvalido() {
        JwtService servicioConTokensVencidos = new JwtService(SECRETO, -1_000);
        String tokenVencido = servicioConTokensVencidos.generarToken(maria);

        assertThat(jwtService.esTokenValido(tokenVencido, maria)).isFalse();
        assertThatThrownBy(() -> jwtService.extraerUsername(tokenVencido))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenManipulado_debeSerInvalido() {
        String[] partes = jwtService.generarToken(maria).split("\\.");
        char primero = partes[2].charAt(0);
        char alterado = primero == 'A' ? 'B' : 'A';
        String tokenAlterado = partes[0] + "." + partes[1] + "." + alterado + partes[2].substring(1);

        assertThat(jwtService.esTokenValido(tokenAlterado, maria)).isFalse();
        assertThatThrownBy(() -> jwtService.extraerUsername(tokenAlterado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFirmadoConOtraClave_debeSerInvalido() {
        JwtService otroServicio = new JwtService(
                "b3Ryby1zZWNyZXRvLWRpc3RpbnRvLXBhcmEtcHJ1ZWJhcy0xMjM0NTY3ODkwLWFiY2RlZg==", 60_000);
        String tokenAjeno = otroServicio.generarToken(maria);

        assertThat(jwtService.esTokenValido(tokenAjeno, maria)).isFalse();
    }

    @Test
    void tokenMalFormado_debeSerInvalido() {
        assertThat(jwtService.esTokenValido("esto-no-es-un-jwt", maria)).isFalse();
    }

    @Test
    void secretoDebil_debeFallarAlCrearElServicio() {
        assertThatThrownBy(() -> new JwtService("c2VjcmV0bw==", 1_000))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getExpiracionSegundos_debeConvertirDesdeMilisegundos() {
        assertThat(new JwtService(SECRETO, 7_200_000).getExpiracionSegundos()).isEqualTo(7_200L);
    }
}
