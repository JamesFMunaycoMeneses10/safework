package com.safework.safework.config;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.safework.safework.service.EmpresaAdministracionService;

/** Alta optativa y de una sola vez. Nunca cambia privilegios de cuentas existentes. */
@Component
@ConditionalOnProperty(name = "safework.plataforma.crear-administrador", havingValue = "true")
public class AdministradorPlataformaBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaction;
    private final String username, password, nombre, apellido, correo;

    public AdministradorPlataformaBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder,
            PlatformTransactionManager manager,
            @Value("${SAFEWORK_PLATFORM_USERNAME:}") String username,
            @Value("${SAFEWORK_PLATFORM_PASSWORD:}") String password,
            @Value("${SAFEWORK_PLATFORM_NOMBRE:}") String nombre,
            @Value("${SAFEWORK_PLATFORM_APELLIDO:}") String apellido,
            @Value("${SAFEWORK_PLATFORM_CORREO:}") String correo) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.transaction = new TransactionTemplate(manager);
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
    }

    @Override
    public void run(ApplicationArguments args) {
        EmpresaAdministracionService.validarClave(password);
        if (!username.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,49}")
                || nombre.isBlank() || nombre.length() > 100
                || apellido.isBlank() || apellido.length() > 100
                || correo.length() > 255 || !correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalStateException("Define datos válidos en SAFEWORK_PLATFORM_USERNAME, NOMBRE, APELLIDO y CORREO; no se creó ninguna cuenta.");
        String hash = encoder.encode(password);
        transaction.executeWithoutResult(status -> {
            // Serializa el aprovisionamiento entre instancias antes de comprobar cuentas.
            var empresaPrincipal = jdbc.queryForList("SELECT id FROM empresas WHERE id=1 FOR UPDATE", Long.class);
            if (empresaPrincipal.size() != 1)
                throw new IllegalStateException("Aplica primero las migraciones del piloto; falta la empresa principal.");
            Long existentes = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE rol='SUPERADMIN'", Long.class);
            if (existentes != null && existentes > 0)
                throw new IllegalStateException("Ya existe un administrador de plataforma. Desactiva el aprovisionamiento y retira sus variables de entorno.");
            Long duplicados = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE LOWER(username)=LOWER(?) OR LOWER(correo)=LOWER(?)",
                    Long.class, username.trim(), correo.trim());
            if (duplicados != null && duplicados > 0)
                throw new IllegalStateException("El usuario o correo de aprovisionamiento ya existe. No se modificaron cuentas existentes.");
            jdbc.update("""
                    INSERT INTO usuarios(username,password,rol,nombre,apellido,correo,estado,fecha_registro,empresa_id)
                    VALUES (?,?,'SUPERADMIN',?,?,?,'ACTIVO',?,1)
                    """, username.trim(), hash, nombre.trim(), apellido.trim(),
                    correo.trim().toLowerCase(Locale.ROOT), Timestamp.valueOf(LocalDateTime.now()));
        });
    }
}
