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
import static org.mockito.Mockito.times;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.model.AccionCorrectivaEvento;
import com.safework.safework.model.Area;
import com.safework.safework.model.Incidente;
import com.safework.safework.model.Riesgo;
import com.safework.safework.model.Trabajador;
import com.safework.safework.model.Usuario;
import com.safework.safework.repository.AccionCorrectivaRepository;
import com.safework.safework.repository.AccionCorrectivaEventoRepository;
import com.safework.safework.repository.IncidenteRepository;
import com.safework.safework.repository.RiesgoRepository;
import com.safework.safework.repository.TrabajadorRepository;
import com.safework.safework.repository.UsuarioRepository;
import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.ArchivoAdjuntoService;
import com.safework.safework.service.AuditoriaService;

class AccionCorrectivaServiceTests {
    private final AccionCorrectivaRepository acciones = mock(AccionCorrectivaRepository.class);
    private final TrabajadorRepository trabajadores = mock(TrabajadorRepository.class);
    private final RiesgoRepository riesgos = mock(RiesgoRepository.class);
    private final IncidenteRepository incidentes = mock(IncidenteRepository.class);
    private final ArchivoAdjuntoService archivos = mock(ArchivoAdjuntoService.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AccionCorrectivaEventoRepository eventos = mock(AccionCorrectivaEventoRepository.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final AccionCorrectivaService servicio =
            new AccionCorrectivaService(acciones, trabajadores, riesgos, incidentes, archivos, usuarios, eventos, auditoria);

    @Test
    void auditaCreacionConUsuario() {
        prepararReferencias();
        when(acciones.save(any())).thenAnswer(invocation -> {
            AccionCorrectiva guardada = invocation.getArgument(0);
            guardada.setId(15L);
            return guardada;
        });

        servicio.guardar(accionBase(), null, "supervisor");

        verify(auditoria).registrar(org.mockito.ArgumentMatchers.eq("ACCIONES CORRECTIVAS"),
                org.mockito.ArgumentMatchers.eq(15L), org.mockito.ArgumentMatchers.eq("CREADO"),
                org.mockito.ArgumentMatchers.eq("supervisor"),
                org.mockito.ArgumentMatchers.contains("Descripción: Asegurar estantería"));
    }

    @Test
    void auditaSoloLosCambiosDeEdicion() {
        AccionCorrectiva anterior = accionBase();
        anterior.setId(15L);
        anterior.setFechaRegistro(LocalDate.now());
        AccionCorrectiva datos = accionBase();
        datos.setId(15L);
        datos.setPrioridad("Crítica");
        when(acciones.findById(15L)).thenReturn(Optional.of(anterior));
        prepararReferencias();
        when(acciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        servicio.guardar(datos, null, "supervisor");

        verify(auditoria).registrar(org.mockito.ArgumentMatchers.eq("ACCIONES CORRECTIVAS"),
                org.mockito.ArgumentMatchers.eq(15L), org.mockito.ArgumentMatchers.eq("ACTUALIZADO"),
                org.mockito.ArgumentMatchers.eq("supervisor"),
                org.mockito.ArgumentMatchers.argThat(detalle -> detalle.contains("Prioridad: Alta")
                        && detalle.contains("Prioridad: Crítica")));
    }

    @Test
    void auditaEliminacionPermitida() {
        AccionCorrectiva accion = accionBase();
        accion.setId(15L);
        when(acciones.findById(15L)).thenReturn(Optional.of(accion));

        servicio.eliminarPorId(15L, "admin");

        verify(acciones).delete(accion);
        verify(acciones).flush();
        verify(auditoria).registrar(org.mockito.ArgumentMatchers.eq("ACCIONES CORRECTIVAS"),
                org.mockito.ArgumentMatchers.eq(15L), org.mockito.ArgumentMatchers.eq("ELIMINADO"),
                org.mockito.ArgumentMatchers.eq("admin"), any());
    }

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
    void permiteResponsableDeOtraArea() {
        AccionCorrectiva accion = accionBase();
        Trabajador responsable = trabajador(1L);
        responsable.setArea(area(20L));
        when(trabajadores.findById(1L)).thenReturn(Optional.of(responsable));
        when(riesgos.findById(2L)).thenReturn(Optional.of(riesgo(2L, 10L)));
        when(acciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccionCorrectiva guardada = servicio.guardar(accion);

        assertEquals(20L, guardada.getResponsable().getArea().getId());
        assertEquals(10L, guardada.getRiesgo().getArea().getId());
    }

    @Test
    void noPermiteCompletarDesdeFormularioGeneral() {
        AccionCorrectiva accion = accionBase();
        accion.setEstado("Completada");
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(accion));
        verify(acciones, never()).save(any());
    }

    @Test
    void responsableEntregaYSupervisorCierra() {
        AccionCorrectiva accion = accionBase();
        accion.setId(5L);
        Usuario ejecutor = usuario(10L, "operario", "TRABAJADOR");
        Usuario supervisor = usuario(11L, "supervisor", "SUPERVISOR");
        Trabajador responsable = trabajador(1L);
        responsable.setUsuario(ejecutor);
        accion.setResponsable(responsable);
        when(acciones.findById(5L)).thenReturn(Optional.of(accion));
        when(trabajadores.findByUsuarioUsername("operario")).thenReturn(Optional.of(responsable));
        when(usuarios.findByUsername("operario")).thenReturn(Optional.of(ejecutor));
        when(usuarios.findByUsername("supervisor")).thenReturn(Optional.of(supervisor));
        when(acciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile archivo = new MockMultipartFile("adjunto", "prueba.jpg", "image/jpeg", new byte[]{1});
        when(archivos.guardar(archivo, true)).thenReturn(
                new ArchivoAdjuntoService.ArchivoGuardado("guardado.jpg", "prueba.jpg", "image/jpeg"));

        AccionCorrectiva entregada = servicio.entregar(5L, "operario", "  Estantería fijada y comprobada  ", archivo);
        assertEquals("En revisión", entregada.getEstado());
        assertNull(entregada.getFechaCierre());
        assertEquals("Estantería fijada y comprobada", entregada.getEvidenciaCierre());
        assertNotNull(entregada.getFechaEnvioRevision());
        assertEquals("guardado.jpg", entregada.getArchivoEvidencia());
        AccionCorrectiva cerrada = servicio.revisar(5L, "supervisor", true, "Verificado en sitio");

        assertEquals("Cerrada", cerrada.getEstado());
        assertEquals(LocalDate.now(), cerrada.getFechaCierre());
        assertEquals(supervisor, cerrada.getRevisadaPor());
    }

    @Test
    void impideAutovalidacion() {
        AccionCorrectiva accion = accionBase();
        accion.setId(5L);
        accion.setEstado("En revisión");
        Usuario supervisor = usuario(11L, "supervisor", "SUPERVISOR");
        accion.setEnviadaPor(supervisor);
        when(acciones.findById(5L)).thenReturn(Optional.of(accion));
        when(usuarios.findByUsername("supervisor")).thenReturn(Optional.of(supervisor));
        assertThrows(IllegalArgumentException.class, () -> servicio.revisar(5L, "supervisor", true, null));
        verify(acciones, never()).save(any());
    }

    @Test
    void devolucionExigeObservacion() {
        AccionCorrectiva accion = accionBase();
        accion.setId(5L);
        accion.setEstado("En revisión");
        when(acciones.findById(5L)).thenReturn(Optional.of(accion));
        when(usuarios.findByUsername("supervisor"))
                .thenReturn(Optional.of(usuario(11L, "supervisor", "SUPERVISOR")));
        assertThrows(IllegalArgumentException.class, () -> servicio.revisar(5L, "supervisor", false, " "));
        verify(acciones, never()).save(any());
    }

    @Test
    void devuelveConObservacionYSinFechaCierre() {
        AccionCorrectiva accion = accionBase();
        accion.setId(5L);
        accion.setEstado("En revisión");
        when(acciones.findById(5L)).thenReturn(Optional.of(accion));
        when(usuarios.findByUsername("supervisor"))
                .thenReturn(Optional.of(usuario(11L, "supervisor", "SUPERVISOR")));
        when(acciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccionCorrectiva devuelta = servicio.revisar(5L, "supervisor", false, "  Falta verificar la fijación  ");

        assertEquals("Devuelta", devuelta.getEstado());
        assertEquals("Falta verificar la fijación", devuelta.getObservacionRevision());
        assertNull(devuelta.getFechaCierre());
    }

    @Test
    void impideEntregaDeOtroTrabajador() {
        AccionCorrectiva accion = accionBase();
        accion.setId(5L);
        when(acciones.findById(5L)).thenReturn(Optional.of(accion));
        when(usuarios.findByUsername("otro"))
                .thenReturn(Optional.of(usuario(12L, "otro", "TRABAJADOR")));
        when(trabajadores.findByUsuarioUsername("otro"))
                .thenReturn(Optional.of(trabajador(8L)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.entregar(5L, "otro", "Trabajo realizado", null));
        verify(acciones, never()).save(any());
    }

    @Test
    void reenvioConservaEntregaYRevisionAnteriores() {
        AccionCorrectiva accion = accionBase();
        accion.setId(5L);
        accion.setEstado("Devuelta");
        Usuario ejecutor = usuario(10L, "operario", "TRABAJADOR");
        Usuario supervisor = usuario(11L, "supervisor", "SUPERVISOR");
        accion.setEnviadaPor(ejecutor);
        accion.setFechaEnvioRevision(LocalDateTime.now().minusDays(1));
        accion.setRevisadaPor(supervisor);
        accion.setFechaRevision(LocalDateTime.now().minusHours(2));
        accion.setObservacionRevision("Falta señalización");
        accion.setEvidenciaCierre("Primera entrega");
        accion.setArchivoEvidencia("anterior.jpg");
        accion.setNombreEvidencia("anterior.jpg");
        accion.setTipoEvidencia("image/jpeg");
        Trabajador responsable = trabajador(1L);
        responsable.setUsuario(ejecutor);
        accion.setResponsable(responsable);
        when(acciones.findById(5L)).thenReturn(Optional.of(accion));
        when(usuarios.findByUsername("operario")).thenReturn(Optional.of(ejecutor));
        when(trabajadores.findByUsuarioUsername("operario")).thenReturn(Optional.of(responsable));
        when(acciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile nuevo = new MockMultipartFile("adjunto", "nuevo.jpg", "image/jpeg", new byte[]{1});
        when(archivos.guardar(nuevo, true)).thenReturn(
                new ArchivoAdjuntoService.ArchivoGuardado("nuevo.jpg", "nuevo.jpg", "image/jpeg"));

        servicio.entregar(5L, "operario", "Segunda entrega", nuevo);

        ArgumentCaptor<AccionCorrectivaEvento> captor = ArgumentCaptor.forClass(AccionCorrectivaEvento.class);
        verify(eventos, times(3)).save(captor.capture());
        assertEquals("ENTREGA", captor.getAllValues().get(0).getTipo());
        assertEquals("anterior.jpg", captor.getAllValues().get(0).getArchivoEvidencia());
        assertEquals("Primera entrega", captor.getAllValues().get(0).getDetalle());
        assertEquals("DEVOLUCION", captor.getAllValues().get(1).getTipo());
        assertEquals("Falta señalización", captor.getAllValues().get(1).getDetalle());
        assertEquals("nuevo.jpg", captor.getAllValues().get(2).getArchivoEvidencia());
        verify(archivos, never()).borrarDespuesDeConfirmar("anterior.jpg");
    }

    private Usuario usuario(Long id, String username, String rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setRol(rol);
        return usuario;
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

    private Area area(Long id) {
        Area area = new Area();
        area.setId(id);
        return area;
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
