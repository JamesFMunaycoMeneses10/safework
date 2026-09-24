package com.safework.safework.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

import com.safework.safework.model.AccionCorrectiva;


/**
 * Repository encargado de realizar
 * las operaciones CRUD de AccionCorrectiva.
 */
public interface AccionCorrectivaRepository 
        extends JpaRepository<AccionCorrectiva, Long> {

    @Query("""
            select a from AccionCorrectiva a
            left join a.responsable t left join a.riesgo r
            where (:riesgoId is null or r.id = :riesgoId)
              and (:incidenteId is null or a.incidente.id = :incidenteId)
              and (:texto = '' or lower(a.descripcion) like lower(concat('%', :texto, '%'))
                   or lower(t.nombres) like lower(concat('%', :texto, '%'))
                   or lower(t.apellidos) like lower(concat('%', :texto, '%'))
                   or lower(r.peligro) like lower(concat('%', :texto, '%')))
              and (:vista = 'todas'
                   or :vista = 'revision' and a.estado = 'En revisión'
                   or :vista = 'devueltas' and a.estado = 'Devuelta'
                   or :vista = 'vencidas' and a.fechaLimite < :hoy
                      and a.estado in ('Pendiente', 'En proceso', 'En revisión', 'Devuelta'))
            """)
    Page<AccionCorrectiva> buscar(@Param("riesgoId") Long riesgoId,
            @Param("incidenteId") Long incidenteId, @Param("texto") String texto,
            @Param("vista") String vista, @Param("hoy") LocalDate hoy, Pageable pageable);

    @Query("""
            select count(a) from AccionCorrectiva a
            where (:riesgoId is null or a.riesgo.id = :riesgoId)
              and (:incidenteId is null or a.incidente.id = :incidenteId)
              and (:vista = 'todas'
                   or :vista = 'revision' and a.estado = 'En revisión'
                   or :vista = 'devueltas' and a.estado = 'Devuelta'
                   or :vista = 'vencidas' and a.fechaLimite < :hoy
                      and a.estado in ('Pendiente', 'En proceso', 'En revisión', 'Devuelta'))
            """)
    long contar(@Param("riesgoId") Long riesgoId, @Param("incidenteId") Long incidenteId,
            @Param("vista") String vista, @Param("hoy") LocalDate hoy);

    @Query("""
            select a from AccionCorrectiva a
            where (:riesgoId is null or a.riesgo.id = :riesgoId)
              and (:incidenteId is null or a.incidente.id = :incidenteId)
              and a.fechaLimite between :hoy and :hasta
              and a.estado in ('Pendiente', 'En proceso', 'En revisión', 'Devuelta')
            order by a.fechaLimite asc, a.id asc
            """)
    java.util.List<AccionCorrectiva> proximas(@Param("riesgoId") Long riesgoId,
            @Param("incidenteId") Long incidenteId, @Param("hoy") LocalDate hoy,
            @Param("hasta") LocalDate hasta);

}
