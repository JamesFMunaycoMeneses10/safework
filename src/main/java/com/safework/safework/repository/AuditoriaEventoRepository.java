package com.safework.safework.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.safework.safework.model.AuditoriaEvento;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {
    @Query("""
            select e from AuditoriaEvento e
            where :texto = '' or lower(e.usuario) like lower(concat('%', :texto, '%'))
               or lower(e.modulo) like lower(concat('%', :texto, '%'))
               or lower(e.operacion) like lower(concat('%', :texto, '%'))
               or lower(e.detalle) like lower(concat('%', :texto, '%'))
            """)
    Page<AuditoriaEvento> buscar(@Param("texto") String texto, Pageable pageable);
}
