package com.safework.safework.service;


import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safework.safework.model.Trabajador;
import com.safework.safework.repository.TrabajadorRepository;
import com.safework.safework.repository.UsuarioRepository;


@Service
public class TrabajadorService {


    private final TrabajadorRepository trabajadorRepository;
    private final UsuarioRepository usuarioRepository;



    public TrabajadorService(
            TrabajadorRepository trabajadorRepository, UsuarioRepository usuarioRepository) {

        this.trabajadorRepository = trabajadorRepository;
        this.usuarioRepository = usuarioRepository;
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

        if (trabajador.getUsuario() != null && trabajador.getUsuario().getId() != null) {
            Long usuarioId = trabajador.getUsuario().getId();
            var usuario = usuarioRepository.findById(usuarioId).orElseThrow(
                    () -> new IllegalArgumentException("La cuenta seleccionada no existe"));
            if (!"ACTIVO".equals(usuario.getEstado())) {
                throw new IllegalArgumentException("La cuenta vinculada debe estar activa");
            }
            if (trabajadorRepository.existsByUsuarioIdAndIdNot(usuarioId,
                    trabajador.getId() == null ? -1L : trabajador.getId())) {
                throw new IllegalArgumentException("La cuenta ya está vinculada a otro trabajador");
            }
            trabajador.setUsuario(usuario);
        } else {
            trabajador.setUsuario(null);
        }

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
