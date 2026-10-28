package com.safework.safework.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos de alta; empresa y rol del administrador se deciden exclusivamente en servidor. */
public class EmpresaAltaFormulario {
    @NotBlank @Size(max = 150)
    private String nombre;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9-]{2,49}", message = "Usa entre 3 y 50 letras, números o guiones")
    private String codigo;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{2,49}", message = "Usa entre 3 y 50 letras, números, puntos, guiones o guiones bajos")
    private String adminUsername;
    @NotBlank @Size(min = 12, max = 72, message = "La contraseña debe tener entre 12 y 72 caracteres")
    private String adminPassword;
    @NotBlank @Size(max = 100)
    private String adminNombre;
    @NotBlank @Size(max = 100)
    private String adminApellido;
    @NotBlank @Email @Size(max = 255)
    private String adminCorreo;

    public String getNombre() { return nombre; }
    public void setNombre(String value) { nombre = value; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String value) { codigo = value; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String value) { adminUsername = value; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String value) { adminPassword = value; }
    public String getAdminNombre() { return adminNombre; }
    public void setAdminNombre(String value) { adminNombre = value; }
    public String getAdminApellido() { return adminApellido; }
    public void setAdminApellido(String value) { adminApellido = value; }
    public String getAdminCorreo() { return adminCorreo; }
    public void setAdminCorreo(String value) { adminCorreo = value; }
}
