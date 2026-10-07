package com.safework.safework.model;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EntregaEppFormulario {
    @NotNull(message = "Seleccione un trabajador")
    private Long trabajadorId;

    @NotBlank(message = "Indique el tipo de EPP")
    @Size(max = 100, message = "El tipo de EPP no puede superar los 100 caracteres")
    private String tipoEpp;

    @NotNull(message = "Indique la fecha de entrega")
    private LocalDate fechaEntrega = LocalDate.now();

    @NotNull(message = "Indique la cantidad")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    @Max(value = 1000, message = "La cantidad no puede superar 1000")
    private Integer cantidad;

    @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
    private String observaciones;

    public Long getTrabajadorId() { return trabajadorId; }
    public void setTrabajadorId(Long trabajadorId) { this.trabajadorId = trabajadorId; }
    public String getTipoEpp() { return tipoEpp; }
    public void setTipoEpp(String tipoEpp) { this.tipoEpp = tipoEpp; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
