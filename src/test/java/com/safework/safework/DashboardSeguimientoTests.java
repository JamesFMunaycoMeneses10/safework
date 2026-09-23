package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import com.safework.safework.controller.DashboardController;
import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.IncidenteService;
import com.safework.safework.service.RiesgoService;
import com.safework.safework.service.TrabajadorService;

class DashboardSeguimientoTests {
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
