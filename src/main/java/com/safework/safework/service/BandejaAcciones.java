package com.safework.safework.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.safework.safework.model.AccionCorrectiva;

/** Clasificación para la bandeja; vencida es una condición, no un estado persistido. */
public final class BandejaAcciones {
    public static final int DIAS_AVISO = 7;
    private static final Set<String> VISTAS = Set.of("todas", "revision", "devueltas", "vencidas");
    private static final Set<String> ESTADOS_ABIERTOS =
            Set.of("Pendiente", "En proceso", "En revisión", "Devuelta");

    private BandejaAcciones() {}

    public record Resumen(long todas, long revision, long devueltas, long vencidas) {}

    public static String validarVista(String vista) {
        String seleccionada = vista == null || vista.isBlank() ? "todas" : vista;
        if (!VISTAS.contains(seleccionada)) {
            throw new IllegalArgumentException("Filtro de acciones no válido");
        }
        return seleccionada;
    }

    public static boolean vencida(AccionCorrectiva accion, LocalDate hoy) {
        return accion.getFechaLimite() != null
                && accion.getFechaLimite().isBefore(hoy)
                && ESTADOS_ABIERTOS.contains(accion.getEstado());
    }

    public static boolean proximaVencer(AccionCorrectiva accion, LocalDate hoy) {
        return accion.getFechaLimite() != null
                && !accion.getFechaLimite().isBefore(hoy)
                && !accion.getFechaLimite().isAfter(hoy.plusDays(DIAS_AVISO))
                && ESTADOS_ABIERTOS.contains(accion.getEstado());
    }

    public static Resumen resumir(List<AccionCorrectiva> acciones, LocalDate hoy) {
        return new Resumen(acciones.size(),
                acciones.stream().filter(a -> "En revisión".equals(a.getEstado())).count(),
                acciones.stream().filter(a -> "Devuelta".equals(a.getEstado())).count(),
                acciones.stream().filter(a -> vencida(a, hoy)).count());
    }

    public static List<AccionCorrectiva> filtrar(List<AccionCorrectiva> acciones, String vista, LocalDate hoy) {
        return switch (validarVista(vista)) {
            case "revision" -> acciones.stream().filter(a -> "En revisión".equals(a.getEstado())).toList();
            case "devueltas" -> acciones.stream().filter(a -> "Devuelta".equals(a.getEstado())).toList();
            case "vencidas" -> acciones.stream().filter(a -> vencida(a, hoy)).toList();
            default -> acciones;
        };
    }
}
