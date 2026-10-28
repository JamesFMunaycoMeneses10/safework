package com.safework.safework.security;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Única consulta global de recuperación. No admite empresa o destinatario aportados por el navegador. */
@Service
public class RecuperacionService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> correo;
    private final boolean enabled;
    private final String baseUrl, remitente;
    private final SecureRandom random=new SecureRandom();
    public RecuperacionService(JdbcTemplate jdbc, PasswordEncoder encoder, ObjectProvider<JavaMailSender> correo,
            @Value("${safework.recuperacion.enabled:false}") boolean enabled,
            @Value("${safework.public-url:}") String baseUrl,
            @Value("${safework.mail.from:}") String remitente) {
        this.jdbc=jdbc; this.encoder=encoder; this.correo=correo; this.enabled=enabled;
        this.baseUrl=baseUrl; this.remitente=remitente;
    }
    public boolean disponible() { return enabled && correo.getIfAvailable() != null && urlValida() && !remitente.isBlank(); }
    private boolean urlValida() {
        try { URI uri=URI.create(baseUrl); return "https".equals(uri.getScheme()) && uri.getHost()!=null && uri.getUserInfo()==null && uri.getQuery()==null && uri.getFragment()==null; }
        catch (IllegalArgumentException ex) { return false; }
    }
    @Transactional
    public void solicitar(String username) {
        if (!disponible() || username == null || username.length()>100) return;
        var cuentas=jdbc.query("""
            SELECT u.id,u.correo FROM usuarios u JOIN empresas e ON e.id=u.empresa_id
            WHERE u.username=? AND u.estado='ACTIVO' AND u.rol<>'SUPERADMIN' AND e.estado='ACTIVO' FOR UPDATE
            """, (rs,n) -> Map.entry(rs.getLong("id"),rs.getString("correo")==null ? "" : rs.getString("correo")),username);
        if (cuentas.isEmpty() || cuentas.getFirst().getValue().isBlank()) return;
        var cuenta=cuentas.getFirst(); Instant ahora=Instant.now();
        Integer recientes=jdbc.queryForObject("SELECT COUNT(*) FROM recuperacion_claves WHERE usuario_id=? AND creado_en>?",Integer.class,
                cuenta.getKey(),java.sql.Timestamp.from(ahora.minusSeconds(300)));
        if (recientes != null && recientes>0) return;
        byte[] bytes=new byte[32]; random.nextBytes(bytes); String token=HexFormat.of().formatHex(bytes);
        jdbc.update("DELETE FROM recuperacion_claves WHERE usuario_id=? OR expira_en<CURRENT_TIMESTAMP",cuenta.getKey());
        jdbc.update("INSERT INTO recuperacion_claves(token_hash,usuario_id,creado_en,expira_en) VALUES(?,?,?,?)",
                hash(token),cuenta.getKey(),java.sql.Timestamp.from(ahora),java.sql.Timestamp.from(ahora.plusSeconds(1200)));
        var mensaje=new SimpleMailMessage(); mensaje.setFrom(remitente); mensaje.setTo(cuenta.getValue());
        mensaje.setSubject("Restablecer acceso a SafeWork");
        mensaje.setText("Se solicitó restablecer tu contraseña. El enlace vence en 20 minutos y se utiliza una sola vez.\n"
                +baseUrl.replaceAll("/+$","")+"/restablecer?token="+token+"\nSi no lo solicitaste, ignora este correo.");
        correo.getObject().send(mensaje);
    }
    public boolean valido(String token) {
        if (!formato(token)) return false;
        Integer n=jdbc.queryForObject("""
            SELECT COUNT(*) FROM recuperacion_claves r JOIN usuarios u ON u.id=r.usuario_id JOIN empresas e ON e.id=u.empresa_id
            WHERE r.token_hash=? AND r.expira_en>CURRENT_TIMESTAMP AND u.estado='ACTIVO' AND u.rol<>'SUPERADMIN' AND e.estado='ACTIVO'
            """,Integer.class,hash(token)); return n != null && n==1;
    }
    @Transactional
    public void restablecer(String token,String clave) {
        PoliticaContrasena.validar(clave);
        if (!formato(token)) throw new IllegalArgumentException("El enlace no es válido o ya venció.");
        var cuentas=jdbc.query("""
            SELECT r.usuario_id FROM recuperacion_claves r JOIN usuarios u ON u.id=r.usuario_id JOIN empresas e ON e.id=u.empresa_id
            WHERE r.token_hash=? AND r.expira_en>CURRENT_TIMESTAMP AND u.estado='ACTIVO' AND u.rol<>'SUPERADMIN' AND e.estado='ACTIVO' FOR UPDATE
            """,(rs,n) -> rs.getLong(1),hash(token));
        if (cuentas.size()!=1) throw new IllegalArgumentException("El enlace no es válido o ya venció.");
        Long id=cuentas.getFirst();
        jdbc.update("UPDATE usuarios SET password=?,version_acceso=version_acceso+1 WHERE id=?",encoder.encode(clave),id);
        jdbc.update("DELETE FROM recuperacion_claves WHERE usuario_id=?",id);
    }
    private boolean formato(String token) { return token != null && token.matches("[a-f0-9]{64}"); }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
