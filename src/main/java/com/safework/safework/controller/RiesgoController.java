package com.safework.safework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.safework.safework.model.Riesgo;
import com.safework.safework.service.AreaService;
import com.safework.safework.service.RiesgoService;

import jakarta.validation.Valid;

/*
 * Controller encargado de recibir
 * las solicitudes web relacionadas
 * con el módulo de riesgos.
 */
@Controller
public class RiesgoController {

        /*
         * Servicio utilizado para trabajar
         * con los riesgos.
         */
        private final RiesgoService riesgoService;

        /*
         * Servicio utilizado para obtener
         * las áreas registradas.
         *
         * Lo necesitamos porque cada riesgo
         * debe pertenecer a un área.
         */
        private final AreaService areaService;

        /*
         * Inyección de dependencias
         * mediante constructor.
         *
         * Spring proporciona automáticamente:
         *
         * - RiesgoService
         * - AreaService
         */
        public RiesgoController(
                        RiesgoService riesgoService,
                        AreaService areaService) {

                this.riesgoService = riesgoService;
                this.areaService = areaService;
        }

        /*
         * Muestra todos los riesgos
         * registrados en SafeWork.
         *
         * URL:
         * http://localhost:8080/riesgos
         */
        @GetMapping("/riesgos")
        public String listarRiesgos(Model model) {

                model.addAttribute(
                                "riesgos",
                                riesgoService.listarTodos());

                return "riesgos/lista";
        }

        /*
         * Muestra el formulario para
         * registrar un nuevo riesgo.
         */
        @GetMapping("/riesgos/nuevo")
        public String mostrarFormularioNuevo(Model model) {

                /*
                 * Creamos un objeto Riesgo vacío
                 * para relacionarlo con el formulario.
                 */
                model.addAttribute(
                                "riesgo",
                                new Riesgo());

                /*
                 * También enviamos todas las áreas.
                 *
                 * De esta manera el usuario podrá
                 * seleccionar dónde se identificó
                 * el riesgo.
                 */
                model.addAttribute(
                                "areas",
                                areaService.listarTodas());

                return "riesgos/formulario";
        }

        /*
         * Guarda un nuevo riesgo
         * o actualiza uno existente.
         */
        @PostMapping("/riesgos/guardar")
        public String guardarRiesgo(
                        @Valid @ModelAttribute Riesgo riesgo,
                        BindingResult resultado,
                        Model model) {

                /*
                 * @Valid verifica las restricciones
                 * colocadas en Riesgo.java:
                 *
                 * - peligro obligatorio
                 * - descripción obligatoria
                 * - probabilidad entre 1 y 5
                 * - severidad entre 1 y 5
                 * - área obligatoria
                 */
                if (resultado.hasErrors()) {

                        /*
                         * Si existe algún error,
                         * debemos volver a cargar las áreas
                         * para que el <select> no quede vacío.
                         */
                        model.addAttribute(
                                        "areas",
                                        areaService.listarTodas());

                        return "riesgos/formulario";
                }

                /*
                 * El Controller NO calcula
                 * el nivel de riesgo.
                 *
                 * Esa lógica está dentro de RiesgoService.
                 *
                 * Allí se realizará:
                 *
                 * probabilidad × severidad
                 */
                riesgoService.guardar(riesgo);

                /*
                 * Después de guardar,
                 * volvemos al listado.
                 */
                return "redirect:/riesgos";
        }

        /*
         * Muestra el formulario para
         * editar un riesgo existente.
         */
        @GetMapping("/riesgos/editar/{id}")
        public String mostrarFormularioEditar(
                        @PathVariable Long id,
                        Model model) {

                /*
                 * Buscamos el riesgo mediante su ID.
                 */
                Riesgo riesgo = riesgoService.buscarPorId(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Riesgo no encontrado con id: " + id));

                /*
                 * Enviamos el riesgo encontrado
                 * al formulario.
                 */
                model.addAttribute(
                                "riesgo",
                                riesgo);

                /*
                 * También enviamos las áreas
                 * disponibles para el selector.
                 */
                model.addAttribute(
                                "areas",
                                areaService.listarTodas());

                return "riesgos/formulario";
        }

        /*
         * Elimina un riesgo mediante su ID.
         *
         * Utilizamos POST porque eliminar
         * modifica información almacenada.
         */
    @PostMapping("/riesgos/eliminar/{id}")
public String eliminarRiesgo(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes) {


    System.out.println("ID recibido para eliminar: " + id);


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