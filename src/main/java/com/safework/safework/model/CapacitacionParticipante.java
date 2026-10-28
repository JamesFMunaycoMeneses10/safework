package com.safework.safework.model;

import jakarta.persistence.*;

@Entity
@Table(name = "capacitacion_participantes", uniqueConstraints =
        @UniqueConstraint(name = "uk_capacitacion_trabajador", columnNames = {"capacitacion_id", "trabajador_id"}))
public class CapacitacionParticipante extends EmpresaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capacitacion_id", nullable = false) private Capacitacion capacitacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajador_id", nullable = false) private Trabajador trabajador;
    @Column(nullable = false, length = 20) private String asistencia = "PENDIENTE";
    public Long getId() { return id; }
    public Capacitacion getCapacitacion() { return capacitacion; }
    public void setCapacitacion(Capacitacion capacitacion) { this.capacitacion = capacitacion; }
    public Trabajador getTrabajador() { return trabajador; }
    public void setTrabajador(Trabajador trabajador) { this.trabajador = trabajador; }
    public String getAsistencia() { return asistencia; }
    public void setAsistencia(String asistencia) { this.asistencia = asistencia; }
}
