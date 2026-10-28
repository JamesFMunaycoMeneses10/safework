package com.safework.safework;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
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
import com.safework.safework.service.CapacitacionService;
import com.safework.safework.tenancy.EmpresaContexto;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:capacitaciones;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "spring.jpa.open-in-view=true"})
@AutoConfigureMockMvc @ActiveProfiles("test") @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CapacitacionesFlujoTests {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager; @Autowired PasswordEncoder encoder;
    @Autowired CapacitacionService service;
    record Empresa(Long id, Long area, Long trabajador, Long otro, String supervisor, String username) {}
    Empresa a,b;
    static final String CLAVE="ClaveDeFlujo2026!";
    <T> T en(Long id, Supplier<T> action) { return EmpresaContexto.ejecutar(id, () -> new TransactionTemplate(manager).execute(s -> action.get())); }
    <T> T guardar(T entity) { em.persist(entity); em.flush(); return entity; }
    @BeforeAll void preparar() { a=crearEmpresa("CAP_A"); b=crearEmpresa("CAP_B"); }
    Empresa crearEmpresa(String marca) {
        jdbc.update("INSERT INTO empresas(nombre,codigo,estado,fecha_registro) VALUES(?,?,'ACTIVO',CURRENT_TIMESTAMP)",marca,marca);
        Long id=jdbc.queryForObject("SELECT id FROM empresas WHERE codigo=?",Long.class,marca);
        return en(id, () -> {
            guardar(new Usuario("sup_"+marca,encoder.encode(CLAVE),"SUPERVISOR","Supervisor",marca,null));
            Usuario u=guardar(new Usuario("tra_"+marca,encoder.encode(CLAVE),"TRABAJADOR","Participante",marca,null));
            Area area=guardar(new Area(marca,"Área"));
            Trabajador t=new Trabajador("12345678","Participante",marca,"Operario",area); t.setUsuario(u); guardar(t);
            Trabajador otro=guardar(new Trabajador("12345679","Otra",marca,"Operaria",area));
            return new Empresa(id,area.getId(),t.getId(),otro.getId(),"sup_"+marca,u.getUsername());
        });
    }
    CapacitacionFormulario datos(Empresa e) {
        var f=new CapacitacionFormulario(); f.setTema("Prevención eléctrica "+UUID.randomUUID());
        f.setObjetivo("Reconocer cables dañados"); f.setInstructor("Instructor SST"); f.setFechaHora(LocalDateTime.now().minusDays(1));
        f.setDuracionMinutos(60); f.setAreaId(e.area()); f.setTrabajadorIds(List.of(e.trabajador())); return f;
    }
    Long crear(Empresa e) { return en(e.id(), () -> service.guardar(datos(e),e.supervisor()).getId()); }
    MockHttpSession login(String username) throws Exception {
        var inicio=mvc.perform(get("/login")).andReturn();
        var sesion=(MockHttpSession)inicio.getRequest().getSession();
        CsrfToken csrf=(CsrfToken)inicio.getRequest().getAttribute("_csrf");
        return (MockHttpSession)mvc.perform(post("/login").session(sesion).param("username",username).param("password",CLAVE)
                .param(csrf.getParameterName(),csrf.getToken())).andExpect(redirectedUrl("/dashboard")).andReturn().getRequest().getSession();
    }
    @Test void cicloCompletoConAsistenciaYAuditoria() {
        Long id=crear(a);
        en(a.id(), () -> {
            var participantes=service.participantes(id);
            service.asistencia(id,Map.of(participantes.getFirst().getId(),"ASISTIO"),a.supervisor());
            service.finalizar(id,a.supervisor()); return null;
        });
        assertThat(en(a.id(), () -> service.obtener(id).getEstado())).isEqualTo("REALIZADA");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_eventos WHERE modulo='CAPACITACIONES' AND registro_id=?",Long.class,id)).isEqualTo(3);
    }
    @Test void noPermiteFinalizarSinAsistenciaNiAntesDelHorario() {
        Long id=crear(a);
        assertThatThrownBy(() -> en(a.id(), () -> {service.finalizar(id,a.supervisor()); return null;})).hasMessageContaining("asistencia");
        var f=datos(a); f.setFechaHora(LocalDateTime.now().plusDays(1));
        Long futuro=en(a.id(), () -> service.guardar(f,a.supervisor()).getId());
        assertThatThrownBy(() -> en(a.id(), () -> { service.asistencia(futuro,Map.of(),a.supervisor()); return null;})).hasMessageContaining("todavía");
    }
    @Test void rechazaRelacionConTrabajadorOAreaDeOtraEmpresa() {
        var f=datos(a); f.setTrabajadorIds(List.of(b.trabajador()));
        assertThatThrownBy(() -> en(a.id(), () -> service.guardar(f,a.supervisor()))).hasMessageContaining("empresa");
        f.setTrabajadorIds(List.of(a.trabajador())); f.setAreaId(b.area());
        assertThatThrownBy(() -> en(a.id(), () -> service.guardar(f,a.supervisor()))).hasMessageContaining("Área");
    }
    @Test void asistenciaNoAdmiteIdsManipuladosYNoModificaNada() {
        Long id=crear(a), otro=crear(b);
        Long ajeno=en(b.id(), () -> service.participantes(otro).getFirst().getId());
        assertThatThrownBy(() -> en(a.id(), () -> { service.asistencia(id,Map.of(ajeno,"ASISTIO"),a.supervisor()); return null;})).hasMessageContaining("identificadores");
        assertThat(en(a.id(), () -> service.participantes(id).getFirst().getAsistencia())).isEqualTo("PENDIENTE");
    }
    @Test void cancelarConservaRegistroYBloqueaEdicion() {
        Long id=crear(a); en(a.id(), () -> { service.cancelar(id,a.supervisor()); return null; });
        assertThat(en(a.id(), () -> service.obtener(id).getEstado())).isEqualTo("CANCELADA");
        assertThatThrownBy(() -> en(a.id(), () -> service.formulario(id))).hasMessageContaining("programada");
    }
    @Test void noDuplicaParticipantesYRechazaFormularioVacio() {
        var f=datos(a); f.setTrabajadorIds(List.of(a.trabajador(),a.trabajador()));
        assertThatThrownBy(() -> en(a.id(), () -> service.guardar(f,a.supervisor()))).hasMessageContaining("repetidos");
        f.setTrabajadorIds(List.of()); assertThatThrownBy(() -> en(a.id(), () -> service.guardar(f,a.supervisor()))).hasMessageContaining("Completa");
    }
    @Test void trabajadorSoloVeSusAsignacionesYNoPuedeGestionar() throws Exception {
        Long id=crear(a), ajeno=crear(b); var sesion=login(a.username());
        String tema=en(a.id(), () -> service.obtener(id).getTema()), secreto=en(b.id(), () -> service.obtener(ajeno).getTema());
        mvc.perform(get("/capacitaciones/mis-capacitaciones").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString(tema))).andExpect(content().string(not(containsString(secreto))));
        for (String url:List.of("/capacitaciones","/capacitaciones/nueva","/capacitaciones/"+id))
            mvc.perform(get(url).session(sesion)).andExpect(status().isForbidden());
        CsrfToken csrf=(CsrfToken)mvc.perform(get("/login").session(sesion)).andReturn().getRequest().getAttribute("_csrf");
        mvc.perform(post("/capacitaciones/"+id+"/cancelar").session(sesion).param(csrf.getParameterName(),csrf.getToken())).andExpect(status().isForbidden());
    }
    @Test void supervisorConsultaPlantillasYNoVeEmpresaAjena() throws Exception {
        Long id=crear(a), ajeno=crear(b); var sesion=login(a.supervisor());
        for (String url:List.of("/capacitaciones","/capacitaciones/nueva","/capacitaciones/"+id,"/capacitaciones/"+id+"/editar"))
            mvc.perform(get(url).session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/capacitaciones/"+ajeno).session(sesion)).andExpect(status().isNotFound());
        mvc.perform(post("/capacitaciones/"+id+"/cancelar").session(sesion)).andExpect(status().isForbidden());
    }
    @Test void guardarDesdeFormularioHttpRespetaEmpresaDelPrincipal() throws Exception {
        var sesion=login(a.supervisor());
        CsrfToken csrf=(CsrfToken)mvc.perform(get("/login").session(sesion)).andReturn().getRequest().getAttribute("_csrf");
        mvc.perform(post("/capacitaciones/guardar").session(sesion).param(csrf.getParameterName(),csrf.getToken())
                .param("tema","Uso correcto del EPP").param("objetivo","Evitar lesiones").param("instructor","Ana SST")
                .param("fechaHora","2026-10-20T10:00").param("duracionMinutos","60").param("areaId",a.area().toString())
                .param("trabajadorIds",a.trabajador().toString()).param("empresaId",b.id().toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT empresa_id FROM capacitaciones WHERE tema='Uso correcto del EPP'",Long.class)).isEqualTo(a.id());
    }
}
