package com.safework.safework;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.safework.safework.controller.LoginController;
import com.safework.safework.security.UsuarioAccesoRepository;
import com.safework.safework.security.UsuarioAccesoRepository.AccesoUsuario;
import com.safework.safework.security.CustomUserDetailsService;
import com.safework.safework.security.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Usa filtros, autenticación BCrypt y Thymeleaf reales; no escribe ni consulta MySQL. */
@WebMvcTest(LoginController.class)
@Import({SecurityConfig.class, CustomUserDetailsService.class,
        LoginAutenticacionTests.SeguridadDePrueba.class})
class LoginAutenticacionTests {
    private static final String PASSWORD = "ClavePrueba2026!";
    private static final String INACTIVA = "Tu cuenta se encuentra inactiva o deshabilitada. Por favor, comunícate con el administrador del sistema.";
    private static final String CREDENCIALES = "Usuario o contraseña incorrectos.";

    @TestConfiguration
    @EnableWebSecurity
    static class SeguridadDePrueba { }

    @MockitoBean com.safework.safework.security.LimiteAcceso limite;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean UsuarioAccesoRepository usuarios;
    private String hash;

    @BeforeEach
    void prepararUsuariosSimulados() {
        hash = encoder.encode(PASSWORD);
        when(usuarios.buscar(anyString())).thenReturn(Optional.empty());
    }

    private void usuario(String username, String rol, String estado) {
        AccesoUsuario usuario = new AccesoUsuario(1L, username, hash, rol, estado,
                1L, "Empresa de prueba", "ACTIVO");
        when(usuarios.buscar(username)).thenReturn(Optional.of(usuario));
    }

    private MockHttpSession intentarLogin(String username, String password, MockHttpSession session)
            throws Exception {
        var formulario = mvc.perform(get("/login").session(session))
                .andExpect(status().isOk()).andReturn();
        CsrfToken csrf = (CsrfToken) formulario.getRequest().getAttribute("_csrf");
        var resultado = mvc.perform(post("/login").session(session)
                .param(csrf.getParameterName(), csrf.getToken())
                .param("username", username).param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    @Test
    void formularioInicialNoMuestraErrorYConservaCsrf() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("errorLogin"))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("alert-danger"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPERVISOR", "TRABAJADOR"})
    void usuarioInactivoMuestraAvisoYNoPuedeAutenticarse(String rol) throws Exception {
        usuario("inactivo", rol, "INACTIVO");
        MockHttpSession session = intentarLogin("inactivo", PASSWORD, new MockHttpSession());

        assertThat(session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION))
                .isInstanceOf(DisabledException.class);
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isNull();
        mvc.perform(get("/login").param("error", "").session(session))
                .andExpect(status().isOk()).andExpect(model().attribute("errorLogin", INACTIVA))
                .andExpect(content().string(containsString(INACTIVA)))
                .andExpect(content().string(not(containsString(CREDENCIALES))));
        mvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void passwordIncorrectaDeUsuarioActivoConservaMensajeGenerico() throws Exception {
        usuario("activo", "TRABAJADOR", "ACTIVO");
        MockHttpSession session = intentarLogin("activo", "incorrecta", new MockHttpSession());
        assertThat(session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION))
                .isInstanceOf(BadCredentialsException.class);
        mvc.perform(get("/login").param("error", "").session(session))
                .andExpect(status().isOk()).andExpect(model().attribute("errorLogin", CREDENCIALES))
                .andExpect(content().string(not(containsString(INACTIVA))));
    }

    @Test
    void usuarioInexistenteNoRevelaSiLaCuentaExiste() throws Exception {
        MockHttpSession session = intentarLogin("desconocido", PASSWORD, new MockHttpSession());
        mvc.perform(get("/login").param("error", "").session(session))
                .andExpect(status().isOk()).andExpect(model().attribute("errorLogin", CREDENCIALES));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPERVISOR", "TRABAJADOR"})
    void usuarioActivoConPasswordValidaSigueEntrandoAlDashboard(String rol) throws Exception {
        usuario("activo", rol, "ACTIVO");
        MockHttpSession session = new MockHttpSession();
        var formulario = mvc.perform(get("/login").session(session)).andReturn();
        CsrfToken csrf = (CsrfToken) formulario.getRequest().getAttribute("_csrf");
        session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new DisabledException("Anterior"));
        var resultado = mvc.perform(post("/login").session(session)
                .param(csrf.getParameterName(), csrf.getToken())
                .param("username", "activo").param("password", PASSWORD))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        var autenticada = resultado.getRequest().getSession(false);
        var contexto = (SecurityContext) autenticada.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(contexto.getAuthentication().getName()).isEqualTo("activo");
        assertThat(contexto.getAuthentication().isAuthenticated()).isTrue();
        assertThat(autenticada.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)).isNull();
    }

    @Test
    void visitaNuevaAlLoginLimpiaElErrorAnterior() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new DisabledException("Anterior"));
        mvc.perform(get("/login").session(session)).andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("errorLogin"));
        assertThat(session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)).isNull();
    }

    @Test
    void falloDeCredencialesPosteriorNoReutilizaElAvisoDeCuentaInactiva() throws Exception {
        usuario("inactivo", "TRABAJADOR", "INACTIVO");
        usuario("activo", "TRABAJADOR", "ACTIVO");
        MockHttpSession session = intentarLogin("inactivo", PASSWORD, new MockHttpSession());
        session = intentarLogin("activo", "incorrecta", session);
        mvc.perform(get("/login").param("error", "").session(session))
                .andExpect(model().attribute("errorLogin", CREDENCIALES));
    }

    @Test
    void elParametroDeLaUrlNoPuedeInventarUnaCuentaInactiva() throws Exception {
        mvc.perform(get("/login").param("error", "disabled"))
                .andExpect(status().isOk()).andExpect(model().attribute("errorLogin", CREDENCIALES))
                .andExpect(content().string(not(containsString(INACTIVA))));
    }

    @Test
    void loginSinCsrfContinuaRechazado() throws Exception {
        usuario("activo", "TRABAJADOR", "ACTIVO");
        mvc.perform(post("/login").param("username", "activo").param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSeExponenMensajesInternosDeExcepciones() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION,
                new AuthenticationServiceException("DETALLE_INTERNO_QUE_NO_DEBE_MOSTRARSE"));
        mvc.perform(get("/login").param("error", "").session(session))
                .andExpect(status().isOk()).andExpect(model().attribute("errorLogin", CREDENCIALES))
                .andExpect(content().string(not(containsString("DETALLE_INTERNO"))));
    }

    @Test
    void recargarElErrorConservaElAvisoDeCuentaInactiva() throws Exception {
        usuario("inactivo", "TRABAJADOR", "INACTIVO");
        MockHttpSession session = intentarLogin("inactivo", PASSWORD, new MockHttpSession());
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/login").param("error", "").session(session))
                    .andExpect(status().isOk()).andExpect(model().attribute("errorLogin", INACTIVA));
        }
    }
}
