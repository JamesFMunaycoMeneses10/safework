package com.safework.safework;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.*;
import java.security.MessageDigest;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Ensayo optativo sobre copia MySQL. Se niega a usar la base habitual. */
@EnabledIfEnvironmentVariable(named = "SAFEWORK_VALIDAR_MIGRACION", matches = "true")
class MigracionMySqlTests {
    private static final List<String> TABLAS = List.of("acciones_correctivas", "acciones_correctivas_historial",
            "archivos_adjuntos", "areas", "auditoria_eventos", "entregas_epp", "hallazgos_inspeccion",
            "incidentes", "inspecciones", "riesgos", "trabajadores", "usuarios");

    @Test void laMigracionConservaTodosLosRegistrosYArchivosDeLaCopia() throws Exception {
        String url = System.getenv("SAFEWORK_MYSQL_TEST_URL");
        assertThat(url).isNotNull().contains("/safework_saas_test_");
        assertThat(url).doesNotContain("/safework_db");
        String user = System.getenv("SAFEWORK_DB_USER"), password = System.getenv("SAFEWORK_DB_PASSWORD");
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            assertThat(connection.getCatalog()).startsWith("safework_saas_test_");
            Map<String, String> antes = huellas(connection);
            var flyway = Flyway.configure().dataSource(url, user, password)
                    .baselineOnMigrate(true).baselineVersion("1").load();
            flyway.migrate();
            flyway.validate();
            assertThat(huellas(connection)).isEqualTo(antes);
            for (String tabla : TABLAS) {
                try (Statement statement = connection.createStatement();
                        ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tabla + " WHERE empresa_id<>1 OR empresa_id IS NULL")) {
                    rs.next(); assertThat(rs.getLong(1)).as(tabla).isZero();
                }
            }
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME LIKE 'fk_empresa_rel_%'")) {
                rs.next(); assertThat(rs.getInt(1)).isEqualTo(17);
            }
        }
    }

    private Map<String, String> huellas(Connection connection) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (String tabla : TABLAS) {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            long count = 0;
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT * FROM " + tabla + " ORDER BY 1")) {
                ResultSetMetaData metadata = rs.getMetaData();
                while (rs.next()) {
                    count++;
                    for (int column=1; column<=metadata.getColumnCount(); column++) {
                        if ("empresa_id".equalsIgnoreCase(metadata.getColumnName(column))) continue;
                        byte[] value = rs.getBytes(column);
                        hash.update((byte)(value == null ? 0 : 1));
                        if (value != null) { hash.update(java.nio.ByteBuffer.allocate(4).putInt(value.length).array()); hash.update(value); }
                    }
                }
            }
            result.put(tabla, count + ":" + HexFormat.of().formatHex(hash.digest()));
        }
        return result;
    }
}
