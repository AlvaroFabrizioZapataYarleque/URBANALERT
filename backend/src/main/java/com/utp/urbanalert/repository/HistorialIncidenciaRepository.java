package com.utp.urbanalert.repository;

import com.utp.urbanalert.model.HistorialIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialIncidenciaRepository extends JpaRepository<HistorialIncidencia, Long> {
    List<HistorialIncidencia> findByIncidenciaIdOrderByFechaAsc(Long incidenciaId);
}
