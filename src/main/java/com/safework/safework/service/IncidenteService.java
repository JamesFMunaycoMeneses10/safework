package com.safework.safework.service;


import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.safework.safework.model.Incidente;
import com.safework.safework.repository.IncidenteRepository;
import com.safework.safework.repository.AreaRepository;
import com.safework.safework.repository.TrabajadorRepository;
import java.time.format.DateTimeFormatter;


/**
 * Servicio encargado de la lógica
 * relacionada con los incidentes
 * y accidentes registrados en SafeWork.
 */
@Service
public class IncidenteService {


    /**
     * Repositorio utilizado para acceder
     * a los datos de los incidentes.
     */
    private final IncidenteRepository incidenteRepository;
    private final AuditoriaService auditoria;
    private final AreaRepository areaRepository;
    private final TrabajadorRepository trabajadorRepository;



    /**
     * Inyección de dependencias
     * mediante constructor.
     */
    @Autowired
    public IncidenteService(IncidenteRepository incidenteRepository, AuditoriaService auditoria,
            AreaRepository areaRepository, TrabajadorRepository trabajadorRepository) {

        this.incidenteRepository = incidenteRepository;
        this.auditoria = auditoria;
        this.areaRepository = areaRepository;
        this.trabajadorRepository = trabajadorRepository;
    }

    /** Constructor de compatibilidad para pruebas unitarias existentes. */
    public IncidenteService(IncidenteRepository incidenteRepository) {
        this(incidenteRepository, null, null, null);
    }

    public IncidenteService(IncidenteRepository incidenteRepository, AuditoriaService auditoria) {
        this(incidenteRepository, auditoria, null, null);
    }



    /**
     * Obtiene todos los incidentes
     * registrados en el sistema.
     */
    public List<Incidente> listarTodos() {

        return incidenteRepository.findAll();
    }



    /**
     * Guarda un incidente nuevo
     * o actualiza uno existente.
     */
    @Transactional
    public Incidente guardar(Incidente datos, String usuario) {
        if (areaRepository == null || trabajadorRepository == null) {
            throw new IllegalStateException("La validación de relaciones de la empresa no está configurada");
        }
        Incidente anterior = datos.getId() == null ? null : incidenteRepository.findById(datos.getId())
                .orElseThrow(() -> new IllegalArgumentException("Incidente no encontrado"));
        String resumenAnterior = anterior == null ? null : resumenAuditoria(anterior);
        if (datos.getArea() == null || datos.getArea().getId() == null
                || datos.getTrabajador() == null || datos.getTrabajador().getId() == null) {
            throw new IllegalArgumentException("Seleccione un área y un trabajador de su empresa");
        }
        var area = areaRepository.findById(datos.getArea().getId()).orElseThrow(
                () -> new IllegalArgumentException("El área seleccionada no está disponible"));
        var trabajador = trabajadorRepository.findById(datos.getTrabajador().getId()).orElseThrow(
                () -> new IllegalArgumentException("El trabajador seleccionado no está disponible"));
        Incidente incidente = anterior == null ? new Incidente() : anterior;
        incidente.setFechaHora(datos.getFechaHora());
        incidente.setTipo(datos.getTipo());
        incidente.setArea(area);
        incidente.setTrabajador(trabajador);
        incidente.setDescripcion(datos.getDescripcion());
        incidente.setGravedad(datos.getGravedad());
        incidente.setEstado(datos.getEstado());
        Incidente guardado = incidenteRepository.save(incidente);
        if (auditoria != null) {
            String detalle = anterior == null ? resumenAuditoria(guardado)
                    : "Antes: " + resumenAnterior + " | Después: " + resumenAuditoria(guardado);
            auditoria.registrar("INCIDENTES", guardado.getId(),
                    anterior == null ? "CREADO" : "ACTUALIZADO", usuario, detalle);
        }
        return guardado;
    }

    public Incidente guardar(Incidente incidente) {
        return guardar(incidente, "Sistema");
    }



    /**
     * Busca un incidente mediante su ID.
     */
    public Optional<Incidente> buscarPorId(Long id) {

        return incidenteRepository.findById(id);
    }



    /**
     * Elimina un incidente mediante su ID.
     *
     * No permite eliminar si existen
     * acciones correctivas asociadas.
     */
    @Transactional
    public void eliminar(Long id) {
        eliminar(id, "Sistema");
    }

    @Transactional
    public void eliminar(Long id, String usuario) {


        Incidente incidente = incidenteRepository.findById(id)

                .orElseThrow(() ->
                        new RuntimeException(
                                "Incidente no encontrado"));



        /*
         * Validamos si el incidente
         * tiene acciones correctivas.
         */
        if (incidente.getAccionesCorrectivas() != null
                && !incidente.getAccionesCorrectivas().isEmpty()) {


            throw new RuntimeException(
                    "No se puede eliminar el incidente porque tiene acciones correctivas asociadas"
            );
        }



        try {
            String resumen = resumenAuditoria(incidente);
            incidenteRepository.delete(incidente);
            incidenteRepository.flush();
            if (auditoria != null) {
                auditoria.registrar("INCIDENTES", id, "ELIMINADO", usuario, resumen);
            }

        } catch (DataIntegrityViolationException e) {


            throw new RuntimeException(
                    "No se puede eliminar el incidente porque tiene registros asociados"
            );

        }

    }

    private String resumenAuditoria(Incidente incidente) {
        String area = incidente.getArea() == null ? "Sin área" : incidente.getArea().getNombre();
        String trabajador = incidente.getTrabajador() == null ? "Sin trabajador"
                : incidente.getTrabajador().getNombres() + " " + incidente.getTrabajador().getApellidos();
        String fecha = incidente.getFechaHora() == null ? "Sin fecha"
                : incidente.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return "Fecha y hora: " + fecha
                + " | Tipo: " + incidente.getTipo()
                + " | Área: " + area
                + " | Trabajador: " + trabajador
                + " | Descripción: " + incidente.getDescripcion()
                + " | Gravedad: " + incidente.getGravedad()
                + " | Estado: " + incidente.getEstado();
    }

}
