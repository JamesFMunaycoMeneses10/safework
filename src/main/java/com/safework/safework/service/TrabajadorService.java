package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.safework.safework.model.Trabajador;
import com.safework.safework.repository.TrabajadorRepository;

/*
 * Servicio encargado de la lógica relacionada
 * con los trabajadores.
 */
@Service
public class TrabajadorService {

    private final TrabajadorRepository trabajadorRepository;

    /*
     * Inyección de dependencias por constructor.
     */
    public TrabajadorService(
            TrabajadorRepository trabajadorRepository) {

        this.trabajadorRepository = trabajadorRepository;
    }

    /*
     * Obtiene todos los trabajadores.
     */
    public List<Trabajador> listarTodos() {

        return trabajadorRepository.findAll();
    }

    /*
     * Guarda un trabajador nuevo
     * o actualiza uno existente.
     */
    public Trabajador guardar(Trabajador trabajador) {

        return trabajadorRepository.save(trabajador);
    }

    /*
     * Busca un trabajador por ID.
     */
    public Optional<Trabajador> buscarPorId(Long id) {

        return trabajadorRepository.findById(id);
    }

    /*
     * Elimina un trabajador por ID.
     */
    public void eliminarPorId(Long id) {

        try {

            trabajadorRepository.deleteById(id);

        } catch (Exception e) {

            throw new RuntimeException(
                    "No se puede eliminar el trabajador porque tiene registros asociados");

        }

    }

    /*
     * Verifica si el DNI ya pertenece
     * a otro trabajador.
     *
     * Si el trabajador es nuevo:
     * usamos existsByDni().
     *
     * Si estamos editando:
     * usamos existsByDniAndIdNot()
     * para ignorar el propio registro.
     */
    public boolean existeDniDuplicado(Trabajador trabajador) {

        // Nuevo trabajador.
        if (trabajador.getId() == null) {

            return trabajadorRepository.existsByDni(
                    trabajador.getDni());
        }

        // Trabajador existente.
        return trabajadorRepository.existsByDniAndIdNot(
                trabajador.getDni(),
                trabajador.getId());
    }

}