package com.safework.safework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.server.ResponseStatusException;
import com.safework.safework.controller.*;
import com.safework.safework.model.*;
import com.safework.safework.service.*;

/** Un identificador ajeno se trata como no disponible, sin revelar otra empresa ni generar HTTP 500. */
class ErroresFormularioEmpresaTests {
    private final AreaService areas = mock(AreaService.class);
    private final TrabajadorService trabajadores = mock(TrabajadorService.class);
    private final IncidenteService incidentes = mock(IncidenteService.class);
    private final InspeccionService inspecciones = mock(InspeccionService.class);

    @Test
    void areaNoDisponibleDevuelveFormularioConErrorSeguro() {
        Area datos = new Area("Almacén", "");
        BeanPropertyBindingResult errores = new BeanPropertyBindingResult(datos, "area");
        when(areas.guardar(datos)).thenThrow(new IllegalArgumentException("DETALLE_INTERNO"));
        assertThat(new AreaController(areas).guardarArea(datos, errores)).isEqualTo("areas/formulario");
        assertThat(errores.getFieldError("nombre").getDefaultMessage())
                .contains("No se pudo guardar").doesNotContain("DETALLE_INTERNO");
    }

    @Test
    void incidenteNoDisponibleConservaDatosYRecargaSelectoresLocales() {
        Incidente datos = new Incidente();
        BeanPropertyBindingResult errores = new BeanPropertyBindingResult(datos, "incidente");
        ExtendedModelMap model = new ExtendedModelMap();
        Area propia = new Area("Área propia", "");
        when(areas.listarTodas()).thenReturn(List.of(propia));
        when(trabajadores.listarTodos()).thenReturn(List.of());
        when(incidentes.guardar(datos, "supervisor"))
                .thenThrow(new IllegalArgumentException("DETALLE_INTERNO"));
        String vista = new IncidenteController(incidentes, areas, trabajadores).guardar(datos, errores,
                model, UsernamePasswordAuthenticationToken.authenticated("supervisor", "", List.of()));
        assertThat(vista).isEqualTo("incidentes/formulario");
        assertThat(errores.getFieldError("area").getDefaultMessage())
                .contains("tu empresa").doesNotContain("DETALLE_INTERNO");
        assertThat(model.get("areas")).isEqualTo(List.of(propia));
        assertThat(model.get("trabajadores")).isEqualTo(List.of());
    }

    @Test
    void inspeccionNoDisponibleConservaDatosYRecargaSelectoresLocales() {
        Inspeccion datos = new Inspeccion();
        BeanPropertyBindingResult errores = new BeanPropertyBindingResult(datos, "inspeccion");
        ExtendedModelMap model = new ExtendedModelMap();
        Area propia = new Area("Área propia", "");
        when(areas.listarTodas()).thenReturn(List.of(propia));
        when(trabajadores.listarTodos()).thenReturn(List.of());
        when(inspecciones.guardar(datos)).thenThrow(new IllegalArgumentException("DETALLE_INTERNO"));
        assertThat(new InspeccionController(inspecciones, areas, trabajadores)
                .guardarInspeccion(datos, errores, model)).isEqualTo("inspecciones/formulario");
        assertThat(errores.getFieldError("area").getDefaultMessage())
                .contains("tu empresa").doesNotContain("DETALLE_INTERNO");
        assertThat(model.get("areas")).isEqualTo(List.of(propia));
        assertThat(model.get("trabajadores")).isEqualTo(List.of());
    }

    @Test
    void editarAreaNoDisponibleEs404() {
        assertThatThrownBy(() -> new AreaController(areas).mostrarFormularioEditar(999L, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void editarIncidenteNoDisponibleEs404() {
        assertThatThrownBy(() -> new IncidenteController(incidentes, areas, trabajadores)
                .editar(999L, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void editarInspeccionNoDisponibleEs404() {
        assertThatThrownBy(() -> new InspeccionController(inspecciones, areas, trabajadores)
                .mostrarFormularioEditar(999L, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
