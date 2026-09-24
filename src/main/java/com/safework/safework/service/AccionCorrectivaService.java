package com.safework.safework.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.model.AccionCorrectivaEvento;
import com.safework.safework.model.Incidente;
import com.safework.safework.model.Riesgo;
import com.safework.safework.model.Trabajador;
import com.safework.safework.model.Usuario;
import com.safework.safework.repository.AccionCorrectivaRepository;
import com.safework.safework.repository.AccionCorrectivaEventoRepository;
import com.safework.safework.repository.IncidenteRepository;
import com.safework.safework.repository.RiesgoRepository;
import com.safework.safework.repository.TrabajadorRepository;
import com.safework.safework.repository.UsuarioRepository;

/** Reglas de seguimiento de acciones correctivas. */
@Service
public class AccionCorrectivaService {

    private static final Set<String> ESTADOS = Set.of(
            "Pendiente", "En proceso", "Cancelada");
    private static final Set<String> PRIORIDADES = Set.of(
            "Baja", "Media", "Alta", "Crítica");

    private final AccionCorrectivaRepository accionRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final RiesgoRepository riesgoRepository;
    private final IncidenteRepository incidenteRepository;
    private final ArchivoAdjuntoService archivos;
    private final UsuarioRepository usuarios;
    private final AccionCorrectivaEventoRepository eventos;
    private final AuditoriaService auditoria;

    public AccionCorrectivaService(AccionCorrectivaRepository accionRepository,
            TrabajadorRepository trabajadorRepository,
            RiesgoRepository riesgoRepository,
            IncidenteRepository incidenteRepository, ArchivoAdjuntoService archivos,
            UsuarioRepository usuarios, AccionCorrectivaEventoRepository eventos,
            AuditoriaService auditoria) {
        this.accionRepository = accionRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.riesgoRepository = riesgoRepository;
        this.incidenteRepository = incidenteRepository;
        this.archivos = archivos;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.auditoria = auditoria;
    }

    public List<AccionCorrectiva> listarTodas() {
        return accionRepository.findAll();
    }

    public Page<AccionCorrectiva> buscar(Long riesgoId, Long incidenteId, String texto,
            String vista, LocalDate hoy, int pagina) {
        return accionRepository.buscar(riesgoId, incidenteId, texto == null ? "" : texto.trim(),
                vista, hoy, PageRequest.of(pagina, 20, Sort.by(Sort.Order.desc("fechaRegistro"),
                        Sort.Order.desc("id"))));
    }

    public BandejaAcciones.Resumen resumen(Long riesgoId, Long incidenteId, LocalDate hoy) {
        return new BandejaAcciones.Resumen(
                accionRepository.contar(riesgoId, incidenteId, "todas", hoy),
                accionRepository.contar(riesgoId, incidenteId, "revision", hoy),
                accionRepository.contar(riesgoId, incidenteId, "devueltas", hoy),
                accionRepository.contar(riesgoId, incidenteId, "vencidas", hoy));
    }

    public List<AccionCorrectiva> proximas(Long riesgoId, Long incidenteId, LocalDate hoy) {
        return accionRepository.proximas(riesgoId, incidenteId, hoy,
                hoy.plusDays(BandejaAcciones.DIAS_AVISO));
    }

    public Optional<AccionCorrectiva> buscarPorId(Long id) {
        return accionRepository.findById(id);
    }

    @Transactional
    public List<AccionCorrectivaEvento> historial(Long accionId) {
        AccionCorrectiva accion = accionRepository.findById(accionId).orElseThrow(
                () -> new IllegalArgumentException("Acción correctiva no encontrada"));
        registrarHistorialPrevioSiFalta(accion);
        return eventos.findByAccionIdOrderByFechaAscIdAsc(accionId);
    }

    public Optional<AccionCorrectivaEvento> buscarEvento(Long accionId, Long eventoId) {
        return eventos.findByIdAndAccionId(eventoId, accionId);
    }

    public AccionCorrectiva guardar(AccionCorrectiva datos) {
        return guardar(datos, null);
    }

    @Transactional
    public AccionCorrectiva guardar(AccionCorrectiva datos, MultipartFile adjunto) {
        return guardar(datos, adjunto, "Sistema");
    }

