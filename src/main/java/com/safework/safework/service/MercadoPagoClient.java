package com.safework.safework.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Adaptador de Checkout Pro con Preferences API. Solo admite un vendedor marcado test_user. */
@Component
public class MercadoPagoClient implements PasarelaPago {
    private static final ParameterizedTypeReference<Map<String,Object>> MAPA=new ParameterizedTypeReference<>() {};
    private final RestClient api;
    private final boolean enabled;
    private final String publicUrl,token,webhookSecret;
    private final long vendedor,compradorPrueba;
    private volatile boolean cuentaVerificada;
    @org.springframework.beans.factory.annotation.Autowired
    public MercadoPagoClient(@Value("${safework.pagos.enabled:false}") boolean enabled,
            @Value("${safework.pagos.access-token:}") String token,
            @Value("${safework.pagos.vendedor-id:0}") long vendedor,
            @Value("${safework.public-url:}") String publicUrl,
            @Value("${safework.pagos.webhook-secret:}") String webhookSecret,
            @Value("${safework.pagos.comprador-id:0}") long compradorPrueba) {
        this(enabled,token,vendedor,publicUrl,webhookSecret,constructorHttp(),compradorPrueba);
    }
    MercadoPagoClient(boolean enabled,String token,long vendedor,String publicUrl,String webhookSecret,RestClient.Builder builder) {
        this(enabled,token,vendedor,publicUrl,webhookSecret,builder,0);
    }
    MercadoPagoClient(boolean enabled,String token,long vendedor,String publicUrl,String webhookSecret,RestClient.Builder builder,long compradorPrueba) {
        this.enabled=enabled;this.token=token;this.vendedor=vendedor;this.publicUrl=publicUrl.replaceAll("/+$","");this.webhookSecret=webhookSecret;
        this.compradorPrueba=compradorPrueba;
        this.api=builder.baseUrl("https://api.mercadopago.com").defaultHeader("Authorization","Bearer "+token).build();
    }
    private static RestClient.Builder constructorHttp() {
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));return RestClient.builder().requestFactory(factory);
    }
    @Override public boolean disponible() {
        if (!enabled || token.isBlank() || vendedor<=0 || webhookSecret.length()<20) return false;
        try { URI uri=URI.create(publicUrl);return "https".equals(uri.getScheme()) && uri.getHost()!=null
                && uri.getUserInfo()==null && uri.getQuery()==null && uri.getFragment()==null; }
        catch (IllegalArgumentException ex) {return false;}
    }
    @SuppressWarnings("unchecked")
    private void verificarCuenta() {
        if (!disponible()) throw new IllegalStateException("Configura las credenciales de prueba y la URL HTTPS para habilitar pagos.");
        if (cuentaVerificada) return;
        var cuenta=api.get().uri(java.net.URI.create("https://api.mercadolibre.com/users/me")).retrieve().body(MAPA);
        if (!esCuentaPrueba(cuenta,vendedor))
            throw new IllegalStateException("La pasarela solo permite una cuenta vendedora de prueba verificada.");
        cuentaVerificada=true;
    }
    private boolean esCuentaPrueba(Map<String,Object> cuenta,long id) {
        return cuenta!=null && cuenta.get("id")!=null && Long.parseLong(cuenta.get("id").toString())==id
                && cuenta.get("tags") instanceof List<?> tags && tags.contains("test_user");
    }
    @Override public Checkout crear(String referencia,String descripcion,BigDecimal precio,String moneda) {
        verificarCuenta();
        var datos=Map.of("items",List.of(Map.of("id",referencia,"title",descripcion,"quantity",1,"currency_id",moneda,"unit_price",precio)),
                "external_reference",referencia,"notification_url",publicUrl+"/webhooks/mercadopago?source_news=webhooks",
                "back_urls",Map.of("success",publicUrl+"/membresia/retorno","pending",publicUrl+"/membresia/retorno","failure",publicUrl+"/membresia/retorno"),
                "expires",true,"expiration_date_to",OffsetDateTime.now(ZoneOffset.UTC).plusHours(24).toString(),
                "payment_methods",Map.of("installments",1));
        var respuesta=api.post().uri("/checkout/preferences").header("X-Idempotency-Key",referencia).body(datos).retrieve().body(MAPA);
        if (respuesta==null || respuesta.get("id")==null || respuesta.get("init_point")==null) throw new IllegalStateException("Respuesta de checkout incompleta.");
        String url=respuesta.get("init_point").toString(); URI destino=URI.create(url);
        if (!"https".equals(destino.getScheme()) || destino.getUserInfo()!=null || destino.getHost()==null
                || !(destino.getHost().equals("www.mercadopago.com.pe") || destino.getHost().equals("www.mercadopago.com")
                     || destino.getHost().equals("mercadopago.com.pe") || destino.getHost().equals("mercadopago.com")))
            throw new IllegalStateException("Dirección de checkout no reconocida.");
        return new Checkout(respuesta.get("id").toString(),url);
    }
    @Override public PagoExterno consultar(String id) {
        verificarCuenta(); if (!id.matches("[0-9]{1,30}")) throw new IllegalArgumentException("Pago no válido.");
        var pago=api.get().uri("/v1/payments/{id}",id).retrieve().body(MAPA);
        if (pago==null || !(pago.get("live_mode") instanceof Boolean modoReal)) throw new IllegalStateException("No se pudo confirmar el pago.");
        if (!id.equals(Objects.toString(pago.get("id"),""))
                || pago.get("collector_id")==null || Long.parseLong(pago.get("collector_id").toString())!=vendedor)
            throw new IllegalArgumentException("El pago no pertenece al vendedor de prueba configurado.");
        long comprador=0;
        if (pago.get("payer") instanceof Map<?,?> datosComprador) {
            String compradorId=Objects.toString(datosComprador.get("id"),"");
            if (compradorId.matches("[0-9]{1,18}")) comprador=Long.parseLong(compradorId);
        }
        if (modoReal) {
            // Se ha observado live_mode=true con saldo ficticio entre usuarios test_user.
            // El vendedor ya fue verificado por /users/me. El comprador debe ser el ID
            // registrado expresamente desde Cuentas de prueba, nunca un nombre o email.
            // La consulta publica /users/{id} no garantiza exponer las mismas etiquetas.
            if (compradorPrueba<=0 || comprador!=compradorPrueba || comprador==vendedor)
                throw new IllegalArgumentException("El comprador no coincide con la cuenta de prueba configurada.");
        }
        return new PagoExterno(pago.get("id").toString(),Objects.toString(pago.get("external_reference"),""),
                new BigDecimal(pago.get("transaction_amount").toString()),pago.get("currency_id").toString(),pago.get("status").toString(),
                modoReal,Long.parseLong(pago.get("collector_id").toString()),
                pago.get("date_approved")==null ? null : OffsetDateTime.parse(pago.get("date_approved").toString()).atZoneSameInstant(ZoneId.of("America/Lima")).toLocalDateTime(),
                Objects.toString(pago.get("date_last_updated"),""),comprador);
    }
}
