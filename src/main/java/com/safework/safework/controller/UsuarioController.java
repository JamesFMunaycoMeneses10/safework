package com.safework.safework.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.safework.safework.model.Usuario;
import com.safework.safework.service.UsuarioService;



@Controller
@RequestMapping("/usuarios")
public class UsuarioController {


    private final UsuarioService usuarioService;



    public UsuarioController(
            UsuarioService usuarioService
    ){

        this.usuarioService = usuarioService;

    }



    // =========================
    // LISTAR USUARIOS
    // =========================

    @GetMapping
    public String listarUsuarios(Model model){


        model.addAttribute(
                "usuarios",
                usuarioService.listarUsuarios()
        );


        return "usuarios/index";

    }




    // =========================
    // FORMULARIO CREAR
    // =========================

    @GetMapping("/nuevo")
    public String nuevoUsuario(Model model){


        model.addAttribute(
                "usuario",
                new Usuario()
        );


        return "usuarios/crear";

    }




    // =========================
    // GUARDAR USUARIO
    // =========================

    @PostMapping("/guardar")
    public String guardarUsuario(
            @ModelAttribute Usuario usuario
    ){


        usuarioService.guardarUsuario(usuario);


        return "redirect:/usuarios";

    }




    // =========================
    // ELIMINAR USUARIO
    // =========================

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(
            @PathVariable Long id
    ){


        usuarioService.eliminarUsuario(id);


        return "redirect:/usuarios";

    }


}