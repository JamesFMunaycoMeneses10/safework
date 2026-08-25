package com.safework.safework.repository;

// JpaRepository proporciona operaciones CRUD ya implementadas.
import org.springframework.data.jpa.repository.JpaRepository;

// Importamos nuestra entidad Area.
import com.safework.safework.model.Area;

/*
 * Repositorio encargado del acceso a datos de la entidad Area.
 *
 * Area -> entidad que administra.
 * Long -> tipo de dato de la clave primaria (id).
 *
 * Al extender JpaRepository heredamos métodos como:
 * save(), findAll(), findById() y deleteById().
 */
public interface AreaRepository extends JpaRepository<Area, Long> {

}