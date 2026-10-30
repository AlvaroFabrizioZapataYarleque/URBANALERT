package com.utp.urbanalert.repository;

import com.utp.urbanalert.model.Ciudadano;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadanoRepository extends JpaRepository<Ciudadano, Long> {
    boolean existsByDni(String dni);
}
