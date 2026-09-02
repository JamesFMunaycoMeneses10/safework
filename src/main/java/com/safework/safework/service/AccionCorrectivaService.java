package com.safework.safework.service;


import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.safework.safework.model.AccionCorrectiva;
import com.safework.safework.repository.AccionCorrectivaRepository;


/**
 * Servicio encargado de la lógica
 * de las acciones correctivas.
 */
@Service
public class AccionCorrectivaService {



    private final AccionCorrectivaRepository accionRepository;



    /**
     * Inyección de dependencia.
     */
    public AccionCorrectivaService(
            AccionCorrectivaRepository accionRepository) {

        this.accionRepository = accionRepository;
    }





    /**
     * Lista todas las acciones correctivas.
     */
    public List<AccionCorrectiva> listarTodas() {

        return accionRepository.findAll();
    }





    /**
     * Guarda una acción nueva
     * o actualiza una existente.
     */
    public AccionCorrectiva guardar(
            AccionCorrectiva accion) {

        return accionRepository.save(accion);
    }





    /**
     * Busca una acción por ID.
     */
    public Optional<AccionCorrectiva> buscarPorId(
            Long id) {

        return accionRepository.findById(id);
    }





    /**
     * Elimina una acción correctiva.
     */
    public void eliminarPorId(Long id) {


        AccionCorrectiva accion =
                accionRepository.findById(id)

                .orElseThrow(() ->
                        new RuntimeException(
                                "Acción correctiva no encontrada"));



        accionRepository.delete(accion);

    }

}