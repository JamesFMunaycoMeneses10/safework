package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.AuditoriaService;

/** Ejecuta las consultas paginadas reales contra la base configurada, sin modificar registros. */
@SpringBootTest
class ConsultasPaginadasTests {
    @Autowired private AccionCorrectivaService acciones;
    @Autowired private AuditoriaService auditoria;

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
