package com.safework.safework.repository;

import com.safework.safework.model.Capacitacion;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CapacitacionRepository extends JpaRepository<Capacitacion, Long> {
    @Query("""
        SELECT c FROM Capacitacion c JOIN FETCH c.area
        WHERE (:estado = '' OR c.estado = :estado)
          AND (:area IS NULL OR c.area.id = :area)
          AND (LOWER(c.tema) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.instructor) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Capacitacion> buscar(@Param("q") String q, @Param("estado") String estado,
            @Param("area") Long area, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Capacitacion c WHERE c.id = :id")
    Optional<Capacitacion> bloquear(@Param("id") Long id);
}
