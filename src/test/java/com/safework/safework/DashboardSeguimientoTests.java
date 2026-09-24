package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.safework.safework.controller.DashboardController;
import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.model.Trabajador;
import com.safework.safework.model.Usuario;
import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.IncidenteService;
import com.safework.safework.service.RiesgoService;
import com.safework.safework.service.TrabajadorService;

class DashboardSeguimientoTests {
    @Test
    void trabajadorVeSoloSusAccionesEnIndicadores() {
        TrabajadorService trabajadores = mock(TrabajadorService.class);
        RiesgoService riesgos = mock(RiesgoService.class);
        IncidenteService incidentes = mock(IncidenteService.class);
        AccionCorrectivaService acciones = mock(AccionCorrectivaService.class);
        Usuario usuario = new Usuario();
        usuario.setUsername("luis123");
        Trabajador luis = new Trabajador();
        luis.setUsuario(usuario);
        AccionCorrectiva propia = new AccionCorrectiva();
        propia.setResponsable(luis);
        propia.setEstado("En revisión");
        propia.setFechaLimite(LocalDate.now().minusDays(1));
        AccionCorrectiva ajena = new AccionCorrectiva();
        ajena.setEstado("En revisión");
        ajena.setFechaLimite(LocalDate.now().minusDays(1));
        when(trabajadores.listarTodos()).thenReturn(List.of());
        when(riesgos.listarTodos()).thenReturn(List.of());
        when(incidentes.listarTodos()).thenReturn(List.of());
        when(acciones.listarTodas()).thenReturn(List.of(propia, ajena));

        ExtendedModelMap modelo = new ExtendedModelMap();
        var autenticacion = new UsernamePasswordAuthenticationToken("luis123", "",
                List.of(new SimpleGrantedAuthority("ROLE_TRABAJADOR")));
        new DashboardController(trabajadores, riesgos, incidentes, acciones)
                .dashboard(modelo, autenticacion);

        assertEquals(1, modelo.get("totalAcciones"));
        assertEquals(1L, modelo.get("accionesVencidas"));
        assertEquals(1L, modelo.get("accionesPorRevisar"));
    }

    @Test
    void noCuentaAccionesCanceladasComoVencidas() {
        TrabajadorService trabajadores = mock(TrabajadorService.class);
        RiesgoService riesgos = mock(RiesgoService.class);
        IncidenteService incidentes = mock(IncidenteService.class);
        AccionCorrectivaService acciones = mock(AccionCorrectivaService.class);

        AccionCorrectiva cancelada = new AccionCorrectiva();
        cancelada.setEstado("Cancelada");
        cancelada.setFechaLimite(LocalDate.now().minusDays(2));
        cancelada.setFechaRegistro(LocalDate.now().minusDays(10));
        when(trabajadores.listarTodos()).thenReturn(List.of());
        when(riesgos.listarTodos()).thenReturn(List.of());
        when(incidentes.listarTodos()).thenReturn(List.of());
        when(acciones.listarTodas()).thenReturn(List.of(cancelada));

        ExtendedModelMap modelo = new ExtendedModelMap();
        new DashboardController(trabajadores, riesgos, incidentes, acciones).dashboard(modelo, null);

        assertEquals(0L, modelo.get("accionesVencidas"));
        assertEquals("SEGURO", modelo.get("estadoSST"));
    }
}
