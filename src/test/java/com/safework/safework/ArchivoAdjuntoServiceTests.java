package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.safework.safework.service.ArchivoAdjuntoService;

class ArchivoAdjuntoServiceTests {
    @TempDir Path directorio;

    @Test
    void guardaFotoConNombreInternoYPermiteLeerla() throws Exception {
        ArchivoAdjuntoService servicio = new ArchivoAdjuntoService(directorio.toString());
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        MockMultipartFile foto = new MockMultipartFile("fotoRiesgo", "../riesgo.png", "image/png", png);
        var guardado = servicio.guardar(foto, false);
        assertEquals("riesgo.png", guardado.nombreOriginal());
        assertTrue(guardado.nombre().endsWith(".png"));
        assertTrue(servicio.abrir(guardado.nombre()).exists());
        servicio.borrarSiExiste(guardado.nombre());
        assertTrue(Files.list(directorio).findAny().isEmpty());
    }

    @Test
    void rechazaPdfEnRiesgosYArchivoFingido() {
        ArchivoAdjuntoService servicio = new ArchivoAdjuntoService(directorio.toString());
        MockMultipartFile pdf = new MockMultipartFile("fotoRiesgo", "riesgo.pdf", "application/pdf",
                "%PDF-1.7 prueba".getBytes());
        assertThrows(IllegalArgumentException.class, () -> servicio.validar(pdf, false));
        MockMultipartFile fingido = new MockMultipartFile("fotoRiesgo", "foto.png", "image/png",
                "contenido falso".getBytes());
        assertThrows(IllegalArgumentException.class, () -> servicio.validar(fingido, false));
        assertThrows(IllegalArgumentException.class, () -> servicio.abrir("../otro.txt"));
    }
}
