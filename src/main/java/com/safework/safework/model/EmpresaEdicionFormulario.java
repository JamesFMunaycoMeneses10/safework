package com.safework.safework.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** La edición no admite roles, IDs de empresa o información de cuentas existentes. */
public class EmpresaEdicionFormulario {
    @NotBlank @Size(max = 150)
    private String nombre;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9-]{2,49}")
    private String codigo;
    @NotBlank @Pattern(regexp = "ACTIVO|INACTIVO")
    private String estado = "ACTIVO";
    public String getNombre() { return nombre; }
    public void setNombre(String value) { nombre = value; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String value) { codigo = value; }
    public String getEstado() { return estado; }
    public void setEstado(String value) { estado = value; }
}
