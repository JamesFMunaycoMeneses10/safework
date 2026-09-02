package com.safework.safework.security;


// Permite crear componentes administrados por Spring
import org.springframework.context.annotation.Bean;

// Indica que esta clase contiene configuraciones
import org.springframework.context.annotation.Configuration;

// Permite configurar las reglas de seguridad HTTP
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

// Representa la cadena de filtros de seguridad de Spring
import org.springframework.security.web.SecurityFilterChain;


// Permite cifrar contraseñas usando BCrypt
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Interfaz utilizada para manejar codificación de contraseñas
import org.springframework.security.crypto.password.PasswordEncoder;



/**
 * Configuración principal de Spring Security.
 *
 * Esta clase controla:
 *
 * - Autenticación de usuarios.
 * - Protección de rutas.
 * - Login.
 * - Logout.
 * - Cifrado de contraseñas.
 *
 */
@Configuration
public class SecurityConfig {



    /**
     * Bean encargado de cifrar contraseñas.
     *
     * IMPORTANTE:
     *
     * Nunca se deben guardar contraseñas
     * directamente en la base de datos.
     *
     * Ejemplo incorrecto:
     *
     * password = 123456
     *
     *
     * Con BCrypt:
     *
     * 123456
     *
     * se transforma en:
     *
     * $2a$10$8sd7s8d7s8d7s8d...
     *
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }







    /**
     * Configuración de las reglas
     * de seguridad del sistema.
     *
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {



        http



        /**
         * Definimos qué rutas
         * pueden ser accedidas.
         *
         */
        .authorizeHttpRequests(auth -> auth



                /**
                 * Rutas públicas.
                 *
                 * Estas páginas NO requieren
                 * autenticación.
                 *
                 *
                 * /login
                 * Página de inicio de sesión.
                 *
                 *
                 * /css/**
                 * Archivos de estilos.
                 *
                 *
                 * /js/**
                 * Archivos JavaScript.
                 *
                 *
                 * /images/**
                 * Imágenes del sistema.
                 *
                 * Ejemplo:
                 *
                 * /images/logo.png
                 *
                 */
                .requestMatchers(

                        "/login",
                        "/css/**",
                        "/js/**",
                        "/images/**"

                )
                .permitAll()





                /**
                 * Todas las demás rutas
                 * requieren un usuario autenticado.
                 *
                 * Ejemplo:
                 *
                 * /dashboard
                 * /trabajadores
                 * /riesgos
                 * /incidentes
                 * /inspecciones
                 * /acciones
                 *
                 */
                .anyRequest()
                .authenticated()



        )





        /**
         * Configuración del formulario
         * de inicio de sesión.
         *
         */
        .formLogin(login -> login



                /**
                 * Página personalizada
                 * creada con Thymeleaf.
                 *
                 */
                .loginPage("/login")



                /**
                 * Después de iniciar sesión
                 * correctamente el usuario
                 * será enviado al dashboard.
                 *
                 */
                .defaultSuccessUrl(

                        "/dashboard",

                        true

                )



                /**
                 * Permite acceder al login
                 * sin estar autenticado.
                 *
                 */
                .permitAll()



        )







        /**
         * Configuración del cierre
         * de sesión.
         *
         */
        .logout(logout -> logout



                /**
                 * Después de cerrar sesión,
                 * regresamos al login.
                 *
                 */
                .logoutSuccessUrl("/login")



        );







        /**
         * Construye la configuración final
         * de seguridad.
         *
         */
        return http.build();

    }


}