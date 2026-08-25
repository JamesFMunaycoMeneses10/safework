package com.safework.safework.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/*
 * Entidad que representa una inspección
 * de seguridad registrada en SafeWork.
 */
@Entity
@Table(name = "inspecciones")
public class Inspeccion {


    /*
     * Identificador interno de la inspección.
     *
     * MySQL lo genera automáticamente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * Fecha programada o realizada
     * de la inspección.
     *
     * LocalDate almacena solamente:
     *
     * año - mes - día
     */
    @NotNull(message = "La fecha es obligatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate fecha;


    /*
     * Tipo de inspección.
     *
     * Ejemplos:
     *
     * - Inspección de seguridad
     * - Inspección de equipos
     * - Inspección de orden y limpieza
     */
    @NotBlank(message = "El tipo de inspección es obligatorio")
    @Size(
            max = 100,
            message = "El tipo de inspección no puede superar los 100 caracteres"
    )
    @Column(
            nullable = false,
            length = 100
    )
    private String tipo;


    /*
     * Área donde se realizará
     * o realizó la inspección.
     *
     * Muchas inspecciones pueden
     * pertenecer a una misma área.
     */
    @NotNull(message = "El área es obligatoria")
    @ManyToOne
    @JoinColumn(
            name = "area_id",
            nullable = false
    )
    private Area area;


    /*
     * Trabajador responsable
     * de realizar la inspección.
     *
     * Un trabajador puede ser responsable
     * de muchas inspecciones.
     */
    @NotNull(message = "El responsable es obligatorio")
    @ManyToOne
    @JoinColumn(
            name = "responsable_id",
            nullable = false
    )
    private Trabajador responsable;


    /*
     * Estado actual de la inspección.
     *
     * Inicialmente utilizaremos:
     *
     * - Programada
     * - Realizada
     * - Cerrada
     *
     * Estos valores posteriormente
     * aparecerán como opciones del formulario.
     */
    @NotBlank(message = "El estado es obligatorio")
    @Size(
            max = 30,
            message = "El estado no puede superar los 30 caracteres"
    )
    @Column(
            nullable = false,
            length = 30
    )
    private String estado;


    /*
     * Comentarios o situaciones encontradas
     * durante la inspección.
     */
    @Size(
            max = 500,
            message = "Las observaciones no pueden superar los 500 caracteres"
    )
    @Column(length = 500)
    private String observaciones;


    /*
     * Constructor vacío requerido por JPA.
     */
    public Inspeccion() {
    }


    /*
     * Constructor con los atributos
     * principales de una inspección.
     */
    public Inspeccion(
            LocalDate fecha,
            String tipo,
            Area area,
            Trabajador responsable,
            String estado,
            String observaciones) {

        this.fecha = fecha;
        this.tipo = tipo;
        this.area = area;
        this.responsable = responsable;
        this.estado = estado;
        this.observaciones = observaciones;
    }


    // =========================
    // GETTERS Y SETTERS
    // =========================


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public LocalDate getFecha() {
        return fecha;
    }


    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }


    public String getTipo() {
        return tipo;
    }


    public void setTipo(String tipo) {
        this.tipo = tipo;
    }


    public Area getArea() {
        return area;
    }


    public void setArea(Area area) {
        this.area = area;
    }


    public Trabajador getResponsable() {
        return responsable;
    }


    public void setResponsable(Trabajador responsable) {
        this.responsable = responsable;
    }


    public String getEstado() {
        return estado;
    }


    public void setEstado(String estado) {
        this.estado = estado;
    }


    public String getObservaciones() {
        return observaciones;
    }


    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

}