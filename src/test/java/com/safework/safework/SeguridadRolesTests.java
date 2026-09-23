package com.safework.safework;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.safework.safework.model.*;
import com.safework.safework.repository.HallazgoInspeccionRepository;
import com.safework.safework.security.SecurityConfig;
import com.safework.safework.service.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prueba controladores, vistas y filtros reales con servicios simulados: no usa MySQL. */
@WebMvcTest
@Import({SecurityConfig.class, SeguridadRolesTests.SeguridadDePrueba.class})
class SeguridadRolesTests {
    @TestConfiguration
    @EnableWebSecurity
    static class SeguridadDePrueba { }

    @MockitoBean UserDetailsService userDetailsService;
    private static final String[] MODULOS = {
        "trabajadores", "riesgos", "incidentes", "inspecciones", "acciones"
    };
    @Autowired MockMvc mvc;
    @MockitoBean TrabajadorService trabajadores;
    @MockitoBean RiesgoService riesgos;
    @MockitoBean IncidenteService incidentes;
    @MockitoBean InspeccionService inspecciones;
    @MockitoBean AccionCorrectivaService acciones;
    @MockitoBean AreaService areas;
    @MockitoBean UsuarioService usuarios;
    @MockitoBean HallazgoInspeccionRepository hallazgos;

    @BeforeEach
    void datosDeConsulta() {
        Area area = new Area("Almacén", "Área de prueba");
        area.setId(1L);
        Trabajador trabajador = new Trabajador("12345678", "Ana", "Prueba", "Operaria", area);
        trabajador.setId(1L);
        Riesgo riesgo = new Riesgo("Caída", "Piso húmedo", 2, 3, 6, "Señalizar", area);
        riesgo.setId(1L);
        Incidente incidente = new Incidente(LocalDateTime.now(), "Incidente", area,
                trabajador, "Prueba", "Leve", "Reportado");
        incidente.setId(1L);
        Inspeccion inspeccion = new Inspeccion(LocalDate.now(), "Inspección de seguridad",
                area, trabajador, "Programada", "Prueba");
        inspeccion.setId(1L);
        AccionCorrectiva accion = new AccionCorrectiva("Señalizar zona de trabajo",
                LocalDate.now(), LocalDate.now().plusDays(3), "Pendiente", "Alta",
                trabajador, riesgo, incidente);
        accion.setId(1L);
        when(areas.listarTodas()).thenReturn(List.of(area));
        when(trabajadores.listarTodos()).thenReturn(List.of(trabajador));
        when(riesgos.listarTodos()).thenReturn(List.of(riesgo));
        when(incidentes.listarTodos()).thenReturn(List.of(incidente));
        when(inspecciones.listarTodas()).thenReturn(List.of(inspeccion));
        when(acciones.listarTodas()).thenReturn(List.of(accion));
        when(areas.buscarPorId(1L)).thenReturn(Optional.of(area));
        when(trabajadores.buscarPorId(1L)).thenReturn(Optional.of(trabajador));
        when(riesgos.buscarPorId(1L)).thenReturn(Optional.of(riesgo));
        when(incidentes.buscarPorId(1L)).thenReturn(Optional.of(incidente));
        when(inspecciones.buscarPorId(1L)).thenReturn(Optional.of(inspeccion));
        when(acciones.buscarPorId(1L)).thenReturn(Optional.of(accion));
    }

    private MockHttpSession sesion(String rol) {
        MockHttpSession session = new MockHttpSession();
        var auth = new UsernamePasswordAuthenticationToken("prueba", "no-usada",
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(auth));
        return session;
    }

