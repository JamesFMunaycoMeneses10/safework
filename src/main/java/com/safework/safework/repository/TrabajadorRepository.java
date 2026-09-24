package com.safework.safework.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.Trabajador;


/*
 * Repositorio encargado del acceso
 * a los datos de Trabajador.
 */
public interface TrabajadorRepository
        extends JpaRepository<Trabajador, Long> {

    java.util.Optional<Trabajador> findByUsuarioUsername(String username);

    boolean existsByUsuarioIdAndIdNot(Long usuarioId, Long id);


    /*
     * Devuelve true si existe un trabajador
     * con el DNI indicado.
     */
    boolean existsByDni(String dni);


    /*
     * Devuelve true si existe un trabajador
     * con ese DNI pero con un ID diferente.
     *
     * Se utiliza durante la edición.
     */
    boolean existsByDniAndIdNot(String dni, Long id);

}
