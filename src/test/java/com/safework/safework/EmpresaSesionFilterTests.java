package com.safework.safework;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import com.safework.safework.security.*;
import com.safework.safework.security.UsuarioAccesoRepository.AccesoUsuario;
import com.safework.safework.tenancy.EmpresaContexto;

class EmpresaSesionFilterTests {
    UsuarioAccesoRepository accesos;
    EmpresaSesionFilter filtro;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    FilterChain chain;
    @BeforeEach void preparar() {
        accesos = mock(UsuarioAccesoRepository.class); filtro = new EmpresaSesionFilter(accesos);
        request = new MockHttpServletRequest("GET", "/dashboard"); response = new MockHttpServletResponse();
        chain = mock(FilterChain.class); SecurityContextHolder.clearContext();
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    void autenticar(String rol) {
        EmpresaPrincipal principal = new EmpresaPrincipal(5L, "usuario", "hash", rol, true, 2L, "Empresa 2");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }
    void datos(String rol, String estado, long empresa, String estadoEmpresa) {
        when(accesos.buscar("usuario")).thenReturn(Optional.of(new AccesoUsuario(5L,"usuario","hash",rol,
                estado,empresa,"Empresa",estadoEmpresa)));
    }
    void verificarRechazo() throws Exception {
        filtro.doFilter(request,response,chain);
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)).isInstanceOf(DisabledException.class);
        verifyNoInteractions(chain);
    }
    @Test void usuarioActivoConservaSesionYSuEmpresa() throws Exception {
        autenticar("TRABAJADOR"); datos("TRABAJADOR","ACTIVO",2L,"ACTIVO");
        filtro.doFilter(request,response,chain);
        verify(chain).doFilter(request,response); assertThat(EmpresaContexto.empresaActualId()).isEqualTo(2L);
    }
    @Test void deshabilitarUsuarioInvalidaUnaSesionYaAbierta() throws Exception {
        autenticar("TRABAJADOR"); datos("TRABAJADOR","INACTIVO",2L,"ACTIVO"); verificarRechazo();
    }
    @Test void deshabilitarEmpresaInvalidaSesionYaAbierta() throws Exception {
        autenticar("ADMIN"); datos("ADMIN","ACTIVO",2L,"INACTIVO"); verificarRechazo();
    }
    @Test void cambiarEmpresaDeCuentaInvalidaSesionAnterior() throws Exception {
        autenticar("ADMIN"); datos("ADMIN","ACTIVO",3L,"ACTIVO"); verificarRechazo();
    }
    @Test void cambiarRolInvalidaPermisosAnteriores() throws Exception {
        autenticar("ADMIN"); datos("TRABAJADOR","ACTIVO",2L,"ACTIVO"); verificarRechazo();
    }
    @Test void cuentaEliminadaNoConservaSesion() throws Exception {
        autenticar("ADMIN"); when(accesos.buscar("usuario")).thenReturn(Optional.empty()); verificarRechazo();
    }
    @Test void plataformaNoHeredaAccesoALosRegistrosDeEmpresa() throws Exception {
        autenticar("SUPERADMIN"); datos("SUPERADMIN","ACTIVO",2L,"INACTIVO");
        filtro.doFilter(request,response,chain); verify(chain).doFilter(request,response);
        assertThat(EmpresaContexto.empresaActualId()).isEqualTo(-1L);
    }
    @Test void visitanteNoHaceConsultasDeCredenciales() throws Exception {
        filtro.doFilter(request,response,chain); verify(chain).doFilter(request,response); verifyNoInteractions(accesos);
    }
}
