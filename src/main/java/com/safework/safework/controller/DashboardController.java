package com.safework.safework.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


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



        model.addAttribute(
                "totalAcciones",
                accionService.listarTodas().size()
        );



        // Últimas acciones correctivas

        model.addAttribute(
                "acciones",
                accionService.listarTodas()
        );



        return "dashboard";

    }


}