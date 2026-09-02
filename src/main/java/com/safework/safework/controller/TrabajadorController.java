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

import jakarta.validation.Valid;

/*
 * Controller encargado de recibir las solicitudes web
 * relacionadas con el módulo de trabajadores.
 */
@Controller
public class TrabajadorController {

        /*
         * Servicio utilizado para trabajar
         * con los trabajadores.
         */
        private final TrabajadorService trabajadorService;

        /*
         * Servicio utilizado para obtener las áreas.
         *
         * Lo necesitamos porque cada trabajador
         * debe pertenecer a un área.
         */
        private final AreaService areaService;

        /*
         * Inyección de dependencias por constructor.
         *
         * Spring proporciona automáticamente:
         *
         * - TrabajadorService
         * - AreaService
         */
        public TrabajadorController(
                        TrabajadorService trabajadorService,
                        AreaService areaService) {

                this.trabajadorService = trabajadorService;
                this.areaService = areaService;
        }

        /*
         * Muestra todos los trabajadores registrados.
         *
         * URL:
         * http://localhost:8080/trabajadores
         */
        @GetMapping("/trabajadores")
        public String listarTrabajadores(Model model) {

                /*
                 * Enviamos al HTML la lista
                 * de trabajadores.
                 */
                model.addAttribute(
                                "trabajadores",
                                trabajadorService.listarTodos());

                return "trabajadores/lista";
        }

        /*
         * Muestra el formulario para registrar
         * un trabajador nuevo.
         */
        @GetMapping("/trabajadores/nuevo")
        public String mostrarFormularioNuevo(Model model) {

                /*
                 * Creamos un trabajador vacío
                 * para relacionarlo con el formulario.
                 */
                model.addAttribute(
                                "trabajador",
                                new Trabajador());

                /*
                 * Enviamos todas las áreas existentes.
                 *
                 * Estas áreas serán utilizadas
                 * para construir el selector <select>.
                 */
                model.addAttribute(
                                "areas",
                                areaService.listarTodas());

                return "trabajadores/formulario";
        }

        /*
         * Guarda un trabajador nuevo
         * o actualiza uno existente.
         *
         * Antes de guardar:
         *
         * 1. Se ejecutan las validaciones de @Valid.
         * 2. Se comprueba que el DNI no esté duplicado.
         */
        @PostMapping("/trabajadores/guardar")
        public String guardarTrabajador(
                        @Valid @ModelAttribute Trabajador trabajador,
                        BindingResult resultado,
                        Model model) {

                /*
                 * Primero verificamos que el campo DNI
                 * no tenga errores de formato.
                 *
                 * Por ejemplo:
                 *
                 * - DNI vacío
                 * - DNI con menos de 8 números
                 * - DNI con letras
                 *
                 * Si el DNI ya tiene un error,
                 * no consultamos todavía si está duplicado.
                 */
                if (!resultado.hasFieldErrors("dni")
                                && trabajadorService.existeDniDuplicado(trabajador)) {

                        /*
                         * Si el DNI ya existe,
                         * agregamos manualmente un error
                         * al campo "dni".
                         *
                         * Thymeleaf mostrará este mensaje
                         * debajo del campo DNI.
                         */
                        resultado.rejectValue(
                                        "dni",
                                        "dni.duplicado",
                                        "Ya existe un trabajador registrado con este DNI");
                }

                /*
                 * Después de comprobar también
                 * el DNI duplicado, revisamos
                 * si existe cualquier error.
                 */
                if (resultado.hasErrors()) {

                        /*
                         * Volvemos a enviar la lista de áreas.
                         *
                         * Esto es necesario porque el formulario
                         * utiliza esta información para mostrar
                         * el selector de áreas.
                         */
                        model.addAttribute(
                                        "areas",
                                        areaService.listarTodas());

                        /*
                         * Regresamos al formulario.
                         *
                         * No se guarda nada en la base de datos.
                         */
                        return "trabajadores/formulario";
                }

                /*
                 * Si no existen errores:
                 *
                 * - DNI correcto
                 * - DNI no duplicado
                 * - nombres correctos
                 * - apellidos correctos
                 * - cargo correcto
                 * - área seleccionada
                 *
                 * recién guardamos el trabajador.
                 */
                trabajadorService.guardar(trabajador);

                /*
                 * Después de guardar,
                 * regresamos al listado.
                 */
                return "redirect:/trabajadores";
        }

        /*
         * Muestra el formulario para editar
         * un trabajador existente.
         */
        @GetMapping("/trabajadores/editar/{id}")
        public String mostrarFormularioEditar(
                        @PathVariable Long id,
                        Model model) {

                /*
                 * Buscamos el trabajador mediante su ID.
                 *
                 * buscarPorId() devuelve Optional<Trabajador>.
                 *
                 * Si existe:
                 * obtenemos el trabajador.
                 *
                 * Si no existe:
                 * lanzamos una excepción.
                 */
                Trabajador trabajador = trabajadorService.buscarPorId(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Trabajador no encontrado con id: " + id));

                /*
                 * Enviamos el trabajador encontrado
                 * al formulario.
                 */
                model.addAttribute(
                                "trabajador",
                                trabajador);

                /*
                 * También enviamos todas las áreas
                 * disponibles para el selector.
                 */
                model.addAttribute(
                                "areas",
                                areaService.listarTodas());

                return "trabajadores/formulario";
        }

        /*
         * Elimina un trabajador mediante su ID.
         *
         * Utilizamos POST porque eliminar
         * modifica información del sistema.
         */
        @PostMapping("/trabajadores/eliminar/{id}")
        public String eliminarTrabajador(
                        @PathVariable Long id,
                        RedirectAttributes redirectAttributes) {

                try {

                        trabajadorService.eliminarPorId(id);

                        redirectAttributes.addFlashAttribute(
                                        "success",
                                        "Trabajador eliminado correctamente");

                } catch (Exception e) {

                        redirectAttributes.addFlashAttribute(
                                        "error",
                                        e.getMessage());

                }

                return "redirect:/trabajadores";
        }
}