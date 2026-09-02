package com.safework.safework.service;


import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safework.safework.model.Riesgo;
import com.safework.safework.repository.RiesgoRepository;


/**
 * Servicio encargado de la lógica
 * relacionada con los riesgos.
 */
@Service
public class RiesgoService {


    /**
     * Repositorio para acceder
     * a la tabla riesgos.
     */
    private final RiesgoRepository riesgoRepository;



    /**
     * Inyección de dependencias.
     */
    public RiesgoService(
            RiesgoRepository riesgoRepository) {

        this.riesgoRepository = riesgoRepository;
    }



    /**
     * Obtiene todos los riesgos registrados.
     */
    public List<Riesgo> listarTodos() {

        return riesgoRepository.findAll();
    }



    /**
     * Busca un riesgo por ID.
     */
    public Optional<Riesgo> buscarPorId(Long id) {

        return riesgoRepository.findById(id);
    }



    /**
     * Guarda un riesgo nuevo
     * o actualiza uno existente.
     *
     * Calcula automáticamente:
     *
     * nivelRiesgo =
     * probabilidad * severidad
     */
    public Riesgo guardar(Riesgo riesgo) {


        if (riesgo.getProbabilidad() != null
                && riesgo.getSeveridad() != null) {


            int nivelRiesgo =
                    riesgo.getProbabilidad()
                    * riesgo.getSeveridad();


            riesgo.setNivelRiesgo(nivelRiesgo);
        }


        return riesgoRepository.save(riesgo);
    }



    /**
     * Elimina un riesgo mediante su ID.
     *
     * No permite eliminar si tiene
     * acciones correctivas asociadas.
     */
    @Transactional
    public void eliminarPorId(Long id) {


        Riesgo riesgo = riesgoRepository.findById(id)

                .orElseThrow(() ->
                        new RuntimeException(
                                "Riesgo no encontrado"));



        /*
         * Validamos relaciones existentes.
         */
        if (riesgo.getAccionesCorrectivas() != null
                && !riesgo.getAccionesCorrectivas().isEmpty()) {


            throw new RuntimeException(
                    "No se puede eliminar el riesgo porque tiene acciones correctivas asociadas");
        }



        try {


            riesgoRepository.delete(riesgo);

            riesgoRepository.flush();



        } catch (DataIntegrityViolationException e) {


            throw new RuntimeException(
                    "No se puede eliminar el riesgo porque tiene registros asociados");

        }

    }

}