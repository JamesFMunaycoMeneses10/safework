package com.safework.safework.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.AccionCorrectivaEvento;

public interface AccionCorrectivaEventoRepository extends JpaRepository<AccionCorrectivaEvento, Long> {
    List<AccionCorrectivaEvento> findByAccionIdOrderByFechaAscIdAsc(Long accionId);
    Optional<AccionCorrectivaEvento> findByIdAndAccionId(Long id, Long accionId);
    boolean existsByAccionId(Long accionId);
}
