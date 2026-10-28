package com.safework.safework.controller;

import jakarta.servlet.http.*;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.safework.safework.security.*;

@Controller
public class RecuperacionController {
    private final RecuperacionService service;
    private final LimiteAcceso limite=new LimiteAcceso();
    public RecuperacionController(RecuperacionService service) { this.service=service; }
    @GetMapping("/recuperar")
    public String formulario(Model model) { model.addAttribute("disponible",service.disponible()); return "login/recuperar"; }
    @PostMapping("/recuperar")
    public String solicitar(@RequestParam(defaultValue="") String username,HttpServletRequest request,RedirectAttributes mensajes) {
        String ip=request.getRemoteAddr();
        if (limite.espera(ip,"recuperacion")==0) {
            limite.fallo(ip,"recuperacion");
            try { service.solicitar(username); } catch (MailException ex) { /* No mostrar direcciones, tokens ni errores SMTP. */ }
        }
        mensajes.addFlashAttribute("success","Si la cuenta está habilitada y tiene un correo registrado, recibirás instrucciones cuando el servicio de correo esté disponible.");
        return "redirect:/recuperar";
    }
    @GetMapping("/restablecer")
    public String enlace(@RequestParam(defaultValue="") String token,Model model,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store"); response.setHeader("Referrer-Policy","no-referrer");
        model.addAttribute("token",token); model.addAttribute("valido",service.valido(token)); return "login/restablecer";
    }
    @PostMapping("/restablecer")
    public String cambiar(@RequestParam(defaultValue="") String token,@RequestParam(defaultValue="") String password,
            @RequestParam(defaultValue="") String confirmacion,Model model,HttpServletResponse response,RedirectAttributes mensajes) {
        response.setHeader("Cache-Control","no-store"); response.setHeader("Referrer-Policy","no-referrer");
        try {
            if (!password.equals(confirmacion)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
            service.restablecer(token,password); mensajes.addFlashAttribute("success","Contraseña actualizada. Inicia sesión nuevamente.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error",ex.getMessage()); model.addAttribute("valido",service.valido(token)); model.addAttribute("token",token);
            return "login/restablecer";
        }
    }
}
