package com.safework.safework;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import com.safework.safework.security.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:recuperacion;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "safework.recuperacion.enabled=true", "safework.public-url=https://safework.example", "safework.mail.from=acceso@safework.example"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class SeguridadAccesoNuevaTests {
    @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder encoder; @Autowired RecuperacionService service;
    @Autowired MockMvc mvc; @Autowired UsuarioAccesoRepository accesos;
    @MockitoBean JavaMailSender mail;
    String username; Long id;
    @BeforeEach void preparar() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS recuperacion_claves(token_hash CHAR(64) PRIMARY KEY,usuario_id BIGINT NOT NULL,creado_en TIMESTAMP NOT NULL,expira_en TIMESTAMP NOT NULL)");
        String codigo=UUID.randomUUID().toString(); username="cuenta_"+codigo;
        jdbc.update("INSERT INTO empresas(nombre,codigo,estado,fecha_registro) VALUES('Empresa',?,'ACTIVO',CURRENT_TIMESTAMP)",codigo);
        Long empresa=jdbc.queryForObject("SELECT id FROM empresas WHERE codigo=?",Long.class,codigo);
        jdbc.update("INSERT INTO usuarios(username,password,rol,nombre,apellido,correo,estado,fecha_registro,empresa_id) VALUES(?,?,'TRABAJADOR','Nombre','Apellido',?,'ACTIVO',CURRENT_DATE,?)",username,encoder.encode("AnteriorClave2026!"),codigo+"@example.test",empresa);
        id=jdbc.queryForObject("SELECT id FROM usuarios WHERE username=?",Long.class,username);
    }
    String tokenEnviado() {
        var captor=ArgumentCaptor.forClass(SimpleMailMessage.class); verify(mail).send(captor.capture());
        return Objects.requireNonNull(captor.getValue().getText()).split("token=")[1].split("\\s")[0];
    }
    @Test void enlaceEsHashEnBaseVenceYSeConsumeUnaSolaVez() {
        service.solicitar(username); String token=tokenEnviado();
        assertThat(service.valido(token)).isTrue();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM recuperacion_claves WHERE usuario_id=?",String.class,id)).isNotEqualTo(token);
        service.restablecer(token,"NuevaClaveSegura2026!");
        assertThat(service.valido(token)).isFalse();
        assertThatThrownBy(() -> service.restablecer(token,"OtraClaveSegura2026!")).hasMessageContaining("venció");
        assertThat(encoder.matches("NuevaClaveSegura2026!",accesos.buscar(username).orElseThrow().password())).isTrue();
        assertThat(accesos.buscar(username).orElseThrow().versionAcceso()).isEqualTo(1);
    }
    @Test void noRecuperaCuentaInactivaYNoRevelaExistenciaEnHttp() throws Exception {
        jdbc.update("UPDATE usuarios SET estado='INACTIVO' WHERE id=?",id);
        service.solicitar(username); verifyNoInteractions(mail);
        for (String user:List.of(username,"cuenta_inexistente")) {
            var form=mvc.perform(get("/recuperar")).andExpect(status().isOk()).andReturn();
            var sesion=(MockHttpSession)form.getRequest().getSession(); CsrfToken csrf=(CsrfToken)form.getRequest().getAttribute("_csrf");
            mvc.perform(post("/recuperar").session(sesion).param("username",user).param(csrf.getParameterName(),csrf.getToken()))
                    .andExpect(redirectedUrl("/recuperar")).andExpect(flash().attributeExists("success"));
        }
    }
    @Test void enlaceVencidoYRepeticionDeSolicitudNoGeneranAcceso() {
        service.solicitar(username); String token=tokenEnviado(); service.solicitar(username); verify(mail,times(1)).send(any(SimpleMailMessage.class));
        jdbc.update("UPDATE recuperacion_claves SET expira_en=? WHERE usuario_id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)),id);
        assertThat(service.valido(token)).isFalse();
        assertThatThrownBy(() -> service.restablecer(token,"NuevaClaveSegura2026!")).hasMessageContaining("venció");
    }
    @Test void formulariosDeRecuperacionExigenCsrfYNoCacheanToken() throws Exception {
        mvc.perform(post("/recuperar").param("username",username)).andExpect(status().isForbidden());
        service.solicitar(username); String token=tokenEnviado();
        mvc.perform(get("/restablecer").param("token",token)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Referrer-Policy","no-referrer"));
        mvc.perform(post("/restablecer").param("token",token).param("password","NuevaClaveSegura2026!")).andExpect(status().isForbidden());
    }
    @Test void cincoFallosBloqueanEseOrigenYCuentaSinBloquearOtroOrigen() throws Exception {
        for (int i=0;i<5;i++) {
            var f=mvc.perform(get("/login")).andReturn(); var s=(MockHttpSession)f.getRequest().getSession(); CsrfToken c=(CsrfToken)f.getRequest().getAttribute("_csrf");
            mvc.perform(post("/login").session(s).param("username",username).param("password","incorrecta").param(c.getParameterName(),c.getToken()))
                    .andExpect(redirectedUrl("/login?error"));
        }
        var f=mvc.perform(get("/login")).andReturn(); var s=(MockHttpSession)f.getRequest().getSession(); CsrfToken c=(CsrfToken)f.getRequest().getAttribute("_csrf");
        mvc.perform(post("/login").session(s).param("username",username).param("password","AnteriorClave2026!").param(c.getParameterName(),c.getToken()))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        mvc.perform(post("/login").with(r -> {r.setRemoteAddr("127.0.0.2");return r;}).session(s).param("username",username)
                .param("password","AnteriorClave2026!").param(c.getParameterName(),c.getToken())).andExpect(redirectedUrl("/dashboard"));
    }
    @Test void cambioDeClaveInvalidaSesionAnterior() throws Exception {
        var f=mvc.perform(get("/login")).andReturn(); var s=(MockHttpSession)f.getRequest().getSession(); CsrfToken c=(CsrfToken)f.getRequest().getAttribute("_csrf");
        s=(MockHttpSession)mvc.perform(post("/login").session(s).param("username",username).param("password","AnteriorClave2026!")
                .param(c.getParameterName(),c.getToken())).andReturn().getRequest().getSession();
        service.solicitar(username); service.restablecer(tokenEnviado(),"NuevaClaveSegura2026!");
        mvc.perform(get("/dashboard").session(s)).andExpect(redirectedUrl("/login?error"));
    }
    @Test void politicaComunRechazaClavesCortasYMasDe72Bytes() {
        assertThatThrownBy(() -> PoliticaContrasena.validar("corta123")).hasMessageContaining("12");
        assertThatThrownBy(() -> PoliticaContrasena.validar("é".repeat(37))).hasMessageContaining("72");
        assertThatCode(() -> PoliticaContrasena.validar("frase suficientemente larga")).doesNotThrowAnyException();
    }
    @Test void limiteExpiraSinDormirQuinceMinutos() {
        class Reloj extends Clock { Instant now=Instant.now(); public java.time.ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(java.time.ZoneId z){return this;} public Instant instant(){return now;} }
        Reloj reloj=new Reloj(); LimiteAcceso limite=new LimiteAcceso(reloj);
        for (int i=0;i<5;i++) limite.fallo("ip","usuario");
        assertThat(limite.espera("ip","usuario")).isPositive(); reloj.now=reloj.now.plusSeconds(901);
        assertThat(limite.espera("ip","usuario")).isZero();
    }
}
