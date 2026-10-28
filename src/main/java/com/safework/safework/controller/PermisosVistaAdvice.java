package com.safework.safework.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Visibilidad de controles. La autorización real permanece en SecurityConfig. */
@ControllerAdvice
public class PermisosVistaAdvice {

    @ModelAttribute("esAdministradorPlataforma")
    public boolean esAdministradorPlataforma(Authentication authentication) {
        return tieneRol(authentication, "ROLE_SUPERADMIN");
    }

    @ModelAttribute("empresaActualNombre")
    public String empresaActualNombre(Authentication authentication) {
        return authentication != null && authentication.getPrincipal()
                instanceof com.safework.safework.security.EmpresaPrincipal principal
                ? principal.getEmpresaNombre() : "";
    }

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
