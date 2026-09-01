package com.safework.safework.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.safework.safework.model.Incidente;


/*
 * Repositorio encargado del acceso
 * a los datos de la entidad Incidente.
 *
 * Gracias a JpaRepository obtenemos
 * automáticamente las operaciones CRUD.
 */
public interface IncidenteRepository
        extends JpaRepository<Incidente, Long> {

}