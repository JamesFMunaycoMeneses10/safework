package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safework.safework.model.Riesgo;
import com.safework.safework.repository.HallazgoInspeccionRepository;
import com.safework.safework.repository.RiesgoRepository;

@Service
public class RiesgoService {

    private final RiesgoRepository riesgoRepository;
    private final HallazgoInspeccionRepository hallazgoRepository;

    public RiesgoService(
            RiesgoRepository riesgoRepository,
            HallazgoInspeccionRepository hallazgoRepository) {

        this.riesgoRepository = riesgoRepository;
        this.hallazgoRepository = hallazgoRepository;
    }

    public List<Riesgo> listarTodos() {
        return riesgoRepository.findAll();
    }

    public Optional<Riesgo> buscarPorId(Long id) {
        return riesgoRepository.findById(id);
    }

    public Riesgo guardar(Riesgo riesgo) {
        if (riesgo.getProbabilidad() != null
                && riesgo.getSeveridad() != null) {

            int nivelRiesgo =
                    riesgo.getProbabilidad() * riesgo.getSeveridad();

            riesgo.setNivelRiesgo(nivelRiesgo);
        }

        return riesgoRepository.save(riesgo);
    }

    @Transactional
    public void eliminarPorId(Long id) {
        Riesgo riesgo = riesgoRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Riesgo no encontrado"));

        if (hallazgoRepository.existsByRiesgoId(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar el riesgo porque está vinculado a un hallazgo de inspección");
        }

        if (riesgo.getAccionesCorrectivas() != null
                && !riesgo.getAccionesCorrectivas().isEmpty()) {
            throw new IllegalStateException(
                    "No se puede eliminar el riesgo porque tiene acciones correctivas asociadas");
        }

        try {
            riesgoRepository.delete(riesgo);
            riesgoRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar el riesgo porque tiene registros asociados", e);
        }
    }
}