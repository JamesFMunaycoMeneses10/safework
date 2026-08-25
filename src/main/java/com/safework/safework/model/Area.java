package com.safework.safework.model;

// Importaciones de JPA para relacionar esta clase con la base de datos.
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    // El nombre es obligatorio y puede tener como máximo 100 caracteres.
    @Column(nullable = false, length = 100)
    private String nombre;

    // La descripción puede tener como máximo 255 caracteres.
    // Como no usamos nullable = false, puede ser nula.
    @Column(length = 255)
    private String descripcion;

    // Constructor vacío.
    // JPA necesita poder crear objetos Area sin recibir parámetros.
    public Area() {
    }

    // Constructor con los datos principales del área.
    // No incluimos id porque MySQL lo genera automáticamente.
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