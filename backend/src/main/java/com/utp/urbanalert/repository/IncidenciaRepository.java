package com.utp.urbanalert.repository;

import com.utp.urbanalert.model.EstadoIncidencia;
import com.utp.urbanalert.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repositorio de incidencias: CRUD heredado de JpaRepository, filtros
 * dinamicos (IncidenciaRepositoryCustom) y consultas JPQL de reportes.
 */
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long>, IncidenciaRepositoryCustom {

    boolean existsByAreaResponsableId(Long areaId);

    /** Cantidad de incidencias por estado. Cada fila: [EstadoIncidencia, Long]. */
    @Query("SELECT i.estado, COUNT(i) FROM Incidencia i GROUP BY i.estado")
    List<Object[]> contarPorEstado();

    /** Cantidad de incidencias por categoria. Cada fila: [CategoriaIncidencia, Long]. */
    @Query("SELECT i.categoria, COUNT(i) FROM Incidencia i GROUP BY i.categoria")
    List<Object[]> contarPorCategoria();

    /** Incidencias en los estados indicados reportadas antes del limite (las mas antiguas primero). */
    @Query("SELECT i FROM Incidencia i JOIN FETCH i.ciudadano LEFT JOIN FETCH i.areaResponsable "
            + "WHERE i.estado IN (:estados) AND i.fechaReporte < :limite "
            + "ORDER BY i.fechaReporte ASC")
    List<Incidencia> findPendientesAnterioresA(@Param("estados") Collection<EstadoIncidencia> estados,
                                                @Param("limite") LocalDateTime limite);

    /** Cantidad de incidencias en los estados indicados reportadas antes del limite. */
    @Query("SELECT COUNT(i) FROM Incidencia i WHERE i.estado IN (:estados) AND i.fechaReporte < :limite")
    long contarPendientesAnterioresA(@Param("estados") Collection<EstadoIncidencia> estados,
                                      @Param("limite") LocalDateTime limite);

    /** Datos para calcular tiempos de resolucion. Cada fila: [CategoriaIncidencia, fechaReporte, fechaResolucion]. */
    @Query("SELECT i.categoria, i.fechaReporte, i.fechaResolucion FROM Incidencia i "
            + "WHERE i.fechaResolucion IS NOT NULL")
    List<Object[]> findTiemposResolucion();
}
