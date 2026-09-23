package com.safework.safework.controller;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.model.Incidente;
import com.safework.safework.model.Riesgo;
import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.IncidenteService;
import com.safework.safework.service.RiesgoService;
import com.safework.safework.service.TrabajadorService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final TrabajadorService trabajadorService;
    private final RiesgoService riesgoService;
    private final IncidenteService incidenteService;
    private final AccionCorrectivaService accionService;

    public DashboardController(TrabajadorService trabajadorService, RiesgoService riesgoService,
                               IncidenteService incidenteService, AccionCorrectivaService accionService) {
        this.trabajadorService = trabajadorService;
        this.riesgoService = riesgoService;
        this.incidenteService = incidenteService;
        this.accionService = accionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        List<Riesgo> riesgos = riesgoService.listarTodos();
        List<Incidente> incidentes = incidenteService.listarTodos();
        List<AccionCorrectiva> acciones = accionService.listarTodas();
        LocalDate hoy = LocalDate.now();

        long pendientes = acciones.stream().filter(a -> "Pendiente".equalsIgnoreCase(a.getEstado())).count();
        long proceso = acciones.stream().filter(a -> "En proceso".equalsIgnoreCase(a.getEstado())).count();
        long completadas = acciones.stream().filter(a -> "Completada".equalsIgnoreCase(a.getEstado())).count();
        long vencidas = acciones.stream().filter(a -> a.getFechaLimite() != null
                && a.getFechaLimite().isBefore(hoy) && !"Completada".equalsIgnoreCase(a.getEstado())).count();
        long proximas = acciones.stream().filter(a -> a.getFechaLimite() != null
                && ChronoUnit.DAYS.between(hoy, a.getFechaLimite()) >= 0
                && ChronoUnit.DAYS.between(hoy, a.getFechaLimite()) <= 7
                && !"Completada".equalsIgnoreCase(a.getEstado())).count();

        // Las bandas coinciden con Riesgo.getClasificacionRiesgo().
        long riesgosBajos = riesgos.stream().filter(r -> r.getNivelRiesgo() != null
                && r.getNivelRiesgo() <= 4).count();
        long riesgosMedios = riesgos.stream().filter(r -> r.getNivelRiesgo() != null
                && r.getNivelRiesgo() > 4 && r.getNivelRiesgo() <= 9).count();
        long riesgosAltos = riesgos.stream().filter(r -> r.getNivelRiesgo() != null
                && r.getNivelRiesgo() > 9 && r.getNivelRiesgo() <= 16).count();
        long riesgosCriticos = riesgos.stream().filter(r -> r.getNivelRiesgo() != null
                && r.getNivelRiesgo() > 16).count();
        int[] incidentesPorMes = new int[12];
        incidentes.stream().filter(i -> i.getFechaHora() != null
                && i.getFechaHora().getYear() == hoy.getYear())
                .forEach(i -> incidentesPorMes[i.getFechaHora().getMonthValue() - 1]++);

        String estadoSST = vencidas > 0 ? "CRÍTICO"
                : pendientes > 0 || proceso > 0 ? "ATENCIÓN" : "SEGURO";
        String mensajeSST = vencidas > 0 ? "Existen acciones correctivas vencidas"
                : pendientes > 0 || proceso > 0 ? "Existen acciones pendientes de seguimiento"
                : "La gestión SST se encuentra controlada";

        model.addAttribute("nombreUsuario", authentication == null ? "Usuario" : authentication.getName());
        model.addAttribute("totalTrabajadores", trabajadorService.listarTodos().size());
        model.addAttribute("totalRiesgos", riesgos.size());
        model.addAttribute("totalIncidentes", incidentes.size());
        model.addAttribute("totalAcciones", acciones.size());
        model.addAttribute("accionesPendientes", pendientes);
        model.addAttribute("accionesProceso", proceso);
        model.addAttribute("accionesCompletadas", completadas);
        model.addAttribute("accionesVencidas", vencidas);
        model.addAttribute("accionesProximas", proximas);
        model.addAttribute("riesgosBajos", riesgosBajos);
        model.addAttribute("riesgosMedios", riesgosMedios);
        model.addAttribute("riesgosAltos", riesgosAltos);
        model.addAttribute("riesgosCriticos", riesgosCriticos);
        model.addAttribute("riesgosDistribucion", new long[] {
                riesgosCriticos, riesgosAltos, riesgosMedios, riesgosBajos});
        model.addAttribute("incidentesPorMes", incidentesPorMes);
        model.addAttribute("anioActual", hoy.getYear());
        model.addAttribute("estadoSST", estadoSST);
        model.addAttribute("mensajeSST", mensajeSST);
        model.addAttribute("ultimasAcciones", acciones.stream()
                .sorted(Comparator.comparing(AccionCorrectiva::getFechaRegistro,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).toList());
        return "dashboard";
    }
}
