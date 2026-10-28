package com.safework.safework.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FirmaMercadoPago {
    private final String secreto;
    public FirmaMercadoPago(@Value("${safework.pagos.webhook-secret:}") String secreto) {this.secreto=secreto;}
    public boolean verificar(String id,String requestId,String signature) {
        if (secreto.isBlank() || id==null || !id.matches("[0-9]{1,30}") || requestId==null
                || !requestId.matches("[A-Za-z0-9-]{1,100}") || signature==null || signature.length()>300) return false;
        try {
            Map<String,String> campos=new HashMap<>();
            for(String parte:signature.split(",")) {var par=parte.trim().split("=",2);if(par.length!=2 || campos.put(par[0],par[1])!=null)return false;}
            String ts=campos.get("ts"),v1=campos.get("v1");
            if(ts==null || !ts.matches("[0-9]{10,13}") || v1==null || !v1.matches("[a-fA-F0-9]{64}")) return false;
            long epoch=Long.parseLong(ts); if(ts.length()>10) epoch/=1000;
            if(Math.abs(Instant.now().getEpochSecond()-epoch)>600) return false;
            String manifest="id:"+id+";request-id:"+requestId+";ts:"+ts+";";
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return MessageDigest.isEqual(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(v1));
        } catch (Exception ex) {return false;}
    }
}
