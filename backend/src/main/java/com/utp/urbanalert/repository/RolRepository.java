package com.utp.urbanalert.repository;

import com.utp.urbanalert.model.NombreRol;
import com.utp.urbanalert.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(NombreRol nombre);
}
