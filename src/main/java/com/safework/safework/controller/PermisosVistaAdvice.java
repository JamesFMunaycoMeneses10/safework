package com.safework.safework.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Visibilidad de controles. La autorización real permanece en SecurityConfig. */
@ControllerAdvice
public class PermisosVistaAdvice {

    @ModelAttribute("esAdministrador")
    public boolean esAdministrador(Authentication authentication) {
        return tieneRol(authentication, "ROLE_ADMIN");
    }

    @ModelAttribute("puedeGestionarSst")
    public boolean puedeGestionarSst(Authentication authentication) {
        return esAdministrador(authentication) || tieneRol(authentication, "ROLE_SUPERVISOR");
    }

    private boolean tieneRol(Authentication authentication, String autoridad) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(granted -> autoridad.equals(granted.getAuthority()));
    }
}
