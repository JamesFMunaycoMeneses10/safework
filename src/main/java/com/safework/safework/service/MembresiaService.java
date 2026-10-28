package com.safework.safework.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.safework.safework.tenancy.EmpresaContexto;

/** JDBC explicito: los accesos de cliente siempre llevan empresa de sesion; el webhook solo concilia una referencia. */
@Service
public class MembresiaService {
    public record Plan(String codigo,String nombre,BigDecimal precio,String moneda,int dias) {}
    public record Pago(String referencia,Long empresa,String plan,BigDecimal importe,String moneda,int dias,
            String estado,String checkout,String externo,LocalDateTime creado,LocalDateTime aprobado) {}
    private final JdbcTemplate jdbc; private final PasarelaPago pasarela; private final TransactionTemplate tx; private final long vendedor;
    public MembresiaService(JdbcTemplate jdbc,PasarelaPago pasarela,PlatformTransactionManager manager,
            @Value("${safework.pagos.vendedor-id:0}") long vendedor) {
        this.jdbc=jdbc;this.pasarela=pasarela;this.tx=new TransactionTemplate(manager);this.vendedor=vendedor;
    }
    public boolean disponible() {return pasarela.disponible();}
    public List<Plan> planes() {return jdbc.query("SELECT codigo,nombre,precio,moneda,dias FROM planes_membresia WHERE activo=TRUE ORDER BY precio",(rs,n)->new Plan(rs.getString(1),rs.getString(2),rs.getBigDecimal(3),rs.getString(4),rs.getInt(5)));}
    public List<Pago> pagos() {return jdbc.query("SELECT * FROM pagos_membresia WHERE empresa_id=? ORDER BY creado_en DESC LIMIT 50",this::fila,empresa());}
    public LocalDateTime vigenteHasta() {
        var resultado=jdbc.query("SELECT vigente_hasta FROM suscripciones_empresa WHERE empresa_id=?",(rs,n)->rs.getTimestamp(1)==null ? null:rs.getTimestamp(1).toLocalDateTime(),empresa());
        return resultado.isEmpty()?null:resultado.getFirst();
    }
    public String iniciar(String planCodigo,String solicitud) {
        if(!disponible())throw new IllegalArgumentException("La integración de pagos de prueba todavía no está configurada.");
        if(solicitud==null || !solicitud.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("Solicitud no válida.");
        long empresa=empresa();
        return tx.execute(s->{
            bloquearEmpresa(empresa);
            var previos=jdbc.query("SELECT * FROM pagos_membresia WHERE empresa_id=? AND solicitud_id=?",this::fila,empresa,solicitud);
            Pago pago;
            if(!previos.isEmpty()) pago=previos.getFirst();
            else {
                Integer pendientes=jdbc.queryForObject("SELECT COUNT(*) FROM pagos_membresia WHERE empresa_id=? AND creado_en>?",Integer.class,empresa,LocalDateTime.now(java.time.ZoneId.of("America/Lima")).minusMinutes(15));
                if(pendientes!=null && pendientes>=5)throw new IllegalArgumentException("Espera antes de generar otra solicitud de pago.");
                Plan plan=planes().stream().filter(p->p.codigo().equals(planCodigo)).findFirst().orElseThrow(()->new IllegalArgumentException("Plan no disponible."));
                String ref=UUID.randomUUID().toString();
                jdbc.update("INSERT INTO pagos_membresia(referencia,empresa_id,solicitud_id,plan_codigo,importe,moneda,dias,estado,creado_en) VALUES(?,?,?,?,?,?,?,'CREADA',?)",ref,empresa,solicitud,plan.codigo(),plan.precio(),plan.moneda(),plan.dias(),LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
                pago=buscar(ref);
            }
            if(pago==null)throw new IllegalStateException("No se pudo registrar la solicitud.");
            if(pago.checkout()!=null)return pago.checkout();
            var checkout=pasarela.crear(pago.referencia(),"SafeWork · "+pago.plan()+" · "+pago.dias()+" días",pago.importe(),pago.moneda());
            jdbc.update("UPDATE pagos_membresia SET preferencia_id=?,checkout_url=? WHERE referencia=? AND empresa_id=?",checkout.id(),checkout.url(),pago.referencia(),empresa);
            return checkout.url();
        });
    }

    public void conciliarPropio(String id) {
        var pago=pasarela.consultar(id); Pago local=buscar(pago.referencia());
        if(local==null || local.empresa()!=empresa())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        aplicar(pago);
    }
    public void notificacion(String id) {aplicar(pasarela.consultar(id));}
    private void aplicar(PasarelaPago.PagoExterno externo) {
        tx.executeWithoutResult(s->{
            Pago pago=buscar(externo.referencia());
            if(pago==null)throw new IllegalArgumentException("Pago no reconocido.");
            bloquearEmpresa(pago.empresa()); pago=buscar(externo.referencia());
            if(externo.real() || externo.vendedor()!=vendedor || externo.importe().compareTo(pago.importe())!=0
                    || !externo.moneda().equals(pago.moneda()) || externo.id()==null || !externo.id().matches("[0-9]{1,30}"))
                throw new IllegalArgumentException("La confirmación no coincide con la operación de prueba.");
            String estado=switch(externo.estado()) {
                case "approved" -> "APROBADA"; case "pending","in_process","authorized" -> "PENDIENTE";
                case "rejected" -> "RECHAZADA"; case "cancelled" -> "CANCELADA";
                case "refunded","charged_back" -> "REEMBOLSADA";
                default -> throw new IllegalArgumentException("Estado de pago no admitido.");
            };
            if(pago.externo()!=null && !pago.externo().equals(externo.id()))throw new IllegalArgumentException("Esta solicitud ya tiene otro pago asociado.");
            String evento=huella(externo.id()+"|"+estado+"|"+externo.actualizado());
            Integer visto=jdbc.queryForObject("SELECT COUNT(*) FROM eventos_pago WHERE clave=?",Integer.class,evento);
            if(visto!=null && visto>0)return;
            if("REEMBOLSADA".equals(pago.estado()) && !"REEMBOLSADA".equals(estado))return;
            if("APROBADA".equals(pago.estado()) && !Set.of("APROBADA","REEMBOLSADA").contains(estado))return;
            LocalDateTime aprobado=pago.aprobado();
            if("APROBADA".equals(estado) && aprobado==null) {
                if(externo.aprobadoEn()==null || externo.aprobadoEn().isAfter(LocalDateTime.now(java.time.ZoneId.of("America/Lima")).plusMinutes(5)))throw new IllegalArgumentException("Fecha de aprobación no válida.");
                aprobado=externo.aprobadoEn();
            }
            jdbc.update("UPDATE pagos_membresia SET estado=?,pago_externo=?,aprobado_en=? WHERE referencia=?",estado,externo.id(),aprobado,pago.referencia());
            jdbc.update("INSERT INTO eventos_pago(clave,referencia,estado,recibido_en) VALUES(?,?,?,?)",evento,pago.referencia(),estado,LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
            var vigencias=jdbc.query("SELECT aprobado_en,dias FROM pagos_membresia WHERE empresa_id=? AND estado='APROBADA' ORDER BY aprobado_en,referencia",(rs,n)->Map.entry(rs.getTimestamp(1).toLocalDateTime(),rs.getInt(2)),pago.empresa());
            LocalDateTime hasta=null;
            for(var v:vigencias)hasta=(hasta==null || v.getKey().isAfter(hasta)?v.getKey():hasta).plusDays(v.getValue());
            if(jdbc.update("UPDATE suscripciones_empresa SET vigente_hasta=?,actualizado_en=? WHERE empresa_id=?",hasta,LocalDateTime.now(java.time.ZoneId.of("America/Lima")),pago.empresa())==0)
                jdbc.update("INSERT INTO suscripciones_empresa(empresa_id,vigente_hasta,actualizado_en) VALUES(?,?,?)",pago.empresa(),hasta,LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
            jdbc.update("INSERT INTO auditoria_eventos(modulo,registro_id,operacion,usuario,fecha,detalle,empresa_id) VALUES('MEMBRESIAS',?,'CONCILIADO','MercadoPago',?,?,?)",
                    pago.empresa(),LocalDateTime.now(java.time.ZoneId.of("America/Lima")),"Referencia "+pago.referencia()+" | Estado: "+estado+" | Integración sin cobro real",pago.empresa());
        });
    }
    private Pago buscar(String referencia) {
        if(referencia==null || referencia.length()!=36)return null;
        var pagos=jdbc.query("SELECT * FROM pagos_membresia WHERE referencia=?",this::fila,referencia);
        return pagos.isEmpty()?null:pagos.getFirst();
    }
    private void bloquearEmpresa(long empresa) {jdbc.queryForObject("SELECT id FROM empresas WHERE id=? FOR UPDATE",Long.class,empresa);}
    private long empresa() {long id=EmpresaContexto.empresaActualId();if(id<=0)throw new org.springframework.security.access.AccessDeniedException("Se requiere una empresa autenticada.");return id;}
    private Pago fila(ResultSet rs,int n)throws SQLException {
        var aprobado=rs.getTimestamp("aprobado_en");
        return new Pago(rs.getString("referencia"),rs.getLong("empresa_id"),rs.getString("plan_codigo"),rs.getBigDecimal("importe"),rs.getString("moneda"),rs.getInt("dias"),rs.getString("estado"),rs.getString("checkout_url"),rs.getString("pago_externo"),rs.getTimestamp("creado_en").toLocalDateTime(),aprobado==null?null:aprobado.toLocalDateTime());
    }
    private String huella(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
