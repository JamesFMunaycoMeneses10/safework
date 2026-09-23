package com.safework.safework.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Campos que un administrador puede enviar desde el formulario de usuarios. */
public class UsuarioFormulario {
    private Long id;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 100, message = "El usuario no debe superar 100 caracteres")
    private String username;

    // Obligatoria al crear; en edición vacía significa conservar la actual.
    private String password;

    @NotBlank(message = "El rol es obligatorio")
    private String rol;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no debe superar 100 caracteres")
    private String apellido;

    @Email(message = "Ingrese un correo válido")
    @Size(max = 150, message = "El correo no debe superar 150 caracteres")
    private String correo;

    private String estado;

    public static UsuarioFormulario desde(Usuario usuario) {
        UsuarioFormulario formulario = new UsuarioFormulario();
        formulario.setId(usuario.getId());
        formulario.setUsername(usuario.getUsername());
        formulario.setRol(usuario.getRol());
        formulario.setNombre(usuario.getNombre());
        formulario.setApellido(usuario.getApellido());
        formulario.setCorreo(usuario.getCorreo());
        formulario.setEstado(usuario.getEstado());
        return formulario;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
