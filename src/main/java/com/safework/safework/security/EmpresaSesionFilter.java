package com.safework.safework.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

/** Registrar solo en SecurityFilterChain, nunca como filtro Servlet adicional. */
public class EmpresaSesionFilter extends OncePerRequestFilter {
    private final UsuarioAccesoRepository accesos;
    public EmpresaSesionFilter(UsuarioAccesoRepository accesos) { this.accesos = accesos; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof EmpresaPrincipal principal) {
            var actual = accesos.buscar(principal.getUsername());
            boolean valido = actual.filter(u -> u.habilitado() && u.id().equals(principal.getUsuarioId())
                    && u.empresaId().equals(principal.getEmpresaId())
                    && u.versionAcceso() == principal.getVersionAcceso()
                    && principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + u.rol())))
                    .isPresent();
            if (!valido) {
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                request.getSession(true).setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION,
                        new DisabledException("La cuenta o empresa ya no está habilitada."));
                response.sendRedirect(request.getContextPath() + "/login?error");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
