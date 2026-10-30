package com.utp.urbanalert.service.impl;

import com.utp.urbanalert.dto.AuthResponse;
import com.utp.urbanalert.dto.LoginRequest;
import com.utp.urbanalert.dto.RegistroRequest;
import com.utp.urbanalert.dto.UsuarioResponse;
import com.utp.urbanalert.exception.ConflictException;
import com.utp.urbanalert.mapper.UsuarioMapper;
import com.utp.urbanalert.model.Ciudadano;
import com.utp.urbanalert.model.NombreRol;
import com.utp.urbanalert.model.Rol;
import com.utp.urbanalert.model.Usuario;
import com.utp.urbanalert.repository.CiudadanoRepository;
import com.utp.urbanalert.repository.RolRepository;
import com.utp.urbanalert.repository.UsuarioRepository;
import com.utp.urbanalert.security.JwtService;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final CiudadanoRepository ciudadanoRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioMapper usuarioMapper;

    /**
     * Transaccional: el perfil de ciudadano y la cuenta se crean juntos o no se
     * crea ninguno (si falla el guardado de la cuenta, el perfil se revierte).
     */
    @Override
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("El nombre de usuario '" + request.getUsername() + "' ya esta en uso");
        }
        if (ciudadanoRepository.existsByDni(request.getDni())) {
            throw new ConflictException("Ya existe un ciudadano registrado con DNI " + request.getDni());
        }

        Rol rolCiudadano = rolRepository.findByNombre(NombreRol.CIUDADANO)
                .orElseThrow(() -> new IllegalStateException("El rol CIUDADANO no esta configurado"));

        Ciudadano ciudadano = ciudadanoRepository.save(new Ciudadano(
                request.getNombre().trim(), request.getDni(), request.getEmail(), request.getTelefono()));

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.getRoles().add(rolCiudadano);
        usuario.setCiudadano(ciudadano);

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Lanza BadCredentialsException / DisabledException si las credenciales no son validas.
        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UsuarioPrincipal principal = (UsuarioPrincipal) autenticacion.getPrincipal();
        return AuthResponse.builder()
                .token(jwtService.generarToken(principal))
                .tipo("Bearer")
                .expiraEnSegundos(jwtService.getExpiracionSegundos())
                .username(principal.getUsername())
                .roles(principal.getRoles().stream().map(NombreRol::name).sorted().toList())
                .build();
    }
}
