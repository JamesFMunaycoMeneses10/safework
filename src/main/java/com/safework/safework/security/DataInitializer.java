package com.safework.safework.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.safework.safework.model.Usuario;
import com.safework.safework.repository.UsuarioRepository;

/**
 * Clase encargada de crear datos iniciales
 * cuando la aplicación inicia.
 *
 * En este caso crea el primer usuario
 * administrador del sistema.
 */
@Configuration
public class DataInitializer {

    /**
     * Este método se ejecuta automáticamente
     * al iniciar Spring Boot.
     *
     * CommandLineRunner permite ejecutar
     * código después de levantar la aplicación.
     */
    @Bean
    CommandLineRunner initDatabase(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            /*
             * Verificamos si ya existe
             * el usuario administrador.
             *
             * Esto evita crear duplicados
             * cada vez que reiniciamos.
             */
            if (usuarioRepository.findByUsername("admin")
                    .isEmpty()) {

                Usuario usuario = new Usuario();

                /*
                 * Nombre de usuario
                 */
                usuario.setUsername("admin");

                /*
                 * La contraseña NO se guarda directamente.
                 *
                 * BCrypt transforma:
                 *
                 * 123456
                 *
                 * en un hash seguro.
                 */
                usuario.setPassword(
                        passwordEncoder.encode("123456"));

                /*
                 * Rol del usuario.
                 *
                 * Spring Security agregará
                 * automáticamente ROLE_
                 *
                 * por eso aquí colocamos:
                 *
                 * ADMIN
                 */
                usuario.setRol("ADMIN");

                usuarioRepository.save(usuario);

                System.out.println(
                        "Usuario administrador creado correctamente");

            }

        };

    }

}