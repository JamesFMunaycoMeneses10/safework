package com.safework.safework;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.service.ReporteAccionesCsv;

class ReporteAccionesCsvTests {
    @Test
    void escapaSeparadoresComillasYFormulas() {
        AccionCorrectiva accion = new AccionCorrectiva();
        accion.setId(12L);
        accion.setDescripcion("=SUMA(1;2) \"prueba\"");
        accion.setEstado("Devuelta");
        accion.setFechaLimite(LocalDate.of(2026, 9, 1));

        String csv = new String(ReporteAccionesCsv.generar(List.of(accion),
                LocalDate.of(2026, 9, 24)), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFFID;Descripción;");
        assertThat(csv).contains("\"'=SUMA(1;2) \"\"prueba\"\"\"");
        assertThat(csv).contains("\"Sí\"\r\n");
    }
}
