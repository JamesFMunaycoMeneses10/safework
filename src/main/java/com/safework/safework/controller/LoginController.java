package com.safework.safework.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(name = "error", required = false) String error,
            HttpServletRequest request, Model model) {

        HttpSession session = request.getSession(false);
        if (error != null) {
            // Spring Security conserva la causa real del rechazo en la sesión.
            // No se confía en un motivo enviado por la URL ni se muestran detalles internos.
            Object fallo = session == null ? null
                    : session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            String mensaje = fallo instanceof DisabledException
                    ? "Tu cuenta se encuentra inactiva o deshabilitada. Por favor, comunícate con el administrador del sistema."
                    : "Usuario o contraseña incorrectos.";
            model.addAttribute("errorLogin", mensaje);
        } else if (session != null) {
            // Una visita nueva al formulario no debe reutilizar errores anteriores.
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        return "login/login";

    }

}
