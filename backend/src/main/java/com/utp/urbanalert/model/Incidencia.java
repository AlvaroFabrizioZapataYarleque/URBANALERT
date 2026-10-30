package com.utp.urbanalert.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte de un problema de infraestructura urbana.
 *
 * Relaciones:
 *  - N:1 con Ciudadano (quien reporta, obligatorio).
 *  - N:1 con AreaResponsable (quien la atiende, se asigna al derivar).
 *  - 1:N con HistorialIncidencia (seguimiento; se elimina junto con la incidencia).
 *
 * Igual que en las demas entidades: @Getter/@Setter (no @Data) y las
 * relaciones se excluyen de equals/hashCode/toString para evitar recursion
 * infinita entre entidades que se referencian entre si.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"ciudadano", "areaResponsable", "historial"})
@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaIncidencia categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadIncidencia prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIncidencia estado;

    @Column(length = 200)
    private String direccionReferencial;

    @Column(nullable = false)
    private LocalDateTime fechaReporte;

    private LocalDateTime fechaActualizacion;

    /** Se completa al pasar a RESUELTA; sirve para calcular el tiempo de resolucion. */
    private LocalDateTime fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudadano_id", nullable = false)
    private Ciudadano ciudadano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private AreaResponsable areaResponsable;

    @OneToMany(mappedBy = "incidencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<HistorialIncidencia> historial = new ArrayList<>();

    /**
     * Constructor de conveniencia para registrar una incidencia nueva:
     * fija el estado inicial (REGISTRADA) y la fecha de reporte (ahora),
     * para que quien registra no tenga que recordar fijarlos a mano.
     */
    public Incidencia(String titulo, String descripcion, CategoriaIncidencia categoria,
                       PrioridadIncidencia prioridad, String direccionReferencial, Ciudadano ciudadano) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.prioridad = prioridad;
        this.direccionReferencial = direccionReferencial;
        this.ciudadano = ciudadano;
        this.estado = EstadoIncidencia.REGISTRADA;
        this.fechaReporte = LocalDateTime.now();
    }
}
