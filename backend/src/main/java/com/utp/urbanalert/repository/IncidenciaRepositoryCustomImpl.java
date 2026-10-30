package com.utp.urbanalert.repository;

import com.utp.urbanalert.dto.IncidenciaFiltro;
import com.utp.urbanalert.model.Incidencia;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion de la consulta JPQL dinamica: la sentencia se arma agregando
 * una condicion por cada criterio informado y SIEMPRE con parametros nombrados
 * (nunca concatenando valores), lo que evita inyeccion JPQL/SQL.
 *
 * Spring Data detecta esta clase por convencion de nombre
 * (interfaz del fragmento + "Impl").
 */
public class IncidenciaRepositoryCustomImpl implements IncidenciaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Incidencia> buscar(IncidenciaFiltro filtro) {
        // JOIN FETCH trae ciudadano y area en la misma consulta (evita el problema N+1).
        StringBuilder jpql = new StringBuilder(
                "SELECT i FROM Incidencia i "
                        + "JOIN FETCH i.ciudadano "
                        + "LEFT JOIN FETCH i.areaResponsable "
                        + "WHERE 1 = 1");
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.getEstado() != null) {
            jpql.append(" AND i.estado = :estado");
            parametros.put("estado", filtro.getEstado());
        }
        if (filtro.getCategoria() != null) {
            jpql.append(" AND i.categoria = :categoria");
            parametros.put("categoria", filtro.getCategoria());
        }
        if (filtro.getPrioridad() != null) {
            jpql.append(" AND i.prioridad = :prioridad");
            parametros.put("prioridad", filtro.getPrioridad());
        }
        if (filtro.getAreaId() != null) {
            jpql.append(" AND i.areaResponsable.id = :areaId");
            parametros.put("areaId", filtro.getAreaId());
        }
        if (filtro.getCiudadanoId() != null) {
            jpql.append(" AND i.ciudadano.id = :ciudadanoId");
            parametros.put("ciudadanoId", filtro.getCiudadanoId());
        }
        if (filtro.getDesde() != null) {
            jpql.append(" AND i.fechaReporte >= :desde");
            parametros.put("desde", filtro.getDesde().atStartOfDay());
        }
        if (filtro.getHasta() != null) {
            // "hasta" es inclusivo: se compara contra el inicio del dia siguiente.
            jpql.append(" AND i.fechaReporte < :hasta");
            parametros.put("hasta", filtro.getHasta().plusDays(1).atStartOfDay());
        }
        jpql.append(" ORDER BY i.fechaReporte DESC");

        TypedQuery<Incidencia> query = entityManager.createQuery(jpql.toString(), Incidencia.class);
        for (Map.Entry<String, Object> parametro : parametros.entrySet()) {
            query.setParameter(parametro.getKey(), parametro.getValue());
        }
        return query.getResultList();
    }
}
