package com.safework.safework.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Identidad autenticada: la empresa no procede del formulario. */
public class EmpresaPrincipal extends User {
    private static final long serialVersionUID = 1L;
    private final Long usuarioId;
    private final Long empresaId;
    private final String empresaNombre;
    public EmpresaPrincipal(Long usuarioId, String username, String password, String rol,
            boolean enabled, Long empresaId, String empresaNombre) {
        super(username, password, enabled, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        this.usuarioId = usuarioId; this.empresaId = empresaId; this.empresaNombre = empresaNombre;
    }
    public Long getUsuarioId() { return usuarioId; }
    public Long getEmpresaId() { return empresaId; }
    public String getEmpresaNombre() { return empresaNombre; }
}
