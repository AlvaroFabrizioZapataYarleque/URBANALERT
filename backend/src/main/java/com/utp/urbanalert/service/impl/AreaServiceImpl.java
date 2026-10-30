package com.utp.urbanalert.service.impl;

import com.utp.urbanalert.dto.AreaRequest;
import com.utp.urbanalert.dto.AreaResponse;
import com.utp.urbanalert.exception.ConflictException;
import com.utp.urbanalert.exception.ResourceNotFoundException;
import com.utp.urbanalert.model.AreaResponsable;
import com.utp.urbanalert.repository.AreaResponsableRepository;
import com.utp.urbanalert.repository.IncidenciaRepository;
import com.utp.urbanalert.repository.UsuarioRepository;
import com.utp.urbanalert.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD de areas responsables, protegiendo la integridad referencial al eliminar. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AreaServiceImpl implements AreaService {

    private final AreaResponsableRepository areaRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public List<AreaResponse> listarTodas() {
        return areaRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public AreaResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    @Transactional
    public AreaResponse crear(AreaRequest request) {
        String nombre = request.getNombre().trim();
        if (areaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe un area con el nombre '" + nombre + "'");
        }
        return toResponse(areaRepository.save(new AreaResponsable(nombre, request.getDescripcion())));
    }

    @Override
    @Transactional
    public AreaResponse actualizar(Long id, AreaRequest request) {
        AreaResponsable area = buscarOLanzar(id);
        String nombre = request.getNombre().trim();
        if (areaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictException("Ya existe otra area con el nombre '" + nombre + "'");
        }
        area.setNombre(nombre);
        area.setDescripcion(request.getDescripcion());
        return toResponse(areaRepository.save(area));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        AreaResponsable area = buscarOLanzar(id);
        if (incidenciaRepository.existsByAreaResponsableId(id)) {
            throw new ConflictException("No se puede eliminar el area: tiene incidencias asignadas");
        }
        if (usuarioRepository.existsByAreaId(id)) {
            throw new ConflictException("No se puede eliminar el area: tiene personal asignado");
        }
        areaRepository.delete(area);
    }

    private AreaResponsable buscarOLanzar(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un area responsable con id " + id));
    }

    private AreaResponse toResponse(AreaResponsable a) {
        return AreaResponse.builder().id(a.getId()).nombre(a.getNombre()).descripcion(a.getDescripcion()).build();
    }
}
