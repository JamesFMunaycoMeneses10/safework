package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.AuditoriaService;
import com.safework.safework.service.TrabajadorService;

/** Ejecuta las consultas paginadas reales contra la base configurada, sin modificar registros. */
@SpringBootTest
class ConsultasPaginadasTests {
    @Autowired private AccionCorrectivaService acciones;
    @Autowired private AuditoriaService auditoria;
    @Autowired private TrabajadorService trabajadores;

    @Test
    void filtrosDeTrabajadoresConservanAreaDniYCargo() {
        var todos = trabajadores.buscarConFiltros(null, "", "");
        assertNotNull(todos);
        assertNotNull(trabajadores.listarCargos());
        assertTrue(trabajadores.buscarConFiltros(null, "dni-inexistente", "").isEmpty());
        if (!todos.isEmpty()) {
            var ejemplo = todos.getFirst();
            var filtrados = trabajadores.buscarConFiltros(ejemplo.getArea().getId(),
                    " " + ejemplo.getDni().substring(0, 4) + " ", ejemplo.getCargo());
            assertTrue(filtrados.stream().anyMatch(t -> t.getId().equals(ejemplo.getId())));
            assertTrue(filtrados.stream().allMatch(t ->
                    t.getArea().getId().equals(ejemplo.getArea().getId())
                    && t.getDni().contains(ejemplo.getDni().substring(0, 4))
                    && t.getCargo().equalsIgnoreCase(ejemplo.getCargo())));
        }
    }

    @Test
    void consultasDeAccionesYBitacora() {
        assertNotNull(acciones.buscar(null, null, "", "todas", LocalDate.now(), 0));
        assertNotNull(acciones.buscar(null, null, "almacén", "vencidas", LocalDate.now(), 0));
        assertNotNull(acciones.resumen(null, null, LocalDate.now()));
        assertNotNull(acciones.proximas(null, null, LocalDate.now()));
        assertNotNull(auditoria.buscar("", 0));
        assertNotNull(auditoria.buscar("supervisor", 0));
    }
}
