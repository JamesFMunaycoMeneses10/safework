package com.safework.safework.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(nullable = false, unique = true)
    private String username;



    @Column(nullable = false)
    private String password;



    @Column(nullable = false)
    private String rol;



    @Column(nullable = false)
    private String nombre;



    @Column(nullable = false)
    private String apellido;



    @Column(unique = true)
    private String correo;



    @Column(nullable = false)
    private String estado;



    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;



    // Constructor vacío requerido por JPA
    public Usuario() {

    }



    // Constructor básico
    public Usuario(String username, String password, String rol) {

        this.username = username;
        this.password = password;
        this.rol = rol;

    }



    // Constructor completo para crear usuarios
    public Usuario(
            String username,
            String password,
            String rol,
            String nombre,
            String apellido,
            String correo
    ) {

        this.username = username;
        this.password = password;
        this.rol = rol;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;

    }



    // Se ejecuta antes de guardar en BD
    @PrePersist
    public void prePersist() {


        if (fechaRegistro == null) {

            fechaRegistro = LocalDateTime.now();

        }


        if (estado == null) {

            estado = "ACTIVO";

        }

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



    public String getUsername() {

        return username;

    }


    public void setUsername(String username) {

        this.username = username;

    }



    public String getPassword() {

        return password;

    }


    public void setPassword(String password) {

        this.password = password;

    }



    public String getRol() {

        return rol;

    }


    public void setRol(String rol) {

        this.rol = rol;

    }



    public String getNombre() {

        return nombre;

    }


    public void setNombre(String nombre) {

        this.nombre = nombre;

    }



    public String getApellido() {

        return apellido;

    }


    public void setApellido(String apellido) {

        this.apellido = apellido;

    }



    public String getCorreo() {

        return correo;

    }


    public void setCorreo(String correo) {

        this.correo = correo;

    }



    public String getEstado() {

        return estado;

    }


    public void setEstado(String estado) {

        this.estado = estado;

    }



    public LocalDateTime getFechaRegistro() {

        return fechaRegistro;

    }


    public void setFechaRegistro(LocalDateTime fechaRegistro) {

        this.fechaRegistro = fechaRegistro;

    }

}