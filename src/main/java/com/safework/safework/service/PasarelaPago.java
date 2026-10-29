package com.safework.safework.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PasarelaPago {
    record Checkout(String id,String url) {}
    record PagoExterno(String id,String referencia,BigDecimal importe,String moneda,String estado,
            boolean real,long vendedor,LocalDateTime aprobadoEn,String actualizado,long comprador) {
        // Sin un comprador identificado, live_mode=true sigue rechazandose.
        public PagoExterno(String id,String referencia,BigDecimal importe,String moneda,String estado,
                boolean real,long vendedor,LocalDateTime aprobadoEn,String actualizado) {
            this(id,referencia,importe,moneda,estado,real,vendedor,aprobadoEn,actualizado,0);
        }
    }
    boolean disponible();
    Checkout crear(String referencia,String descripcion,BigDecimal precio,String moneda);
    PagoExterno consultar(String id);
}
