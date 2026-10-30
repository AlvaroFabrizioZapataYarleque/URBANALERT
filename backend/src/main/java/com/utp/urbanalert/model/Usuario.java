package com.utp.urbanalert.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Cuenta de acceso al sistema (credenciales + roles).
 *
 * Relaciones:
 *  - N:M con Rol.
 *  - 1:1 opcional con Ciudadano: solo las cuentas de ciudadanos tienen perfil.
 *  - N:1 opcional con AreaResponsable: solo los operadores pertenecen a un area.
 *
 * La contrasena se guarda SIEMPRE como hash BCrypt, nunca en texto plano.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"password", "roles", "ciudadano", "area"})
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudadano_id", unique = true)
    private Ciudadano ciudadano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private AreaResponsable area;
}
