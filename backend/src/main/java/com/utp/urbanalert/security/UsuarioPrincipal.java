package com.utp.urbanalert.security;

import com.utp.urbanalert.model.NombreRol;
import com.utp.urbanalert.model.Permiso;
import com.utp.urbanalert.model.Rol;
import com.utp.urbanalert.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Usuario autenticado tal como lo ve Spring Security (UserDetails).
 *
 * Es un objeto liviano e independiente de la entidad JPA: conserva los datos
 * necesarios para autorizar sin volver a la base de datos (id, rol, y a que
 * ciudadano/area esta vinculado). Las autoridades son los roles (ROLE_ADMIN,
 * ...) mas los permisos de esos roles (INCIDENCIA_CREAR, ...).
 */
@Getter
@AllArgsConstructor
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    /** Perfil de ciudadano vinculado (solo cuentas con rol CIUDADANO). */
    private final Long ciudadanoId;
    /** Area a la que pertenece (solo cuentas con rol OPERADOR). */
    private final Long areaId;
    private final Set<NombreRol> roles;
    private final Set<Permiso> permisos;

    /** Construye el principal a partir de la entidad. Debe invocarse dentro de una transaccion. */
    public static UsuarioPrincipal desde(Usuario usuario) {
        Set<NombreRol> roles = new HashSet<>();
        Set<Permiso> permisos = new HashSet<>();
        for (Rol rol : usuario.getRoles()) {
            roles.add(rol.getNombre());
            permisos.addAll(rol.getPermisos());
        }
        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.isEnabled(),
                usuario.getCiudadano() != null ? usuario.getCiudadano().getId() : null,
                usuario.getArea() != null ? usuario.getArea().getId() : null,
                roles,
                permisos);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> autoridades = new HashSet<>();
        roles.forEach(rol -> autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name())));
        permisos.forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return autoridades;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean esAdmin() {
        return roles.contains(NombreRol.ADMIN);
    }

    public boolean esOperador() {
        return roles.contains(NombreRol.OPERADOR);
    }

    public boolean esCiudadano() {
        return roles.contains(NombreRol.CIUDADANO);
    }
}
