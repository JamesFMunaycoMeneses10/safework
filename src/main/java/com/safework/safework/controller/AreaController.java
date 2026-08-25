package com.safework.safework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.safework.safework.model.Area;
import com.safework.safework.service.AreaService;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
/*
 * Controller encargado de gestionar las solicitudes web
 * relacionadas con el módulo de áreas.
 */
@Controller
public class AreaController {

    /*
     * Servicio que contiene las operaciones
     * relacionadas con Area.
     */
    private final AreaService areaService;

    /*
     * Inyección de dependencias por constructor.
     *
     * Spring proporciona automáticamente
     * una instancia de AreaService.
     */
    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    /*
     * Muestra la lista de áreas.
     *
     * URL:
     * http://localhost:8080/areas
     */
    @GetMapping("/areas")
    public String listarAreas(Model model) {

        // Enviamos la lista de áreas al HTML.
        model.addAttribute("areas", areaService.listarTodas());

        return "areas/lista";
    }

    /*
     * Muestra el formulario para registrar
     * una nueva área.
     *
     * URL:
     * http://localhost:8080/areas/nueva
     */
    @GetMapping("/areas/nueva")
    public String mostrarFormularioNuevaArea(Model model) {

        /*
         * Creamos un objeto Area vacío que será
         * utilizado por el formulario de Thymeleaf.
         */
        model.addAttribute("area", new Area());

        return "areas/formulario";
    }

    /*
     * Muestra el formulario para editar un área existente.
     *
     * {id} representa el identificador del área
     * que queremos modificar.
     *
     * Ejemplo:
     * http://localhost:8080/areas/editar/1
     */
    @GetMapping("/areas/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {

        /*
         * Buscamos el área mediante su id.
         *
         * buscarPorId() devuelve Optional<Area>,
         * porque el área podría existir o no.
         *
         * orElseThrow() obtiene el Area si existe.
         * Si no existe, genera un error indicando
         * que no se encontró el registro.
         */
        Area area = areaService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Área no encontrada con id: " + id));

        // Enviamos el área encontrada al formulario.
        model.addAttribute("area", area);

        // Reutilizamos el mismo formulario.
        return "areas/formulario";
    }
/*
 * Elimina un área utilizando su identificador.
 *
 * {id} representa el id del área que se desea eliminar.
 *
 * Ejemplo:
 * http://localhost:8080/areas/eliminar/1
 */
/*
 * Elimina un área utilizando su identificador.
 *
 * Utilizamos POST porque eliminar modifica
 * la información almacenada en el sistema.
 */
@PostMapping("/areas/eliminar/{id}")
public String eliminarArea(@PathVariable Long id) {

    // Eliminamos el área mediante la capa Service.
    areaService.eliminarPorId(id);

    // Después regresamos al listado.
    return "redirect:/areas";
}
    /*
     * Recibe los datos enviados por el formulario
     * y guarda el área.
     */
   /*
 * Recibe los datos enviados desde el formulario
 * y valida el objeto Area antes de guardarlo.
 */
@PostMapping("/areas/guardar")
public String guardarArea(
        @Valid @ModelAttribute Area area,
        BindingResult resultado) {

    /*
     * Si alguna validación de Area falla,
     * regresamos al formulario.
     *
     * Por ejemplo:
     * - nombre vacío
     * - nombre mayor a 100 caracteres
     * - descripción mayor a 255 caracteres
     */
    if (resultado.hasErrors()) {
        return "areas/formulario";
    }

    // Si no existen errores, guardamos el área.
    areaService.guardar(area);

    // Después regresamos al listado.
    return "redirect:/areas";
}
}