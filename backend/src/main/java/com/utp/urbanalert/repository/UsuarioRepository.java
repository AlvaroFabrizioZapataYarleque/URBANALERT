package com.utp.urbanalert.repository;

import com.utp.urbanalert.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByAreaId(Long areaId);
}
