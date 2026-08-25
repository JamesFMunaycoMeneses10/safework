package com.safework.safework.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.Inspeccion;


/*
 * Repositorio encargado del acceso
 * a los datos de la entidad Inspeccion.
 *
 * Gracias a JpaRepository obtenemos
 * automáticamente las operaciones CRUD.
 */
public interface InspeccionRepository
        extends JpaRepository<Inspeccion, Long> {

}