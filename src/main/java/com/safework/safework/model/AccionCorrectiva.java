    package com.safework.safework.model;

    import java.time.LocalDate;

    import jakarta.persistence.Column;
    import jakarta.persistence.Entity;
    import jakarta.persistence.GeneratedValue;
    import jakarta.persistence.GenerationType;
    import jakarta.persistence.Id;
    import jakarta.persistence.JoinColumn;
    import jakarta.persistence.ManyToOne;
    import jakarta.persistence.Table;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import jakarta.validation.constraints.Size;
    import org.springframework.format.annotation.DateTimeFormat;

    /**
     * Entidad que representa una acción correctiva
     * generada a partir de un riesgo o incidente.
     */
    @Entity
    @Table(name = "acciones_correctivas")
    public class AccionCorrectiva {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /**
         * Descripción de la acción que debe realizarse.
         */
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        @Column(nullable = false, length = 255)
        private String descripcion;

        /**
         * Fecha en la que se registra la acción.
         */
        @NotNull(message = "La fecha de registro es obligatoria")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Column(nullable = false)
        private LocalDate fechaRegistro;

        /**
         * Fecha límite para completar la acción.
         */
        @NotNull(message = "La fecha límite es obligatoria")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Column(nullable = false)
        private LocalDate fechaLimite;

        /**
         * Estado actual de la acción.
         */
        @NotBlank(message = "El estado es obligatorio")
        @Column(nullable = false)
        private String estado;

        /**
         * Nivel de prioridad.
         */
        @NotBlank(message = "La prioridad es obligatoria")
        @Column(nullable = false)
        private String prioridad;

        /**
         * Trabajador responsable de ejecutar la acción.
         */
        @ManyToOne
        @JoinColumn(name = "trabajador_id")
        private Trabajador responsable;

        /**
         * Riesgo asociado.
         */
        @ManyToOne
        @JoinColumn(name = "riesgo_id")
        private Riesgo riesgo;

        /**
         * Incidente relacionado.
         */
        @ManyToOne
        @JoinColumn(name = "incidente_id")
        private Incidente incidente;

        /** Evidencia textual que respalda el cierre de la acción. */
        @Size(max = 1000, message = "La evidencia no puede superar los 1000 caracteres")
        @Column(name = "evidencia_cierre", length = 1000)
        private String evidenciaCierre;

        /** Fecha asignada por el servidor al completar la acción. */
        @Column(name = "fecha_cierre")
        private LocalDate fechaCierre;

        @Column(name = "archivo_evidencia", length = 40)
        private String archivoEvidencia;
        @Column(name = "nombre_evidencia", length = 255)
        private String nombreEvidencia;
        @Column(name = "tipo_evidencia", length = 50)
        private String tipoEvidencia;

        public AccionCorrectiva() {
        }

        public AccionCorrectiva(
                String descripcion,
                LocalDate fechaRegistro,
                LocalDate fechaLimite,
                String estado,
                String prioridad,
                Trabajador responsable,
                Riesgo riesgo,
                Incidente incidente) {

            this.descripcion = descripcion;
            this.fechaRegistro = fechaRegistro;
            this.fechaLimite = fechaLimite;
            this.estado = estado;
            this.prioridad = prioridad;
            this.responsable = responsable;
            this.riesgo = riesgo;
            this.incidente = incidente;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public LocalDate getFechaRegistro() {
            return fechaRegistro;
        }

        public void setFechaRegistro(LocalDate fechaRegistro) {
            this.fechaRegistro = fechaRegistro;
        }

        public LocalDate getFechaLimite() {
            return fechaLimite;
        }

        public void setFechaLimite(LocalDate fechaLimite) {
            this.fechaLimite = fechaLimite;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public String getPrioridad() {
            return prioridad;
        }

        public void setPrioridad(String prioridad) {
            this.prioridad = prioridad;
        }

        public Trabajador getResponsable() {
            return responsable;
        }

        public void setResponsable(Trabajador responsable) {
            this.responsable = responsable;
        }

        public Riesgo getRiesgo() {
            return riesgo;
        }

        public void setRiesgo(Riesgo riesgo) {
            this.riesgo = riesgo;
        }

        public Incidente getIncidente() {
            return incidente;
        }

        public void setIncidente(Incidente incidente) {
            this.incidente = incidente;
        }

        public String getEvidenciaCierre() {
            return evidenciaCierre;
        }

        public void setEvidenciaCierre(String evidenciaCierre) {
            this.evidenciaCierre = evidenciaCierre;
        }

        public LocalDate getFechaCierre() {
            return fechaCierre;
        }

        public void setFechaCierre(LocalDate fechaCierre) {
            this.fechaCierre = fechaCierre;
        }

        public String getArchivoEvidencia() { return archivoEvidencia; }
        public void setArchivoEvidencia(String valor) { this.archivoEvidencia = valor; }
        public String getNombreEvidencia() { return nombreEvidencia; }
        public void setNombreEvidencia(String valor) { this.nombreEvidencia = valor; }
        public String getTipoEvidencia() { return tipoEvidencia; }
        public void setTipoEvidencia(String valor) { this.tipoEvidencia = valor; }

    }
