package com.safework.safework.repository;

import com.safework.safework.model.CapacitacionParticipante;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface CapacitacionParticipanteRepository extends JpaRepository<CapacitacionParticipante, Long> {
    List<CapacitacionParticipante> findByCapacitacionIdOrderByTrabajadorApellidosAscTrabajadorNombresAsc(Long id);
    @Query("""
        SELECT p FROM CapacitacionParticipante p JOIN FETCH p.capacitacion c JOIN FETCH c.area
        WHERE p.trabajador.usuario.username = :username ORDER BY c.fechaHora DESC, c.id DESC
        """)
    List<CapacitacionParticipante> propias(String username);
}
