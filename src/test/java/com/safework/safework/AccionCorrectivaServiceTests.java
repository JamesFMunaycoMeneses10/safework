package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.model.Area;
import com.safework.safework.model.Incidente;
import com.safework.safework.model.Riesgo;
import com.safework.safework.model.Trabajador;
import com.safework.safework.repository.AccionCorrectivaRepository;
import com.safework.safework.repository.IncidenteRepository;
import com.safework.safework.repository.RiesgoRepository;
import com.safework.safework.repository.TrabajadorRepository;
import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.ArchivoAdjuntoService;

class AccionCorrectivaServiceTests {
    private final AccionCorrectivaRepository acciones = mock(AccionCorrectivaRepository.class);
    private final TrabajadorRepository trabajadores = mock(TrabajadorRepository.class);
    private final RiesgoRepository riesgos = mock(RiesgoRepository.class);
    private final IncidenteRepository incidentes = mock(IncidenteRepository.class);
    private final ArchivoAdjuntoService archivos = mock(ArchivoAdjuntoService.class);
    private final AccionCorrectivaService servicio =
            new AccionCorrectivaService(acciones, trabajadores, riesgos, incidentes, archivos);

    @Test
    void exigeResponsable() {
        AccionCorrectiva accion = accionBase();
        accion.setResponsable(null);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(accion));
        verify(acciones, never()).save(any());
    }

    @Test
    void exigeRiesgoOIncidente() {
        AccionCorrectiva accion = accionBase();
        accion.setRiesgo(null);
        when(trabajadores.findById(1L)).thenReturn(Optional.of(trabajador(1L)));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(accion));
        verify(acciones, never()).save(any());
    }

    @Test
    void rechazaFechaLimiteAnteriorAlRegistro() {
        AccionCorrectiva accion = accionBase();
        accion.setFechaLimite(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(accion));
        verify(acciones, never()).save(any());
    }

    @Test
    void rechazaRiesgoEIncidenteDeAreasDistintas() {
        AccionCorrectiva accion = accionBase();
        Incidente incidente = new Incidente();
        incidente.setId(3L);
        accion.setIncidente(incidente);
        when(trabajadores.findById(1L)).thenReturn(Optional.of(trabajador(1L)));
        when(riesgos.findById(2L)).thenReturn(Optional.of(riesgo(2L, 10L)));
        when(incidentes.findById(3L)).thenReturn(Optional.of(incidente(3L, 20L)));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(accion));
        verify(acciones, never()).save(any());
    }

    @Test
    void noCompletaSinEvidencia() {
        AccionCorrectiva accion = accionBase();
        accion.setEstado("Completada");
        prepararReferencias();
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(accion));
        verify(acciones, never()).save(any());
    }

    @Test
    void guardaCierreConEvidenciaYFechaDelServidor() {
        AccionCorrectiva accion = accionBase();
        accion.setEstado("Completada");
        accion.setEvidenciaCierre("  Estantería fijada y comprobada  ");
        prepararReferencias();
        when(acciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile archivo = new MockMultipartFile("archivoEvidencia", "prueba.jpg", "image/jpeg", new byte[]{1});
        when(archivos.guardar(archivo, true)).thenReturn(
                new ArchivoAdjuntoService.ArchivoGuardado("guardado.jpg", "prueba.jpg", "image/jpeg"));

        AccionCorrectiva guardada = servicio.guardar(accion, archivo);

        assertEquals("Estantería fijada y comprobada", guardada.getEvidenciaCierre());
        assertEquals(LocalDate.now(), guardada.getFechaCierre());
        assertEquals(LocalDate.now(), guardada.getFechaRegistro());
        assertNotNull(guardada.getResponsable());
        assertNotNull(guardada.getRiesgo());
        assertNull(guardada.getIncidente());
        assertEquals("guardado.jpg", guardada.getArchivoEvidencia());
    }

    private void prepararReferencias() {
        when(trabajadores.findById(1L)).thenReturn(Optional.of(trabajador(1L)));
        when(riesgos.findById(2L)).thenReturn(Optional.of(riesgo(2L, 10L)));
    }

    private AccionCorrectiva accionBase() {
        AccionCorrectiva accion = new AccionCorrectiva();
        accion.setDescripcion("Asegurar estantería");
        accion.setFechaLimite(LocalDate.now().plusDays(5));
        accion.setEstado("Pendiente");
        accion.setPrioridad("Alta");
        accion.setResponsable(trabajador(1L));
        accion.setRiesgo(riesgo(2L, 10L));
        return accion;
    }

    private Trabajador trabajador(Long id) {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(id);
        return trabajador;
    }

    private Riesgo riesgo(Long id, Long areaId) {
        Riesgo riesgo = new Riesgo();
        riesgo.setId(id);
        Area area = new Area();
        area.setId(areaId);
        riesgo.setArea(area);
        return riesgo;
    }

    private Incidente incidente(Long id, Long areaId) {
        Incidente incidente = new Incidente();
        incidente.setId(id);
        Area area = new Area();
        area.setId(areaId);
        incidente.setArea(area);
        return incidente;
    }
}
