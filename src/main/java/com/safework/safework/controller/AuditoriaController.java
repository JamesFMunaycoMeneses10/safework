package com.safework.safework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.safework.safework.service.AuditoriaService;
import com.safework.safework.service.AuditoriaPresentacion;

@Controller
@RequestMapping("/auditoria")
public class AuditoriaController {
    private final AuditoriaService auditoria;

    public AuditoriaController(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        if (pagina < 0 || buscar.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro de bitácora no válido");
        }
        var resultados = auditoria.buscar(buscar, pagina);
        model.addAttribute("eventos", resultados.getContent().stream()
                .map(AuditoriaPresentacion::desde).toList());
        model.addAttribute("resultados", resultados);
        model.addAttribute("buscar", buscar);
        return "auditoria";
    }
}
