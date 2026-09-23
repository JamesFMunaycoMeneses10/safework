package com.safework.safework.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.ArchivoAdjuntoService;
import com.safework.safework.service.IncidenteService;
import com.safework.safework.service.RiesgoService;
import com.safework.safework.service.TrabajadorService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/acciones")
public class AccionCorrectivaController {

    private final AccionCorrectivaService accionService;
    private final TrabajadorService trabajadorService;
    private final RiesgoService riesgoService;
    private final IncidenteService incidenteService;
    private final ArchivoAdjuntoService archivos;

    public AccionCorrectivaController(AccionCorrectivaService accionService,
            TrabajadorService trabajadorService, RiesgoService riesgoService,
            IncidenteService incidenteService, ArchivoAdjuntoService archivos) {
        this.accionService = accionService;
        this.trabajadorService = trabajadorService;
        this.riesgoService = riesgoService;
        this.incidenteService = incidenteService;
        this.archivos = archivos;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long riesgoId,
            @RequestParam(required = false) Long incidenteId, Model model) {
        List<AccionCorrectiva> acciones = accionService.listarTodas();
        if (riesgoId != null) {
            acciones = acciones.stream()
                    .filter(a -> a.getRiesgo() != null && riesgoId.equals(a.getRiesgo().getId()))
                    .toList();
            model.addAttribute("riesgoFiltrado", riesgoService.buscarPorId(riesgoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riesgo no encontrado")));
        }
        if (incidenteId != null) {
            acciones = acciones.stream()
                    .filter(a -> a.getIncidente() != null && incidenteId.equals(a.getIncidente().getId()))
                    .toList();
            model.addAttribute("incidenteFiltrado", incidenteService.buscarPorId(incidenteId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente no encontrado")));
        }
        model.addAttribute("acciones", acciones);
        return "acciones/lista";
    }

    @GetMapping("/nueva")
    public String nuevaAccion(@RequestParam(required = false) Long riesgoId,
            @RequestParam(required = false) Long incidenteId, Model model) {
        AccionCorrectiva accion = new AccionCorrectiva();
        accion.setFechaRegistro(LocalDate.now());
        accion.setEstado("Pendiente");
        if (riesgoId != null) {
            accion.setRiesgo(riesgoService.buscarPorId(riesgoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riesgo no encontrado")));
        }
        if (incidenteId != null) {
            accion.setIncidente(incidenteService.buscarPorId(incidenteId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente no encontrado")));
        }
        cargarCombos(model);
        model.addAttribute("accion", accion);
        return "acciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("accion") AccionCorrectiva accion,
            BindingResult result, @RequestParam(name = "adjuntoEvidencia", required = false) MultipartFile archivo,
            Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            cargarCombos(model);
            return "acciones/formulario";
        }
        try {
            accionService.guardar(accion, archivo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model);
            return "acciones/formulario";
        }
        redirectAttributes.addFlashAttribute("mensaje", "Acción correctiva guardada correctamente");
        return "redirect:/acciones";
    }

    @GetMapping("/{id}/evidencia")
    public ResponseEntity<Resource> verEvidencia(@PathVariable Long id) {
        AccionCorrectiva accion = accionService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (accion.getArchivoEvidencia() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            Resource recurso = archivos.abrir(accion.getArchivoEvidencia());
            String nombre = accion.getNombreEvidencia() == null ? "evidencia" : accion.getNombreEvidencia();
            return ResponseEntity.ok().header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre.replaceAll("[^a-zA-Z0-9._-]", "_") + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.parseMediaType(accion.getTipoEvidencia())).body(recurso);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        AccionCorrectiva accion = accionService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acción no encontrada"));
        cargarCombos(model);
        model.addAttribute("accion", accion);
        return "acciones/formulario";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accionService.eliminarPorId(id);
            redirectAttributes.addFlashAttribute("mensaje", "Acción correctiva eliminada correctamente");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/acciones";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("trabajadores", trabajadorService.listarTodos());
        model.addAttribute("riesgos", riesgoService.listarTodos());
        model.addAttribute("incidentes", incidenteService.listarTodos());
    }
}
