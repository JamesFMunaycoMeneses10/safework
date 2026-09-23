package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.safework.safework.model.Riesgo;
import com.safework.safework.repository.HallazgoInspeccionRepository;
import com.safework.safework.repository.RiesgoRepository;

@Service
public class RiesgoService {

    private final RiesgoRepository riesgoRepository;
    private final HallazgoInspeccionRepository hallazgoRepository;
    private final ArchivoAdjuntoService archivos;

    public RiesgoService(
            RiesgoRepository riesgoRepository,
            HallazgoInspeccionRepository hallazgoRepository, ArchivoAdjuntoService archivos) {

        this.riesgoRepository = riesgoRepository;
        this.hallazgoRepository = hallazgoRepository;
        this.archivos = archivos;
    }

    public List<Riesgo> listarTodos() {
        return riesgoRepository.findAll();
    }

    public Optional<Riesgo> buscarPorId(Long id) {
        return riesgoRepository.findById(id);
    }

    public Riesgo guardar(Riesgo riesgo) {
        return guardar(riesgo, null);
    }

    @Transactional
    public Riesgo guardar(Riesgo riesgo, MultipartFile foto) {
        Riesgo anterior = riesgo.getId() == null ? null : riesgoRepository.findById(riesgo.getId())
                .orElseThrow(() -> new IllegalArgumentException("Riesgo no encontrado"));
        boolean nuevaFoto = foto != null && !foto.isEmpty();
        if (nuevaFoto) archivos.validar(foto, false);
        if (riesgo.getProbabilidad() != null
                && riesgo.getSeveridad() != null) {

            int nivelRiesgo =
                    riesgo.getProbabilidad() * riesgo.getSeveridad();

            riesgo.setNivelRiesgo(nivelRiesgo);
        }

        if (anterior != null) {
            riesgo.setFotoArchivo(anterior.getFotoArchivo());
            riesgo.setFotoNombre(anterior.getFotoNombre());
            riesgo.setFotoTipo(anterior.getFotoTipo());
        }
        ArchivoAdjuntoService.ArchivoGuardado guardado = null;
        try {
            if (nuevaFoto) {
                guardado = archivos.guardar(foto, false);
                archivos.borrarSiHayRollback(guardado.nombre());
                riesgo.setFotoArchivo(guardado.nombre());
                riesgo.setFotoNombre(guardado.nombreOriginal());
                riesgo.setFotoTipo(guardado.tipoContenido());
            }
            Riesgo resultado = riesgoRepository.save(riesgo);
            if (nuevaFoto && anterior != null) archivos.borrarDespuesDeConfirmar(anterior.getFotoArchivo());
            return resultado;
        } catch (RuntimeException e) {
            if (guardado != null) archivos.borrarSiExiste(guardado.nombre());
            throw e;
        }
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
            archivos.borrarDespuesDeConfirmar(riesgo.getFotoArchivo());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar el riesgo porque tiene registros asociados", e);
        }
    }
}
