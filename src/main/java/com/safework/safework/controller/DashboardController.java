package com.safework.safework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.safework.safework.model.AccionCorrectiva;

import com.safework.safework.service.TrabajadorService;
import com.safework.safework.service.RiesgoService;
import com.safework.safework.service.IncidenteService;
import com.safework.safework.service.AccionCorrectivaService;

@Controller
public class DashboardController {

        private final TrabajadorService trabajadorService;

        private final RiesgoService riesgoService;

        private final IncidenteService incidenteService;

        private final AccionCorrectivaService accionService;

        public DashboardController(

                        TrabajadorService trabajadorService,

                        RiesgoService riesgoService,

                        IncidenteService incidenteService,

                        AccionCorrectivaService accionService) {

                this.trabajadorService = trabajadorService;

                this.riesgoService = riesgoService;

                this.incidenteService = incidenteService;

                this.accionService = accionService;

        }

        @GetMapping("/dashboard")

        public String dashboard(Model model) {

                // ==========================
                // Indicadores generales
                // ==========================

                model.addAttribute(

                                "totalTrabajadores",

                                trabajadorService.listarTodos().size()

                );

                model.addAttribute(

                                "totalRiesgos",

                                riesgoService.listarTodos().size()

                );

                model.addAttribute(

                                "totalIncidentes",

                                incidenteService.listarTodos().size()

                );

                List<AccionCorrectiva> acciones =

                                accionService.listarTodas();

                model.addAttribute(

                                "totalAcciones",

                                acciones.size()

                );

                // ==========================
                // Estado acciones correctivas
                // ==========================

                long accionesPendientes =

                                acciones.stream()

                                                .filter(a -> a.getEstado().equals("Pendiente"))

                                                .count();

                long accionesProceso =

                                acciones.stream()

                                                .filter(a -> a.getEstado().equals("En proceso"))

                                                .count();

                long accionesCompletadas =

                                acciones.stream()

                                                .filter(a -> a.getEstado().equals("Completada"))

                                                .count();

                model.addAttribute(

                                "accionesPendientes",

                                accionesPendientes

                );

                model.addAttribute(

                                "accionesProceso",

                                accionesProceso

                );

                model.addAttribute(

                                "accionesCompletadas",

                                accionesCompletadas

                );

                // ==========================
                // Alertas SST
                // ==========================

                LocalDate hoy = LocalDate.now();

                long accionesVencidas =

                                acciones.stream()

                                                .filter(a ->

                                                a.getFechaLimite().isBefore(hoy)

                                                                &&

                                                                !a.getEstado().equals("Completada")

                                                )

                                                .count();

                long accionesProximas =

                                acciones.stream()

                                                .filter(a -> {

                                                        long dias = ChronoUnit.DAYS.between(

                                                                        hoy,

                                                                        a.getFechaLimite()

                                                );

                                                        return dias >= 0

                                                                        && dias <= 7

                                                                        && !a.getEstado().equals("Completada");

                                                })

                                                .count();

                model.addAttribute(

                                "accionesVencidas",

                                accionesVencidas

                );

                model.addAttribute(

                                "accionesProximas",

                                accionesProximas

                );

                // ==========================
                // Últimas acciones
                // ==========================

                model.addAttribute(

                                "acciones",

                                acciones

                );

                return "dashboard";

        }

}