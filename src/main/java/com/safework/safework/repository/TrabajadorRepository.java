package com.safework.safework.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.safework.safework.model.Trabajador;


/*
 * Repositorio encargado del acceso
 * a los datos de Trabajador.
 */
public interface TrabajadorRepository
        extends JpaRepository<Trabajador, Long> {

    @Query("""
            SELECT t FROM Trabajador t JOIN FETCH t.area a
            WHERE (:areaId IS NULL OR a.id = :areaId)
              AND (:dni IS NULL OR t.dni LIKE CONCAT('%', :dni, '%'))
              AND (:cargo IS NULL OR t.cargo = :cargo)
            ORDER BY t.apellidos, t.nombres, t.id
            """)
    List<Trabajador> buscarConFiltros(@Param("areaId") Long areaId,
                                      @Param("dni") String dni,
                                      @Param("cargo") String cargo);

    @Query("SELECT DISTINCT t.cargo FROM Trabajador t ORDER BY t.cargo")
    List<String> listarCargos();

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
