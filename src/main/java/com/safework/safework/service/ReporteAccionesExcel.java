package com.safework.safework.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.safework.safework.model.AccionCorrectiva;

/** Reporte de una sola hoja; los valores de usuario se escriben como texto, nunca como fórmulas. */
public final class ReporteAccionesExcel {
    private static final String[] CABECERAS = {
            "ID", "Descripción", "Responsable", "Origen", "Prioridad", "Estado",
            "Fecha de registro", "Fecha límite", "Vencida"
    };

    private ReporteAccionesExcel() {}

    public static byte[] generar(List<AccionCorrectiva> acciones, LocalDate hoy) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            XSSFSheet hoja = libro.createSheet("Acciones correctivas");
            hoja.setDisplayGridlines(false);
            hoja.createFreezePane(0, 4);
            int[] anchos = {11, 55, 29, 46, 16, 19, 20, 18, 14};
            for (int i = 0; i < anchos.length; i++) hoja.setColumnWidth(i, anchos[i] * 256);

            CellStyle titulo = estiloTitulo(libro);
            CellStyle cabecera = estiloCabecera(libro);
            CellStyle texto = estiloDatos(libro);
            CellStyle alterno = estiloDatos(libro);
            alterno.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            alterno.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle fecha = estiloFecha(libro, texto);
            CellStyle fechaAlterna = estiloFecha(libro, alterno);
            CellStyle vencida = estiloDatos(libro);
            vencida.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            vencida.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row filaTitulo = hoja.createRow(0);
            filaTitulo.setHeightInPoints(34);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("Reporte de acciones correctivas");
            celdaTitulo.setCellStyle(titulo);
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            Row metadata = hoja.createRow(1);
            metadata.createCell(0).setCellValue("Generado: " + hoy + "   |   Acciones: " + acciones.size());
            hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

            Row encabezado = hoja.createRow(3);
            encabezado.setHeightInPoints(29);
            for (int i = 0; i < CABECERAS.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(CABECERAS[i]);
                celda.setCellStyle(cabecera);
            }

            for (int indice = 0; indice < acciones.size(); indice++) {
                AccionCorrectiva accion = acciones.get(indice);
                Row fila = hoja.createRow(indice + 4);
                fila.setHeightInPoints(34);
                CellStyle base = indice % 2 == 0 ? texto : alterno;
                CellStyle estiloFecha = indice % 2 == 0 ? fecha : fechaAlterna;
                if (accion.getId() != null) {
                    Cell id = fila.createCell(0);
                    id.setCellValue(accion.getId());
                    id.setCellStyle(base);
                }
                ponerTexto(fila, 1, accion.getDescripcion(), base);
                ponerTexto(fila, 2, accion.getResponsable() == null ? "Sin asignar"
                        : accion.getResponsable().getNombres() + " " + accion.getResponsable().getApellidos(), base);
                String origen = accion.getRiesgo() == null ? "" : "Riesgo: " + accion.getRiesgo().getPeligro();
                if (accion.getIncidente() != null) {
                    origen += (origen.isEmpty() ? "" : " / ") + "Incidente #" + accion.getIncidente().getId();
                }
                ponerTexto(fila, 3, origen, base);
                ponerTexto(fila, 4, accion.getPrioridad(), base);
                ponerTexto(fila, 5, accion.getEstado(), base);
                ponerFecha(fila, 6, accion.getFechaRegistro(), estiloFecha);
                ponerFecha(fila, 7, accion.getFechaLimite(), estiloFecha);
                boolean estaVencida = BandejaAcciones.vencida(accion, hoy);
                ponerTexto(fila, 8, estaVencida ? "Sí" : "No", estaVencida ? vencida : base);
            }
            hoja.setAutoFilter(new CellRangeAddress(3, Math.max(4, acciones.size() + 3), 0, 8));
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private static void ponerTexto(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor == null ? "" : valor);
        celda.setCellStyle(estilo);
    }

    private static void ponerFecha(Row fila, int columna, LocalDate valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        if (valor != null) celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private static CellStyle estiloTitulo(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 17);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        return estilo;
    }

    private static CellStyle estiloCabecera(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        return estilo;
    }

    private static CellStyle estiloDatos(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setWrapText(true);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setBorderBottom(BorderStyle.HAIR);
        estilo.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return estilo;
    }

    private static CellStyle estiloFecha(Workbook libro, CellStyle base) {
        CellStyle estilo = libro.createCellStyle();
        estilo.cloneStyleFrom(base);
        estilo.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
        estilo.setAlignment(HorizontalAlignment.CENTER);
        return estilo;
    }
}
