package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.safework.safework.controller.HallazgoInspeccionController;
import com.safework.safework.model.Area;
import com.safework.safework.model.HallazgoInspeccion;
import com.safework.safework.model.Inspeccion;
import com.safework.safework.model.Riesgo;
import com.safework.safework.repository.HallazgoInspeccionRepository;
import com.safework.safework.repository.InspeccionRepository;
import com.safework.safework.repository.RiesgoRepository;
import com.safework.safework.service.InspeccionService;
import com.safework.safework.service.RiesgoService;

import static org.mockito.Mockito.mock;
import org.mockito.ArgumentCaptor;

/** Pruebas de la lógica de hallazgos sin modificar MySQL. */
class HallazgosFlujoTests {

    private final HallazgoInspeccionRepository hallazgos = mock(HallazgoInspeccionRepository.class);
    private final InspeccionRepository inspeccionesRepo = mock(InspeccionRepository.class);
    private final RiesgoRepository riesgosRepo = mock(RiesgoRepository.class);
    private final InspeccionService inspecciones = new InspeccionService(inspeccionesRepo, hallazgos);
    private final RiesgoService riesgos = new RiesgoService(riesgosRepo, hallazgos);
    private final HallazgoInspeccionController controlador =
            new HallazgoInspeccionController(hallazgos, inspecciones, riesgos);

    @Test
    void guardaHallazgoConRiesgoDeLaMismaArea() {
        Area almacen = area(1L);
        Inspeccion inspeccion = inspeccion(10L, almacen);
        Riesgo riesgo = riesgo(20L, almacen);
        when(inspeccionesRepo.findById(10L)).thenReturn(Optional.of(inspeccion));
        when(riesgosRepo.findById(20L)).thenReturn(Optional.of(riesgo));

        String ruta = controlador.guardar(10L, "  Estantería inestable  ", 20L);

        ArgumentCaptor<HallazgoInspeccion> captor = ArgumentCaptor.forClass(HallazgoInspeccion.class);
        verify(hallazgos).save(captor.capture());
        assertEquals("Estantería inestable", captor.getValue().getDescripcion());
        assertSame(inspeccion, captor.getValue().getInspeccion());
        assertSame(riesgo, captor.getValue().getRiesgo());
        assertEquals("redirect:/inspecciones/10/hallazgos", ruta);
    }

    @Test
    void rechazaRiesgoDeOtraArea() {
        when(inspeccionesRepo.findById(10L)).thenReturn(Optional.of(inspeccion(10L, area(1L))));
        when(riesgosRepo.findById(20L)).thenReturn(Optional.of(riesgo(20L, area(2L))));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controlador.guardar(10L, "Estantería inestable", 20L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(hallazgos, never()).save(any());
    }

    @Test
    void bloqueaEliminarInspeccionConHallazgos() {
        when(inspeccionesRepo.existsById(10L)).thenReturn(true);
        when(hallazgos.existsByInspeccionId(10L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> inspecciones.eliminarPorId(10L));
        verify(inspeccionesRepo, never()).deleteById(10L);
    }

    @Test
    void bloqueaEliminarRiesgoVinculadoAHallazgo() {
        when(riesgosRepo.findById(20L)).thenReturn(Optional.of(riesgo(20L, area(1L))));
        when(hallazgos.existsByRiesgoId(20L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> riesgos.eliminarPorId(20L));
        verify(riesgosRepo, never()).delete(any(Riesgo.class));
    }

    private Area area(Long id) {
        Area area = new Area();
        area.setId(id);
        return area;
    }

    private Inspeccion inspeccion(Long id, Area area) {
        Inspeccion inspeccion = new Inspeccion();
        inspeccion.setId(id);
        inspeccion.setArea(area);
        inspeccion.setFecha(LocalDate.of(2026, 9, 15));
        return inspeccion;
    }

    private Riesgo riesgo(Long id, Area area) {
        Riesgo riesgo = new Riesgo();
        riesgo.setId(id);
        riesgo.setArea(area);
        return riesgo;
    }
}
