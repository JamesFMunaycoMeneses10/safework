package com.safework.safework;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.safework.safework.repository.UsuarioRepository;
import com.safework.safework.security.UsuarioAccesoRepository;
import com.safework.safework.tenancy.EmpresaContexto;

/** Comprueba la restricción SQL real, no solo el filtrado de la pantalla. */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CuentaPlataformaJpaTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired UsuarioAccesoRepository accesoGlobal;
    private Long empresaId;
    private Long plataformaId;
    private String plataformaUsername;

    @BeforeAll
    void prepararSoloEnH2() {
        String url = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)
                connection -> connection.getMetaData().getURL());
        assertThat(url).startsWith("jdbc:h2:mem:");
        String marca = UUID.randomUUID().toString().substring(0, 8);
        String codigo = "proteccion-plataforma-" + marca;
        plataformaUsername = "plataforma-" + marca;
        jdbc.update("INSERT INTO empresas(nombre,codigo,estado,fecha_registro) VALUES(?,?,'ACTIVO',CURRENT_TIMESTAMP)",
                "Empresa con cuenta global", codigo);
        empresaId = jdbc.queryForObject("SELECT id FROM empresas WHERE codigo=?", Long.class, codigo);
        jdbc.update("""
                INSERT INTO usuarios(username,password,rol,nombre,apellido,estado,fecha_registro,empresa_id)
                VALUES (?,'HASH_PRUEBA','SUPERADMIN','Plataforma','Prueba','ACTIVO',CURRENT_TIMESTAMP,?)
                """, plataformaUsername, empresaId);
        plataformaId = jdbc.queryForObject("SELECT id FROM usuarios WHERE username=?", Long.class, plataformaUsername);
    }

    @Test
    void repositorioDeEmpresaNoListaNiEncuentraLaCuentaGlobal() {
        EmpresaContexto.ejecutar(empresaId, () -> {
            assertThat(usuarios.findAll()).isEmpty();
            assertThat(usuarios.findById(plataformaId)).isEmpty();
            assertThat(usuarios.findByUsername(plataformaUsername)).isEmpty();
            assertThat(usuarios.findByEstado("ACTIVO")).isEmpty();
            return null;
        });
    }

    @Test
    void autenticacionGlobalConservaAccesoALaCuentaPlataforma() {
        var cuenta = accesoGlobal.buscar(plataformaUsername).orElseThrow();
        assertThat(cuenta.id()).isEqualTo(plataformaId);
        assertThat(cuenta.rol()).isEqualTo("SUPERADMIN");
        assertThat(cuenta.habilitado()).isTrue();
    }

    @Test
    void deleteByIdDesdeEmpresaNoEliminaLaCuentaGlobal() {
        EmpresaContexto.ejecutar(empresaId, () -> { usuarios.deleteById(plataformaId); return null; });
        assertThat(accesoGlobal.buscar(plataformaUsername)).isPresent();
    }
}
