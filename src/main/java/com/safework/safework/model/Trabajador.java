package com.safework.safework.model;

// Importaciones de JPA.
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Importaciones para validación.
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


// Indica que Trabajador será una entidad administrada por JPA.
@Entity

// La tabla en MySQL se llamará "trabajadores".
@Table(name = "trabajadores")
public class Trabajador {


    // Clave primaria.
    @Id

    // MySQL generará el ID automáticamente.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * DNI obligatorio.
     *
     * @Pattern exige exactamente 8 números.
     */
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(
        regexp = "\\d{8}",
        message = "El DNI debe contener exactamente 8 números"
    )
    @Column(nullable = false, unique = true, length = 8)
    private String dni;


    // Nombres del trabajador.
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(
        max = 100,
        message = "Los nombres no pueden superar los 100 caracteres"
    )
    @Column(nullable = false, length = 100)
    private String nombres;


    // Apellidos del trabajador.
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(
        max = 100,
        message = "Los apellidos no pueden superar los 100 caracteres"
    )
    @Column(nullable = false, length = 100)
    private String apellidos;


    // Cargo que desempeña.
    @NotBlank(message = "El cargo es obligatorio")
    @Size(
        max = 100,
        message = "El cargo no puede superar los 100 caracteres"
    )
    @Column(nullable = false, length = 100)
    private String cargo;


    /*
     * Relación con Area.
     *
     * Muchos trabajadores pueden pertenecer
     * a una misma área.
     */
    @NotNull(message = "El área es obligatoria")
    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;


    // Constructor vacío requerido por JPA.
    public Trabajador() {
    }


    // Constructor con los datos principales.
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


    // Getter y setter del ID.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    // Getter y setter del DNI.
    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }


    // Getter y setter de nombres.
    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }


    // Getter y setter de apellidos.
    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }


    // Getter y setter del cargo.
    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }


    // Getter y setter del área.
    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }
}