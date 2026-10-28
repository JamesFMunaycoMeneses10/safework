package com.safework.safework.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import com.safework.safework.security.FirmaMercadoPago;
import com.safework.safework.service.MembresiaService;

@RestController
public class MercadoPagoWebhookController {
    private final FirmaMercadoPago firma;private final MembresiaService service;
    public MercadoPagoWebhookController(FirmaMercadoPago firma,MembresiaService service){this.firma=firma;this.service=service;}
    @PostMapping("/webhooks/mercadopago") public ResponseEntity<Void> notificar(@RequestParam(name="data.id",required=false) String id,
            @RequestHeader(name="x-request-id",required=false) String requestId,@RequestHeader(name="x-signature",required=false) String signature) {
        if(!service.disponible())return ResponseEntity.status(503).build();
        if(!firma.verificar(id,requestId,signature))return ResponseEntity.status(401).build();
        try{service.notificacion(id);return ResponseEntity.ok().build();}
        catch(IllegalArgumentException ex){return ResponseEntity.badRequest().build();}
        catch(IllegalStateException|RestClientException ex){return ResponseEntity.status(503).build();}
    }
}
