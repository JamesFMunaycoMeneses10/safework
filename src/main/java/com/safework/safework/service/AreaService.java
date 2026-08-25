package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.safework.safework.model.Area;
import com.safework.safework.repository.AreaRepository;


// Indica que esta clase pertenece a la capa de servicios
// y será administrada por Spring.
@Service
public class AreaService {

    /*
     * Repositorio utilizado para acceder a los datos
     * de la entidad Area.
     */
    private final AreaRepository areaRepository;


    /*
     * Inyección de dependencias por constructor.
     *
     * Spring proporciona automáticamente una instancia
     * de AreaRepository cuando crea AreaService.
     */
    public AreaService(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }


    /*
     * Obtiene todas las áreas almacenadas
     * en la base de datos.
     *
     * findAll() es proporcionado por JpaRepository.
     */
    public List<Area> listarTodas() {
        return areaRepository.findAll();
    }


    /*
     * Guarda una nueva área o actualiza
     * una que ya existe.
     *
     * save() es proporcionado por JpaRepository.
     */
    public Area guardar(Area area) {
        return areaRepository.save(area);
    }


    /*
     * Busca un área mediante su identificador.
     *
     * Optional<Area> indica que puede existir
     * un área con ese id o puede no existir.
     *
     * findById() es proporcionado por JpaRepository.
     */
    public Optional<Area> buscarPorId(Long id) {
        return areaRepository.findById(id);
    }


    /*
     * Elimina un área utilizando su identificador.
     *
     * void significa que este método no devuelve
     * ningún valor.
     *
     * deleteById() es proporcionado por JpaRepository.
     */
    public void eliminarPorId(Long id) {
        areaRepository.deleteById(id);
    }

}