package com.safework.safework.tenancy;

import java.util.Map;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmpresaHibernateConfiguracion implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {
    @Override public Long resolveCurrentTenantIdentifier() { return EmpresaContexto.empresaActualId(); }
    @Override public boolean validateExistingCurrentSessions() { return true; }
    @Override public void customize(Map<String, Object> properties) {
        properties.put("hibernate.tenant_identifier_resolver", this);
    }
}
