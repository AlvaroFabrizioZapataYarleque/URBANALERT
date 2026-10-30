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
 * Ciudadano que reporta una o mas incidencias.
 * Relacion: un Ciudadano puede tener muchas Incidencias (1:N).
 *
 * Nota sobre Lombok + JPA: se usa @Getter/@Setter en lugar de @Data porque
 * @Data tambien genera equals()/hashCode()/toString() sobre TODOS los
 * campos, incluida la lista "incidencias". Como Incidencia tiene una
 * referencia de vuelta a Ciudadano, eso produce una recursion infinita
 * (StackOverflowError) apenas se imprime o compara un Ciudadano. Por eso
 * el equals/hashCode se basa solo en "id" y el toString excluye la lista.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "incidencias")
@Entity
@Table(name = "ciudadanos")
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 15)
    private String dni;

    @Column(length = 120)
    private String email;

    @Column(length = 20)
    private String telefono;

    @OneToMany(mappedBy = "ciudadano", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Incidencia> incidencias = new ArrayList<>();

    /** Constructor de conveniencia usado al registrar un ciudadano nuevo. */
    public Ciudadano(String nombre, String dni, String email, String telefono) {
        this.nombre = nombre;
        this.dni = dni;
        this.email = email;
        this.telefono = telefono;
    }
}
