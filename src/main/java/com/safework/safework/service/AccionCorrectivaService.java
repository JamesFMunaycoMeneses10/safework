package com.safework.safework.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.model.Incidente;
import com.safework.safework.model.Riesgo;
import com.safework.safework.model.Trabajador;
import com.safework.safework.repository.AccionCorrectivaRepository;
import com.safework.safework.repository.IncidenteRepository;
import com.safework.safework.repository.RiesgoRepository;
import com.safework.safework.repository.TrabajadorRepository;

/** Reglas de seguimiento de acciones correctivas. */
@Service
public class AccionCorrectivaService {

    private static final Set<String> ESTADOS = Set.of(
            "Pendiente", "En proceso", "Completada", "Cancelada");
    private static final Set<String> PRIORIDADES = Set.of(
            "Baja", "Media", "Alta", "Crítica");

    private final AccionCorrectivaRepository accionRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final RiesgoRepository riesgoRepository;
    private final IncidenteRepository incidenteRepository;
    private final ArchivoAdjuntoService archivos;

    public AccionCorrectivaService(AccionCorrectivaRepository accionRepository,
            TrabajadorRepository trabajadorRepository,
            RiesgoRepository riesgoRepository,
            IncidenteRepository incidenteRepository, ArchivoAdjuntoService archivos) {
        this.accionRepository = accionRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.riesgoRepository = riesgoRepository;
        this.incidenteRepository = incidenteRepository;
        this.archivos = archivos;
    }

    public List<AccionCorrectiva> listarTodas() {
        return accionRepository.findAll();
    }

    public Optional<AccionCorrectiva> buscarPorId(Long id) {
        return accionRepository.findById(id);
    }

    public AccionCorrectiva guardar(AccionCorrectiva datos) {
        return guardar(datos, null);
    }

    @Transactional
    public AccionCorrectiva guardar(AccionCorrectiva datos, MultipartFile adjunto) {
        if (datos == null) {
            throw new IllegalArgumentException("La acción correctiva es obligatoria");
        }

        AccionCorrectiva accion = datos.getId() == null ? new AccionCorrectiva()
                : accionRepository.findById(datos.getId()).orElseThrow(
                        () -> new IllegalArgumentException("Acción correctiva no encontrada"));

        LocalDate fechaRegistro = accion.getId() == null ? LocalDate.now() : accion.getFechaRegistro();
        if (datos.getFechaLimite() == null || datos.getFechaLimite().isBefore(fechaRegistro)) {
            throw new IllegalArgumentException("La fecha límite no puede ser anterior a la fecha de registro");
        }
        if (datos.getDescripcion() == null || datos.getDescripcion().isBlank()
                || datos.getDescripcion().length() > 255) {
            throw new IllegalArgumentException("Ingrese una descripción de hasta 255 caracteres");
        }
        if (!ESTADOS.contains(datos.getEstado()) || !PRIORIDADES.contains(datos.getPrioridad())) {
            throw new IllegalArgumentException("Seleccione un estado y una prioridad válidos");
        }
        if (accion.getId() != null
                && ("Completada".equals(accion.getEstado()) || "Cancelada".equals(accion.getEstado()))
                && !accion.getEstado().equals(datos.getEstado())) {
            throw new IllegalArgumentException("Una acción cerrada no puede cambiar de estado");
        }

        Trabajador responsable = buscarResponsable(datos);
        Riesgo riesgo = buscarRiesgo(datos);
        Incidente incidente = buscarIncidente(datos);
        if (riesgo == null && incidente == null) {
            throw new IllegalArgumentException("Asocie la acción a un riesgo o a un incidente");
        }
        if (riesgo != null && incidente != null
                && (riesgo.getArea() == null || incidente.getArea() == null
                    || !riesgo.getArea().getId().equals(incidente.getArea().getId()))) {
            throw new IllegalArgumentException("El riesgo y el incidente deben pertenecer a la misma área");
        }

        String evidencia = datos.getEvidenciaCierre() == null ? "" : datos.getEvidenciaCierre().trim();
        if (evidencia.length() > 1000) {
            throw new IllegalArgumentException("La evidencia no puede superar los 1000 caracteres");
        }
        if ("Completada".equals(datos.getEstado()) && evidencia.isBlank()) {
            throw new IllegalArgumentException("Registre la evidencia antes de completar la acción");
        }
        boolean nuevoArchivo = adjunto != null && !adjunto.isEmpty();
        if ("Completada".equals(datos.getEstado()) && !nuevoArchivo && accion.getArchivoEvidencia() == null) {
            throw new IllegalArgumentException("Adjunte una foto o PDF para completar la acción");
        }
        if (nuevoArchivo) archivos.validar(adjunto, true);

        accion.setDescripcion(datos.getDescripcion().trim());
        accion.setFechaRegistro(fechaRegistro);
        accion.setFechaLimite(datos.getFechaLimite());
        accion.setEstado(datos.getEstado());
        accion.setPrioridad(datos.getPrioridad());
        accion.setResponsable(responsable);
        accion.setRiesgo(riesgo);
        accion.setIncidente(incidente);
        accion.setEvidenciaCierre(evidencia.isBlank() ? null : evidencia);
        accion.setFechaCierre("Completada".equals(datos.getEstado())
                ? (accion.getFechaCierre() == null ? LocalDate.now() : accion.getFechaCierre())
                : null);

        String anterior = accion.getArchivoEvidencia();
        ArchivoAdjuntoService.ArchivoGuardado guardado = null;
        try {
            if (nuevoArchivo) {
                guardado = archivos.guardar(adjunto, true);
                archivos.borrarSiHayRollback(guardado.nombre());
                accion.setArchivoEvidencia(guardado.nombre());
                accion.setNombreEvidencia(guardado.nombreOriginal());
                accion.setTipoEvidencia(guardado.tipoContenido());
            }
            AccionCorrectiva resultado = accionRepository.save(accion);
            if (nuevoArchivo) archivos.borrarDespuesDeConfirmar(anterior);
            return resultado;
        } catch (RuntimeException e) {
            if (guardado != null) archivos.borrarSiExiste(guardado.nombre());
            throw e;
        }
    }

    private Trabajador buscarResponsable(AccionCorrectiva datos) {
        if (datos.getResponsable() == null || datos.getResponsable().getId() == null) {
            throw new IllegalArgumentException("Seleccione un responsable para la acción");
        }
        return trabajadorRepository.findById(datos.getResponsable().getId()).orElseThrow(
                () -> new IllegalArgumentException("El responsable seleccionado no existe"));
    }

    private Riesgo buscarRiesgo(AccionCorrectiva datos) {
        if (datos.getRiesgo() == null || datos.getRiesgo().getId() == null) return null;
        return riesgoRepository.findById(datos.getRiesgo().getId()).orElseThrow(
                () -> new IllegalArgumentException("El riesgo seleccionado no existe"));
    }

    private Incidente buscarIncidente(AccionCorrectiva datos) {
        if (datos.getIncidente() == null || datos.getIncidente().getId() == null) return null;
        return incidenteRepository.findById(datos.getIncidente().getId()).orElseThrow(
                () -> new IllegalArgumentException("El incidente seleccionado no existe"));
    }

    @Transactional
    public void eliminarPorId(Long id) {
        AccionCorrectiva accion = accionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Acción correctiva no encontrada"));
        accionRepository.delete(accion);
        archivos.borrarDespuesDeConfirmar(accion.getArchivoEvidencia());
    }
}
