package com.safework.safework.controller;


import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.safework.safework.model.Incidente;
import com.safework.safework.service.AreaService;
import com.safework.safework.service.IncidenteService;
import com.safework.safework.service.TrabajadorService;

import jakarta.validation.Valid;



/**
 * Controller encargado de gestionar
 * el módulo de Incidentes y Accidentes
 * dentro de SafeWork.
 */
@Controller
@RequestMapping("/incidentes")
public class IncidenteController {


    private final IncidenteService incidenteService;

    private final AreaService areaService;

    private final TrabajadorService trabajadorService;



    /**
     * Inyección de dependencias.
     */
    public IncidenteController(
            IncidenteService incidenteService,
            AreaService areaService,
            TrabajadorService trabajadorService) {

        this.incidenteService = incidenteService;
        this.areaService = areaService;
        this.trabajadorService = trabajadorService;
    }



    /**
     * Mostrar lista de incidentes registrados.
     *
     * URL:
     * /incidentes
     */
    @GetMapping
    public String listar(Model model) {


        model.addAttribute(
                "incidentes",
                incidenteService.listarTodos()
        );


        return "incidentes/lista";
    }




    /**
     * Mostrar formulario para registrar
     * un nuevo incidente.
     */
    @GetMapping("/nuevo")
    public String nuevo(Model model) {


        model.addAttribute(
                "incidente",
                new Incidente()
        );


        cargarDatosFormulario(model);


        return "incidentes/formulario";
    }





    /**
     * Guardar incidente.
     */
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("incidente") Incidente incidente,
            BindingResult resultado,
            Model model, Authentication authentication) {


        if (resultado.hasErrors()) {


            cargarDatosFormulario(model);


            return "incidentes/formulario";
        }



        incidenteService.guardar(incidente, authentication.getName());



        return "redirect:/incidentes";
    }





    /**
     * Editar incidente existente.
     */
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model) {


        Incidente incidente =
                incidenteService.buscarPorId(id)
                .orElseThrow();



        model.addAttribute(
                "incidente",
                incidente
        );



        cargarDatosFormulario(model);



        return "incidentes/formulario";
    }







    /**
     * Eliminar incidente.
     *
     * Valida si tiene acciones correctivas
     * asociadas antes de eliminar.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {


        try {


            incidenteService.eliminar(id, authentication.getName());



            redirectAttributes.addFlashAttribute(
                    "success",
                    "Incidente eliminado correctamente"
            );



        } catch (RuntimeException e) {



            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

        }



        return "redirect:/incidentes";
    }







    /**
     * Carga información necesaria
     * para los combos del formulario:
     *
     * - Áreas
     * - Trabajadores
     */
    private void cargarDatosFormulario(Model model) {


        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );


        model.addAttribute(
                "trabajadores",
                trabajadorService.listarTodos()
        );
    }

}
