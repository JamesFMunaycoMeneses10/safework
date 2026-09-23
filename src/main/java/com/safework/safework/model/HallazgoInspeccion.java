package com.safework.safework.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "hallazgos_inspeccion")
public class HallazgoInspeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String descripcion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inspeccion_id", nullable = false)
    private Inspeccion inspeccion;

    @ManyToOne
    @JoinColumn(name = "riesgo_id")
    private Riesgo riesgo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Inspeccion getInspeccion() { return inspeccion; }
    public void setInspeccion(Inspeccion inspeccion) {
        this.inspeccion = inspeccion;
    }

    public Riesgo getRiesgo() { return riesgo; }
    public void setRiesgo(Riesgo riesgo) {
        this.riesgo = riesgo;
    }
}