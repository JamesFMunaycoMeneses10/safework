package com.safework.safework.model;


import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;



@Entity
@Table(name = "trabajadores")

public class Trabajador {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(
        regexp = "\\d{8}",
        message = "El DNI debe contener exactamente 8 números"
    )
    @Column(nullable = false, unique = true, length = 8)
    private String dni;



    @NotBlank(message = "Los nombres son obligatorios")
    @Size(
        max = 100,
        message = "Los nombres no pueden superar los 100 caracteres"
    )
    @Column(nullable = false, length = 100)
    private String nombres;



    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(
        max = 100,
        message = "Los apellidos no pueden superar los 100 caracteres"
    )
    @Column(nullable = false, length = 100)
    private String apellidos;



    @NotBlank(message = "El cargo es obligatorio")
    @Size(
        max = 100,
        message = "El cargo no puede superar los 100 caracteres"
    )
    @Column(nullable = false, length = 100)
    private String cargo;



    @NotNull(message = "El área es obligatoria")
    @ManyToOne
    @JoinColumn(
        name = "area_id",
        nullable = false
    )
    private Area area;



    /*
     * Un trabajador puede tener
     * varios incidentes asociados.
     */
    @OneToMany(mappedBy = "trabajador")
    private List<Incidente> incidentes;



    /*
     * Un trabajador puede ser responsable
     * de varias acciones correctivas.
     */
    @OneToMany(mappedBy = "responsable")
    private List<AccionCorrectiva> accionesCorrectivas;



    public Trabajador() {

    }



    public Trabajador(
            String dni,
            String nombres,
            String apellidos,
            String cargo,
            Area area) {

        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.cargo = cargo;
        this.area = area;

    }



    public Long getId() {

        return id;

    }


    public void setId(Long id) {

        this.id = id;

    }



    public String getDni() {

        return dni;

    }


    public void setDni(String dni) {

        this.dni = dni;

    }



    public String getNombres() {

        return nombres;

    }


    public void setNombres(String nombres) {

        this.nombres = nombres;

    }



    public String getApellidos() {

        return apellidos;

    }


    public void setApellidos(String apellidos) {

        this.apellidos = apellidos;

    }



    public String getCargo() {

        return cargo;

    }


    public void setCargo(String cargo) {

        this.cargo = cargo;

    }



    public Area getArea() {

        return area;

    }


    public void setArea(Area area) {

        this.area = area;

    }



    public List<Incidente> getIncidentes() {

        return incidentes;

    }


    public void setIncidentes(List<Incidente> incidentes) {

        this.incidentes = incidentes;

    }



    public List<AccionCorrectiva> getAccionesCorrectivas() {

        return accionesCorrectivas;

    }


    public void setAccionesCorrectivas(
            List<AccionCorrectiva> accionesCorrectivas) {

        this.accionesCorrectivas = accionesCorrectivas;

    }

}