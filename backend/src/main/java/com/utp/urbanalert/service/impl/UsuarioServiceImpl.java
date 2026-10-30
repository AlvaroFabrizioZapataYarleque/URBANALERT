package com.utp.urbanalert.service.impl;

import com.utp.urbanalert.dto.CrearUsuarioRequest;
import com.utp.urbanalert.dto.UsuarioResponse;
import com.utp.urbanalert.exception.BusinessValidationException;
import com.utp.urbanalert.exception.ConflictException;
import com.utp.urbanalert.exception.ResourceNotFoundException;
import com.utp.urbanalert.mapper.UsuarioMapper;
import com.utp.urbanalert.model.AreaResponsable;
import com.utp.urbanalert.model.NombreRol;
import com.utp.urbanalert.model.Rol;
import com.utp.urbanalert.model.Usuario;
import com.utp.urbanalert.repository.AreaResponsableRepository;
import com.utp.urbanalert.repository.RolRepository;
import com.utp.urbanalert.repository.UsuarioRepository;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaResponsableRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Override
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream().map(usuarioMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        if (request.getRol() == NombreRol.CIUDADANO) {
            throw new BusinessValidationException(
                    "Los ciudadanos se registran por su cuenta en /api/auth/register");
        }
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("El nombre de usuario '" + request.getUsername() + "' ya esta en uso");
        }

        AreaResponsable area = null;
        if (request.getRol() == NombreRol.OPERADOR) {
            if (request.getAreaId() == null) {
                throw new BusinessValidationException("Un operador debe pertenecer a un area (areaId)");
            }
            area = areaRepository.findById(request.getAreaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No existe un area responsable con id " + request.getAreaId()));
        }

        Rol rol = rolRepository.findByNombre(request.getRol())
                .orElseThrow(() -> new IllegalStateException("El rol " + request.getRol() + " no esta configurado"));

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.getRoles().add(rol);
        usuario.setArea(area);

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarHabilitado(Long id, boolean habilitado, UsuarioPrincipal actor) {
        if (!habilitado && id.equals(actor.getId())) {
            throw new BusinessValidationException("No puede deshabilitar su propia cuenta");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con id " + id));
        usuario.setEnabled(habilitado);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }
}
