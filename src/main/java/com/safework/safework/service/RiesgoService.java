package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.safework.safework.model.Riesgo;
import com.safework.safework.repository.RiesgoRepository;

/*
 * Servicio encargado de la lógica
 * relacionada con los riesgos.
 */
@Service
public class RiesgoService {

    /*
     * Repositorio que utilizaremos
     * para acceder a la tabla riesgos.
     */
    private final RiesgoRepository riesgoRepository;

    /*
     * Inyección de dependencias
     * mediante constructor.
     *
     * Spring proporciona automáticamente
     * una instancia de RiesgoRepository.
     */
    public RiesgoService(
            RiesgoRepository riesgoRepository) {

        this.riesgoRepository = riesgoRepository;
    }

    /*
     * Obtiene todos los riesgos
     * registrados en el sistema.
     */
    public List<Riesgo> listarTodos() {

        return riesgoRepository.findAll();
    }

    /*
     * Busca un riesgo mediante su ID.
     */
    public Optional<Riesgo> buscarPorId(Long id) {

        return riesgoRepository.findById(id);
    }

    /*
     * Guarda un riesgo nuevo
     * o actualiza uno existente.
     *
     * Antes de guardar calculamos
     * automáticamente el nivel de riesgo.
     */
    public Riesgo guardar(Riesgo riesgo) {

        /*
         * Solo realizamos el cálculo
         * si probabilidad y severidad
         * tienen un valor.
         */
        if (riesgo.getProbabilidad() != null
                && riesgo.getSeveridad() != null) {

            /*
             * Fórmula:
             *
             * Nivel de riesgo =
             * Probabilidad × Severidad
             */
            int nivelRiesgo = riesgo.getProbabilidad()
                    * riesgo.getSeveridad();

            /*
             * Guardamos el resultado
             * dentro del objeto Riesgo.
             */
            riesgo.setNivelRiesgo(
                    nivelRiesgo);
        }

        /*
         * Después del cálculo,
         * guardamos en la base de datos.
         */
        return riesgoRepository.save(riesgo);
    }

    /*
     * Elimina un riesgo mediante su ID.
     */
    @Transactional
    public void eliminarPorId(Long id) {

        Riesgo riesgo = riesgoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Riesgo no encontrado"));

        if (riesgo.getAccionesCorrectivas() != null &&
                !riesgo.getAccionesCorrectivas().isEmpty()) {

            throw new RuntimeException(
                    "No se puede eliminar el riesgo porque tiene acciones correctivas asociadas");

        }

        riesgoRepository.delete(riesgo);
    }
}