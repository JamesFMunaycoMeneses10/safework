package com.safework.safework.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.safework.safework.model.Incidente;
import com.safework.safework.repository.IncidenteRepository;

/*
 * Servicio encargado de la lógica
 * relacionada con los incidentes
 * y accidentes registrados en SafeWork.
 */
@Service
public class IncidenteService {

    /*
     * Repositorio utilizado para acceder
     * a los datos de los incidentes.
     */
    private final IncidenteRepository incidenteRepository;

    /*
     * Inyección de dependencias
     * mediante constructor.
     *
     * Spring proporciona automáticamente
     * una instancia de IncidenteRepository.
     */
    public IncidenteService(
            IncidenteRepository incidenteRepository) {

        this.incidenteRepository = incidenteRepository;
    }

    /*
     * Obtiene todos los incidentes
     * registrados en el sistema.
     */
    public List<Incidente> listarTodos() {

        return incidenteRepository.findAll();
    }

    /*
     * Guarda un incidente nuevo
     * o actualiza uno existente.
     */
    public Incidente guardar(Incidente incidente) {

        return incidenteRepository.save(incidente);
    }

    /*
     * Busca un incidente mediante su ID.
     */
    public Optional<Incidente> buscarPorId(Long id) {

        return incidenteRepository.findById(id);
    }

    /*
     * Elimina un incidente mediante su ID.
     */
    public void eliminarPorId(Long id) {

        incidenteRepository.deleteById(id);
    }

}