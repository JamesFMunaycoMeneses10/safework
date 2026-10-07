package com.safework.safework;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.safework.safework.model.EntregaEpp;
import com.safework.safework.model.EntregaEppFormulario;
import com.safework.safework.model.Trabajador;
import com.safework.safework.model.Usuario;
import com.safework.safework.repository.EntregaEppRepository;
import com.safework.safework.repository.TrabajadorRepository;
import com.safework.safework.repository.UsuarioRepository;
import com.safework.safework.service.EntregaEppService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EntregaEppServiceTests {
    private EntregaEppRepository entregas;
    private TrabajadorRepository trabajadores;
    private UsuarioRepository usuarios;
    private EntregaEppService servicio;

    @BeforeEach
    void preparar() {
        entregas = mock(EntregaEppRepository.class);
        trabajadores = mock(TrabajadorRepository.class);
        usuarios = mock(UsuarioRepository.class);
        servicio = new EntregaEppService(entregas, trabajadores, usuarios);
    }

    private EntregaEppFormulario formulario() {
        var datos = new EntregaEppFormulario();
        datos.setTrabajadorId(7L);
        datos.setTipoEpp("  Casco de seguridad  ");
        datos.setFechaEntrega(LocalDate.now());
        datos.setCantidad(1);
        datos.setObservaciones("  Talla M  ");
        return datos;
    }

    @Test
    void registraEntregaConTrabajadorYAutorReales() {
        Trabajador trabajador = new Trabajador();
        Usuario autor = new Usuario();
        when(trabajadores.findById(7L)).thenReturn(Optional.of(trabajador));
        when(usuarios.findByUsername("supervisor")).thenReturn(Optional.of(autor));
        when(entregas.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntregaEpp resultado = servicio.registrar(formulario(), "supervisor");
        assertThat(resultado.getTrabajador()).isSameAs(trabajador);
        assertThat(resultado.getRegistradoPor()).isSameAs(autor);
        assertThat(resultado.getTipoEpp()).isEqualTo("Casco de seguridad");
        assertThat(resultado.getObservaciones()).isEqualTo("Talla M");
    }

    @Test
    void rechazaDatosInvalidosAntesDeGuardar() {
        var datos = formulario();
        datos.setCantidad(0);
        assertThatThrownBy(() -> servicio.registrar(datos, "supervisor"))
                .isInstanceOf(IllegalArgumentException.class);
        datos.setCantidad(1);
        datos.setFechaEntrega(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> servicio.registrar(datos, "supervisor"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(entregas, never()).save(any());
    }

    @Test
    void historialPropioSeConsultaPorCuentaNoPorIdEnviadoPorUsuario() {
        when(trabajadores.findByUsuarioUsername("enzo")).thenReturn(Optional.of(new Trabajador()));
        when(entregas.findByTrabajadorUsuarioUsernameOrderByFechaEntregaDescIdDesc("enzo"))
                .thenReturn(List.of(new EntregaEpp()));
        assertThat(servicio.listarPropias("enzo")).hasSize(1);
        verify(entregas).findByTrabajadorUsuarioUsernameOrderByFechaEntregaDescIdDesc("enzo");
        verify(entregas, never()).findAllByOrderByFechaEntregaDescIdDesc();
    }
}
