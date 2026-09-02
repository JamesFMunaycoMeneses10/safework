package com.safework.safework.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.Usuario;


public interface UsuarioRepository 
        extends JpaRepository<Usuario, Long> {


    Optional<Usuario> findByUsername(String username);


}