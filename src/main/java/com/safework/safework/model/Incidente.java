package com.safework.safework.model;

import java.time.LocalDateTime;

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
 * Entidad que representa un incidente
 * o accidente laboral registrado
 * dentro de SafeWork.
 */
@Entity
@Table(name = "incidentes")
public class Incidente {


    /*
     * Identificador interno.
     *
     * MySQL lo genera automáticamente
     * mediante AUTO_INCREMENT.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * Fecha y hora en la que ocurrió
     * el incidente o accidente.
     *
     * Utilizamos LocalDateTime porque
     * necesitamos almacenar:
     *
     * - Fecha
     * - Hora
     */
    @NotNull(message = "La fecha y hora son obligatorias")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(nullable = false)
    private LocalDateTime fechaHora;


    /*
     * Tipo de evento.
     *
     * Inicialmente utilizaremos:
     *
     * - Incidente
     * - Accidente
     */
    @NotBlank(message = "El tipo de evento es obligatorio")
    @Size(
            max = 30,
            message = "El tipo no puede superar los 30 caracteres"
    )
    @Column(
            nullable = false,
            length = 30
    )
    private String tipo;


    /*
     * Área donde ocurrió el evento.
     *
     * Muchas situaciones pueden ocurrir
     * dentro de una misma área.
     */
    @NotNull(message = "El área es obligatoria")
    @ManyToOne
    @JoinColumn(
            name = "area_id",
            nullable = false
    )
    private Area area;


    /*
     * Trabajador principal involucrado
     * en el incidente o accidente.
     *
     * Un trabajador puede estar relacionado
     * con diferentes incidentes a lo largo
     * del tiempo.
     */
    @NotNull(message = "El trabajador involucrado es obligatorio")
    @ManyToOne
    @JoinColumn(
            name = "trabajador_id",
            nullable = false
    )
    private Trabajador trabajador;


    /*
     * Descripción detallada
     * de lo ocurrido.
     */
    @NotBlank(message = "La descripción es obligatoria")
    @Size(
            max = 500,
            message = "La descripción no puede superar los 500 caracteres"
    )
    @Column(
            nullable = false,
            length = 500
    )
    private String descripcion;


    /*
     * Nivel de gravedad del evento.
     *
     * Inicialmente utilizaremos:
     *
     * - Leve
     * - Moderada
     * - Grave
     */
    @NotBlank(message = "La gravedad es obligatoria")
    @Size(
            max = 30,
            message = "La gravedad no puede superar los 30 caracteres"
    )
    @Column(
            nullable = false,
            length = 30
    )
    private String gravedad;


    /*
     * Estado actual del incidente.
     *
     * Inicialmente utilizaremos:
     *
     * - Reportado
     * - En investigación
     * - Cerrado
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
     * Constructor vacío requerido por JPA.
     */
    public Incidente() {
    }


    /*
     * Constructor con los principales
     * atributos del incidente.
     */
    public Incidente(
            LocalDateTime fechaHora,
            String tipo,
            Area area,
            Trabajador trabajador,
            String descripcion,
            String gravedad,
            String estado) {

        this.fechaHora = fechaHora;
        this.tipo = tipo;
        this.area = area;
        this.trabajador = trabajador;
        this.descripcion = descripcion;
        this.gravedad = gravedad;
        this.estado = estado;
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


    public LocalDateTime getFechaHora() {
        return fechaHora;
    }


    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
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


    public Trabajador getTrabajador() {
        return trabajador;
    }


    public void setTrabajador(Trabajador trabajador) {
        this.trabajador = trabajador;
    }


    public String getDescripcion() {
        return descripcion;
    }


    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }


    public String getGravedad() {
        return gravedad;
    }


    public void setGravedad(String gravedad) {
        this.gravedad = gravedad;
    }


    public String getEstado() {
        return estado;
    }


    public void setEstado(String estado) {
        this.estado = estado;
    }

}