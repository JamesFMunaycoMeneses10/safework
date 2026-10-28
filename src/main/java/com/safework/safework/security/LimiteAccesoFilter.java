package com.safework.safework.security;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

public class LimiteAccesoFilter extends OncePerRequestFilter {
    private final LimiteAcceso limite;
    public LimiteAccesoFilter(LimiteAcceso limite) { this.limite=limite; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if ("POST".equals(request.getMethod()) && (request.getContextPath() + "/login").equals(request.getRequestURI())) {
            String usuario=request.getParameter("username"); usuario=usuario == null ? "" : usuario;
            long espera=limite.espera(request.getRemoteAddr(),usuario);
            if (espera > 0 || usuario.length() > 100) {
                response.setStatus(429); response.setHeader("Retry-After",Long.toString(Math.max(60,espera)));
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("<!doctype html><html lang='es'><meta charset='utf-8'><title>Acceso temporalmente limitado</title><h1>Demasiados intentos de acceso</h1><p>Espera 15 minutos antes de volver a intentarlo.</p><a href='"+request.getContextPath()+"/login'>Volver al inicio de sesión</a></html>"); return;
            }
        }
        chain.doFilter(request,response);
    }
}
