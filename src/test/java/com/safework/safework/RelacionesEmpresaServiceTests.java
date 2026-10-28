package com.safework.safework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.safework.safework.model.*;
import com.safework.safework.repository.*;
import com.safework.safework.service.*;

/** Las referencias del formulario siempre se resuelven con repositorios de la empresa actual. */
class RelacionesEmpresaServiceTests {
    private final AreaRepository areas = mock(AreaRepository.class);
    private final TrabajadorRepository trabajadores = mock(TrabajadorRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final HallazgoInspeccionRepository hallazgos = mock(HallazgoInspeccionRepository.class);

    private Area area(Long id) {
        Area area = new Area("Almacén", "");
        area.setId(id);
        return area;
    }

    private Trabajador trabajador(Long id) {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(id);
        return trabajador;
    }

    @Test
    void areaConIdNoVisibleNoSeActualizaNiElimina() {
        AreaService servicio = new AreaService(areas);
        assertThatThrownBy(() -> servicio.guardar(area(99L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.eliminarPorId(99L)).isInstanceOf(IllegalArgumentException.class);
        verify(areas, never()).save(any());
        verify(areas, never()).delete(any());
    }

    @Test
    void editarAreaCopiaSoloCamposSobreEntidadExistente() {
        Area existente = area(1L);
        Area formulario = area(1L);
        formulario.setNombre("Nuevo nombre");
        when(areas.findById(1L)).thenReturn(Optional.of(existente));
        when(areas.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(new AreaService(areas).guardar(formulario)).isSameAs(existente);
        assertThat(existente.getNombre()).isEqualTo("Nuevo nombre");
        verify(areas, never()).save(formulario);
    }

    @Test
    void trabajadorNoAdmiteAreaDeOtraEmpresa() {
        TrabajadorService servicio = new TrabajadorService(trabajadores, usuarios, areas);
        Trabajador datos = trabajador(null);
        datos.setArea(area(99L));
        assertThatThrownBy(() -> servicio.guardar(datos)).isInstanceOf(IllegalArgumentException.class);
        verify(trabajadores, never()).save(any());
    }

    @Test
    void trabajadorNoActualizaUnIdNoVisible() {
        Trabajador datos = trabajador(99L);
        datos.setArea(area(1L));
        assertThatThrownBy(() -> new TrabajadorService(trabajadores, usuarios, areas).guardar(datos))
                .isInstanceOf(IllegalArgumentException.class);
        verify(trabajadores, never()).save(any());
    }

    @Test
    void trabajadorNoAdmiteCuentaNoVisible() {
        when(areas.findById(1L)).thenReturn(Optional.of(area(1L)));
        Trabajador datos = trabajador(null);
        datos.setArea(area(1L));
        Usuario cuenta = new Usuario();
        cuenta.setId(99L);
        datos.setUsuario(cuenta);
        assertThatThrownBy(() -> new TrabajadorService(trabajadores, usuarios, areas).guardar(datos))
                .isInstanceOf(IllegalArgumentException.class);
        verify(trabajadores, never()).save(any());
    }

    @Test
    void inspeccionNoAdmiteResponsableNoVisible() {
        InspeccionRepository inspecciones = mock(InspeccionRepository.class);
        when(areas.findById(1L)).thenReturn(Optional.of(area(1L)));
        Inspeccion datos = new Inspeccion();
        datos.setArea(area(1L));
        datos.setResponsable(trabajador(99L));
        assertThatThrownBy(() -> new InspeccionService(inspecciones, hallazgos, areas, trabajadores)
                .guardar(datos)).isInstanceOf(IllegalArgumentException.class);
        verify(inspecciones, never()).save(any());
    }

    @Test
    void incidenteNoAdmiteTrabajadorNoVisible() {
        IncidenteRepository incidentes = mock(IncidenteRepository.class);
        when(areas.findById(1L)).thenReturn(Optional.of(area(1L)));
        Incidente datos = new Incidente();
        datos.setArea(area(1L));
        datos.setTrabajador(trabajador(99L));
        assertThatThrownBy(() -> new IncidenteService(incidentes, null, areas, trabajadores)
                .guardar(datos)).isInstanceOf(IllegalArgumentException.class);
        verify(incidentes, never()).save(any());
    }

    @Test
    void riesgoNoAdmiteAreaNoVisibleNiGuardaSuFoto() {
        RiesgoRepository riesgos = mock(RiesgoRepository.class);
        ArchivoAdjuntoService archivos = mock(ArchivoAdjuntoService.class);
        Riesgo datos = new Riesgo();
        datos.setArea(area(99L));
        assertThatThrownBy(() -> new RiesgoService(riesgos, hallazgos, archivos, null, areas)
                .guardar(datos)).isInstanceOf(IllegalArgumentException.class);
        verify(riesgos, never()).save(any());
        verifyNoInteractions(archivos);
    }
}
