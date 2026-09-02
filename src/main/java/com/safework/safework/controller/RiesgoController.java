package com.safework.safework.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.safework.safework.model.Riesgo;
import com.safework.safework.service.AreaService;
import com.safework.safework.service.RiesgoService;

import jakarta.validation.Valid;


/**
 * Controller encargado de gestionar
 * el módulo de riesgos.
 */
@Controller
@RequestMapping("/riesgos")
public class RiesgoController {



    private final RiesgoService riesgoService;

    private final AreaService areaService;



    public RiesgoController(
            RiesgoService riesgoService,
            AreaService areaService) {

        this.riesgoService = riesgoService;
        this.areaService = areaService;
    }




    /**
     * Lista todos los riesgos.
     */
    @GetMapping
    public String listarRiesgos(Model model) {


        model.addAttribute(
                "riesgos",
                riesgoService.listarTodos()
        );


        return "riesgos/lista";
    }





    /**
     * Formulario nuevo riesgo.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {


        model.addAttribute(
                "riesgo",
                new Riesgo()
        );


        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );


        return "riesgos/formulario";
    }






    /**
     * Guarda o actualiza un riesgo.
     */
    @PostMapping("/guardar")
    public String guardarRiesgo(
            @Valid @ModelAttribute("riesgo") Riesgo riesgo,
            BindingResult resultado,
            Model model) {



        if(resultado.hasErrors()){


            model.addAttribute(
                    "areas",
                    areaService.listarTodas()
            );


            return "riesgos/formulario";
        }



        riesgoService.guardar(riesgo);



        return "redirect:/riesgos";
    }





    /**
     * Editar riesgo.
     */
    @GetMapping("/editar/{id}")
    public String editarRiesgo(
            @PathVariable Long id,
            Model model) {


        Riesgo riesgo = riesgoService.buscarPorId(id)

                .orElseThrow(() ->
                        new RuntimeException(
                                "Riesgo no encontrado"));



        model.addAttribute(
                "riesgo",
                riesgo
        );



        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );



        return "riesgos/formulario";
    }






    /**
     * Eliminar riesgo.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminarRiesgo(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {


        try {


            riesgoService.eliminarPorId(id);



            redirectAttributes.addFlashAttribute(
                    "success",
                    "Riesgo eliminado correctamente"
            );



        } catch(RuntimeException e){



            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );


        }



        return "redirect:/riesgos";
    }


}