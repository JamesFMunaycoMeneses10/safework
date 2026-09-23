package com.safework.safework.model;


import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * Entidad que representa un riesgo
 * identificado dentro de SafeWork.
 */
@Entity
@Table(name = "riesgos")
public class Riesgo {


    /**
     * Identificador interno del riesgo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    /**
     * Peligro identificado.
     */
    @NotBlank(message = "El peligro es obligatorio")
    @Size(max = 150,
          message = "El peligro no puede superar los 150 caracteres")
    @Column(nullable = false, length = 150)
    private String peligro;



    /**
     * Descripción detallada del riesgo.
     */
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255,
          message = "La descripción no puede superar los 255 caracteres")
    @Column(nullable = false, length = 255)
    private String descripcion;



    /**
     * Probabilidad del riesgo.
     */
    @NotNull(message = "La probabilidad es obligatoria")
    @Min(value = 1, message = "La probabilidad mínima es 1")
    @Max(value = 5, message = "La probabilidad máxima es 5")
    @Column(nullable = false)
    private Integer probabilidad;



    /**
     * Severidad del riesgo.
     */
    @NotNull(message = "La severidad es obligatoria")
    @Min(value = 1, message = "La severidad mínima es 1")
    @Max(value = 5, message = "La severidad máxima es 5")
    @Column(nullable = false)
    private Integer severidad;



    /**
     * Resultado de probabilidad x severidad.
     */
    @Column(nullable = false)
    private Integer nivelRiesgo;



    /**
     * Medida de control aplicada.
     */
    @Size(max = 255,
          message = "La medida de control no puede superar los 255 caracteres")
    @Column(length = 255)
    private String medidaControl;

    @Column(name = "foto_archivo", length = 40)
    private String fotoArchivo;
    @Column(name = "foto_nombre", length = 255)
    private String fotoNombre;
    @Column(name = "foto_tipo", length = 50)
    private String fotoTipo;



    /**
     * Área donde fue identificado el riesgo.
     */
    @NotNull(message = "El área es obligatoria")
    @ManyToOne
    @JoinColumn(
            name = "area_id",
            nullable = false
    )
    private Area area;



    /**
     * Acciones correctivas relacionadas
     * con este riesgo.
     *
     * Un riesgo puede tener
     * varias acciones correctivas.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "riesgo")
    private List<AccionCorrectiva> accionesCorrectivas = new ArrayList<>();



    /**
     * Constructor vacío requerido por JPA.
     */
    public Riesgo() {

    }



    /**
     * Constructor principal.
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

    public String getFotoArchivo() { return fotoArchivo; }
    public void setFotoArchivo(String valor) { this.fotoArchivo = valor; }
    public String getFotoNombre() { return fotoNombre; }
    public void setFotoNombre(String valor) { this.fotoNombre = valor; }
    public String getFotoTipo() { return fotoTipo; }
    public void setFotoTipo(String valor) { this.fotoTipo = valor; }



    public Area getArea() {

        return area;
    }


    public void setArea(Area area) {

        this.area = area;
    }



    public List<AccionCorrectiva> getAccionesCorrectivas() {

        return accionesCorrectivas;
    }


    public void setAccionesCorrectivas(
            List<AccionCorrectiva> accionesCorrectivas) {

        this.accionesCorrectivas = accionesCorrectivas;
    }



    /**
     * Clasificación automática del riesgo.
     */
    @Transient
    public String getClasificacionRiesgo() {


        if (nivelRiesgo == null) {

            return "Sin calcular";
        }


        if (nivelRiesgo <= 4) {

            return "Bajo";
        }


        if (nivelRiesgo <= 9) {

            return "Moderado";
        }


        if (nivelRiesgo <= 16) {

            return "Alto";
        }


        return "Crítico";
    }

}
