package com.safework.safework.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.safework.safework.repository.AccionCorrectivaRepository;
import com.safework.safework.repository.RiesgoRepository;

/** Migración idempotente: conserva los archivos antiguos como respaldo. */
@Component
public class MigracionAdjuntos implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MigracionAdjuntos.class);
    private final RiesgoRepository riesgos;
    private final AccionCorrectivaRepository acciones;
    private final ArchivoAdjuntoService archivos;

    public MigracionAdjuntos(RiesgoRepository riesgos, AccionCorrectivaRepository acciones,
            ArchivoAdjuntoService archivos) {
        this.riesgos = riesgos;
        this.acciones = acciones;
        this.archivos = archivos;
    }

    @Override
    public void run(ApplicationArguments args) {
        int migrados = 0;
        int faltantes = 0;
        for (var riesgo : riesgos.findAll()) {
            if (riesgo.getFotoArchivo() == null) continue;
            if (archivos.migrarArchivoLocal(riesgo.getFotoArchivo(), riesgo.getFotoNombre(), false)) migrados++;
            else faltantes++;
        }
        for (var accion : acciones.findAll()) {
            if (accion.getArchivoEvidencia() == null) continue;
            if (archivos.migrarArchivoLocal(accion.getArchivoEvidencia(), accion.getNombreEvidencia(), true)) migrados++;
            else faltantes++;
        }
        if (migrados > 0 || faltantes > 0) {
            log.info("Migración de adjuntos: {} referencias disponibles en MySQL, {} archivos antiguos no encontrados",
                    migrados, faltantes);
        }
    }
}
