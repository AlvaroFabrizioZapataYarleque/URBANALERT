package com.utp.urbanalert.service.impl;

import com.utp.urbanalert.dto.CiudadanoRequest;
import com.utp.urbanalert.dto.CiudadanoResponse;
import com.utp.urbanalert.dto.PerfilRequest;
import com.utp.urbanalert.exception.BusinessValidationException;
import com.utp.urbanalert.exception.ConflictException;
import com.utp.urbanalert.exception.ResourceNotFoundException;
import com.utp.urbanalert.model.Ciudadano;
import com.utp.urbanalert.repository.CiudadanoRepository;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.CiudadanoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CiudadanoServiceImpl implements CiudadanoService {

    private final CiudadanoRepository ciudadanoRepository;

    @Override
    public List<CiudadanoResponse> listarTodos() {
        return ciudadanoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public CiudadanoResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    @Transactional
    public CiudadanoResponse registrar(CiudadanoRequest request) {
        if (ciudadanoRepository.existsByDni(request.getDni())) {
            throw new ConflictException("Ya existe un ciudadano registrado con DNI " + request.getDni());
        }
        Ciudadano ciudadano = new Ciudadano(
                request.getNombre().trim(), request.getDni(), request.getEmail(), request.getTelefono());
        return toResponse(ciudadanoRepository.save(ciudadano));
    }

    @Override
    public CiudadanoResponse obtenerMiPerfil(UsuarioPrincipal actor) {
        return toResponse(buscarOLanzar(idPerfil(actor)));
    }

    @Override
    @Transactional
    public CiudadanoResponse actualizarMiPerfil(PerfilRequest request, UsuarioPrincipal actor) {
        Ciudadano ciudadano = buscarOLanzar(idPerfil(actor));
        ciudadano.setNombre(request.getNombre().trim());
        ciudadano.setEmail(request.getEmail());
        ciudadano.setTelefono(request.getTelefono());
        return toResponse(ciudadanoRepository.save(ciudadano));
    }

    private Long idPerfil(UsuarioPrincipal actor) {
        if (actor.getCiudadanoId() == null) {
            throw new BusinessValidationException("Su usuario no tiene un perfil de ciudadano asociado");
        }
        return actor.getCiudadanoId();
    }

    private Ciudadano buscarOLanzar(Long id) {
        return ciudadanoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un ciudadano con id " + id));
    }

    private CiudadanoResponse toResponse(Ciudadano c) {
        return CiudadanoResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .dni(c.getDni())
                .email(c.getEmail())
                .telefono(c.getTelefono())
                .build();
    }
}