    private MockHttpServletRequestBuilder postConCsrf(String url, MockHttpSession session)
            throws Exception {
        var result = mvc.perform(get("/login").session(session)).andExpect(status().isOk()).andReturn();
        CsrfToken token = (CsrfToken) result.getRequest().getAttribute("_csrf");
        return post(url).session(session).param(token.getParameterName(), token.getToken());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPERVISOR", "TRABAJADOR"})
    void hallazgosSeMuestranSegunRol(String rol) throws Exception {
        String html = mvc.perform(get("/inspecciones/1/hallazgos").session(sesion(rol)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Hallazgos de la inspección", "dashboard-sidebar");
        assertThat(html.contains("Registrar hallazgo")).isEqualTo(!rol.equals("TRABAJADOR"));
    }

    @Test
    void trabajadorNoPuedeRegistrarHallazgo() throws Exception {
        mvc.perform(postConCsrf("/inspecciones/1/hallazgos", sesion("TRABAJADOR"))
                .param("descripcion", "Prueba"))
                .andExpect(status().isForbidden());
        verify(hallazgos, never()).save(any());
    }

    static Stream<Arguments> rolesYModulos() {
        return Stream.of("ADMIN", "SUPERVISOR", "TRABAJADOR")
                .flatMap(rol -> Stream.of(MODULOS).map(modulo -> Arguments.of(rol, modulo)));
    }

    @ParameterizedTest
    @MethodSource("rolesYModulos")
    void listadosYBotonesSegunRol(String rol, String modulo) throws Exception {
        String html = mvc.perform(get("/" + modulo).session(sesion(rol)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        boolean gestiona = !rol.equals("TRABAJADOR");
        assertThat(html.contains("/" + modulo + "/editar/1")).isEqualTo(gestiona);
        assertThat(html.contains("/" + modulo + "/eliminar/1")).isEqualTo(gestiona);
        assertThat(html.contains("/" + modulo + "/nuev")).isEqualTo(gestiona);
        assertThat(html.contains("href=\"/areas\"")).isEqualTo(rol.equals("ADMIN"));
        assertThat(html.contains("href=\"/usuarios\"")).isEqualTo(rol.equals("ADMIN"));
    }

    @ParameterizedTest
    @MethodSource("rolesYModulos")
    void formulariosDirectosSegunRol(String rol, String modulo) throws Exception {
        var session = sesion(rol);
        String nuevo = List.of("acciones", "inspecciones").contains(modulo) ? "nueva" : "nuevo";
        int esperado = rol.equals("TRABAJADOR") ? 403 : 200;
        mvc.perform(get("/" + modulo + "/" + nuevo).session(session))
                .andExpect(status().is(esperado));
        mvc.perform(get("/" + modulo + "/editar/1").session(session))
                .andExpect(status().is(esperado));
    }

    @ParameterizedTest
    @MethodSource("rolesYModulos")
    void eliminacionDirectaSegunRolConCsrfValido(String rol, String modulo) throws Exception {
        mvc.perform(postConCsrf("/" + modulo + "/eliminar/1", sesion(rol)))
                .andExpect(status().is(rol.equals("TRABAJADOR") ? 403 : 302));
        if (rol.equals("TRABAJADOR")) {
            verify(trabajadores, never()).eliminarPorId(anyLong());
            verify(riesgos, never()).eliminarPorId(anyLong());
            verify(incidentes, never()).eliminar(anyLong());
            verify(inspecciones, never()).eliminarPorId(anyLong());
            verify(acciones, never()).eliminarPorId(anyLong());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"trabajadores", "riesgos", "incidentes", "inspecciones", "acciones"})
    void trabajadorNoPuedeGuardarNiActualizar(String modulo) throws Exception {
        mvc.perform(postConCsrf("/" + modulo + "/guardar", sesion("TRABAJADOR"))
                .param("id", "1")).andExpect(status().isForbidden());
        verify(trabajadores, never()).guardar(any());
        verify(riesgos, never()).guardar(any());
        verify(incidentes, never()).guardar(any());
        verify(inspecciones, never()).guardar(any());
        verify(acciones, never()).guardar(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPERVISOR", "TRABAJADOR"})
    void administracionYDashboard(String rol) throws Exception {
        var session = sesion(rol);
        mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk());
        for (String url : List.of("/usuarios", "/usuarios/nuevo", "/areas", "/areas/nueva")) {
            mvc.perform(get(url).session(session)).andExpect(status().is(rol.equals("ADMIN") ? 200 : 403));
        }
        mvc.perform(get("/usuarios/eliminar/1").session(session))
                .andExpect(status().is(rol.equals("ADMIN") ? 405 : 403));
        mvc.perform(postConCsrf("/usuarios/eliminar/1", session))
                .andExpect(status().is(rol.equals("ADMIN") ? 302 : 403));
        mvc.perform(postConCsrf("/areas/eliminar/1", session))
                .andExpect(status().is(rol.equals("ADMIN") ? 302 : 403));
    }

    @Test
    void anonimoDebeAutenticarse() throws Exception {
        for (String modulo : MODULOS) {
            mvc.perform(get("/" + modulo)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void csrfSigueProtegiendoInclusoAlAdministrador() throws Exception {
        mvc.perform(post("/acciones/eliminar/1").session(sesion("ADMIN")))
                .andExpect(status().isForbidden());
        verify(acciones, never()).eliminarPorId(anyLong());
    }

    @Test
    void logoutCierraLaSesion() throws Exception {
        var session = sesion("TRABAJADOR");
        mvc.perform(postConCsrf("/logout", session)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertThat(session.isInvalid()).isTrue();
    }
}
