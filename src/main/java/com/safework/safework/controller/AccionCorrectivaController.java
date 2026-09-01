package com.safework.safework.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.LocalDate;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.TrabajadorService;
import com.safework.safework.service.RiesgoService;
import com.safework.safework.service.IncidenteService;



@Controller
@RequestMapping("/acciones")
public class AccionCorrectivaController {



    private final AccionCorrectivaService accionService;
    private final TrabajadorService trabajadorService;
    private final RiesgoService riesgoService;
    private final IncidenteService incidenteService;



    public AccionCorrectivaController(
            AccionCorrectivaService accionService,
            TrabajadorService trabajadorService,
            RiesgoService riesgoService,
            IncidenteService incidenteService) {

        this.accionService = accionService;
        this.trabajadorService = trabajadorService;
        this.riesgoService = riesgoService;
        this.incidenteService = incidenteService;
    }





    @GetMapping
    public String listar(Model model) {


        model.addAttribute(
                "acciones",
                accionService.listarTodas()
        );


        return "acciones/lista";
    }





    @GetMapping("/nueva")
    public String nuevaAccion(Model model) {


        AccionCorrectiva accion = new AccionCorrectiva();


        accion.setFechaRegistro(
                LocalDate.now()
        );


        cargarCombos(model);


        model.addAttribute(
                "accion",
                accion
        );


        return "acciones/formulario";
    }





    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("accion") AccionCorrectiva accion,
            BindingResult result,
            Model model) {



        if(result.hasErrors()){

            cargarCombos(model);

            return "acciones/formulario";
        }



        /*
         * Si tiene ID significa que estamos editando.
         * Si no tiene ID es un registro nuevo.
         */

        if(accion.getId() != null){


            AccionCorrectiva existente =
                    accionService.buscarPorId(accion.getId())
                    .orElse(null);



            if(existente != null){


                existente.setDescripcion(
                        accion.getDescripcion()
                );


                existente.setFechaRegistro(
                        accion.getFechaRegistro()
                );


                existente.setFechaLimite(
                        accion.getFechaLimite()
                );


                existente.setEstado(
                        accion.getEstado()
                );


                existente.setPrioridad(
                        accion.getPrioridad()
                );


                existente.setResponsable(
                        accion.getResponsable()
                );


                existente.setRiesgo(
                        accion.getRiesgo()
                );


                existente.setIncidente(
                        accion.getIncidente()
                );



                accionService.guardar(existente);

            }



        }else{


            accionService.guardar(accion);


        }



        return "redirect:/acciones";
    }







    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model) {



        AccionCorrectiva accion =
                accionService.buscarPorId(id)
                .orElse(null);



        if(accion == null){

            return "redirect:/acciones";

        }



        cargarCombos(model);



        model.addAttribute(
                "accion",
                accion
        );



        return "acciones/formulario";
    }








    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id) {


        accionService.eliminarPorId(id);


        return "redirect:/acciones";
    }







    private void cargarCombos(Model model){


        model.addAttribute(
                "trabajadores",
                trabajadorService.listarTodos()
        );



        model.addAttribute(
                "riesgos",
                riesgoService.listarTodos()
        );



        model.addAttribute(
                "incidentes",
                incidenteService.listarTodos()
        );

    }


}