package com.utp.urbanalert.service.impl;

import com.utp.urbanalert.dto.HistorialResponse;
import com.utp.urbanalert.dto.IncidenciaActualizacionRequest;
import com.utp.urbanalert.dto.IncidenciaFiltro;
import com.utp.urbanalert.dto.IncidenciaRequest;
import com.utp.urbanalert.dto.IncidenciaResponse;
import com.utp.urbanalert.exception.BusinessValidationException;
import com.utp.urbanalert.exception.ResourceNotFoundException;
import com.utp.urbanalert.mapper.IncidenciaMapper;
import com.utp.urbanalert.model.AreaResponsable;
import com.utp.urbanalert.model.Ciudadano;
import com.utp.urbanalert.model.EstadoIncidencia;
import com.utp.urbanalert.model.HistorialIncidencia;
import com.utp.urbanalert.model.Incidencia;
import com.utp.urbanalert.repository.AreaResponsableRepository;
import com.utp.urbanalert.repository.CiudadanoRepository;
import com.utp.urbanalert.repository.HistorialIncidenciaRepository;
import com.utp.urbanalert.repository.IncidenciaRepository;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.IncidenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Logica de negocio de las incidencias.
 *
 * Transacciones: la clase es de solo lectura por defecto; cada operacion que
 * modifica datos declara @Transactional. En registrar/actualizar/cambiarEstado/
 * asignarArea la incidencia y su registro de historial se guardan en la MISMA
 * transaccion: si falla uno, no queda guardado el otro (nunca hay un cambio
 * sin su rastro de auditoria ni un rastro sin cambio).
 *
 * Validacion: el formato de los datos (campos vacios, longitudes) se valida en
 * el borde con Bean Validation (@Valid en los DTO); aqui solo se validan reglas
 * de negocio (transiciones, existencia de entidades, alcance del usuario).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidenciaServiceImpl implements IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final CiudadanoRepository ciudadanoRepository;
    private final AreaResponsableRepository areaResponsableRepository;
    private final HistorialIncidenciaRepository historialRepository;
    private final IncidenciaMapper mapper;

    // ------------------------------------------------------------ consultas

    @Override
    public List<IncidenciaResponse> listar(IncidenciaFiltro filtro, UsuarioPrincipal actor) {
        if (filtro.getDesde() != null && filtro.getHasta() != null
                && filtro.getDesde().isAfter(filtro.getHasta())) {
            throw new BusinessValidationException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }

        // El alcance lo fija el servidor segun el rol: el cliente no puede ampliarlo.
        if (actor.esAdmin()) {
            // sin restriccion adicional
        } else if (actor.esOperador()) {
            if (actor.getAreaId() == null) {
                return List.of();
            }
            filtro.setAreaId(actor.getAreaId());
        } else if (actor.esCiudadano()) {
            if (actor.getCiudadanoId() == null) {
                return List.of();
            }
            filtro.setCiudadanoId(actor.getCiudadanoId());
        } else {
            return List.of();
        }

        return incidenciaRepository.buscar(filtro).stream().map(mapper::toResponse).toList();
    }

    @Override
    public IncidenciaResponse obtenerPorId(Long id, UsuarioPrincipal actor) {
        Incidencia incidencia = buscarOLanzar(id);
        verificarAcceso(incidencia, actor);
        return mapper.toResponse(incidencia);
    }

    @Override
    public List<HistorialResponse> obtenerHistorial(Long id, UsuarioPrincipal actor) {
        Incidencia incidencia = buscarOLanzar(id);
        verificarAcceso(incidencia, actor);
        return historialRepository.findByIncidenciaIdOrderByFechaAsc(id).stream()
                .map(mapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------ comandos

    @Override
    @Transactional
    public IncidenciaResponse registrar(IncidenciaRequest request, UsuarioPrincipal actor) {
        Ciudadano ciudadano = resolverCiudadano(request, actor);

        Incidencia incidencia = new Incidencia(
                request.getTitulo().trim(),
                request.getDescripcion().trim(),
                request.getCategoria(),
                request.getPrioridad(),
                request.getDireccionReferencial(),
                ciudadano);

        // Solo el personal municipal puede derivar a un area desde el registro.
        if (!actor.esCiudadano() && request.getAreaId() != null) {
            incidencia.setAreaResponsable(buscarAreaOLanzar(request.getAreaId()));
        }

        Incidencia guardada = incidenciaRepository.save(incidencia);
        registrarHistorial(guardada, null, EstadoIncidencia.REGISTRADA, "Incidencia registrada", actor);
        return mapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public IncidenciaResponse actualizar(Long id, IncidenciaActualizacionRequest request, UsuarioPrincipal actor) {
        Incidencia incidencia = buscarOLanzar(id);
        verificarAcceso(incidencia, actor);

        if (incidencia.getEstado() == EstadoIncidencia.CERRADA) {
            throw new BusinessValidationException("No se puede modificar una incidencia CERRADA");
        }
        if (actor.esCiudadano() && incidencia.getEstado() != EstadoIncidencia.REGISTRADA) {
            throw new BusinessValidationException(
                    "Solo puede modificar su incidencia mientras este REGISTRADA");
        }

        incidencia.setTitulo(request.getTitulo().trim());
        incidencia.setDescripcion(request.getDescripcion().trim());
        incidencia.setCategoria(request.getCategoria());
        incidencia.setPrioridad(request.getPrioridad());
        incidencia.setDireccionReferencial(request.getDireccionReferencial());
        incidencia.setFechaActualizacion(LocalDateTime.now());

        Incidencia guardada = incidenciaRepository.save(incidencia);
        registrarHistorial(guardada, guardada.getEstado(), guardada.getEstado(),
                "Datos de la incidencia actualizados", actor);
        return mapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public IncidenciaResponse cambiarEstado(Long id, EstadoIncidencia nuevoEstado, UsuarioPrincipal actor) {
        Incidencia incidencia = buscarOLanzar(id);
        verificarAcceso(incidencia, actor);
        EstadoIncidencia anterior = incidencia.getEstado();

        if (!anterior.puedePasarA(nuevoEstado)) {
            throw new BusinessValidationException(
                    "Transicion de estado no permitida: " + anterior + " -> " + nuevoEstado);
        }
        if (nuevoEstado == EstadoIncidencia.EN_PROCESO && incidencia.getAreaResponsable() == null) {
            throw new BusinessValidationException(
                    "Debe asignar un area responsable antes de iniciar la atencion");
        }
        if (nuevoEstado == EstadoIncidencia.CERRADA && !actor.esAdmin()) {
            throw new AccessDeniedException("Solo un administrador puede cerrar una incidencia");
        }

        LocalDateTime ahora = LocalDateTime.now();
        incidencia.setEstado(nuevoEstado);
        incidencia.setFechaActualizacion(ahora);
        if (nuevoEstado == EstadoIncidencia.RESUELTA) {
            incidencia.setFechaResolucion(ahora);
        } else if (anterior == EstadoIncidencia.RESUELTA && nuevoEstado == EstadoIncidencia.EN_PROCESO) {
            incidencia.setFechaResolucion(null); // se reabrio: deja de estar resuelta
        }

        Incidencia guardada = incidenciaRepository.save(incidencia);
        registrarHistorial(guardada, anterior, nuevoEstado,
                "Estado cambiado de " + anterior + " a " + nuevoEstado, actor);
        return mapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public IncidenciaResponse asignarArea(Long id, Long areaId, UsuarioPrincipal actor) {
        Incidencia incidencia = buscarOLanzar(id);
        if (incidencia.getEstado() == EstadoIncidencia.CERRADA) {
            throw new BusinessValidationException("No se puede reasignar una incidencia CERRADA");
        }

        AreaResponsable nueva = buscarAreaOLanzar(areaId);
        String anterior = incidencia.getAreaResponsable() != null
                ? incidencia.getAreaResponsable().getNombre()
                : "sin asignar";

        incidencia.setAreaResponsable(nueva);
        incidencia.setFechaActualizacion(LocalDateTime.now());

        Incidencia guardada = incidenciaRepository.save(incidencia);
        registrarHistorial(guardada, guardada.getEstado(), guardada.getEstado(),
                "Area derivada: " + anterior + " -> " + nueva.getNombre(), actor);
        return mapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioPrincipal actor) {
        Incidencia incidencia = buscarOLanzar(id);
        verificarAcceso(incidencia, actor);

        if (actor.esCiudadano() && incidencia.getEstado() != EstadoIncidencia.REGISTRADA) {
            throw new BusinessValidationException(
                    "Solo puede eliminar su incidencia mientras este REGISTRADA");
        }
        // El historial se elimina en cascada junto con la incidencia.
        incidenciaRepository.delete(incidencia);
    }

    // ------------------------------------------------------------ helpers privados

    private Incidencia buscarOLanzar(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una incidencia con id " + id));
    }

    private AreaResponsable buscarAreaOLanzar(Long areaId) {
        return areaResponsableRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un area responsable con id " + areaId));
    }

    /**
     * Un ciudadano siempre reporta a nombre de su propio perfil. El personal
     * municipal debe indicar a que ciudadano corresponde el reporte.
     */
    private Ciudadano resolverCiudadano(IncidenciaRequest request, UsuarioPrincipal actor) {
        Long ciudadanoId = actor.esCiudadano() ? actor.getCiudadanoId() : request.getCiudadanoId();
        if (ciudadanoId == null) {
            throw new BusinessValidationException(actor.esCiudadano()
                    ? "Su usuario no tiene un perfil de ciudadano asociado"
                    : "Debe indicar el ciudadano que reporta (ciudadanoId)");
        }
        return ciudadanoRepository.findById(ciudadanoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un ciudadano con id " + ciudadanoId));
    }

    /** Autorizacion a nivel de dato: que el usuario pueda ver/modificar ESTA incidencia. */
    private void verificarAcceso(Incidencia incidencia, UsuarioPrincipal actor) {
        if (actor.esAdmin()) {
            return;
        }
        if (actor.esOperador()) {
            AreaResponsable area = incidencia.getAreaResponsable();
            if (area != null && area.getId().equals(actor.getAreaId())) {
                return;
            }
            throw new AccessDeniedException("La incidencia no pertenece a su area");
        }
        if (actor.esCiudadano() && incidencia.getCiudadano().getId().equals(actor.getCiudadanoId())) {
            return;
        }
        throw new AccessDeniedException("No tiene acceso a esta incidencia");
    }

    private void registrarHistorial(Incidencia incidencia, EstadoIncidencia anterior,
                                    EstadoIncidencia nuevo, String detalle, UsuarioPrincipal actor) {
        historialRepository.save(
                new HistorialIncidencia(incidencia, anterior, nuevo, detalle, actor.getUsername()));
    }
}
