package com.safework.safework.service;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.safework.safework.model.*;
import com.safework.safework.repository.*;
import jakarta.validation.Validator;

@Service
@Transactional(readOnly = true)
public class CapacitacionService {
    private final CapacitacionRepository capacitaciones;
    private final CapacitacionParticipanteRepository participantes;
    private final AreaRepository areas;
    private final TrabajadorRepository trabajadores;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final Validator validator;
    public CapacitacionService(CapacitacionRepository capacitaciones, CapacitacionParticipanteRepository participantes,
            AreaRepository areas, TrabajadorRepository trabajadores, UsuarioRepository usuarios,
            AuditoriaService auditoria, Validator validator) {
        this.capacitaciones = capacitaciones; this.participantes = participantes; this.areas = areas;
        this.trabajadores = trabajadores; this.usuarios = usuarios; this.auditoria = auditoria; this.validator = validator;
    }
    public Page<Capacitacion> buscar(String q, String estado, Long area, int pagina) {
        if (q.length() > 150 || pagina < 0 || pagina > 100000 ||
                !Set.of("", "PROGRAMADA", "REALIZADA", "CANCELADA").contains(estado))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros no válidos");
        return capacitaciones.buscar(q.trim(), estado, area,
                PageRequest.of(pagina, 20, Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id"))));
    }
    public Capacitacion obtener(Long id) {
        return capacitaciones.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public List<CapacitacionParticipante> participantes(Long id) {
        obtener(id);
        return participantes.findByCapacitacionIdOrderByTrabajadorApellidosAscTrabajadorNombresAsc(id);
    }
    public List<CapacitacionParticipante> propias(String username) { return participantes.propias(username); }
    public CapacitacionFormulario formulario(Long id) {
        Capacitacion c = obtener(id);
        exigirProgramada(c);
        CapacitacionFormulario f = new CapacitacionFormulario();
        f.setId(c.getId()); f.setTema(c.getTema()); f.setObjetivo(c.getObjetivo()); f.setInstructor(c.getInstructor());
        f.setFechaHora(c.getFechaHora()); f.setDuracionMinutos(c.getDuracionMinutos()); f.setAreaId(c.getArea().getId());
        f.setTrabajadorIds(participantes(id).stream().map(p -> p.getTrabajador().getId()).toList());
        return f;
    }
    @Transactional
    public Capacitacion guardar(CapacitacionFormulario f, String actor) {
        if (f == null || !validator.validate(f).isEmpty()) throw new IllegalArgumentException("Completa los datos y selecciona al menos un participante (máximo 500).");
        Set<Long> ids = new LinkedHashSet<>(f.getTrabajadorIds());
        if (ids.size() != f.getTrabajadorIds().size()) throw new IllegalArgumentException("Hay participantes repetidos.");
        var area = areas.findById(f.getAreaId()).orElseThrow(() -> new IllegalArgumentException("Área no disponible."));
        List<Trabajador> personas = trabajadores.findAllById(ids);
        if (personas.size() != ids.size()) throw new IllegalArgumentException("Uno de los trabajadores no está disponible en tu empresa.");
        boolean nueva = f.getId() == null;
        Capacitacion c = nueva ? new Capacitacion() : bloquear(f.getId());
        exigirProgramada(c);
        c.setTema(f.getTema().trim()); c.setObjetivo(f.getObjetivo().trim()); c.setInstructor(f.getInstructor().trim());
        c.setFechaHora(f.getFechaHora()); c.setDuracionMinutos(f.getDuracionMinutos()); c.setArea(area);
        if (nueva) c.setRegistradoPor(usuarios.findByUsername(actor).orElseThrow(() -> new IllegalArgumentException("Cuenta no disponible.")));
        capacitaciones.saveAndFlush(c);
        List<CapacitacionParticipante> actuales = participantes.findByCapacitacionIdOrderByTrabajadorApellidosAscTrabajadorNombresAsc(c.getId());
        participantes.deleteAll(actuales.stream().filter(p -> !ids.contains(p.getTrabajador().getId())).toList());
        Set<Long> previos = new HashSet<>(); actuales.forEach(p -> previos.add(p.getTrabajador().getId()));
        for (Trabajador persona : personas) if (!previos.contains(persona.getId())) {
            CapacitacionParticipante p = new CapacitacionParticipante(); p.setCapacitacion(c); p.setTrabajador(persona); participantes.save(p);
        }
        auditoria.registrar("CAPACITACIONES", c.getId(), nueva ? "CREADO" : "ACTUALIZADO", actor,
                "Tema: " + c.getTema() + " | Participantes: " + ids.size());
        return c;
    }
    @Transactional
    public void asistencia(Long id, Map<Long,String> registros, String actor) {
        Capacitacion c = bloquear(id); exigirProgramada(c);
        if (c.getFechaHora().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("La capacitación todavía no ha comenzado.");
        var lista = participantes.findByCapacitacionIdOrderByTrabajadorApellidosAscTrabajadorNombresAsc(id);
        Set<Long> esperados = new HashSet<>(); lista.forEach(p -> esperados.add(p.getId()));
        if (registros == null || !esperados.equals(registros.keySet()) || registros.values().stream().anyMatch(v -> !Set.of("ASISTIO", "FALTO").contains(v)))
            throw new IllegalArgumentException("Marca la asistencia de todos los participantes sin modificar sus identificadores.");
        lista.forEach(p -> p.setAsistencia(registros.get(p.getId())));
        participantes.saveAll(lista);
        auditoria.registrar("CAPACITACIONES", id, "ASISTENCIA", actor, "Asistencia registrada para " + lista.size() + " participantes.");
    }
    @Transactional
    public void finalizar(Long id, String actor) {
        Capacitacion c = bloquear(id); exigirProgramada(c);
        if (c.getFechaHora().plusMinutes(c.getDuracionMinutos()).isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Espera a que termine el horario de la capacitación.");
        var lista = participantes.findByCapacitacionIdOrderByTrabajadorApellidosAscTrabajadorNombresAsc(id);
        if (lista.isEmpty() || lista.stream().anyMatch(p -> "PENDIENTE".equals(p.getAsistencia())))
            throw new IllegalArgumentException("Registra toda la asistencia antes de finalizar.");
        c.setEstado("REALIZADA");
        auditoria.registrar("CAPACITACIONES", id, "FINALIZADA", actor, "Capacitación realizada. Tema: " + c.getTema());
    }
    @Transactional
    public void cancelar(Long id, String actor) {
        Capacitacion c = bloquear(id); exigirProgramada(c); c.setEstado("CANCELADA");
        auditoria.registrar("CAPACITACIONES", id, "CANCELADA", actor, "Capacitación cancelada. Tema: " + c.getTema());
    }
    private Capacitacion bloquear(Long id) {
        return capacitaciones.bloquear(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private void exigirProgramada(Capacitacion c) {
        if (!"PROGRAMADA".equals(c.getEstado())) throw new IllegalArgumentException("Solo se puede modificar una capacitación programada.");
    }
}
