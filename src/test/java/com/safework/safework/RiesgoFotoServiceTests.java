package com.safework.safework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.safework.safework.model.Riesgo;
import com.safework.safework.repository.HallazgoInspeccionRepository;
import com.safework.safework.repository.RiesgoRepository;
import com.safework.safework.service.ArchivoAdjuntoService;
import com.safework.safework.service.RiesgoService;

class RiesgoFotoServiceTests {
    @Test
    void alReemplazarFotoBorraLaAnteriorYConservaLaNueva() {
        RiesgoRepository repositorio = mock(RiesgoRepository.class);
        ArchivoAdjuntoService archivos = mock(ArchivoAdjuntoService.class);
        RiesgoService servicio = new RiesgoService(repositorio, mock(HallazgoInspeccionRepository.class), archivos);
        Riesgo anterior = new Riesgo();
        anterior.setId(7L);
        anterior.setFotoArchivo("foto-vieja.jpg");
        when(repositorio.findById(7L)).thenReturn(Optional.of(anterior));
        MockMultipartFile foto = new MockMultipartFile("fotoRiesgo", "nueva.jpg", "image/jpeg", new byte[]{1});
        when(archivos.guardar(foto, false)).thenReturn(
                new ArchivoAdjuntoService.ArchivoGuardado("foto-nueva.jpg", "nueva.jpg", "image/jpeg"));
        when(repositorio.save(any(Riesgo.class))).thenAnswer(invocacion -> {
            Riesgo recibida = invocacion.getArgument(0);
            // Simula el merge de JPA, que modifica la entidad administrada.
            anterior.setFotoArchivo(recibida.getFotoArchivo());
            return anterior;
        });
        Riesgo datos = new Riesgo();
        datos.setId(7L);

        Riesgo guardado = servicio.guardar(datos, foto);

        assertEquals("foto-nueva.jpg", guardado.getFotoArchivo());
        verify(archivos).borrarDespuesDeConfirmar("foto-vieja.jpg");
    }
}
