package com.safework.safework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.safework.safework.model.*;
import com.safework.safework.repository.*;
import com.safework.safework.service.*;

/** La cuenta global no se administra desde el directorio de usuarios de una empresa. */
class ProteccionCuentaPlataformaTests {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UsuarioService servicio = new UsuarioService(usuarios, encoder);

    private Usuario plataforma() {
        Usuario usuario = new Usuario("plataforma", "HASH_ORIGINAL", "SUPERADMIN");
        usuario.setId(100L);
        usuario.setEstado("ACTIVO");
        return usuario;
    }

    @Test
    void listadoNoExponeCuentaDePlataforma() {
        Usuario adminEmpresa = new Usuario("empresa", "HASH", "ADMIN");
        when(usuarios.findAll()).thenReturn(List.of(plataforma(), adminEmpresa));
        assertThat(servicio.listarUsuarios()).containsExactly(adminEmpresa);
    }

    @Test
    void consultaPorIdNoExponeCuentaDePlataforma() {
        when(usuarios.findById(100L)).thenReturn(Optional.of(plataforma()));
        assertThat(servicio.buscarPorId(100L)).isNull();
    }

    @Test
    void administradorDeEmpresaNoPuedeCambiarClaveNiRolDePlataforma() {
        Usuario cuenta = plataforma();
        when(usuarios.findById(100L)).thenReturn(Optional.of(cuenta));
        UsuarioFormulario formulario = new UsuarioFormulario();
        formulario.setId(100L);
        formulario.setRol("ADMIN");
        formulario.setPassword("IntentoDeCambio2026!");
        assertThatThrownBy(() -> servicio.guardarUsuario(formulario, "admin-empresa"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Usuario no encontrado");
        assertThat(cuenta.getRol()).isEqualTo("SUPERADMIN");
        assertThat(cuenta.getPassword()).isEqualTo("HASH_ORIGINAL");
        verify(usuarios, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void administradorDeEmpresaNoPuedeEliminarCuentaDePlataforma() {
        when(usuarios.findById(100L)).thenReturn(Optional.of(plataforma()));
        assertThatThrownBy(() -> servicio.eliminarUsuario(100L, "admin-empresa"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Usuario no encontrado");
        verify(usuarios, never()).delete(any());
    }

    @Test
    void cuentaDePlataformaNoSeVinculaAUnTrabajador() {
        AreaRepository areas = mock(AreaRepository.class);
        TrabajadorRepository trabajadores = mock(TrabajadorRepository.class);
        Area propia = new Area("Área", "");
        propia.setId(1L);
        when(areas.findById(1L)).thenReturn(Optional.of(propia));
        when(usuarios.findById(100L)).thenReturn(Optional.of(plataforma()));
        Trabajador formulario = new Trabajador();
        formulario.setArea(propia);
        formulario.setUsuario(plataforma());
        assertThatThrownBy(() -> new TrabajadorService(trabajadores, usuarios, areas).guardar(formulario))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no está disponible");
        verify(trabajadores, never()).save(any());
    }
}
