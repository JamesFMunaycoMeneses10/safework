package com.safework.safework.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.safework.safework.model.Trabajador;
import com.safework.safework.service.AreaService;
import com.safework.safework.service.TrabajadorService;
import com.safework.safework.repository.UsuarioRepository;

import jakarta.validation.Valid;


@Controller
public class TrabajadorController {



    private final TrabajadorService trabajadorService;

    private final AreaService areaService;
    private final UsuarioRepository usuarioRepository;



    public TrabajadorController(
            TrabajadorService trabajadorService,
            AreaService areaService, UsuarioRepository usuarioRepository) {

        this.trabajadorService = trabajadorService;
        this.areaService = areaService;
        this.usuarioRepository = usuarioRepository;

    }





    /*
     * Lista todos los trabajadores registrados
     */
    @GetMapping("/trabajadores")

    public String listarTrabajadores(Model model) {


        model.addAttribute(
                "trabajadores",
                trabajadorService.listarTodos()
        );


        return "trabajadores/lista";

    }







    /*
     * Formulario nuevo trabajador
     */
    @GetMapping("/trabajadores/nuevo")

    public String mostrarFormularioNuevo(Model model) {


        model.addAttribute(
                "trabajador",
                new Trabajador()
        );


        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );
        model.addAttribute("usuarios", usuarioRepository.findAll());


        return "trabajadores/formulario";

    }







    /*
     * Guardar o actualizar trabajador
     */
    @PostMapping("/trabajadores/guardar")

    public String guardarTrabajador(

            @Valid @ModelAttribute("trabajador") Trabajador trabajador,

            BindingResult resultado,

            Model model) {



        /*
         * Validación DNI duplicado
         */

        if(!resultado.hasFieldErrors("dni")
                &&
           trabajadorService.existeDniDuplicado(trabajador)) {



            resultado.rejectValue(
                    "dni",
                    "dni.duplicado",
                    "Ya existe un trabajador registrado con este DNI"
            );

        }




        if(resultado.hasErrors()) {



            model.addAttribute(
                    "areas",
                    areaService.listarTodas()
            );
            model.addAttribute("usuarios", usuarioRepository.findAll());


            return "trabajadores/formulario";

        }





        try {
            trabajadorService.guardar(trabajador);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("areas", areaService.listarTodas());
            model.addAttribute("usuarios", usuarioRepository.findAll());
            return "trabajadores/formulario";
        }



        return "redirect:/trabajadores";

    }







    /*
     * Editar trabajador
     */
    @GetMapping("/trabajadores/editar/{id}")

    public String mostrarFormularioEditar(

            @PathVariable Long id,

            Model model) {



        Trabajador trabajador =

                trabajadorService.buscarPorId(id)

                .orElseThrow(() ->

                    new IllegalArgumentException(
                            "Trabajador no encontrado con id: "
                            + id
                    )
                );




        model.addAttribute(
                "trabajador",
                trabajador
        );



        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );
        model.addAttribute("usuarios", usuarioRepository.findAll());



        return "trabajadores/formulario";

    }







    /*
     * Eliminar trabajador
     */
    @PostMapping("/trabajadores/eliminar/{id}")

    public String eliminarTrabajador(

            @PathVariable Long id,

            RedirectAttributes redirectAttributes) {



        try {



            trabajadorService.eliminarPorId(id);



            redirectAttributes.addFlashAttribute(
                    "success",
                    "Trabajador eliminado correctamente"
            );



        } catch(Exception e) {



            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );


        }




        return "redirect:/trabajadores";

    }



}
