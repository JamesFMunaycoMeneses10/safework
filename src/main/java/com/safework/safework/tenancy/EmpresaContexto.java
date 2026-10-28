package com.safework.safework.tenancy;

import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContextHolder;
import com.safework.safework.security.EmpresaPrincipal;

/** La empresa nunca procede de parámetros HTTP; sin identidad conocida no hay datos visibles. */
public final class EmpresaContexto {
    public static final long SIN_EMPRESA = -1L;
    private static final ThreadLocal<Long> TAREA = new ThreadLocal<>();
    private EmpresaContexto() {}
    public static Long empresaActualId() {
        if (TAREA.get() != null) return TAREA.get();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof EmpresaPrincipal principal) {
            if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"))) return SIN_EMPRESA;
            return principal.getEmpresaId();
        }
        return SIN_EMPRESA;
    }
    /** Exclusivo para tareas internas y pruebas; siempre restaura el contexto anterior. */
    public static <T> T ejecutar(Long empresaId, Supplier<T> tarea) {
        if (empresaId == null || empresaId <= 0) throw new IllegalArgumentException("Empresa inválida");
        Long anterior = TAREA.get();
        try { TAREA.set(empresaId); return tarea.get(); }
        finally { if (anterior == null) TAREA.remove(); else TAREA.set(anterior); }
    }
}
