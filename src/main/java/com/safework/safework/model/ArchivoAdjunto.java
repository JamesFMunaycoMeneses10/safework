package com.safework.safework.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** Contenido binario de una foto o evidencia, separado de las tablas de listado. */
@Entity
@Table(name = "archivos_adjuntos")
public class ArchivoAdjunto {
    @Id
    @Column(length = 40)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String nombreOriginal;

    @Column(nullable = false, length = 50)
    private String tipoContenido;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] contenido;

    protected ArchivoAdjunto() {}

    public ArchivoAdjunto(String nombre, String nombreOriginal, String tipoContenido, byte[] contenido) {
        this.nombre = nombre;
        this.nombreOriginal = nombreOriginal;
        this.tipoContenido = tipoContenido;
        this.contenido = contenido;
    }

    public String getNombre() { return nombre; }
    public String getNombreOriginal() { return nombreOriginal; }
    public String getTipoContenido() { return tipoContenido; }
    public byte[] getContenido() { return contenido; }
}
