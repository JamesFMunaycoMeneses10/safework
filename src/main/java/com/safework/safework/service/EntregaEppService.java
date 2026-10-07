package com.safework.safework.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safework.safework.model.EntregaEpp;
import com.safework.safework.model.EntregaEppFormulario;
import com.safework.safework.repository.EntregaEppRepository;
import com.safework.safework.repository.TrabajadorRepository;
import com.safework.safework.repository.UsuarioRepository;

@Service
public class EntregaEppService {
    private final EntregaEppRepository entregas;
    private final TrabajadorRepository trabajadores;
    private final UsuarioRepository usuarios;

    public EntregaEppService(EntregaEppRepository entregas, TrabajadorRepository trabajadores,
            UsuarioRepository usuarios) {
        this.entregas = entregas;
        this.trabajadores = trabajadores;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<EntregaEpp> listarTodas() {
        return entregas.findAllByOrderByFechaEntregaDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<EntregaEpp> listarPropias(String username) {
        if (trabajadores.findByUsuarioUsername(username).isEmpty()) {
            throw new IllegalArgumentException("Tu cuenta todavía no está vinculada a un trabajador");
        }
        return entregas.findByTrabajadorUsuarioUsernameOrderByFechaEntregaDescIdDesc(username);
    }

    @Transactional
    public EntregaEpp registrar(EntregaEppFormulario datos, String username) {
        if (datos == null || datos.getTrabajadorId() == null) {
            throw new IllegalArgumentException("Seleccione un trabajador");
        }
        String tipo = datos.getTipoEpp() == null ? "" : datos.getTipoEpp().trim();
        if (tipo.isEmpty() || tipo.length() > 100) {
            throw new IllegalArgumentException("Indique un tipo de EPP de hasta 100 caracteres");
        }
        if (datos.getFechaEntrega() == null || datos.getFechaEntrega().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrega no puede ser futura");
        }
        if (datos.getCantidad() == null || datos.getCantidad() < 1 || datos.getCantidad() > 1000) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y 1000");
        }
        String observaciones = datos.getObservaciones() == null ? "" : datos.getObservaciones().trim();
        if (observaciones.length() > 500) {
            throw new IllegalArgumentException("Las observaciones no pueden superar los 500 caracteres");
        }

        var trabajador = trabajadores.findById(datos.getTrabajadorId())
                .orElseThrow(() -> new IllegalArgumentException("El trabajador seleccionado no existe"));
        var usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta que registra la entrega no existe"));
        EntregaEpp entrega = new EntregaEpp();
        entrega.setTrabajador(trabajador);
        entrega.setTipoEpp(tipo);
        entrega.setFechaEntrega(datos.getFechaEntrega());
        entrega.setCantidad(datos.getCantidad());
        entrega.setObservaciones(observaciones.isEmpty() ? null : observaciones);
        entrega.setRegistradoPor(usuario);
        return entregas.save(entrega);
    }
}
