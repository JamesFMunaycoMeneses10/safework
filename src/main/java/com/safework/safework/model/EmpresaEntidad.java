package com.safework.safework.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.TenantId;
import com.safework.safework.tenancy.EmpresaIntegridadListener;

/** Hibernate filtra las consultas y asigna la empresa en cada alta. */
@MappedSuperclass
@EntityListeners(EmpresaIntegridadListener.class)
public abstract class EmpresaEntidad {
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;
    public Long getEmpresaId() { return empresaId; }
}
