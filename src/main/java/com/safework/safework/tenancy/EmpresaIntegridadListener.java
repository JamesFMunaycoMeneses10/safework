package com.safework.safework.tenancy;

import java.lang.reflect.Field;
import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import com.safework.safework.model.EmpresaEntidad;

/** Valida asociaciones manipuladas sin consultas JPA durante el flush. */
@Component
public class EmpresaIntegridadListener {
    private final JdbcTemplate jdbc;
    public EmpresaIntegridadListener(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @PrePersist @PreUpdate
    public void validar(Object object) {
        if (!(object instanceof EmpresaEntidad entidad)) return;
        Long empresaId = EmpresaContexto.empresaActualId();
        if (empresaId <= 0 || (entidad.getEmpresaId() != null && !empresaId.equals(entidad.getEmpresaId())))
            throw new AccessDeniedException("El registro no pertenece a la empresa actual.");
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(JoinColumn.class)
                    || !(field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class))) continue;
            try {
                field.setAccessible(true);
                Object related = field.get(object);
                if (!(related instanceof EmpresaEntidad)) continue;
                Class<?> tipo = field.getType();
                Object id = idDe(related, tipo);
                if (id == null) throw new AccessDeniedException("La relación debe guardarse antes del registro.");
                Table tabla = tipo.getAnnotation(Table.class);
                String idColumn = "id";
                for (Field candidate : tipo.getDeclaredFields()) if (candidate.isAnnotationPresent(Id.class)) {
                    var column = candidate.getAnnotation(Column.class);
                    idColumn = column != null && !column.name().isBlank() ? column.name() : candidate.getName();
                    break;
                }
                Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla.name()
                        + " WHERE " + idColumn + " = ? AND empresa_id = ?", Long.class, id, empresaId);
                if (count == null || count != 1) throw new AccessDeniedException("La relación no pertenece a la empresa actual.");
            } catch (IllegalAccessException ex) { throw new IllegalStateException("No se pudo validar la relación.", ex); }
        }
    }
    private Object idDe(Object entity, Class<?> tipo) throws IllegalAccessException {
        if (entity instanceof HibernateProxy proxy) return proxy.getHibernateLazyInitializer().getIdentifier();
        for (Field candidate : tipo.getDeclaredFields()) if (candidate.isAnnotationPresent(Id.class)) {
            candidate.setAccessible(true); return candidate.get(entity);
        }
        throw new IllegalStateException("Entidad sin identificador.");
    }
}
