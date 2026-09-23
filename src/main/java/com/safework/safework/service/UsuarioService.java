package com.safework.safework.service;

import java.util.List;
import java.util.Set;

import com.safework.safework.model.UsuarioFormulario;
import com.safework.safework.model.Usuario;
import com.safework.safework.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private static final Set<String> ROLES = Set.of("ADMIN", "SUPERVISOR", "TRABAJADOR");
    private static final Set<String> ESTADOS = Set.of("ACTIVO", "INACTIVO");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Transactional
    public Usuario guardarUsuario(UsuarioFormulario formulario, String administradorActual) {
        boolean nuevo = formulario.getId() == null;
        Usuario usuario = nuevo ? new Usuario() : usuarioRepository.findById(formulario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String username = limpiar(formulario.getUsername());
        String correo = limpiar(formulario.getCorreo());
        String rol = limpiar(formulario.getRol());
        String estado = nuevo ? "ACTIVO" : limpiar(formulario.getEstado());

        if (username.isBlank() || limpiar(formulario.getNombre()).isBlank()
                || limpiar(formulario.getApellido()).isBlank()) {
            throw new IllegalArgumentException("Complete el usuario, nombre y apellido");
        }
        if (!ROLES.contains(rol) || !ESTADOS.contains(estado)) {
            throw new IllegalArgumentException("Seleccione un rol y estado válidos");
        }
        if (usuarioRepository.existsByUsernameAndIdNot(username, nuevo ? -1L : usuario.getId())) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }
        if (!correo.isBlank() && usuarioRepository.existsByCorreoAndIdNot(correo, nuevo ? -1L : usuario.getId())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        if (!nuevo && usuario.getUsername().equals(administradorActual)
                && (!"ADMIN".equals(rol) || !"ACTIVO".equals(estado))) {
            throw new IllegalArgumentException("No puede quitarse su propio acceso de administrador");
        }
        if (!nuevo && "ADMIN".equals(usuario.getRol()) && "ACTIVO".equals(usuario.getEstado())
                && (!"ADMIN".equals(rol) || !"ACTIVO".equals(estado))) {
            protegerUltimoAdministrador();
        }

        String nuevaPassword = formulario.getPassword();
        if (nuevo && (nuevaPassword == null || nuevaPassword.isBlank())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            if (nuevaPassword.length() < 8) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
            }
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        }

        usuario.setUsername(username);
        usuario.setNombre(limpiar(formulario.getNombre()));
        usuario.setApellido(limpiar(formulario.getApellido()));
        usuario.setCorreo(correo.isBlank() ? null : correo);
        usuario.setRol(rol);
        usuario.setEstado(estado);
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("El usuario o correo ya está registrado, o los datos no son válidos", e);
        }
    }

    @Transactional
    public void eliminarUsuario(Long id, String administradorActual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (usuario.getUsername().equals(administradorActual)) {
            throw new IllegalArgumentException("No puede eliminar su propia cuenta");
        }
        if ("ADMIN".equals(usuario.getRol()) && "ACTIVO".equals(usuario.getEstado())) {
            protegerUltimoAdministrador();
        }
        try {
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("No se puede eliminar el usuario porque tiene registros relacionados", e);
        }
    }

    private void protegerUltimoAdministrador() {
        if (usuarioRepository.countByRolAndEstado("ADMIN", "ACTIVO") <= 1) {
            throw new IllegalArgumentException("Debe permanecer al menos un administrador activo");
        }
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
