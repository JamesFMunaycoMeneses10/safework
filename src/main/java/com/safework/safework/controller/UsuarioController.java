package com.safework.safework.controller;

import com.safework.safework.model.Usuario;
import com.safework.safework.model.UsuarioFormulario;
import com.safework.safework.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        return "usuarios/index";
    }

    @GetMapping("/nuevo")
    public String nuevoUsuario(Model model) {
        model.addAttribute("usuario", new UsuarioFormulario());
        return "usuarios/crear";
    }

    @GetMapping("/editar/{id}")
    public String editarUsuario(@PathVariable Long id, Model model,
                                RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/usuarios";
        }
        model.addAttribute("usuario", UsuarioFormulario.desde(usuario));
        return "usuarios/crear";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@Valid @ModelAttribute("usuario") UsuarioFormulario usuario,
                                 BindingResult errores, Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (usuario.getId() == null && (usuario.getPassword() == null
                || usuario.getPassword().isBlank())) {
            errores.rejectValue("password", "password.obligatoria", "La contraseña es obligatoria");
        }
        if (errores.hasErrors()) {
            return "usuarios/crear";
        }
        try {
            usuarioService.guardarUsuario(usuario, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Usuario guardado correctamente");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException e) {
            errores.reject("usuario.invalido", e.getMessage());
            return "usuarios/crear";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarUsuario(id, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado correctamente");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
