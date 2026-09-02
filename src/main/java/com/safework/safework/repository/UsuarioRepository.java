package com.safework.safework.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.Usuario;
    // =========================
    // Este repositorio trabajara con la entidad usuario y su id es de tipo Long
    // =========================


public interface UsuarioRepository 
extends JpaRepository<Usuario,Long>{

//Busca un Usuario donde el campo username sea igual al valor enviado
    Optional<Usuario> findByUsername(String username);

// Buscar usuarios por estado
    List<Usuario> findByEstado(String estado);


}