package com.safework.safework.security;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Límite de intentos fallidos por origen y cuenta; acotado en memoria para este servidor. */
@Component
public class LimiteAcceso {
    private record Contador(int fallos, Instant hasta) {}
    private final Map<String,Contador> cuentas = new HashMap<>(), origenes = new HashMap<>();
    private final Clock clock;
    private static final Duration VENTANA = Duration.ofMinutes(15);
    public LimiteAcceso() { this(Clock.systemUTC()); }
    public LimiteAcceso(Clock clock) { this.clock=clock; }
    private String clave(String ip, String usuario) { return ip + "\u0000" + usuario.strip().toLowerCase(Locale.ROOT); }
    private void limpiar() {
        Instant ahora=clock.instant(); cuentas.values().removeIf(v -> !v.hasta().isAfter(ahora));
        origenes.values().removeIf(v -> !v.hasta().isAfter(ahora));
    }
    public synchronized long espera(String ip, String usuario) {
        limpiar();
        Contador cuenta=cuentas.get(clave(ip,usuario)), origen=origenes.get(ip);
        Instant hasta=clock.instant();
        if (cuenta != null && cuenta.fallos() >= 5) hasta=cuenta.hasta();
        if (origen != null && origen.fallos() >= 30 && origen.hasta().isAfter(hasta)) hasta=origen.hasta();
        if (cuentas.size() >= 10000 && cuenta == null || origenes.size() >= 10000 && origen == null) return 60;
        return Math.max(0,Duration.between(clock.instant(),hasta).toSeconds()+ (hasta.isAfter(clock.instant()) ? 1 : 0));
    }
    public synchronized void fallo(String ip, String usuario) {
        limpiar(); incrementar(cuentas,clave(ip,usuario)); incrementar(origenes,ip);
    }
    public synchronized void exito(String ip, String usuario) { cuentas.remove(clave(ip,usuario)); }
    private void incrementar(Map<String,Contador> mapa, String key) {
        var previo=mapa.get(key);
        if (previo == null && mapa.size() >= 10000) return;
        mapa.put(key,previo == null ? new Contador(1,clock.instant().plus(VENTANA)) : new Contador(previo.fallos()+1,previo.hasta()));
    }
}
