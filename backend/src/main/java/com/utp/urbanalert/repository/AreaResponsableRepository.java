package com.utp.urbanalert.repository;

import com.utp.urbanalert.model.AreaResponsable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaResponsableRepository extends JpaRepository<AreaResponsable, Long> {
    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
