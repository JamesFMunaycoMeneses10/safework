package com.safework.safework;

import com.safework.safework.model.UsuarioFormulario;
import com.safework.safework.model.Usuario;
import com.safework.safework.repository.UsuarioRepository;
import com.safework.safework.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UsuarioServiceTests {
    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new UsuarioService(repository, encoder);
    }

    private UsuarioFormulario formulario() {
        UsuarioFormulario f = new UsuarioFormulario();
        f.setUsername("nuevo");
        f.setNombre("Ana");
        f.setApellido("Prueba");
        f.setCorreo("ana@empresa.com");
        f.setRol("TRABAJADOR");
        f.setPassword("claveSegura123");
        return f;
    }

    private Usuario existente(Long id, String username, String rol, String estado) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("$2a$hashExistente");
        u.setRol(rol);
        u.setEstado(estado);
        u.setNombre("Nombre");
        u.setApellido("Apellido");
        return u;
    }

    @Test
    void nuevoUsuarioCodificaContrasenaYEmpiezaActivo() {
        when(encoder.encode("claveSegura123")).thenReturn("hashNuevo");
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Usuario guardado = service.guardarUsuario(formulario(), "admin");
        assertThat(guardado.getPassword()).isEqualTo("hashNuevo");
        assertThat(guardado.getEstado()).isEqualTo("ACTIVO");
        verify(encoder).encode("claveSegura123");
    }

    @Test
    void editarSinContrasenaConservaHashYFecha() {
        Usuario previo = existente(2L, "nuevo", "TRABAJADOR", "ACTIVO");
        previo.prePersist();
        when(repository.findById(2L)).thenReturn(Optional.of(previo));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UsuarioFormulario f = formulario();
        f.setId(2L);
        f.setPassword("");
        f.setEstado("INACTIVO");
        Usuario guardado = service.guardarUsuario(f, "admin");
        assertThat(guardado.getPassword()).isEqualTo("$2a$hashExistente");
        assertThat(guardado.getFechaRegistro()).isEqualTo(previo.getFechaRegistro());
        assertThat(guardado.getEstado()).isEqualTo("INACTIVO");
        verifyNoInteractions(encoder);
    }

    @Test
    void rechazaUsernameYCorreoDuplicados() {
        when(repository.existsByUsernameAndIdNot(eq("nuevo"), any())).thenReturn(true);
        assertThatThrownBy(() -> service.guardarUsuario(formulario(), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usuario ya está registrado");
        verify(repository, never()).saveAndFlush(any());

        reset(repository);
        when(repository.existsByCorreoAndIdNot(eq("ana@empresa.com"), any())).thenReturn(true);
        assertThatThrownBy(() -> service.guardarUsuario(formulario(), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo ya está registrado");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaRolOEstadoManipulado() {
        UsuarioFormulario f = formulario();
        f.setRol("ROOT");
        assertThatThrownBy(() -> service.guardarUsuario(f, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void protegeUltimoAdministradorActivo() {
        Usuario actual = existente(1L, "admin", "ADMIN", "ACTIVO");
        when(repository.findById(1L)).thenReturn(Optional.of(actual));
        when(repository.countByRolAndEstado("ADMIN", "ACTIVO")).thenReturn(1L);
        assertThatThrownBy(() -> service.eliminarUsuario(1L, "otroAdmin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un administrador activo");
        verify(repository, never()).delete(any());
    }

    @Test
    void impideEliminarLaPropiaCuenta() {
        when(repository.findById(1L)).thenReturn(Optional.of(existente(1L, "admin", "ADMIN", "ACTIVO")));
        assertThatThrownBy(() -> service.eliminarUsuario(1L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propia cuenta");
        verify(repository, never()).delete(any());
    }

    @Test
    void edicionNoPermiteAlterarElRolPropio() {
        Usuario previo = existente(1L, "admin", "ADMIN", "ACTIVO");
        when(repository.findById(1L)).thenReturn(Optional.of(previo));
        UsuarioFormulario f = formulario();
        f.setId(1L);
        f.setUsername("admin");
        f.setRol("TRABAJADOR");
        f.setEstado("ACTIVO");
        assertThatThrownBy(() -> service.guardarUsuario(f, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propio acceso");
        verify(repository, never()).saveAndFlush(any());
    }
}
