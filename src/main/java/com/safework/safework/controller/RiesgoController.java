package com.safework.safework.controller;


import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.safework.safework.model.Riesgo;
import com.safework.safework.service.AreaService;
import com.safework.safework.service.ArchivoAdjuntoService;
import com.safework.safework.service.RiesgoService;

import jakarta.validation.Valid;


/**
 * Controller encargado de gestionar
 * el módulo de riesgos.
 */
@Controller
@RequestMapping("/riesgos")
public class RiesgoController {



    private final RiesgoService riesgoService;

    private final AreaService areaService;
    private final ArchivoAdjuntoService archivos;



    public RiesgoController(
            RiesgoService riesgoService,
            AreaService areaService, ArchivoAdjuntoService archivos) {

        this.riesgoService = riesgoService;
        this.areaService = areaService;
        this.archivos = archivos;
    }




    /**
     * Lista todos los riesgos.
     */
    @GetMapping
    public String listarRiesgos(@RequestParam(required = false) String nivel, Model model) {

        if (nivel != null && !"criticos".equals(nivel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro de riesgos no válido");
        }


        model.addAttribute(
                "riesgos",
                "criticos".equals(nivel)
                        ? riesgoService.listarTodos().stream()
                                .filter(r -> r.getNivelRiesgo() != null && r.getNivelRiesgo() > 16)
                                .toList()
                        : riesgoService.listarTodos()
        );

        model.addAttribute("soloCriticos", "criticos".equals(nivel));


        return "riesgos/lista";
    }





    /**
     * Formulario nuevo riesgo.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {


        model.addAttribute(
                "riesgo",
                new Riesgo()
        );


        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );


        return "riesgos/formulario";
    }






    /**
     * Guarda o actualiza un riesgo.
     */
    @PostMapping("/guardar")
    public String guardarRiesgo(
            @Valid @ModelAttribute("riesgo") Riesgo riesgo,
            BindingResult resultado,
            @RequestParam(name = "fotoRiesgo", required = false) MultipartFile foto,
            Model model, Authentication authentication) {



        if(resultado.hasErrors()){


            model.addAttribute(
                    "areas",
                    areaService.listarTodas()
            );


            return "riesgos/formulario";
        }



        try {
            riesgoService.guardar(riesgo, foto, authentication.getName());
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("areas", areaService.listarTodas());
            return "riesgos/formulario";
        }



        return "redirect:/riesgos";
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> verFoto(@PathVariable Long id) {
        Riesgo riesgo = riesgoService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (riesgo.getFotoArchivo() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            return ResponseEntity.ok().header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.parseMediaType(riesgo.getFotoTipo()))
                    .body(archivos.abrir(riesgo.getFotoArchivo()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }





    /**
     * Editar riesgo.
     */
    @GetMapping("/editar/{id}")
    public String editarRiesgo(
            @PathVariable Long id,
            Model model) {


        Riesgo riesgo = riesgoService.buscarPorId(id)

                .orElseThrow(() ->
                        new RuntimeException(
                                "Riesgo no encontrado"));



        model.addAttribute(
                "riesgo",
                riesgo
        );



        model.addAttribute(
                "areas",
                areaService.listarTodas()
        );



        return "riesgos/formulario";
    }






    /**
     * Eliminar riesgo.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminarRiesgo(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {


        try {


            riesgoService.eliminarPorId(id, authentication.getName());



            redirectAttributes.addFlashAttribute(
                    "success",
                    "Riesgo eliminado correctamente"
            );



        } catch(RuntimeException e){



            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );


        }



        return "redirect:/riesgos";
    }


}
