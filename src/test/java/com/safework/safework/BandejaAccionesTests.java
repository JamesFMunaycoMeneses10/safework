package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.service.BandejaAcciones;

class BandejaAccionesTests {
    private final LocalDate hoy = LocalDate.of(2026, 9, 23);

    @Test
    void vencidaSoloSiPlazoPasoYAccionSigueAbierta() {
        assertTrue(BandejaAcciones.vencida(accion("Pendiente", hoy.minusDays(1)), hoy));
        assertTrue(BandejaAcciones.vencida(accion("En revisión", hoy.minusDays(1)), hoy));
        assertTrue(BandejaAcciones.vencida(accion("Devuelta", hoy.minusDays(1)), hoy));
        assertFalse(BandejaAcciones.vencida(accion("Pendiente", hoy), hoy));
        assertFalse(BandejaAcciones.vencida(accion("Cerrada", hoy.minusDays(1)), hoy));
        assertFalse(BandejaAcciones.vencida(accion("Completada", hoy.minusDays(1)), hoy));
        assertFalse(BandejaAcciones.vencida(accion("Cancelada", hoy.minusDays(1)), hoy));
    }

    @Test
    void avisaDesdeHoyHastaSieteDiasSinIncluirCerradasNiVencidas() {
        assertTrue(BandejaAcciones.proximaVencer(accion("Pendiente", hoy), hoy));
        assertTrue(BandejaAcciones.proximaVencer(accion("Devuelta", hoy.plusDays(7)), hoy));
        assertFalse(BandejaAcciones.proximaVencer(accion("Pendiente", hoy.plusDays(8)), hoy));
        assertFalse(BandejaAcciones.proximaVencer(accion("Pendiente", hoy.minusDays(1)), hoy));
        assertFalse(BandejaAcciones.proximaVencer(accion("Cerrada", hoy.plusDays(1)), hoy));
        assertFalse(BandejaAcciones.proximaVencer(accion("Completada", hoy.plusDays(1)), hoy));
        assertFalse(BandejaAcciones.proximaVencer(accion("Cancelada", hoy.plusDays(1)), hoy));
    }

    @Test
    void contadoresYFiltrosPuedenSolaparseSinCambiarEstado() {
        AccionCorrectiva revisionVencida = accion("En revisión", hoy.minusDays(1));
        AccionCorrectiva devuelta = accion("Devuelta", hoy.plusDays(1));
        AccionCorrectiva cerrada = accion("Cerrada", hoy.minusDays(3));
        List<AccionCorrectiva> acciones = List.of(revisionVencida, devuelta, cerrada);

        var resumen = BandejaAcciones.resumir(acciones, hoy);
        assertEquals(3, resumen.todas());
        assertEquals(1, resumen.revision());
        assertEquals(1, resumen.devueltas());
        assertEquals(1, resumen.vencidas());
        assertEquals(List.of(revisionVencida), BandejaAcciones.filtrar(acciones, "revision", hoy));
        assertEquals(List.of(devuelta), BandejaAcciones.filtrar(acciones, "devueltas", hoy));
        assertEquals(List.of(revisionVencida), BandejaAcciones.filtrar(acciones, "vencidas", hoy));
        assertEquals("En revisión", revisionVencida.getEstado());
    }

    @Test
    void rechazaVistaNoReconocida() {
        assertThrows(IllegalArgumentException.class, () -> BandejaAcciones.validarVista("cerradas"));
    }

    private AccionCorrectiva accion(String estado, LocalDate fechaLimite) {
        AccionCorrectiva accion = new AccionCorrectiva();
        accion.setEstado(estado);
        accion.setFechaLimite(fechaLimite);
        return accion;
    }
}
