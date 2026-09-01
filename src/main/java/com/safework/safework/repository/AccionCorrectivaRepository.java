package com.safework.safework.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.safework.safework.model.AccionCorrectiva;


/**
 * Repository encargado de realizar
 * las operaciones CRUD de AccionCorrectiva.
 */
@Repository
public interface AccionCorrectivaRepository 
        extends JpaRepository<AccionCorrectiva, Long> {

}