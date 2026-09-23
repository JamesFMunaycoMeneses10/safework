package com.safework.safework.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** Almacena adjuntos fuera de /static y nunca usa el nombre enviado por el usuario como ruta. */
@Service
public class ArchivoAdjuntoService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Pattern NOMBRE_SEGURO = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|pdf)");
    private final Path directorio;

    public ArchivoAdjuntoService(@Value("${safework.archivos.directorio}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
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
            Files.createDirectories(directorio);
            String nombre = UUID.randomUUID() + "." + extension;
            Files.write(rutaSegura(nombre), contenido, StandardOpenOption.CREATE_NEW);
            String original = archivo.getOriginalFilename();
            if (original == null || original.isBlank()) original = "archivo." + extension;
            original = original.replace('\\', '/');
            original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
            if (original.length() > 255) original = original.substring(0, 255);
            return new ArchivoGuardado(nombre, original, tipo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo adjunto", e);
        }
    }

    public Resource abrir(String nombre) {
        Path ruta = rutaSegura(nombre);
        if (!Files.isRegularFile(ruta)) {
            throw new IllegalArgumentException("El archivo adjunto no está disponible");
        }
        return new FileSystemResource(ruta);
    }

    public void borrarDespuesDeConfirmar(String nombre) {
        if (nombre == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { borrarSiExiste(nombre); }
            });
        } else {
            borrarSiExiste(nombre);
        }
    }

    public void borrarSiHayRollback(String nombre) {
        if (nombre == null || !TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) borrarSiExiste(nombre);
            }
        });
    }

    public void borrarSiExiste(String nombre) {
        if (nombre == null) return;
        try {
            Files.deleteIfExists(rutaSegura(nombre));
        } catch (IOException e) {
            // El archivo queda para limpieza administrativa; no se elimina el registro de BD.
        }
    }

    private Path rutaSegura(String nombre) {
        if (nombre == null || !NOMBRE_SEGURO.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        Path ruta = directorio.resolve(nombre).normalize();
        if (!ruta.startsWith(directorio)) throw new IllegalArgumentException("Ruta de archivo inválida");
        return ruta;
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
