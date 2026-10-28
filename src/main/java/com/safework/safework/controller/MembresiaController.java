package com.safework.safework.controller;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.safework.safework.service.MembresiaService;

@Controller @RequestMapping("/membresia")
public class MembresiaController {
    private final MembresiaService service;
    public MembresiaController(MembresiaService service){this.service=service;}
    @GetMapping public String pagina(Model model) {
        model.addAttribute("planes",service.planes());model.addAttribute("pagos",service.pagos());
        var hasta=service.vigenteHasta();model.addAttribute("vigenteHasta",hasta);
        model.addAttribute("vigente",hasta!=null && hasta.isAfter(LocalDateTime.now(java.time.ZoneId.of("America/Lima"))));
        model.addAttribute("disponible",service.disponible());model.addAttribute("solicitudId",UUID.randomUUID().toString());return "membresia";
    }
    @PostMapping("/contratar") public String contratar(@RequestParam String plan,@RequestParam String solicitudId,RedirectAttributes mensajes) {
        try{return "redirect:"+service.iniciar(plan,solicitudId);}
        catch(IllegalArgumentException|IllegalStateException|RestClientException ex){mensajes.addFlashAttribute("error","No se pudo abrir el pago de prueba. Revisa la configuración o inténtalo más tarde.");return "redirect:/membresia";}
    }
    @GetMapping("/retorno") public String retorno(RedirectAttributes mensajes) {
        mensajes.addFlashAttribute("success","El retorno del navegador no confirma el pago. La membresía cambia solo después de una verificación con Mercado Pago.");return "redirect:/membresia";
    }
    @PostMapping("/conciliar") public String conciliar(@RequestParam String pagoId,RedirectAttributes mensajes) {
        try{service.conciliarPropio(pagoId);mensajes.addFlashAttribute("success","Estado consultado directamente con Mercado Pago.");}
        catch(IllegalArgumentException|IllegalStateException|RestClientException ex){mensajes.addFlashAttribute("error","No se pudo verificar ese pago de prueba.");}
        return "redirect:/membresia";
    }
}
