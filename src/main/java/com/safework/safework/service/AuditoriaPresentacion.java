package com.safework.safework.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.safework.safework.model.AuditoriaEvento;

/** Organiza los detalles históricos para mostrar la bitácora de forma legible. */
public record AuditoriaPresentacion(AuditoriaEvento evento, List<Cambio> cambios,
        List<Dato> datos, boolean estructurado) {
    private static final String SEPARADOR_EDICION = " | Después: ";
    private static final Pattern SEPARADOR_CAMPO = Pattern.compile(
            " \\| (?=(?:Peligro|Descripción|Área|Probabilidad|Severidad|Nivel|Medida|Fecha y hora|Tipo|Trabajador|Gravedad|Estado|Responsable|Riesgo|Incidente|Prioridad|Fecha límite): )");

    public record Cambio(String campo, String antes, String despues) {}
    public record Dato(String campo, String valor) {}

    public static AuditoriaPresentacion desde(AuditoriaEvento evento) {
        String detalle = evento.getDetalle();
        if (detalle == null || detalle.isBlank()) {
            return new AuditoriaPresentacion(evento, List.of(), List.of(), false);
        }
        if ("ACTUALIZADO".equals(evento.getOperacion()) && detalle.startsWith("Antes: ")) {
            int separador = detalle.indexOf(SEPARADOR_EDICION);
            if (separador >= 0) {
                Map<String, String> antes = campos(detalle.substring("Antes: ".length(), separador));
                Map<String, String> despues = campos(detalle.substring(separador + SEPARADOR_EDICION.length()));
                if (!antes.isEmpty() && !despues.isEmpty()) {
                    List<Cambio> cambios = new ArrayList<>();
                    despues.forEach((campo, valor) -> {
                        if (!Objects.equals(antes.get(campo), valor)) {
                            cambios.add(new Cambio(campo, antes.getOrDefault(campo, "—"), valor));
                        }
                    });
                    return new AuditoriaPresentacion(evento, cambios, List.of(), true);
                }
            }
        }
        Map<String, String> datos = campos(detalle);
        if (!datos.isEmpty() && !"ACTUALIZADO".equals(evento.getOperacion())) {
            return new AuditoriaPresentacion(evento, List.of(),
                    datos.entrySet().stream().map(e -> new Dato(e.getKey(), e.getValue())).toList(), true);
        }
        return new AuditoriaPresentacion(evento, List.of(), List.of(), false);
    }

    private static Map<String, String> campos(String texto) {
        Map<String, String> resultado = new LinkedHashMap<>();
        for (String parte : SEPARADOR_CAMPO.split(texto)) {
            int dosPuntos = parte.indexOf(": ");
            if (dosPuntos < 1) return Map.of();
            resultado.put(parte.substring(0, dosPuntos), parte.substring(dosPuntos + 2));
        }
        return resultado;
    }
}
