package com.safework.safework.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "capacitaciones")
public class Capacitacion extends EmpresaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String tema;
    @Column(nullable = false, length = 1000) private String objetivo;
    @Column(nullable = false, length = 150) private String instructor;
    @Column(nullable = false) private LocalDateTime fechaHora;
    @Column(nullable = false) private Integer duracionMinutos;
    @Column(nullable = false, length = 20) private String estado = "PROGRAMADA";
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false) private Area area;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false) private Usuario registradoPor;
    public Long getId() { return id; }
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }
}
