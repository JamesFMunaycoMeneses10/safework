package com.safework.safework.service;


import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safework.safework.model.Trabajador;
import com.safework.safework.repository.TrabajadorRepository;


@Service
public class TrabajadorService {


    private final TrabajadorRepository trabajadorRepository;



    public TrabajadorService(
            TrabajadorRepository trabajadorRepository) {

        this.trabajadorRepository = trabajadorRepository;
    }



    /**
     * Obtiene todos los trabajadores registrados.
     */
    public List<Trabajador> listarTodos() {

        return trabajadorRepository.findAll();

    }



    /**
     * Guarda un trabajador nuevo
     * o actualiza uno existente.
     */
    public Trabajador guardar(Trabajador trabajador) {

        return trabajadorRepository.save(trabajador);

    }



    /**
     * Busca un trabajador mediante su ID.
     */
    public Optional<Trabajador> buscarPorId(Long id) {

        return trabajadorRepository.findById(id);

    }





    /**
     * Eliminación controlada.
     *
     * No permite eliminar trabajadores
     * que tengan información relacionada.
     */
    @Transactional
    public void eliminarPorId(Long id) {


        Trabajador trabajador = trabajadorRepository.findById(id)

                .orElseThrow(() ->

                    new RuntimeException(
                        "Trabajador no encontrado"
                    )

                );



        /*
         * Validamos si tiene incidentes asociados
         */
        if(trabajador.getIncidentes() != null

                &&

           !trabajador.getIncidentes().isEmpty()) {


            throw new RuntimeException(

                "No se puede eliminar el trabajador porque tiene incidentes asociados"

            );

        }





        /*
         * Validamos si tiene acciones correctivas asociadas
         */
        if(trabajador.getAccionesCorrectivas() != null

                &&

           !trabajador.getAccionesCorrectivas().isEmpty()) {


            throw new RuntimeException(

                "No se puede eliminar el trabajador porque tiene acciones correctivas asociadas"

            );

        }





        try {


            trabajadorRepository.delete(trabajador);

            trabajadorRepository.flush();



        } catch (DataIntegrityViolationException e) {


            throw new RuntimeException(

                "No se puede eliminar el trabajador porque tiene inspecciones u otros registros asociados"

            );

        }

    }





    /**
     * Verifica si existe otro trabajador
     * con el mismo DNI.
     *
     * Se utiliza para evitar duplicados.
     */
    public boolean existeDniDuplicado(

            Trabajador trabajador) {



        /*
         * Caso nuevo trabajador
         */
        if(trabajador.getId() == null) {


            return trabajadorRepository.existsByDni(

                    trabajador.getDni()

            );

        }





        /*
         * Caso edición.
         *
         * Ignoramos el propio registro.
         */
        return trabajadorRepository.existsByDniAndIdNot(

                trabajador.getDni(),

                trabajador.getId()

        );


    }


}