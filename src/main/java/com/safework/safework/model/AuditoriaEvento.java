package com.safework.safework.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registro inmutable de operaciones relevantes para seguimiento de SST. */
@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String modulo;

    @Column(name = "registro_id", nullable = false)
    private Long registroId;

    @Column(nullable = false, length = 20)
    private String operacion;

    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 2000)
    private String detalle;

    protected AuditoriaEvento() {}

    public AuditoriaEvento(String modulo, Long registroId, String operacion,
            String usuario, LocalDateTime fecha, String detalle) {
        this.modulo = modulo;
        this.registroId = registroId;
        this.operacion = operacion;
        this.usuario = usuario;
        this.fecha = fecha;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public String getModulo() { return modulo; }
    public Long getRegistroId() { return registroId; }
    public String getOperacion() { return operacion; }
    public String getUsuario() { return usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public String getDetalle() { return detalle; }
}
