package com.safework.safework.controller;

import java.time.LocalDate;
import java.io.IOException;
import org.springframework.format.annotation.DateTimeFormat;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
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
import com.safework.safework.model.AccionCorrectivaEvento;
import com.safework.safework.service.AccionCorrectivaService;
import com.safework.safework.service.ArchivoAdjuntoService;
import com.safework.safework.service.BandejaAcciones;
import com.safework.safework.service.IncidenteService;
import com.safework.safework.service.RiesgoService;
import com.safework.safework.service.ReporteAccionesCsv;
import com.safework.safework.service.ReporteAccionesExcel;
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
            @RequestParam(required = false) Long incidenteId,
            @RequestParam(required = false) String vista,
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina,
            Authentication authentication, Model model) {
        if (pagina < 0 || buscar.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro de acciones no válido");
        }
        String vistaActiva;
        try {
            vistaActiva = BandejaAcciones.validarVista(vista);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (riesgoId != null) {
            model.addAttribute("riesgoFiltrado", riesgoService.buscarPorId(riesgoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riesgo no encontrado")));
        }
        if (incidenteId != null) {
            model.addAttribute("incidenteFiltrado", incidenteService.buscarPorId(incidenteId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente no encontrado")));
        }
        LocalDate hoy = LocalDate.now();
        boolean gestionaSst = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_SUPERVISOR".equals(a.getAuthority()));
        String usuarioActual = authentication.getName();
        List<AccionCorrectiva> avisos = accionService.proximas(riesgoId, incidenteId, hoy).stream()
                .filter(a -> gestionaSst || (a.getResponsable() != null
                        && a.getResponsable().getUsuario() != null
                        && usuarioActual.equals(a.getResponsable().getUsuario().getUsername())))
                .toList();
        model.addAttribute("avisosProximos", avisos);
        model.addAttribute("avisosProximosPrimeros", avisos.stream().limit(3).toList());
        model.addAttribute("resumen", accionService.resumen(riesgoId, incidenteId, hoy));
        model.addAttribute("vistaActiva", vistaActiva);
        model.addAttribute("riesgoId", riesgoId);
        model.addAttribute("incidenteId", incidenteId);
        var resultados = accionService.buscar(riesgoId, incidenteId, buscar, vistaActiva, hoy, pagina);
        model.addAttribute("acciones", resultados.getContent());
        model.addAttribute("resultados", resultados);
        model.addAttribute("buscar", buscar);
        model.addAttribute("accionesVencidasIds", resultados.getContent().stream()
                .filter(a -> BandejaAcciones.vencida(a, hoy)).map(AccionCorrectiva::getId).toList());
        model.addAttribute("accionesProximasIds", resultados.getContent().stream()
                .filter(a -> BandejaAcciones.proximaVencer(a, hoy)).map(AccionCorrectiva::getId).toList());
        model.addAttribute("usuarioActual", usuarioActual);
        return "acciones/lista";
    }

    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        AccionCorrectiva accion = accionService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acción no encontrada"));
        LocalDate hoy = LocalDate.now();
        model.addAttribute("accion", accion);
        model.addAttribute("vencida", BandejaAcciones.vencida(accion, hoy));
        model.addAttribute("proximaVencer", BandejaAcciones.proximaVencer(accion, hoy));
        return "acciones/detalle";
    }

    @GetMapping(value = "/reporte.csv", produces = "text/csv")
    public ResponseEntity<byte[]> descargarReporte(
            @RequestParam(required = false) Long riesgoId,
            @RequestParam(required = false) Long incidenteId,
            @RequestParam(required = false) String vista,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        LocalDate hoy = LocalDate.now();
        byte[] archivo = ReporteAccionesCsv.generar(
                accionesDelReporte(riesgoId, incidenteId, vista, fechaDesde, fechaHasta, hoy), hoy);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=acciones-correctivas-" + hoy + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(archivo);
    }

    @GetMapping(value = "/reporte.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam(required = false) Long riesgoId,
            @RequestParam(required = false) Long incidenteId,
            @RequestParam(required = false) String vista,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta)
            throws IOException {
        LocalDate hoy = LocalDate.now();
        byte[] archivo = ReporteAccionesExcel.generar(
                accionesDelReporte(riesgoId, incidenteId, vista, fechaDesde, fechaHasta, hoy), hoy);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=acciones-correctivas-" + hoy + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }

    private List<AccionCorrectiva> accionesDelReporte(Long riesgoId, Long incidenteId, String vista,
            LocalDate fechaDesde, LocalDate fechaHasta, LocalDate hoy) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango de fechas no es válido");
        }
        String vistaActiva;
        try {
            vistaActiva = BandejaAcciones.validarVista(vista);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (riesgoId != null && riesgoService.buscarPorId(riesgoId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Riesgo no encontrado");
        }
        if (incidenteId != null && incidenteService.buscarPorId(incidenteId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente no encontrado");
        }
        List<AccionCorrectiva> acciones = accionService.listarTodas().stream()
                .filter(a -> riesgoId == null || a.getRiesgo() != null && riesgoId.equals(a.getRiesgo().getId()))
                .filter(a -> incidenteId == null || a.getIncidente() != null && incidenteId.equals(a.getIncidente().getId()))
                .filter(a -> fechaDesde == null || a.getFechaRegistro() != null
                        && !a.getFechaRegistro().isBefore(fechaDesde))
                .filter(a -> fechaHasta == null || a.getFechaRegistro() != null
                        && !a.getFechaRegistro().isAfter(fechaHasta))
                .toList();
        return BandejaAcciones.filtrar(acciones, vistaActiva, hoy);
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
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (result.hasErrors()) {
            cargarCombos(model);
            return "acciones/formulario";
        }
        try {
            accionService.guardar(accion, archivo, authentication.getName());
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

    @GetMapping("/{id}/historial")
    public String historial(@PathVariable Long id, Model model) {
        AccionCorrectiva accion = accionService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("accion", accion);
        model.addAttribute("eventos", accionService.historial(id));
        return "acciones/historial";
    }

    @GetMapping("/{id}/historial/{eventoId}/archivo")
    public ResponseEntity<Resource> descargarArchivoHistorico(@PathVariable Long id,
            @PathVariable Long eventoId) {
        AccionCorrectivaEvento evento = accionService.buscarEvento(id, eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (evento.getArchivoEvidencia() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            Resource recurso = archivos.abrir(evento.getArchivoEvidencia());
            String nombre = evento.getNombreEvidencia() == null ? "evidencia" : evento.getNombreEvidencia();
            return ResponseEntity.ok().header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + nombre.replaceAll("[^a-zA-Z0-9._-]", "_") + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.parseMediaType(evento.getTipoEvidencia()))
                    .body(recurso);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        AccionCorrectiva accion = accionService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acción no encontrada"));
        if (!"Pendiente".equals(accion.getEstado()) && !"En proceso".equals(accion.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta acción ya está en el flujo de revisión");
        }
        cargarCombos(model);
        model.addAttribute("accion", accion);
        return "acciones/formulario";
    }

    @GetMapping("/{id}/entregar")
    public String formularioEntrega(@PathVariable Long id, Principal principal, Model model) {
        AccionCorrectiva accion = accionService.buscarPorId(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (accion.getResponsable() == null || accion.getResponsable().getUsuario() == null
                || !principal.getName().equals(accion.getResponsable().getUsuario().getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!List.of("Pendiente", "En proceso", "Devuelta").contains(accion.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La acción no admite una nueva entrega");
        }
        model.addAttribute("accion", accion);
        return "acciones/entregar";
    }

    @PostMapping("/{id}/entregar")
    public String entregar(@PathVariable Long id, Principal principal,
            @RequestParam String evidencia, @RequestParam(required = false) MultipartFile adjunto,
            RedirectAttributes redirectAttributes) {
        try {
            accionService.entregar(id, principal.getName(), evidencia, adjunto);
            redirectAttributes.addFlashAttribute("mensaje", "Evidencia enviada para revisión");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/acciones/" + id + "/entregar";
        }
        return "redirect:/acciones";
    }

    @GetMapping("/{id}/revisar")
    public String formularioRevision(@PathVariable Long id, Model model) {
        AccionCorrectiva accion = accionService.buscarPorId(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"En revisión".equals(accion.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La acción no está pendiente de revisión");
        }
        model.addAttribute("accion", accion);
        return "acciones/revisar";
    }

    @PostMapping("/{id}/revisar")
    public String revisar(@PathVariable Long id, Principal principal,
            @RequestParam String decision, @RequestParam(required = false) String observacion,
            RedirectAttributes redirectAttributes) {
        if (!"aprobar".equals(decision) && !"devolver".equals(decision)) {
            redirectAttributes.addFlashAttribute("error", "Seleccione una decisión válida");
            return "redirect:/acciones/" + id + "/revisar";
        }
        try {
            accionService.revisar(id, principal.getName(), "aprobar".equals(decision), observacion);
            redirectAttributes.addFlashAttribute("mensaje", "Revisión registrada correctamente");
            return "redirect:/acciones";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/acciones/" + id + "/revisar";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            accionService.eliminarPorId(id, authentication.getName());
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
