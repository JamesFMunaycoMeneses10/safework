package com.safework.safework;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.service.ReporteAccionesExcel;

class ReporteAccionesExcelTests {
    @Test
    void creaLibroLegibleConFechasYDatosSinFormulas() throws Exception {
        AccionCorrectiva accion = new AccionCorrectiva();
        accion.setId(12L);
        accion.setDescripcion("=SUMA(1;2)");
        accion.setEstado("Devuelta");
        accion.setFechaRegistro(LocalDate.of(2026, 9, 23));
        accion.setFechaLimite(LocalDate.of(2026, 9, 30));

        byte[] bytes = ReporteAccionesExcel.generar(List.of(accion), LocalDate.of(2026, 10, 1));
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var hoja = libro.getSheet("Acciones correctivas");
            assertThat(hoja.getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Reporte de acciones correctivas");
            assertThat(hoja.getRow(3).getCell(7).getStringCellValue()).isEqualTo("Fecha límite");
            assertThat(hoja.getRow(4).getCell(1).getCellType()).isEqualTo(CellType.STRING);
            assertThat(hoja.getRow(4).getCell(1).getStringCellValue()).isEqualTo("=SUMA(1;2)");
            assertThat(hoja.getRow(4).getCell(7).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(hoja.getRow(4).getCell(7).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 9, 30));
            assertThat(hoja.getRow(4).getCell(8).getStringCellValue()).isEqualTo("Sí");
            assertThat(hoja.getCTWorksheet().getAutoFilter()).isNotNull();
            assertThat(hoja.getPaneInformation().isFreezePane()).isTrue();
        }
    }
}
