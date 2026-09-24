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
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class RiesgoService {

    private final RiesgoRepository riesgoRepository;
    private final HallazgoInspeccionRepository hallazgoRepository;
    private final ArchivoAdjuntoService archivos;
    private final AuditoriaService auditoria;

    @Autowired
    public RiesgoService(
            RiesgoRepository riesgoRepository,
            HallazgoInspeccionRepository hallazgoRepository, ArchivoAdjuntoService archivos,
            AuditoriaService auditoria) {

        this.riesgoRepository = riesgoRepository;
        this.hallazgoRepository = hallazgoRepository;
        this.archivos = archivos;
        this.auditoria = auditoria;
    }

    /** Constructor de compatibilidad para pruebas unitarias antiguas. */
    public RiesgoService(RiesgoRepository riesgoRepository,
            HallazgoInspeccionRepository hallazgoRepository, ArchivoAdjuntoService archivos) {
        this(riesgoRepository, hallazgoRepository, archivos, null);
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
        return guardar(riesgo, foto, "Sistema");
    }

    @Transactional
    public Riesgo guardar(Riesgo riesgo, MultipartFile foto, String usuario) {
        Riesgo anterior = riesgo.getId() == null ? null : riesgoRepository.findById(riesgo.getId())
                .orElseThrow(() -> new IllegalArgumentException("Riesgo no encontrado"));
        String resumenAnterior = anterior == null ? null : resumenAuditoria(anterior);
        // Con JPA save() puede hacer merge sobre la entidad administrada "anterior".
        // Conservar el nombre previo antes de guardar evita borrar la foto recién subida.
        String fotoAnterior = anterior == null ? null : anterior.getFotoArchivo();
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
        if (nuevaFoto) {
            var guardado = archivos.guardar(foto, false);
            riesgo.setFotoArchivo(guardado.nombre());
            riesgo.setFotoNombre(guardado.nombreOriginal());
            riesgo.setFotoTipo(guardado.tipoContenido());
        }
        Riesgo resultado = riesgoRepository.save(riesgo);
        if (auditoria != null) {
            String operacion = anterior == null ? "CREADO" : "ACTUALIZADO";
            String detalle = anterior == null ? resumenAuditoria(resultado)
                    : "Antes: " + resumenAnterior + " | Después: " + resumenAuditoria(resultado);
            auditoria.registrar("RIESGOS", resultado.getId(), operacion, usuario, detalle);
        }
        if (nuevaFoto) archivos.borrarDespuesDeConfirmar(fotoAnterior);
        return resultado;
    }

    @Transactional
    public void eliminarPorId(Long id) {
        eliminarPorId(id, "Sistema");
    }

    @Transactional
    public void eliminarPorId(Long id, String usuario) {
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
            String resumen = resumenAuditoria(riesgo);
            riesgoRepository.delete(riesgo);
            riesgoRepository.flush();
            if (auditoria != null) {
                auditoria.registrar("RIESGOS", id, "ELIMINADO", usuario, resumen);
            }
            archivos.borrarDespuesDeConfirmar(riesgo.getFotoArchivo());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar el riesgo porque tiene registros asociados", e);
        }
    }

    private String resumenAuditoria(Riesgo riesgo) {
        String area = riesgo.getArea() == null ? "Sin área" : riesgo.getArea().getNombre();
        return "Peligro: " + riesgo.getPeligro()
                + " | Descripción: " + riesgo.getDescripcion()
                + " | Área: " + area
                + " | Probabilidad: " + riesgo.getProbabilidad()
                + " | Severidad: " + riesgo.getSeveridad()
                + " | Nivel: " + riesgo.getNivelRiesgo()
                + " | Medida: " + riesgo.getMedidaControl();
    }
}
