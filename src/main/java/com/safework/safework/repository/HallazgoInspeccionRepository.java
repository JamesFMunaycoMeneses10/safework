package com.safework.safework.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.safework.safework.model.HallazgoInspeccion;

public interface HallazgoInspeccionRepository
        extends JpaRepository<HallazgoInspeccion, Long> {

    List<HallazgoInspeccion> findByInspeccionIdOrderByIdAsc(Long inspeccionId);

    boolean existsByInspeccionId(Long inspeccionId);

    boolean existsByRiesgoId(Long riesgoId);
}