package com.safework.safework.service;

import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.safework.safework.model.EmpresaAltaFormulario;
import com.safework.safework.model.EmpresaEdicionFormulario;

/** Control de plataforma: JDBC explícito y autorizado; no expone los datos SST de clientes. */
@Service
public class EmpresaAdministracionService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transacciones;
    private final Validator validator;

    public record EmpresaResumen(Long id, String nombre, String codigo, String estado,
            LocalDateTime fechaRegistro, long usuarios) { }

    public EmpresaAdministracionService(JdbcTemplate jdbc, PasswordEncoder encoder,
            PlatformTransactionManager transacciones, Validator validator) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.transacciones = new TransactionTemplate(transacciones);
        this.validator = validator;
    }

    public List<EmpresaResumen> listar() {
        exigirPlataforma();
        return jdbc.query("""
                SELECT e.id,e.nombre,e.codigo,e.estado,e.fecha_registro,
                    (SELECT COUNT(*) FROM usuarios u WHERE u.empresa_id=e.id AND u.rol <> 'SUPERADMIN') AS usuarios
                FROM empresas e ORDER BY e.id
                """, (rs, row) -> new EmpresaResumen(rs.getLong("id"), rs.getString("nombre"),
                rs.getString("codigo"), rs.getString("estado"),
                rs.getTimestamp("fecha_registro").toLocalDateTime(), rs.getLong("usuarios")));
    }

    public EmpresaEdicionFormulario obtener(Long id) {
        exigirPlataforma();
        var resultados = jdbc.query("SELECT nombre,codigo,estado FROM empresas WHERE id=?", (rs, row) -> {
            var form = new EmpresaEdicionFormulario();
            form.setNombre(rs.getString("nombre"));
            form.setCodigo(rs.getString("codigo"));
            form.setEstado(rs.getString("estado"));
            return form;
        }, id);
        if (resultados.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return resultados.getFirst();
    }

    public Long crear(EmpresaAltaFormulario form) {
        exigirPlataforma();
        validar(form);
        validarClave(form.getAdminPassword());
        String hash = encoder.encode(form.getAdminPassword());
        try {
            return transacciones.execute(status -> {
                if (contar("SELECT COUNT(*) FROM empresas WHERE LOWER(codigo)=LOWER(?)", form.getCodigo().trim()) > 0
                        || contar("SELECT COUNT(*) FROM usuarios WHERE LOWER(username)=LOWER(?)", form.getAdminUsername().trim()) > 0
                        || contar("SELECT COUNT(*) FROM usuarios WHERE LOWER(correo)=LOWER(?)", form.getAdminCorreo().trim()) > 0) {
                    throw new IllegalArgumentException("El código, usuario o correo ya se encuentra registrado.");
                }
                LocalDateTime ahora = LocalDateTime.now();
                var key = new GeneratedKeyHolder();
                jdbc.update(connection -> {
                    var stmt = connection.prepareStatement("INSERT INTO empresas(nombre,codigo,estado,fecha_registro) VALUES (?,?,'ACTIVO',?)", Statement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, form.getNombre().trim());
                    stmt.setString(2, form.getCodigo().trim().toLowerCase(Locale.ROOT));
                    stmt.setTimestamp(3, Timestamp.valueOf(ahora));
                    return stmt;
                }, key);
                Number numero = key.getKey();
                if (numero == null) throw new IllegalStateException("No se pudo crear la empresa.");
                long empresaId = numero.longValue();
                jdbc.update("""
                        INSERT INTO usuarios(username,password,rol,nombre,apellido,correo,estado,fecha_registro,empresa_id)
                        VALUES (?,?,'ADMIN',?,?,?,'ACTIVO',?,?)
                        """, form.getAdminUsername().trim(), hash, form.getAdminNombre().trim(),
                        form.getAdminApellido().trim(), form.getAdminCorreo().trim().toLowerCase(Locale.ROOT),
                        Timestamp.valueOf(ahora), empresaId);
                return empresaId;
            });
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("No se pudo registrar: revisa que el código, usuario y correo sean únicos.");
        }
    }

    public void actualizar(Long id, EmpresaEdicionFormulario form) {
        exigirPlataforma();
        validar(form);
        try {
            transacciones.executeWithoutResult(status -> {
                if (contar("SELECT COUNT(*) FROM empresas WHERE LOWER(codigo)=LOWER(?) AND id<>?", form.getCodigo().trim(), id) > 0)
                    throw new IllegalArgumentException("El código de empresa ya se encuentra registrado.");
                int filas = jdbc.update("UPDATE empresas SET nombre=?,codigo=?,estado=? WHERE id=?",
                        form.getNombre().trim(), form.getCodigo().trim().toLowerCase(Locale.ROOT), form.getEstado(), id);
                if (filas == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            });
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("El código de empresa ya se encuentra registrado.");
        }
    }

    private long contar(String sql, Object... args) {
        Long valor = jdbc.queryForObject(sql, Long.class, args);
        return valor == null ? 0L : valor;
    }

    private void validar(Object formulario) {
        if (formulario == null || !validator.validate(formulario).isEmpty())
            throw new IllegalArgumentException("Revisa los datos obligatorios y el formato del formulario.");
    }

    public static void validarClave(String clave) {
        com.safework.safework.security.PoliticaContrasena.validar(clave);
    }

    private void exigirPlataforma() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_SUPERADMIN".equals(a.getAuthority())))
            throw new AccessDeniedException("Solo el administrador de plataforma puede gestionar empresas.");
    }
}
