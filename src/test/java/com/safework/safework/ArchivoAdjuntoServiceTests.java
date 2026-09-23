package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.safework.safework.service.ArchivoAdjuntoService;
import com.safework.safework.model.ArchivoAdjunto;
import com.safework.safework.repository.ArchivoAdjuntoRepository;
import org.mockito.ArgumentCaptor;

class ArchivoAdjuntoServiceTests {
    @TempDir Path directorio;

    @Test
    void guardaFotoConNombreInternoYPermiteLeerla() throws Exception {
        ArchivoAdjuntoRepository repositorio = mock(ArchivoAdjuntoRepository.class);
        ArchivoAdjuntoService servicio = new ArchivoAdjuntoService(repositorio, directorio.toString());
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        MockMultipartFile foto = new MockMultipartFile("fotoRiesgo", "../riesgo.png", "image/png", png);
        var guardado = servicio.guardar(foto, false);
        assertEquals("riesgo.png", guardado.nombreOriginal());
        assertTrue(guardado.nombre().endsWith(".png"));
        ArgumentCaptor<ArchivoAdjunto> captura = ArgumentCaptor.forClass(ArchivoAdjunto.class);
        verify(repositorio).save(captura.capture());
        assertEquals(guardado.nombre(), captura.getValue().getNombre());
        when(repositorio.findById(guardado.nombre())).thenReturn(Optional.of(captura.getValue()));
        assertTrue(servicio.abrir(guardado.nombre()).exists());
        servicio.borrarSiExiste(guardado.nombre());
        verify(repositorio).deleteById(guardado.nombre());
        assertTrue(Files.list(directorio).findAny().isEmpty());
    }

    @Test
    void rechazaPdfEnRiesgosYArchivoFingido() {
        ArchivoAdjuntoService servicio = new ArchivoAdjuntoService(mock(ArchivoAdjuntoRepository.class), directorio.toString());
        MockMultipartFile pdf = new MockMultipartFile("fotoRiesgo", "riesgo.pdf", "application/pdf",
                "%PDF-1.7 prueba".getBytes());
        assertThrows(IllegalArgumentException.class, () -> servicio.validar(pdf, false));
        MockMultipartFile fingido = new MockMultipartFile("fotoRiesgo", "foto.png", "image/png",
                "contenido falso".getBytes());
        assertThrows(IllegalArgumentException.class, () -> servicio.validar(fingido, false));
        assertThrows(IllegalArgumentException.class, () -> servicio.abrir("../otro.txt"));
    }

    @Test
    void migraArchivoLocalSinBorrarOriginal() throws Exception {
        ArchivoAdjuntoRepository repositorio = mock(ArchivoAdjuntoRepository.class);
        ArchivoAdjuntoService servicio = new ArchivoAdjuntoService(repositorio, directorio.toString());
        String nombre = "cc539e99-944a-4c14-882e-5f29524f0603.jpg";
        Files.write(directorio.resolve(nombre), new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        assertTrue(servicio.migrarArchivoLocal(nombre, "foto.jpg", false));
        verify(repositorio).save(any(ArchivoAdjunto.class));
        assertTrue(Files.exists(directorio.resolve(nombre)));
    }
}
