package com.safework.safework.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.safework.safework.model.AuditoriaEvento;
import com.safework.safework.repository.AuditoriaEventoRepository;

@Service
public class AuditoriaService {
    private final AuditoriaEventoRepository repository;

    public AuditoriaService(AuditoriaEventoRepository repository) {
        this.repository = repository;
    }

    public void registrar(String modulo, Long registroId, String operacion, String usuario, String detalle) {
        repository.save(new AuditoriaEvento(modulo, registroId, operacion,
                usuario == null || usuario.isBlank() ? "Sistema" : usuario,
                LocalDateTime.now(), detalle));
    }

    public Page<AuditoriaEvento> buscar(String texto, int pagina) {
        return repository.buscar(texto == null ? "" : texto.trim(),
                PageRequest.of(pagina, 20, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))));
    }
}
