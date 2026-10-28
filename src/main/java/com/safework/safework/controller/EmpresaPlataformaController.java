package com.safework.safework.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.safework.safework.model.EmpresaAltaFormulario;
import com.safework.safework.model.EmpresaEdicionFormulario;
import com.safework.safework.service.EmpresaAdministracionService;

@Controller
@RequestMapping("/plataforma/empresas")
public class EmpresaPlataformaController {
    private final EmpresaAdministracionService empresas;
    public EmpresaPlataformaController(EmpresaAdministracionService empresas) { this.empresas = empresas; }

    @InitBinder("alta")
    void limitarAlta(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "codigo", "adminUsername", "adminPassword", "adminNombre", "adminApellido", "adminCorreo");
    }
    @InitBinder("edicion")
    void limitarEdicion(WebDataBinder binder) { binder.setAllowedFields("nombre", "codigo", "estado"); }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("empresas", empresas.listar());
        return "plataforma/empresas";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        // Se verifica también en el servicio antes de mostrar información de plataforma.
        empresas.listar();
        model.addAttribute("alta", new EmpresaAltaFormulario());
        model.addAttribute("esAlta", true);
        return "plataforma/empresa-formulario";
    }

    @PostMapping("/guardar")
    public String crear(@Valid @ModelAttribute("alta") EmpresaAltaFormulario alta, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        // No conservar ni imprimir la contraseña en una respuesta de validación.
        if (!errors.hasErrors()) {
            try {
                empresas.crear(alta);
                alta.setAdminPassword(null);
                redirect.addFlashAttribute("success", "Empresa y cuenta administradora creadas. Comparte las credenciales por un canal seguro.");
                return "redirect:/plataforma/empresas";
            } catch (IllegalArgumentException ex) {
                errors.reject("empresa", ex.getMessage());
            }
        }
        alta.setAdminPassword(null);
        model.addAttribute("esAlta", true);
        return "plataforma/empresa-formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("edicion", empresas.obtener(id));
        model.addAttribute("empresaId", id);
        model.addAttribute("esAlta", false);
        return "plataforma/empresa-formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("edicion") EmpresaEdicionFormulario edicion,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                empresas.actualizar(id, edicion);
                redirect.addFlashAttribute("success", "Empresa actualizada. Deshabilitarla bloquea el acceso de sus usuarios sin eliminar datos.");
                return "redirect:/plataforma/empresas";
            } catch (IllegalArgumentException ex) {
                errors.reject("empresa", ex.getMessage());
            }
        }
        model.addAttribute("empresaId", id);
        model.addAttribute("esAlta", false);
        return "plataforma/empresa-formulario";
    }
}
