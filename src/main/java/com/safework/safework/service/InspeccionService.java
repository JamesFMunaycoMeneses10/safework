package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.safework.safework.model.Inspeccion;
import com.safework.safework.repository.HallazgoInspeccionRepository;
import com.safework.safework.repository.InspeccionRepository;
import com.safework.safework.repository.AreaRepository;
import com.safework.safework.repository.TrabajadorRepository;

@Service
public class InspeccionService {

    private final InspeccionRepository inspeccionRepository;
    private final HallazgoInspeccionRepository hallazgoRepository;
    private final AreaRepository areaRepository;
    private final TrabajadorRepository trabajadorRepository;

    @Autowired
    public InspeccionService(
            InspeccionRepository inspeccionRepository,
            HallazgoInspeccionRepository hallazgoRepository,
            AreaRepository areaRepository, TrabajadorRepository trabajadorRepository) {

        this.inspeccionRepository = inspeccionRepository;
        this.hallazgoRepository = hallazgoRepository;
        this.areaRepository = areaRepository;
        this.trabajadorRepository = trabajadorRepository;
    }

    /** Compatibilidad para las pruebas de consulta y eliminación anteriores. */
    public InspeccionService(InspeccionRepository inspecciones,
            HallazgoInspeccionRepository hallazgos) {
        this(inspecciones, hallazgos, null, null);
    }

    public List<Inspeccion> listarTodas() {
        return inspeccionRepository.findAll();
    }

    @Transactional
    public Inspeccion guardar(Inspeccion datos) {
        if (areaRepository == null || trabajadorRepository == null) {
            throw new IllegalStateException("La validación de relaciones de la empresa no está configurada");
        }
        Inspeccion inspeccion = datos.getId() == null ? new Inspeccion()
                : inspeccionRepository.findById(datos.getId()).orElseThrow(
                        () -> new IllegalArgumentException("Inspección no encontrada"));
        if (datos.getArea() == null || datos.getArea().getId() == null
                || datos.getResponsable() == null || datos.getResponsable().getId() == null) {
            throw new IllegalArgumentException("Seleccione un área y un responsable de su empresa");
        }
        var area = areaRepository.findById(datos.getArea().getId()).orElseThrow(
                () -> new IllegalArgumentException("El área seleccionada no está disponible"));
        var responsable = trabajadorRepository.findById(datos.getResponsable().getId()).orElseThrow(
                () -> new IllegalArgumentException("El responsable seleccionado no está disponible"));
        inspeccion.setFecha(datos.getFecha());
        inspeccion.setTipo(datos.getTipo());
        inspeccion.setArea(area);
        inspeccion.setResponsable(responsable);
        inspeccion.setEstado(datos.getEstado());
        inspeccion.setObservaciones(datos.getObservaciones());
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
