package com.safework.safework;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.safework.safework.model.*;
import com.safework.safework.tenancy.EmpresaContexto;

/** HTTP, seguridad, servicios, plantillas y Hibernate reales; sin mocks ni MySQL del usuario. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:safework_web_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.open-in-view=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmpresaWebAislamientoTests {
    static final String PASSWORD = "SoloPruebas2026!";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired PasswordEncoder passwords;
    Datos a, b;
    record Datos(Long empresa, String marca, String admin, String trabajador, Long area,
            Long persona, Long riesgo, Long accion, Long evento, Long inspeccion) {}

    @BeforeAll void preparar() {
        a = crear("PRIVADO_EMPRESA_A"); b = crear("PRIVADO_EMPRESA_B");
        enEmpresa(a.empresa(), () -> {
            em.persist(new Usuario("plataforma_web", passwords.encode(PASSWORD), "SUPERADMIN", "Plataforma", "Pruebas", null));
            return null;
        });
    }

    private <T> T enEmpresa(Long empresa, Supplier<T> action) {
        return EmpresaContexto.ejecutar(empresa, () -> new TransactionTemplate(manager).execute(s -> action.get()));
    }
    private <T> T persistir(T entity) { em.persist(entity); em.flush(); return entity; }

    private Datos crear(String marca) {
        String codigo = "web-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO empresas(nombre,codigo,estado,fecha_registro) VALUES(?,?,'ACTIVO',CURRENT_TIMESTAMP)", marca,codigo);
        Long empresa = jdbc.queryForObject("SELECT id FROM empresas WHERE codigo=?",Long.class,codigo);
        return enEmpresa(empresa, () -> {
            Usuario admin = persistir(new Usuario("admin_" + marca, passwords.encode(PASSWORD), "ADMIN", "Administrador", marca, null));
            Usuario user = persistir(new Usuario("trabajador_" + marca, passwords.encode(PASSWORD), "TRABAJADOR", "Trabajador", marca, null));
            Area area = persistir(new Area(marca, "Descripción " + marca));
            Trabajador persona = new Trabajador("23456789", "Trabajador", marca, "Cargo " + marca, area);
            persona.setUsuario(user); persistir(persona);
            ArchivoAdjunto foto = persistir(new ArchivoAdjunto(UUID.randomUUID()+".png", "foto.png", "image/png", marca.getBytes(StandardCharsets.UTF_8)));
            Riesgo riesgo = new Riesgo(marca, "Riesgo " + marca, 3, 3, 9, "Control", area);
            riesgo.setFotoArchivo(foto.getNombre()); riesgo.setFotoNombre("foto.png"); riesgo.setFotoTipo("image/png"); persistir(riesgo);
            ArchivoAdjunto evidencia = persistir(new ArchivoAdjunto(UUID.randomUUID()+".pdf", "evidencia.pdf", "application/pdf", marca.getBytes(StandardCharsets.UTF_8)));
            AccionCorrectiva accion = new AccionCorrectiva(marca, LocalDate.now(), LocalDate.now().plusDays(3), "Pendiente", "Alta", persona, riesgo, null);
            accion.setArchivoEvidencia(evidencia.getNombre()); accion.setNombreEvidencia("evidencia.pdf"); accion.setTipoEvidencia("application/pdf"); persistir(accion);
            AccionCorrectivaEvento evento = persistir(new AccionCorrectivaEvento(accion, "ENTREGADA", user.getUsername(), LocalDateTime.now(), marca,
                    evidencia.getNombre(), "evidencia.pdf", "application/pdf"));
            Inspeccion inspeccion = persistir(new Inspeccion(LocalDate.now(), marca, area, persona, "Programada", marca));
            HallazgoInspeccion hallazgo = new HallazgoInspeccion(); hallazgo.setDescripcion(marca); hallazgo.setInspeccion(inspeccion); hallazgo.setRiesgo(riesgo); persistir(hallazgo);
            EntregaEpp entrega = new EntregaEpp(); entrega.setTipoEpp(marca); entrega.setTrabajador(persona); entrega.setRegistradoPor(admin);
            entrega.setFechaEntrega(LocalDate.now()); entrega.setCantidad(1); persistir(entrega);
            persistir(new AuditoriaEvento("RIESGOS", riesgo.getId(), "CREADO", admin.getUsername(), LocalDateTime.now(), marca));
            return new Datos(empresa,marca,admin.getUsername(),user.getUsername(),area.getId(),persona.getId(),riesgo.getId(),accion.getId(),evento.getId(),inspeccion.getId());
        });
    }

    MockHttpSession login(String username, String target) throws Exception {
        var form = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) form.getRequest().getSession();
        CsrfToken csrf = (CsrfToken) form.getRequest().getAttribute("_csrf");
        var login = mvc.perform(post("/login").session(session).param("username",username).param("password",PASSWORD)
                .param(csrf.getParameterName(),csrf.getToken()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(target)).andReturn();
        return (MockHttpSession) login.getRequest().getSession();
    }
    CsrfToken csrf(MockHttpSession session) throws Exception {
        return (CsrfToken) mvc.perform(get("/login").session(session)).andReturn().getRequest().getAttribute("_csrf");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/dashboard","/areas","/trabajadores","/riesgos","/incidentes","/inspecciones","/acciones","/epp","/auditoria","/usuarios"})
    void pantallasDeEmpresaNoExponenDatosDeLaOtra(String path) throws Exception {
        MockHttpSession session = login(a.admin(), "/dashboard");
        mvc.perform(get(path).session(session)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(b.marca()))));
    }

    @Test void dashboardCuentaSoloLaEmpresaActual() throws Exception {
        mvc.perform(get("/dashboard").session(login(a.admin(),"/dashboard")))
                .andExpect(status().isOk()).andExpect(model().attribute("totalTrabajadores",1))
                .andExpect(model().attribute("totalRiesgos",1)).andExpect(model().attribute("totalAcciones",1));
    }

    @Test void parametrosEmpresaNoCambianElContexto() throws Exception {
        mvc.perform(get("/acciones").session(login(a.admin(),"/dashboard"))
                .param("empresaId",b.empresa().toString()).param("empresa_id",b.empresa().toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(a.marca())))
                .andExpect(content().string(not(containsString(b.marca()))));
    }

    @Test void referenciasPorUrlARegistrosAjenosResponden404() throws Exception {
        MockHttpSession session = login(a.admin(),"/dashboard");
        for (String path : List.of("/areas/editar/"+b.area(),"/acciones/detalle/"+b.accion(),
                "/acciones/"+b.accion()+"/historial", "/acciones/"+b.accion()+"/evidencia",
                "/acciones/"+b.accion()+"/historial/"+b.evento()+"/archivo", "/riesgos/"+b.riesgo()+"/foto"))
            mvc.perform(get(path).session(session)).andExpect(status().isNotFound());
    }

    @Test void archivosPropiosSeDescarganYLosAjenosNo() throws Exception {
        MockHttpSession session = login(a.admin(),"/dashboard");
        mvc.perform(get("/acciones/"+a.accion()+"/evidencia").session(session))
                .andExpect(status().isOk()).andExpect(content().bytes(a.marca().getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get("/acciones/"+a.accion()+"/historial/"+b.evento()+"/archivo").session(session))
                .andExpect(status().isNotFound());
    }

    @Test void reportesCsvYExcelSoloIncluyenLaEmpresaActual() throws Exception {
        MockHttpSession session = login(a.admin(),"/dashboard");
        mvc.perform(get("/acciones/reporte.csv").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString(a.marca())))
                .andExpect(content().string(not(containsString(b.marca()))));
        byte[] excel = mvc.perform(get("/acciones/reporte.xlsx").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        StringBuilder texto = new StringBuilder();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            for (var sheet : workbook) for (var row : sheet) for (var cell : row) texto.append(cell.toString());
        }
        assertThat(texto.toString()).contains(a.marca()).doesNotContain(b.marca());
        mvc.perform(get("/acciones/reporte.csv").session(session).param("riesgoId",b.riesgo().toString()))
                .andExpect(status().isNotFound());
    }

    @Test void trabajadorSoloVeSusEntregasYNoAccedeAPlataforma() throws Exception {
        MockHttpSession session = login(a.trabajador(),"/dashboard");
        mvc.perform(get("/epp/mis-entregas").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString(a.marca())))
                .andExpect(content().string(not(containsString(b.marca()))));
        mvc.perform(get("/trabajadores").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/plataforma/empresas").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/acciones/"+b.accion()+"/entregar").session(session)).andExpect(status().isNotFound());
    }

    @Test void superadminNoHeredaAccesoSstYAdminNoAccedeALaPlataforma() throws Exception {
        MockHttpSession plataforma = login("plataforma_web","/plataforma/empresas");
        for (String path : List.of("/dashboard","/acciones","/usuarios","/riesgos","/epp"))
            mvc.perform(get(path).session(plataforma)).andExpect(status().isForbidden());
        mvc.perform(get("/plataforma/empresas").session(login(a.admin(),"/dashboard"))).andExpect(status().isForbidden());
    }

    @Test void formulariosDePlataformaFuncionanYNoDevuelvenContrasenas() throws Exception {
        MockHttpSession plataforma = login("plataforma_web","/plataforma/empresas");
        for (String path : List.of("/plataforma/empresas","/plataforma/empresas/nueva","/plataforma/empresas/editar/"+a.empresa()))
            mvc.perform(get(path).session(plataforma)).andExpect(status().isOk());
        CsrfToken token = csrf(plataforma);
        mvc.perform(post("/plataforma/empresas/guardar").session(plataforma).param(token.getParameterName(),token.getToken())
                .param("nombre","Nueva empresa").param("codigo","NUEVA-WEB").param("adminUsername","admin_nueva_web")
                .param("adminPassword","Corta123").param("adminNombre","Nombre").param("adminApellido","Apellido")
                .param("adminCorreo","nuevo@example.test"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("alta"))
                .andExpect(content().string(not(containsString("Corta123"))));
    }

    @Test void escriturasDePlataformaExigenRolYCsrf() throws Exception {
        MockHttpSession admin = login(a.admin(),"/dashboard"); CsrfToken token = csrf(admin);
        mvc.perform(post("/plataforma/empresas/guardar").session(admin).param(token.getParameterName(),token.getToken()))
                .andExpect(status().isForbidden());
        MockHttpSession plataforma = login("plataforma_web","/plataforma/empresas");
        mvc.perform(post("/plataforma/empresas/guardar").session(plataforma)).andExpect(status().isForbidden());
    }

    @Test void editarAreaAjenaEsRechazadoSinModificarLaFila() throws Exception {
        MockHttpSession session = login(a.admin(),"/dashboard"); CsrfToken token = csrf(session);
        mvc.perform(post("/areas/guardar").session(session).param(token.getParameterName(),token.getToken())
                .param("id",b.area().toString()).param("nombre","Intento ajeno").param("descripcion","No permitido"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("area"));
        assertThat(jdbc.queryForObject("SELECT nombre FROM areas WHERE id=?",String.class,b.area())).isEqualTo(b.marca());
    }

    @Test void guardarRiesgoConAreaAjenaNoProduceError500NiCreaRegistro() throws Exception {
        MockHttpSession session = login(a.admin(),"/dashboard"); CsrfToken token = csrf(session);
        int antes = jdbc.queryForObject("SELECT count(*) FROM riesgos WHERE empresa_id=?",Integer.class,a.empresa());
        mvc.perform(post("/riesgos/guardar").session(session).param(token.getParameterName(),token.getToken())
                .param("peligro","Manipulado").param("descripcion","Descripción de prueba").param("probabilidad","2")
                .param("severidad","2").param("area.id",b.area().toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM riesgos WHERE empresa_id=?",Integer.class,a.empresa())).isEqualTo(antes);
    }

    @AfterAll void limpiar() {
        if (a == null || b == null) return;
        for (String table : List.of("entregas_epp","acciones_correctivas_historial","hallazgos_inspeccion","acciones_correctivas",
                "auditoria_eventos","archivos_adjuntos","incidentes","inspecciones","riesgos","trabajadores","usuarios","areas"))
            jdbc.update("DELETE FROM "+table+" WHERE empresa_id IN (?,?)",a.empresa(),b.empresa());
        jdbc.update("DELETE FROM empresas WHERE id IN (?,?)",a.empresa(),b.empresa());
    }
}
