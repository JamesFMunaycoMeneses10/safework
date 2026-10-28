package com.safework.safework.service;


import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.safework.safework.model.Trabajador;
import com.safework.safework.repository.TrabajadorRepository;
import com.safework.safework.repository.UsuarioRepository;
import com.safework.safework.repository.AreaRepository;


@Service
public class TrabajadorService {


    private final TrabajadorRepository trabajadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;



    @Autowired
    public TrabajadorService(
            TrabajadorRepository trabajadorRepository, UsuarioRepository usuarioRepository,
            AreaRepository areaRepository) {

        this.trabajadorRepository = trabajadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.areaRepository = areaRepository;
    }



    /**
     * Obtiene todos los trabajadores registrados.
     */
    public List<Trabajador> listarTodos() {

        return trabajadorRepository.findAll();

    }

    public List<Trabajador> buscarConFiltros(Long areaId, String dni, String cargo) {
        return trabajadorRepository.buscarConFiltros(areaId,
                dni == null || dni.isBlank() ? null : dni.trim(),
                cargo == null || cargo.isBlank() ? null : cargo.trim());
    }

    public List<String> listarCargos() {
        return trabajadorRepository.listarCargos();
    }



    /**
     * Guarda un trabajador nuevo
     * o actualiza uno existente.
     */
    @Transactional
    public Trabajador guardar(Trabajador datos) {
        Trabajador trabajador = datos.getId() == null ? new Trabajador()
                : trabajadorRepository.findById(datos.getId()).orElseThrow(
                        () -> new IllegalArgumentException("Trabajador no encontrado"));
        if (datos.getArea() == null || datos.getArea().getId() == null) {
            throw new IllegalArgumentException("Seleccione un área de su empresa");
        }
        var area = areaRepository.findById(datos.getArea().getId()).orElseThrow(
                () -> new IllegalArgumentException("El área seleccionada no está disponible"));

        if (datos.getUsuario() != null && datos.getUsuario().getId() != null) {
            Long usuarioId = datos.getUsuario().getId();
            var usuario = usuarioRepository.findById(usuarioId).orElseThrow(
                    () -> new IllegalArgumentException("La cuenta seleccionada no existe"));
            if ("SUPERADMIN".equals(usuario.getRol())) {
                throw new IllegalArgumentException("La cuenta seleccionada no está disponible");
            }
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

        trabajador.setDni(datos.getDni());
        trabajador.setNombres(datos.getNombres());
        trabajador.setApellidos(datos.getApellidos());
        trabajador.setCargo(datos.getCargo());
        trabajador.setArea(area);

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
