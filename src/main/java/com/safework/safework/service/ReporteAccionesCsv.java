package com.safework.safework.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import com.safework.safework.model.AccionCorrectiva;

/** Exportación compatible con Excel, sin incluir archivos ni datos de acceso. */
public final class ReporteAccionesCsv {
    private ReporteAccionesCsv() {}

    public static byte[] generar(List<AccionCorrectiva> acciones, LocalDate hoy) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID;Descripción;Responsable;Origen;Prioridad;Estado;Fecha de registro;Fecha límite;Vencida\r\n");
        for (AccionCorrectiva accion : acciones) {
            String responsable = accion.getResponsable() == null ? "Sin asignar"
                    : accion.getResponsable().getNombres() + " " + accion.getResponsable().getApellidos();
            String origen = accion.getRiesgo() == null ? "" : "Riesgo: " + accion.getRiesgo().getPeligro();
            if (accion.getIncidente() != null) {
                origen += (origen.isEmpty() ? "" : " / ") + "Incidente #" + accion.getIncidente().getId();
            }
            fila(csv, String.valueOf(accion.getId()), accion.getDescripcion(), responsable, origen,
                    accion.getPrioridad(), accion.getEstado(), fecha(accion.getFechaRegistro()),
                    fecha(accion.getFechaLimite()), BandejaAcciones.vencida(accion, hoy) ? "Sí" : "No");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.toString();
    }

    private static void fila(StringBuilder csv, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) csv.append(';');
            String valor = campos[i] == null ? "" : campos[i];
            // Evita que un dato escrito por un usuario se ejecute como fórmula al abrirlo en Excel.
            if (valor.stripLeading().matches("^[=+@\\-].*")) valor = "'" + valor;
            csv.append('"').append(valor.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }
}
