package com.safework.safework.controller;

import java.security.Principal;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.safework.safework.model.CapacitacionFormulario;
import com.safework.safework.service.*;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/capacitaciones")
public class CapacitacionController {
    private final CapacitacionService service;
    private final AreaService areas;
    private final TrabajadorService trabajadores;
    public CapacitacionController(CapacitacionService service, AreaService areas, TrabajadorService trabajadores) {
        this.service = service; this.areas = areas; this.trabajadores = trabajadores;
    }
    @GetMapping
    public String lista(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String estado,
            @RequestParam(required = false) Long area, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("estadosCapacitacion", List.of("PROGRAMADA", "REALIZADA", "CANCELADA"));
        model.addAttribute("resultado", service.buscar(q, estado, area, pagina));
        model.addAttribute("q", q); model.addAttribute("estado", estado); model.addAttribute("area", area);
        model.addAttribute("areas", areas.listarTodas()); return "capacitaciones/lista";
    }
    @GetMapping("/mis-capacitaciones")
    public String propias(Principal principal, Model model) {
        model.addAttribute("participaciones", service.propias(principal.getName())); return "capacitaciones/propias";
    }
    @GetMapping("/nueva")
    public String nueva(Model model) { model.addAttribute("capacitacion", new CapacitacionFormulario()); opciones(model); return "capacitaciones/formulario"; }
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("capacitacion", service.formulario(id)); opciones(model); return "capacitaciones/formulario";
    }
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("capacitacion") CapacitacionFormulario datos, BindingResult errores,
            Principal principal, Model model, RedirectAttributes mensajes) {
        if (!errores.hasErrors()) try {
            var c = service.guardar(datos, principal.getName()); mensajes.addFlashAttribute("success", "Capacitación guardada correctamente.");
            return "redirect:/capacitaciones/" + c.getId();
        } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
        opciones(model); return "capacitaciones/formulario";
    }
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("capacitacion", service.obtener(id)); model.addAttribute("participantes", service.participantes(id));
        return "capacitaciones/detalle";
    }
    @PostMapping("/{id}/asistencia")
    public String asistencia(@PathVariable Long id, @RequestParam(defaultValue = "") List<Long> participanteIds,
            @RequestParam(defaultValue = "") List<String> estados, Principal principal, RedirectAttributes mensajes) {
        try {
            if (participanteIds.size() != estados.size() || participanteIds.size() > 500) throw new IllegalArgumentException("Asistencia no válida.");
            Map<Long,String> registros = new LinkedHashMap<>();
            for (int i = 0; i < participanteIds.size(); i++) if (registros.put(participanteIds.get(i), estados.get(i)) != null)
                throw new IllegalArgumentException("Participante repetido.");
            service.asistencia(id, registros, principal.getName()); mensajes.addFlashAttribute("success", "Asistencia registrada.");
        } catch (IllegalArgumentException ex) { mensajes.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/capacitaciones/" + id;
    }
    @PostMapping("/{id}/finalizar")
    public String finalizar(@PathVariable Long id, Principal principal, RedirectAttributes mensajes) {
        try { service.finalizar(id, principal.getName()); mensajes.addFlashAttribute("success", "Capacitación finalizada."); }
        catch (IllegalArgumentException ex) { mensajes.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/capacitaciones/" + id;
    }
    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Principal principal, RedirectAttributes mensajes) {
        try { service.cancelar(id, principal.getName()); mensajes.addFlashAttribute("success", "Capacitación cancelada."); }
        catch (IllegalArgumentException ex) { mensajes.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/capacitaciones/" + id;
    }
    private void opciones(Model model) { model.addAttribute("areas", areas.listarTodas()); model.addAttribute("trabajadores", trabajadores.listarTodos()); }
}
