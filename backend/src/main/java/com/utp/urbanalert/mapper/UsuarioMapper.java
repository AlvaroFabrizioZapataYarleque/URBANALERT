package com.utp.urbanalert.mapper;

import com.utp.urbanalert.dto.UsuarioResponse;
import com.utp.urbanalert.model.AreaResponsable;
import com.utp.urbanalert.model.Usuario;
import org.springframework.stereotype.Component;

/** Convierte la entidad Usuario en su DTO de salida (sin exponer la contrasena). */
@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario u) {
        AreaResponsable area = u.getArea();
        return UsuarioResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .habilitado(u.isEnabled())
                .roles(u.getRoles().stream().map(rol -> rol.getNombre().name()).sorted().toList())
                .ciudadanoId(u.getCiudadano() != null ? u.getCiudadano().getId() : null)
                .areaId(area != null ? area.getId() : null)
                .areaNombre(area != null ? area.getNombre() : null)
                .fechaCreacion(u.getFechaCreacion())
                .build();
    }
}
