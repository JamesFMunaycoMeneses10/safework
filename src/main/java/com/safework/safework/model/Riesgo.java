package com.safework.safework.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import jakarta.persistence.OneToMany;

/*
 * Entidad que representa un riesgo
 * identificado dentro de SafeWork.
 */
@Entity
@Table(name = "riesgos")
public class Riesgo {

    /*
     * Identificador interno del riesgo.
     *
     * MySQL lo genera automáticamente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Peligro identificado.
     *
     * Ejemplos:
     *
     * - Piso mojado
     * - Caída de objetos
     * - Sobreesfuerzo
     * - Ruido excesivo
     */
    @NotBlank(message = "El peligro es obligatorio")
    @Size(max = 150, message = "El peligro no puede superar los 150 caracteres")
    @Column(nullable = false, length = 150)
    private String peligro;

    /*
     * Descripción más detallada
     * del riesgo identificado.
     */
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    @Column(nullable = false, length = 255)
    private String descripcion;

    /*
     * Probabilidad de que ocurra el evento.
     *
     * Utilizaremos una escala del 1 al 5.
     */
    @NotNull(message = "La probabilidad es obligatoria")
    @Min(value = 1, message = "La probabilidad mínima es 1")
    @Max(value = 5, message = "La probabilidad máxima es 5")
    @Column(nullable = false)
    private Integer probabilidad;

    /*
     * Severidad o consecuencia del evento.
     *
     * También utilizaremos una escala
     * del 1 al 5.
     */
    @NotNull(message = "La severidad es obligatoria")
    @Min(value = 1, message = "La severidad mínima es 1")
    @Max(value = 5, message = "La severidad máxima es 5")
    @Column(nullable = false)
    private Integer severidad;

    /*
     * Resultado de:
     *
     * probabilidad × severidad
     *
     * Este valor NO será ingresado
     * manualmente por el usuario.
     *
     * SafeWork lo calculará automáticamente.
     */
    @Column(nullable = false)
    private Integer nivelRiesgo;

    /*
     * Medida que puede aplicarse para
     * reducir o controlar el riesgo.
     */
    @Size(max = 255, message = "La medida de control no puede superar los 255 caracteres")
    @Column(length = 255)
    private String medidaControl;

    /*
     * Área donde se identificó el riesgo.
     *
     * Muchas situaciones de riesgo pueden
     * pertenecer a una misma área.
     */
    @NotNull(message = "El área es obligatoria")
    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @OneToMany(mappedBy = "riesgo")
    private List<AccionCorrectiva> accionesCorrectivas;

    /*
     * Constructor vacío.
     *
     * JPA necesita este constructor
     * para crear objetos automáticamente.
     */
    public Riesgo() {
    }

    /*
     * Constructor con los principales atributos.
     */
    public Riesgo(
            String peligro,
            String descripcion,
            Integer probabilidad,
            Integer severidad,
            Integer nivelRiesgo,
            String medidaControl,
            Area area) {

        this.peligro = peligro;
        this.descripcion = descripcion;
        this.probabilidad = probabilidad;
        this.severidad = severidad;
        this.nivelRiesgo = nivelRiesgo;
        this.medidaControl = medidaControl;
        this.area = area;
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

    public String getPeligro() {
        return peligro;
    }

    public void setPeligro(String peligro) {
        this.peligro = peligro;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getProbabilidad() {
        return probabilidad;
    }

    public void setProbabilidad(Integer probabilidad) {
        this.probabilidad = probabilidad;
    }

    public Integer getSeveridad() {
        return severidad;
    }

    public void setSeveridad(Integer severidad) {
        this.severidad = severidad;
    }

    public Integer getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(Integer nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public String getMedidaControl() {
        return medidaControl;
    }

    public void setMedidaControl(String medidaControl) {
        this.medidaControl = medidaControl;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    /*
     * Devuelve la clasificación del riesgo
     * según el nivel calculado.
     *
     * Este valor no se almacena en MySQL.
     * Se calcula automáticamente cuando
     * necesitamos mostrarlo.
     */
    @Transient
    public String getClasificacionRiesgo() {

        /*
         * Si todavía no existe nivel de riesgo,
         * evitamos errores.
         */
        if (nivelRiesgo == null) {
            return "Sin calcular";
        }

        /*
         * Nivel entre 1 y 4.
         */
        if (nivelRiesgo <= 4) {
            return "Bajo";
        }

        /*
         * Nivel entre 5 y 9.
         */
        if (nivelRiesgo <= 9) {
            return "Moderado";
        }

        /*
         * Nivel entre 10 y 16.
         */
        if (nivelRiesgo <= 16) {
            return "Alto";
        }

        /*
         * Nivel entre 17 y 25.
         */
        return "Crítico";
    }

   public List<AccionCorrectiva> getAccionesCorrectivas() {
    return accionesCorrectivas;
}


public void setAccionesCorrectivas(List<AccionCorrectiva> accionesCorrectivas) {
    this.accionesCorrectivas = accionesCorrectivas;
} 
}