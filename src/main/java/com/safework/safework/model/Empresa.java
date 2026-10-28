package com.safework.safework.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Catálogo de la plataforma; no contiene registros SST. */
@Entity @Table(name = "empresas")
public class Empresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150)
    private String nombre;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9-]{2,49}")
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;
    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
    public Empresa() {}
    public Empresa(String nombre, String codigo) { this.nombre = nombre; this.codigo = codigo; }
    @PrePersist void inicializar() { if (fechaRegistro == null) fechaRegistro = LocalDateTime.now(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
