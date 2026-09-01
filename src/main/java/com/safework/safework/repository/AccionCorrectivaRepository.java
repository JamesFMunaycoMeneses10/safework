package com.safework.safework.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.AccionCorrectiva;


/**
 * Repository encargado de realizar
 * las operaciones CRUD de AccionCorrectiva.
 */
public interface AccionCorrectivaRepository 
        extends JpaRepository<AccionCorrectiva, Long> {

}