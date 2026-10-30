package com.utp.urbanalert.security;

import com.utp.urbanalert.model.Usuario;
import com.utp.urbanalert.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsService personalizado: Spring Security lo invoca para obtener
 * al usuario desde la BASE DE DATOS (tanto al iniciar sesion como al validar
 * cada token), en lugar de usar usuarios en memoria.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return UsuarioPrincipal.desde(usuario);
    }
}
