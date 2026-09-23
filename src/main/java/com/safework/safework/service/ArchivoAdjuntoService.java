package com.safework.safework.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.safework.safework.model.ArchivoAdjunto;
import com.safework.safework.repository.ArchivoAdjuntoRepository;

/** Guarda los adjuntos en MySQL; solo lee la antigua carpeta durante la migración. */
@Service
public class ArchivoAdjuntoService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Pattern NOMBRE_SEGURO = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|pdf)");
    private final Path directorioAnterior;
    private final ArchivoAdjuntoRepository repositorio;

    public ArchivoAdjuntoService(ArchivoAdjuntoRepository repositorio,
            @Value("${safework.archivos.directorio}") String directorioAnterior) {
        this.repositorio = repositorio;
        this.directorioAnterior = Path.of(directorioAnterior).toAbsolutePath().normalize();
    }

    public record ArchivoGuardado(String nombre, String nombreOriginal, String tipoContenido) {}

    public void validar(MultipartFile archivo, boolean permitirPdf) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Seleccione un archivo no vacío");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo no puede superar los 5 MB");
        }
        try {
            detectarTipo(archivo.getBytes(), permitirPdf);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo", e);
        }
    }

    @Transactional
    public ArchivoGuardado guardar(MultipartFile archivo, boolean permitirPdf) {
        validar(archivo, permitirPdf);
        try {
            byte[] contenido = archivo.getBytes();
            String tipo = detectarTipo(contenido, permitirPdf);
            String extension = switch (tipo) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                default -> "pdf";
            };
            String nombre = UUID.randomUUID() + "." + extension;
            String original = archivo.getOriginalFilename();
            if (original == null || original.isBlank()) original = "archivo." + extension;
            original = original.replace('\\', '/');
            original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
            if (original.length() > 255) original = original.substring(0, 255);
            repositorio.save(new ArchivoAdjunto(nombre, original, tipo, contenido));
            return new ArchivoGuardado(nombre, original, tipo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo adjunto", e);
        }
    }

    public Resource abrir(String nombre) {
        validarNombre(nombre);
        ArchivoAdjunto adjunto = repositorio.findById(nombre)
                .orElseThrow(() -> new IllegalArgumentException("El archivo adjunto no está disponible"));
        return new ByteArrayResource(adjunto.getContenido());
    }

    /** La llamada se realiza dentro de la transacción del registro padre. */
    @Transactional
    public void borrarDespuesDeConfirmar(String nombre) {
        borrarSiExiste(nombre);
    }

    @Transactional
    public void borrarSiExiste(String nombre) {
        if (nombre == null) return;
        validarNombre(nombre);
        repositorio.deleteById(nombre);
    }

    /** Copia un archivo del almacenamiento anterior a MySQL sin borrar el original. */
    @Transactional
    public boolean migrarArchivoLocal(String nombre, String nombreOriginal, boolean permitirPdf) {
        validarNombre(nombre);
        if (repositorio.existsById(nombre)) return true;
        Path ruta = directorioAnterior.resolve(nombre).normalize();
        if (!ruta.startsWith(directorioAnterior) || !Files.isRegularFile(ruta)) return false;
        try {
            if (Files.size(ruta) > MAX_BYTES) return false;
            byte[] contenido = Files.readAllBytes(ruta);
            String tipo = detectarTipo(contenido, permitirPdf);
            String original = nombreOriginal == null || nombreOriginal.isBlank() ? nombre : nombreOriginal;
            repositorio.save(new ArchivoAdjunto(nombre, original, tipo, contenido));
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_SEGURO.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
    }

    private String detectarTipo(byte[] bytes, boolean permitirPdf) {
        if (bytes.length < 12) throw new IllegalArgumentException("Archivo inválido o demasiado pequeño");
        if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff)
            return "image/jpeg";
        if ((bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a)
            return "image/png";
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P')
            return "image/webp";
        if (permitirPdf && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D'
                && bytes[3] == 'F' && bytes[4] == '-') return "application/pdf";
        throw new IllegalArgumentException(permitirPdf
                ? "Adjunte una imagen JPG, PNG, WebP o un PDF válido"
                : "Adjunte una foto JPG, PNG o WebP válida");
    }
}
