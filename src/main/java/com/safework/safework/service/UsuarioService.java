package com.safework.safework.service;


import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.safework.safework.model.Usuario;
import com.safework.safework.repository.UsuarioRepository;



@Service
public class UsuarioService {


    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;



    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ){

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;

    }




    // =========================
    // LISTAR USUARIOS , SELECT* FROM USUARIOS , JALADOS DEL JPA DE USUARIO REPOSITORY
    // =========================

    public List<Usuario> listarUsuarios(){

        return usuarioRepository.findAll();

    }





    // =========================
    // BUSCAR POR ID
    // =========================

    public Usuario buscarPorId(Long id){

        return usuarioRepository.findById(id)
                .orElse(null);

    }





    // =========================
    // GUARDAR USUARIO
    // =========================

    public Usuario guardarUsuario(Usuario usuario){


        /*
         * BCrypt cifra la contraseña antes
         * de guardarla en la base de datos.
         *
         * Ejemplo:
         *
         * Antes:
         * 123456
         *
         * Después:
         * $2a$10$8sd87asd7as8d7...
         *
         */


        usuario.setPassword(
                passwordEncoder.encode(
                        usuario.getPassword()
                )
        );


        return usuarioRepository.save(usuario);

    }





    // =========================
    // ELIMINAR USUARIO
    // =========================

    public void eliminarUsuario(Long id){

        usuarioRepository.deleteById(id);

    }


}