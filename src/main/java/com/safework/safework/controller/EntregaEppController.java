package com.safework.safework.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.safework.safework.model.EntregaEppFormulario;
import com.safework.safework.service.EntregaEppService;
import com.safework.safework.service.TrabajadorService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/epp")
public class EntregaEppController {
    private final EntregaEppService entregas;
    private final TrabajadorService trabajadores;

    public EntregaEppController(EntregaEppService entregas, TrabajadorService trabajadores) {
        this.entregas = entregas;
        this.trabajadores = trabajadores;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("entregas", entregas.listarTodas());
        model.addAttribute("propio", false);
        return "epp/lista";
    }

    @GetMapping("/mis-entregas")
    public String misEntregas(Principal principal, Model model) {
        try {
            model.addAttribute("entregas", entregas.listarPropias(principal.getName()));
        } catch (IllegalArgumentException e) {
            model.addAttribute("entregas", java.util.List.of());
            model.addAttribute("aviso", e.getMessage());
        }
        model.addAttribute("propio", true);
        return "epp/lista";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("entrega", new EntregaEppFormulario());
        model.addAttribute("trabajadores", trabajadores.listarTodos());
        return "epp/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("entrega") EntregaEppFormulario entrega,
            BindingResult resultado, Principal principal, Model model,
            RedirectAttributes mensajes) {
        if (!resultado.hasErrors()) {
            try {
                entregas.registrar(entrega, principal.getName());
                mensajes.addFlashAttribute("success", "Entrega de EPP registrada correctamente");
                return "redirect:/epp";
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        model.addAttribute("trabajadores", trabajadores.listarTodos());
        return "epp/formulario";
    }
}
