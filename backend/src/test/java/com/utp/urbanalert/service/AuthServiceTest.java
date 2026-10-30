package com.utp.urbanalert.service;

import com.utp.urbanalert.dto.AuthResponse;
import com.utp.urbanalert.dto.LoginRequest;
import com.utp.urbanalert.dto.RegistroRequest;
import com.utp.urbanalert.dto.UsuarioResponse;
import com.utp.urbanalert.exception.ConflictException;
import com.utp.urbanalert.mapper.UsuarioMapper;
import com.utp.urbanalert.model.Ciudadano;
import com.utp.urbanalert.model.NombreRol;
import com.utp.urbanalert.model.Permiso;
import com.utp.urbanalert.model.Rol;
import com.utp.urbanalert.model.Usuario;
import com.utp.urbanalert.repository.CiudadanoRepository;
import com.utp.urbanalert.repository.RolRepository;
import com.utp.urbanalert.repository.UsuarioRepository;
import com.utp.urbanalert.security.JwtService;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CiudadanoRepository ciudadanoRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Spy
    private UsuarioMapper usuarioMapper = new UsuarioMapper();

    @InjectMocks
    private AuthServiceImpl authService;

    private RegistroRequest registroValido() {
        RegistroRequest request = new RegistroRequest();
        request.setUsername("pedro.gomez");
        request.setPassword("Clave1234");
        request.setNombre("Pedro Gomez");
        request.setDni("70123456");
        request.setEmail("pedro@mail.com");
        request.setTelefono("955123456");
        return request;
    }

    // ---------------------------------------------------------------- registro

    @Test
    void registrar_conDatosValidos_creaPerfilYCuentaConRolCiudadano() {
        Rol rolCiudadano = new Rol(NombreRol.CIUDADANO, Set.of(Permiso.INCIDENCIA_VER));
        when(usuarioRepository.existsByUsername("pedro.gomez")).thenReturn(false);
        when(ciudadanoRepository.existsByDni("70123456")).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.CIUDADANO)).thenReturn(Optional.of(rolCiudadano));
        when(passwordEncoder.encode("Clave1234")).thenReturn("HASH_BCRYPT");
        when(ciudadanoRepository.save(any(Ciudadano.class))).thenAnswer(inv -> {
            Ciudadano c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(9L);
            return u;
        });

        UsuarioResponse respuesta = authService.registrar(registroValido());

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        // La contrasena se guarda como hash, jamas en texto plano.
        assertThat(guardado.getValue().getPassword()).isEqualTo("HASH_BCRYPT");
        assertThat(respuesta.getRoles()).containsExactly("CIUDADANO");
        assertThat(respuesta.getCiudadanoId()).isEqualTo(5L);
        assertThat(respuesta.getUsername()).isEqualTo("pedro.gomez");
    }

    @Test
    void registrar_conUsernameRepetido_lanzaConflictException() {
        when(usuarioRepository.existsByUsername("pedro.gomez")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(registroValido()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pedro.gomez");

        verify(ciudadanoRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_conDniRepetido_lanzaConflictException() {
        when(usuarioRepository.existsByUsername("pedro.gomez")).thenReturn(false);
        when(ciudadanoRepository.existsByDni("70123456")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(registroValido()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("70123456");

        verify(usuarioRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- login

    @Test
    void login_conCredencialesValidas_devuelveTokenYRoles() {
        UsuarioPrincipal principal = new UsuarioPrincipal(1L, "maria", "hash", true, 10L, null,
                Set.of(NombreRol.CIUDADANO), Set.of(Permiso.INCIDENCIA_VER));
        Authentication autenticacion =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(autenticacion);
        when(jwtService.generarToken(principal)).thenReturn("token.jwt.de.prueba");
        when(jwtService.getExpiracionSegundos()).thenReturn(7200L);

        LoginRequest request = new LoginRequest();
        request.setUsername("maria");
        request.setPassword("Ciudadano123*");

        AuthResponse respuesta = authService.login(request);

        assertThat(respuesta.getToken()).isEqualTo("token.jwt.de.prueba");
        assertThat(respuesta.getTipo()).isEqualTo("Bearer");
        assertThat(respuesta.getExpiraEnSegundos()).isEqualTo(7200L);
        assertThat(respuesta.getUsername()).isEqualTo("maria");
        assertThat(respuesta.getRoles()).containsExactly("CIUDADANO");
    }

    @Test
    void login_conCredencialesInvalidas_propagaBadCredentialsYNoGeneraToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest();
        request.setUsername("maria");
        request.setPassword("incorrecta");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }
}
