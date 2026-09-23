package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safework.safework.model.Inspeccion;
import com.safework.safework.repository.HallazgoInspeccionRepository;
import com.safework.safework.repository.InspeccionRepository;

@Service
public class InspeccionService {

    private final InspeccionRepository inspeccionRepository;
    private final HallazgoInspeccionRepository hallazgoRepository;

    public InspeccionService(
            InspeccionRepository inspeccionRepository,
            HallazgoInspeccionRepository hallazgoRepository) {

        this.inspeccionRepository = inspeccionRepository;
        this.hallazgoRepository = hallazgoRepository;
    }

    public List<Inspeccion> listarTodas() {
        return inspeccionRepository.findAll();
    }

    public Inspeccion guardar(Inspeccion inspeccion) {
        return inspeccionRepository.save(inspeccion);
    }

    public Optional<Inspeccion> buscarPorId(Long id) {
        return inspeccionRepository.findById(id);
    }

    @Transactional
    public void eliminarPorId(Long id) {
        if (!inspeccionRepository.existsById(id)) {
            throw new IllegalArgumentException("La inspección no existe");
        }

        if (hallazgoRepository.existsByInspeccionId(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar la inspección porque tiene hallazgos registrados");
        }

        try {
            inspeccionRepository.deleteById(id);
            inspeccionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar la inspección porque tiene registros asociados", e);
        }
    }
}