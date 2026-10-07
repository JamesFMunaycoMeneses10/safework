package com.safework.safework.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.EntregaEpp;

public interface EntregaEppRepository extends JpaRepository<EntregaEpp, Long> {
    List<EntregaEpp> findAllByOrderByFechaEntregaDescIdDesc();
    List<EntregaEpp> findByTrabajadorUsuarioUsernameOrderByFechaEntregaDescIdDesc(String username);
}