    @Transactional
    public AccionCorrectiva guardar(AccionCorrectiva datos, MultipartFile adjunto, String usuario) {
        if (datos == null) {
            throw new IllegalArgumentException("La acción correctiva es obligatoria");
        }

        AccionCorrectiva accion = datos.getId() == null ? new AccionCorrectiva()
                : accionRepository.findById(datos.getId()).orElseThrow(
                        () -> new IllegalArgumentException("Acción correctiva no encontrada"));
        boolean nueva = accion.getId() == null;
        String resumenAnterior = nueva ? null : resumenAuditoria(accion);

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
        if (accion.getId() != null && ("En revisión".equals(accion.getEstado())
                || "Devuelta".equals(accion.getEstado())
                || "Cerrada".equals(accion.getEstado()) || "Completada".equals(accion.getEstado())
                || "Cancelada".equals(accion.getEstado()))) {
            throw new IllegalArgumentException("Esta acción no puede modificarse desde el formulario general");
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
        boolean nuevoArchivo = adjunto != null && !adjunto.isEmpty();
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
        accion.setFechaCierre(null);

        String anterior = accion.getArchivoEvidencia();
        if (nuevoArchivo) {
            var guardado = archivos.guardar(adjunto, true);
            accion.setArchivoEvidencia(guardado.nombre());
            accion.setNombreEvidencia(guardado.nombreOriginal());
            accion.setTipoEvidencia(guardado.tipoContenido());
        }
        AccionCorrectiva resultado = accionRepository.save(accion);
        if (auditoria != null) {
            auditoria.registrar("ACCIONES CORRECTIVAS", resultado.getId(),
                    nueva ? "CREADO" : "ACTUALIZADO", usuario,
                    nueva ? resumenAuditoria(resultado)
                            : "Antes: " + resumenAnterior + " | Después: " + resumenAuditoria(resultado));
        }
        if (nuevoArchivo) archivos.borrarDespuesDeConfirmar(anterior);
        return resultado;
    }

    @Transactional
    public AccionCorrectiva entregar(Long id, String username, String evidencia, MultipartFile adjunto) {
        AccionCorrectiva accion = accionRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Acción correctiva no encontrada"));
        if (!Set.of("Pendiente", "En proceso", "Devuelta").contains(accion.getEstado())) {
            throw new IllegalArgumentException("La acción no admite una nueva entrega");
        }
        Usuario usuario = usuarios.findByUsername(username).orElseThrow(
                () -> new IllegalArgumentException("Usuario no encontrado"));
        Trabajador responsable = trabajadorRepository.findByUsuarioUsername(username).orElseThrow(
                () -> new IllegalArgumentException("Tu cuenta no está vinculada a un trabajador"));
        if (accion.getResponsable() == null || !responsable.getId().equals(accion.getResponsable().getId())) {
            throw new IllegalArgumentException("Solo el responsable asignado puede entregar la evidencia");
        }
        String texto = evidencia == null ? "" : evidencia.trim();
        if (texto.isBlank() || texto.length() > 1000) {
            throw new IllegalArgumentException("Describe el cumplimiento en un máximo de 1000 caracteres");
        }
        boolean nuevoArchivo = adjunto != null && !adjunto.isEmpty();
        if (!nuevoArchivo && accion.getArchivoEvidencia() == null) {
            throw new IllegalArgumentException("Adjunta una foto o PDF de evidencia");
        }
        if (nuevoArchivo) archivos.validar(adjunto, true);
        registrarHistorialPrevioSiFalta(accion);
        if (nuevoArchivo) {
            var guardado = archivos.guardar(adjunto, true);
            accion.setArchivoEvidencia(guardado.nombre());
            accion.setNombreEvidencia(guardado.nombreOriginal());
            accion.setTipoEvidencia(guardado.tipoContenido());
        }
        accion.setEvidenciaCierre(texto);
        accion.setEstado("En revisión");
        accion.setEnviadaPor(usuario);
        accion.setFechaEnvioRevision(LocalDateTime.now());
        accion.setRevisadaPor(null);
        accion.setFechaRevision(null);
        accion.setObservacionRevision(null);
        AccionCorrectiva resultado = accionRepository.save(accion);
        eventos.save(new AccionCorrectivaEvento(resultado, "ENTREGA", usuario.getUsername(),
                resultado.getFechaEnvioRevision(), texto, resultado.getArchivoEvidencia(),
                resultado.getNombreEvidencia(), resultado.getTipoEvidencia()));
        return resultado;
    }

    @Transactional
    public AccionCorrectiva revisar(Long id, String username, boolean aprobar, String observacion) {
        AccionCorrectiva accion = accionRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Acción correctiva no encontrada"));
        if (!"En revisión".equals(accion.getEstado())) {
            throw new IllegalArgumentException("La acción no está pendiente de revisión");
        }
        Usuario revisor = usuarios.findByUsername(username).orElseThrow(
                () -> new IllegalArgumentException("Usuario no encontrado"));
        if (!Set.of("ADMIN", "SUPERVISOR").contains(revisor.getRol())) {
            throw new IllegalArgumentException("Solo un supervisor SST o administrador puede revisar");
        }
        if (accion.getEnviadaPor() != null && revisor.getId().equals(accion.getEnviadaPor().getId())) {
            throw new IllegalArgumentException("No puedes validar tu propia entrega");
        }
        String nota = observacion == null ? "" : observacion.trim();
        if (nota.length() > 1000 || (!aprobar && nota.isBlank())) {
            throw new IllegalArgumentException("Al devolver, indica una observación de hasta 1000 caracteres");
        }
        registrarHistorialPrevioSiFalta(accion);
        accion.setEstado(aprobar ? "Cerrada" : "Devuelta");
        accion.setFechaCierre(aprobar ? LocalDate.now() : null);
        accion.setRevisadaPor(revisor);
        accion.setFechaRevision(LocalDateTime.now());
        accion.setObservacionRevision(nota.isBlank() ? null : nota);
        AccionCorrectiva resultado = accionRepository.save(accion);
        eventos.save(new AccionCorrectivaEvento(resultado,
                aprobar ? "APROBACION" : "DEVOLUCION", revisor.getUsername(),
                resultado.getFechaRevision(), resultado.getObservacionRevision(), null, null, null));
        return resultado;
    }

    /** Conserva la última entrega del flujo anterior antes de que una nueva la reemplace. */
    private void registrarHistorialPrevioSiFalta(AccionCorrectiva accion) {
        if (accion.getId() == null || eventos.existsByAccionId(accion.getId())) return;
        if (accion.getEnviadaPor() != null && accion.getFechaEnvioRevision() != null) {
            eventos.save(new AccionCorrectivaEvento(accion, "ENTREGA",
                    accion.getEnviadaPor().getUsername(), accion.getFechaEnvioRevision(),
                    accion.getEvidenciaCierre(), accion.getArchivoEvidencia(),
                    accion.getNombreEvidencia(), accion.getTipoEvidencia()));
        }
        if (accion.getRevisadaPor() != null && accion.getFechaRevision() != null
                && ("Devuelta".equals(accion.getEstado()) || "Cerrada".equals(accion.getEstado()))) {
            eventos.save(new AccionCorrectivaEvento(accion,
                    "Cerrada".equals(accion.getEstado()) ? "APROBACION" : "DEVOLUCION",
                    accion.getRevisadaPor().getUsername(), accion.getFechaRevision(),
                    accion.getObservacionRevision(), null, null, null));
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
        eliminarPorId(id, "Sistema");
    }

    @Transactional
    public void eliminarPorId(Long id, String usuario) {
        AccionCorrectiva accion = accionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Acción correctiva no encontrada"));
        if ("En revisión".equals(accion.getEstado()) || "Devuelta".equals(accion.getEstado())
                || "Cerrada".equals(accion.getEstado()) || "Completada".equals(accion.getEstado())) {
            throw new IllegalArgumentException("No se puede eliminar una acción con historial de revisión");
        }
        String resumen = resumenAuditoria(accion);
        accionRepository.delete(accion);
        accionRepository.flush();
        if (auditoria != null) {
            auditoria.registrar("ACCIONES CORRECTIVAS", id, "ELIMINADO", usuario, resumen);
        }
        archivos.borrarDespuesDeConfirmar(accion.getArchivoEvidencia());
    }

    private String resumenAuditoria(AccionCorrectiva accion) {
        String responsable = accion.getResponsable() == null ? "Sin responsable"
                : accion.getResponsable().getNombres() + " " + accion.getResponsable().getApellidos();
        String riesgo = accion.getRiesgo() == null ? "—" : "#" + accion.getRiesgo().getId();
        String incidente = accion.getIncidente() == null ? "—" : "#" + accion.getIncidente().getId();
        return "Descripción: " + accion.getDescripcion()
                + " | Responsable: " + responsable.trim()
                + " | Riesgo: " + riesgo
                + " | Incidente: " + incidente
                + " | Prioridad: " + accion.getPrioridad()
                + " | Estado: " + accion.getEstado()
                + " | Fecha límite: " + accion.getFechaLimite();
    }
}
