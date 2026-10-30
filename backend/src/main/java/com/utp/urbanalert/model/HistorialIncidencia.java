package com.utp.urbanalert.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Registro de seguimiento de una incidencia: cada cambio relevante (alta,
 * cambio de estado, derivacion a un area, edicion) deja una fila indicando
 * que paso, cuando y quien lo hizo.
 * Relacion: una Incidencia tiene muchos registros de historial (1:N).
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "incidencia")
@Entity
@Table(name = "historial_incidencias")
public class HistorialIncidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidencia_id", nullable = false)
    private Incidencia incidencia;

    /** Nulo cuando el registro corresponde al alta de la incidencia. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoIncidencia estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIncidencia estadoNuevo;

    @Column(nullable = false, length = 300)
    private String detalle;

    @Column(nullable = false, length = 50)
    private String realizadoPor;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public HistorialIncidencia(Incidencia incidencia, EstadoIncidencia estadoAnterior,
                                EstadoIncidencia estadoNuevo, String detalle, String realizadoPor) {
        this.incidencia = incidencia;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.detalle = detalle;
        this.realizadoPor = realizadoPor;
        this.fecha = LocalDateTime.now();
    }
}
