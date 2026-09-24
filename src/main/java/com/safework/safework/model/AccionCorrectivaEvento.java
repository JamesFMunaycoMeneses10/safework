package com.safework.safework.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Instantánea de una entrega o decisión; no se modifica al reenviar evidencia. */
@Entity
@Table(name = "acciones_correctivas_historial")
public class AccionCorrectivaEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accion_id", nullable = false)
    private AccionCorrectiva accion;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 255)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 1000)
    private String detalle;

    @Column(name = "archivo_evidencia", length = 40)
    private String archivoEvidencia;

    @Column(name = "nombre_evidencia", length = 255)
    private String nombreEvidencia;

    @Column(name = "tipo_evidencia", length = 50)
    private String tipoEvidencia;

    protected AccionCorrectivaEvento() {}

    public AccionCorrectivaEvento(AccionCorrectiva accion, String tipo, String usuario,
            LocalDateTime fecha, String detalle, String archivoEvidencia,
            String nombreEvidencia, String tipoEvidencia) {
        this.accion = accion;
        this.tipo = tipo;
        this.usuario = usuario;
        this.fecha = fecha;
        this.detalle = detalle;
        this.archivoEvidencia = archivoEvidencia;
        this.nombreEvidencia = nombreEvidencia;
        this.tipoEvidencia = tipoEvidencia;
    }

    public Long getId() { return id; }
    public AccionCorrectiva getAccion() { return accion; }
    public String getTipo() { return tipo; }
    public String getUsuario() { return usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public String getDetalle() { return detalle; }
    public String getArchivoEvidencia() { return archivoEvidencia; }
    public String getNombreEvidencia() { return nombreEvidencia; }
    public String getTipoEvidencia() { return tipoEvidencia; }
}
