package com.safework.safework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.safework.safework.model.Inspeccion;
import com.safework.safework.service.AreaService;
import com.safework.safework.service.InspeccionService;
import com.safework.safework.service.TrabajadorService;

import jakarta.validation.Valid;


/*
 * Controller encargado de recibir
 * las solicitudes web relacionadas
 * con el módulo de inspecciones.
 */
@Controller
public class InspeccionController {


    /*
     * Servicio encargado de trabajar
     * con las inspecciones.
     */
    private final InspeccionService inspeccionService;


    /*
     * Servicio utilizado para obtener
     * las áreas registradas.
     */
    private final AreaService areaService;


    /*
     * Servicio utilizado para obtener
     * los trabajadores registrados.
     *
     * Los trabajadores podrán ser
     * responsables de una inspección.
     */
    private final TrabajadorService trabajadorService;


    /*
     * Inyección de dependencias por constructor.
     *
     * Spring proporciona automáticamente:
     *
     * - InspeccionService
     * - AreaService
     * - TrabajadorService
     */
    public InspeccionController(
            InspeccionService inspeccionService,
            AreaService areaService,
            TrabajadorService trabajadorService) {

        this.inspeccionService = inspeccionService;
        this.areaService = areaService;
        this.trabajadorService = trabajadorService;
    }


    /*
     * Muestra todas las inspecciones
     * registradas en SafeWork.
     *
     * URL:
     * http://localhost:8080/inspecciones
     */
    @GetMapping("/inspecciones")
    public String listarInspecciones(Model model) {


        /*
         * Enviamos la lista de inspecciones
         * hacia lista.html.
         */
        model.addAttribute(
                "inspecciones",
                inspeccionService.listarTodas()
        );


        return "inspecciones/lista";
    }


    /*
     * Muestra el formulario para registrar
     * una nueva inspección.
     */
    @GetMapping("/inspecciones/nueva")
    public String mostrarFormularioNuevo(Model model) {


        /*
         * Creamos una inspección vacía
         * para relacionarla con el formulario.
         */
        model.addAttribute(
                "inspeccion",
                new Inspeccion()
        );


        /*
         * Enviamos todas las áreas.
         *
         * El usuario seleccionará
         * dónde se realizará la inspección.
         */
        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );


        /*
         * Enviamos todos los trabajadores.
         *
         * El usuario seleccionará
         * quién será el responsable.
         */
        model.addAttribute(
                "trabajadores",
                trabajadorService.listarTodos()
        );


        return "inspecciones/formulario";
    }


    /*
     * Guarda una inspección nueva
     * o actualiza una existente.
     */
    @PostMapping("/inspecciones/guardar")
    public String guardarInspeccion(
            @Valid @ModelAttribute Inspeccion inspeccion,
            BindingResult resultado,
            Model model) {


        /*
         * Si existen errores de validación,
         * regresamos al formulario.
         */
        if (resultado.hasErrors()) {


            /*
             * Es necesario volver a enviar
             * las áreas.
             */
            model.addAttribute(
                    "areas",
                    areaService.listarTodas()
            );


            /*
             * También debemos volver a enviar
             * los trabajadores.
             *
             * Si no hacemos esto, el selector
             * de responsable quedaría vacío.
             */
            model.addAttribute(
                    "trabajadores",
                    trabajadorService.listarTodos()
            );


            return "inspecciones/formulario";
        }


        /*
         * Si todos los datos son correctos,
         * guardamos mediante la capa Service.
         */
        inspeccionService.guardar(inspeccion);


        /*
         * Después de guardar,
         * regresamos al listado.
         */
        return "redirect:/inspecciones";
    }


    /*
     * Muestra el formulario para editar
     * una inspección existente.
     */
    @GetMapping("/inspecciones/editar/{id}")
    public String mostrarFormularioEditar(
            @PathVariable Long id,
            Model model) {


        /*
         * Buscamos la inspección mediante su ID.
         */
        Inspeccion inspeccion =
                inspeccionService.buscarPorId(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Inspección no encontrada con id: " + id
                                )
                        );


        /*
         * Enviamos la inspección encontrada
         * al formulario.
         */
        model.addAttribute(
                "inspeccion",
                inspeccion
        );


        /*
         * Enviamos todas las áreas
         * disponibles.
         */
        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );


        /*
         * Enviamos todos los trabajadores
         * disponibles.
         */
        model.addAttribute(
                "trabajadores",
                trabajadorService.listarTodos()
        );


        return "inspecciones/formulario";
    }


    /*
     * Elimina una inspección mediante su ID.
     *
     * Utilizamos POST porque eliminar
     * modifica los datos del sistema.
     */
    @PostMapping("/inspecciones/eliminar/{id}")
    public String eliminarInspeccion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            inspeccionService.eliminarPorId(id);
            redirectAttributes.addFlashAttribute(
                    "success", "Inspección eliminada correctamente");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/inspecciones";
    }

}
