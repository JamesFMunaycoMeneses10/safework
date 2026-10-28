package com.safework.safework.security;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consulta global de credenciales; no expone listados de usuarios ni datos SST. */
@Repository
public class UsuarioAccesoRepository {
    public record AccesoUsuario(Long id, String username, String password, String rol, String estado,
            Long empresaId, String empresaNombre, String empresaEstado) {
        public boolean habilitado() {
            return "ACTIVO".equals(estado) && ("SUPERADMIN".equals(rol) || "ACTIVO".equals(empresaEstado));
        }
    }
    private final JdbcTemplate jdbc;
    public UsuarioAccesoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<AccesoUsuario> buscar(String username) {
        return jdbc.query("""
                SELECT u.id, u.username, u.password, u.rol, u.estado, u.empresa_id,
                       e.nombre AS empresa_nombre, e.estado AS empresa_estado
                FROM usuarios u JOIN empresas e ON e.id = u.empresa_id WHERE u.username = ?
                """, (rs, row) -> new AccesoUsuario(rs.getLong("id"), rs.getString("username"),
                rs.getString("password"), rs.getString("rol"), rs.getString("estado"),
                rs.getLong("empresa_id"), rs.getString("empresa_nombre"), rs.getString("empresa_estado")), username)
                .stream().findFirst();
    }
}
