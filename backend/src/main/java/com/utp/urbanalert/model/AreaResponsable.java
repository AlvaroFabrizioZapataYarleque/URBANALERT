package com.utp.urbanalert.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Area municipal responsable de atender un tipo de incidencia
 * (ej. Alumbrado Publico, Pistas y Veredas, Semaforizacion, Limpieza Publica).
 * Relacion: una AreaResponsable atiende muchas Incidencias (1:N).
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "incidencias")
@Entity
@Table(name = "areas_responsables")
public class AreaResponsable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 250)
    private String descripcion;

    @OneToMany(mappedBy = "areaResponsable")
    private List<Incidencia> incidencias = new ArrayList<>();

    /** Constructor de conveniencia usado al registrar una nueva area. */
    public AreaResponsable(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}
