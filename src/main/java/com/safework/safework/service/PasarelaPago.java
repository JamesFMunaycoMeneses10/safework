package com.safework.safework.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PasarelaPago {
    record Checkout(String id,String url) {}
    record PagoExterno(String id,String referencia,BigDecimal importe,String moneda,String estado,
            boolean real,long vendedor,LocalDateTime aprobadoEn,String actualizado) {}
    boolean disponible();
    Checkout crear(String referencia,String descripcion,BigDecimal precio,String moneda);
    PagoExterno consultar(String id);
}
