package com.safework.safework.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.safework.safework.model.Inspeccion;
import com.safework.safework.model.Riesgo;
import com.safework.safework.model.HallazgoInspeccion;
import com.safework.safework.repository.HallazgoInspeccionRepository;
import com.safework.safework.service.InspeccionService;
import com.safework.safework.service.RiesgoService;

@Controller
public class HallazgoInspeccionController {

    private final HallazgoInspeccionRepository hallazgoRepository;
    private final InspeccionService inspeccionService;
    private final RiesgoService riesgoService;

    public HallazgoInspeccionController(
            HallazgoInspeccionRepository hallazgoRepository,
            InspeccionService inspeccionService,
            RiesgoService riesgoService) {
        this.hallazgoRepository = hallazgoRepository;
        this.inspeccionService = inspeccionService;
        this.riesgoService = riesgoService;
    }

    @GetMapping("/inspecciones/{id}/hallazgos")
    public String listar(@PathVariable Long id, Model model) {
        Inspeccion inspeccion = inspeccionService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inspección no encontrada"));

        List<Riesgo> riesgosDelArea = riesgoService.listarTodos().stream()
                .filter(r -> r.getArea() != null
                        && r.getArea().getId().equals(inspeccion.getArea().getId()))
                .toList();

        model.addAttribute("inspeccion", inspeccion);
        model.addAttribute("hallazgos",
                hallazgoRepository.findByInspeccionIdOrderByIdAsc(id));
        model.addAttribute("riesgos", riesgosDelArea);
        return "inspecciones/hallazgos";
    }

    @PostMapping("/inspecciones/{id}/hallazgos")
    public String guardar(
            @PathVariable Long id,
            @RequestParam String descripcion,
            @RequestParam(required = false) Long riesgoId) {

        Inspeccion inspeccion = inspeccionService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inspección no encontrada"));

        String texto = descripcion == null ? "" : descripcion.trim();
        if (texto.isEmpty() || texto.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La descripción debe tener entre 1 y 500 caracteres");
        }

        Riesgo riesgo = null;
        if (riesgoId != null) {
            riesgo = riesgoService.buscarPorId(riesgoId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Riesgo no encontrado"));

            if (riesgo.getArea() == null
                    || !riesgo.getArea().getId().equals(inspeccion.getArea().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El riesgo debe pertenecer al área de la inspección");
            }
        }

        HallazgoInspeccion hallazgo = new HallazgoInspeccion();
        hallazgo.setDescripcion(texto);
        hallazgo.setInspeccion(inspeccion);
        hallazgo.setRiesgo(riesgo);
        hallazgoRepository.save(hallazgo);

        return "redirect:/inspecciones/" + id + "/hallazgos";
    }
}