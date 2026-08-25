package com.safework.safework.model;

// Importaciones de JPA para relacionar esta clase con la base de datos.
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Importaciones para validar los datos antes de guardarlos.
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


// Indica que esta clase será una entidad administrada por JPA.
@Entity

// Define que la entidad Area se relaciona con la tabla "areas" en MySQL.
@Table(name = "areas")
public class Area {

    // Clave primaria de la tabla.
    @Id

    // Hace que MySQL genere automáticamente el id.
    // IDENTITY normalmente se relaciona con AUTO_INCREMENT en MySQL.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * Valida que el nombre no sea:
     * null, vacío "" o solamente espacios.
     */
    @NotBlank(message = "El nombre del área es obligatorio")

    /*
     * Valida que el nombre tenga como máximo
     * 100 caracteres.
     */
    @Size(
        max = 100,
        message = "El nombre no puede superar los 100 caracteres"
    )

    /*
     * Configuración de la columna en MySQL.
     *
     * nullable = false:
     * la columna no puede ser NULL.
     *
     * length = 100:
     * máximo 100 caracteres en la base de datos.
     */
    @Column(nullable = false, length = 100)
    private String nombre;


    /*
     * La descripción es opcional,
     * pero si se escribe no puede superar
     * los 255 caracteres.
     */
    @Size(
        max = 255,
        message = "La descripción no puede superar los 255 caracteres"
    )

    // La columna descripción admite hasta 255 caracteres.
    @Column(length = 255)
    private String descripcion;


    /*
     * Constructor vacío.
     *
     * JPA necesita poder crear objetos Area
     * sin recibir parámetros.
     */
    public Area() {
    }


    /*
     * Constructor con los datos principales del área.
     *
     * No incluimos id porque MySQL
     * lo genera automáticamente.
     */
    public Area(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }


    // Getter: permite obtener el id.
    public Long getId() {
        return id;
    }


    // Setter: permite modificar el id.
    public void setId(Long id) {
        this.id = id;
    }


    // Getter: permite obtener el nombre.
    public String getNombre() {
        return nombre;
    }


    // Setter: permite modificar el nombre.
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }


    // Getter: permite obtener la descripción.
    public String getDescripcion() {
        return descripcion;
    }


    // Setter: permite modificar la descripción.
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}