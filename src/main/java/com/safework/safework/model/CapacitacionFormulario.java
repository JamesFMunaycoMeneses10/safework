package com.safework.safework.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;

public class CapacitacionFormulario {
    private Long id;
    @NotBlank @Size(max = 150) private String tema;
    @NotBlank @Size(max = 1000) private String objetivo;
    @NotBlank @Size(max = 150) private String instructor;
    @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime fechaHora;
    @NotNull @Min(5) @Max(1440) private Integer duracionMinutos = 60;
    @NotNull private Long areaId;
    @NotEmpty @Size(max = 500) private List<@NotNull Long> trabajadorIds = new ArrayList<>();
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public List<Long> getTrabajadorIds() { return trabajadorIds; }
    public void setTrabajadorIds(List<Long> trabajadorIds) { this.trabajadorIds = trabajadorIds; }
}
