package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.safework.safework.model.Inspeccion;
import com.safework.safework.repository.InspeccionRepository;


/*
 * Servicio encargado de la lógica
 * relacionada con las inspecciones.
 */
@Service
public class InspeccionService {


    /*
     * Repositorio utilizado para acceder
     * a los datos de las inspecciones.
     */
    private final InspeccionRepository inspeccionRepository;


    /*
     * Inyección de dependencias por constructor.
     *
     * Spring proporciona automáticamente
     * una instancia de InspeccionRepository.
     */
    public InspeccionService(
            InspeccionRepository inspeccionRepository) {

        this.inspeccionRepository = inspeccionRepository;
    }


    /*
     * Obtiene todas las inspecciones
     * registradas en SafeWork.
     */
    public List<Inspeccion> listarTodas() {

        return inspeccionRepository.findAll();
    }


    /*
     * Guarda una inspección nueva
     * o actualiza una existente.
     */
    public Inspeccion guardar(Inspeccion inspeccion) {

        return inspeccionRepository.save(inspeccion);
    }


    /*
     * Busca una inspección mediante su ID.
     */
    public Optional<Inspeccion> buscarPorId(Long id) {

        return inspeccionRepository.findById(id);
    }


    /*
     * Elimina una inspección mediante su ID.
     */
    public void eliminarPorId(Long id) {

        inspeccionRepository.deleteById(id);
    }

}