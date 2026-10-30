package com.utp.urbanalert.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Rol de seguridad con el conjunto de permisos que concede.
 * Relacion: un Rol lo tienen muchos Usuarios (N:M, ver Usuario.roles).
 * Se identifica por su nombre (clave de negocio), no por el id.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "nombre")
@ToString
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private NombreRol nombre;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roles_permisos", joinColumns = @JoinColumn(name = "rol_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso", nullable = false, length = 40)
    private Set<Permiso> permisos = new HashSet<>();

    public Rol(NombreRol nombre, Set<Permiso> permisos) {
        this.nombre = nombre;
        this.permisos = new HashSet<>(permisos);
    }
}
