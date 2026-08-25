package com.safework.safework.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.Riesgo;


/*
 * Repositorio encargado del acceso
 * a los datos de la entidad Riesgo.
 *
 * Gracias a JpaRepository obtenemos
 * automáticamente operaciones CRUD.
 */
public interface RiesgoRepository
        extends JpaRepository<Riesgo, Long> {

}