package com.safework.safework;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.safework.safework.service.*;
import com.safework.safework.tenancy.EmpresaContexto;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:membresias;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "safework.pagos.vendedor-id=12345","safework.pagos.webhook-secret=secreto-hmac-de-pruebas-2026"})
@AutoConfigureMockMvc @ActiveProfiles("test") @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MembresiaIntegracionTests {
    @Autowired JdbcTemplate jdbc; @Autowired MembresiaService service; @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder; @MockitoBean PasarelaPago pasarela;
    Long a,b;String admin,trabajador;String paymentId;
    <T>T en(long id,Supplier<T> action){return EmpresaContexto.ejecutar(id,action);}
    @BeforeAll void esquema(){new ResourceDatabasePopulator(new ClassPathResource("db/migration/V5__membresias_en_integracion.sql")).execute(Objects.requireNonNull(jdbc.getDataSource()));}
    @BeforeEach void preparar(){
        paymentId=Long.toString(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        a=empresa();b=empresa();admin="adm_"+UUID.randomUUID();trabajador="tra_"+UUID.randomUUID();
        for(var cuenta:Map.of(admin,"ADMIN",trabajador,"TRABAJADOR").entrySet())jdbc.update("INSERT INTO usuarios(username,password,rol,nombre,apellido,estado,fecha_registro,empresa_id) VALUES(?,?,?,'Nombre','Apellido','ACTIVO',CURRENT_TIMESTAMP,?)",cuenta.getKey(),encoder.encode("ClaveIntegracion2026!"),cuenta.getValue(),a);
        when(pasarela.disponible()).thenReturn(true);
        when(pasarela.crear(anyString(),anyString(),any(),anyString())).thenAnswer(i->new PasarelaPago.Checkout("pref-"+i.getArgument(0),"https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id="+i.getArgument(0)));
    }
    Long empresa(){String code=UUID.randomUUID().toString();jdbc.update("INSERT INTO empresas(nombre,codigo,estado,fecha_registro) VALUES('Cliente',?,'ACTIVO',CURRENT_TIMESTAMP)",code);return jdbc.queryForObject("SELECT id FROM empresas WHERE codigo=?",Long.class,code);}
    String iniciar(long empresa){en(empresa,()->service.iniciar("SST_MENSUAL",UUID.randomUUID().toString()));return en(empresa,()->service.pagos().getFirst().referencia());}
    PasarelaPago.PagoExterno pago(String ref,String estado){return new PasarelaPago.PagoExterno(paymentId,ref,new BigDecimal("49.00"),"PEN",estado,false,12345,LocalDateTime.now(ZoneId.of("America/Lima")).minusHours(1),estado+"-actualizacion");}
    MockHttpSession login(String user)throws Exception{
        var f=mvc.perform(get("/login")).andReturn();var s=(MockHttpSession)f.getRequest().getSession();CsrfToken c=(CsrfToken)f.getRequest().getAttribute("_csrf");
        return (MockHttpSession)mvc.perform(post("/login").session(s).param("username",user).param("password","ClaveIntegracion2026!").param(c.getParameterName(),c.getToken())).andExpect(redirectedUrl("/dashboard")).andReturn().getRequest().getSession();
    }
    @Test void precioSaleDelCatalogoYSolicitudRepetidaNoDuplicaPago(){
        String solicitud=UUID.randomUUID().toString();String url=en(a,()->service.iniciar("SST_MENSUAL",solicitud));
        assertThat(en(a,()->service.iniciar("SST_MENSUAL",solicitud))).isEqualTo(url);
        assertThat(en(a,()->service.pagos().size())).isEqualTo(1);
        verify(pasarela,times(1)).crear(anyString(),anyString(),eq(new BigDecimal("49.00")),eq("PEN"));
    }
    @Test void aprobacionDuplicadaNoExtiendeVigenciaDosVeces(){
        String ref=iniciar(a);when(pasarela.consultar(paymentId)).thenReturn(pago(ref,"approved"));
        service.notificacion(paymentId);var hasta=en(a,()->service.vigenteHasta());
        service.notificacion(paymentId);assertThat(en(a,()->service.vigenteHasta())).isEqualTo(hasta);
        assertThat(en(a,()->service.pagos().getFirst().estado())).isEqualTo("APROBADA");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM eventos_pago WHERE referencia=?",Integer.class,ref)).isEqualTo(1);
    }
    @Test void rechazaPagoRealImporteMonedaOVendedorQueNoCoinciden(){
        String ref=iniciar(a);var p=pago(ref,"approved");
        for(var invalido:List.of(new PasarelaPago.PagoExterno(p.id(),ref,p.importe(),p.moneda(),p.estado(),true,p.vendedor(),p.aprobadoEn(),"real"),
                new PasarelaPago.PagoExterno(p.id(),ref,BigDecimal.ONE,p.moneda(),p.estado(),false,p.vendedor(),p.aprobadoEn(),"importe"),
                new PasarelaPago.PagoExterno(p.id(),ref,p.importe(),"USD",p.estado(),false,p.vendedor(),p.aprobadoEn(),"moneda"),
                new PasarelaPago.PagoExterno(p.id(),ref,p.importe(),p.moneda(),p.estado(),false,999,p.aprobadoEn(),"vendedor"))){
            when(pasarela.consultar(p.id())).thenReturn(invalido);assertThatThrownBy(()->service.notificacion(p.id())).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(en(a,()->service.vigenteHasta())).isNull();
    }
    @Test void conciliacionDeClienteNoPuedeAfectarOtraEmpresa(){
        String ref=iniciar(b);when(pasarela.consultar(paymentId)).thenReturn(pago(ref,"approved"));
        assertThatThrownBy(()->en(a,()->{service.conciliarPropio(paymentId);return null;})).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(en(b,()->service.pagos().getFirst().estado())).isEqualTo("CREADA");assertThat(en(a,()->service.pagos())).isEmpty();
    }
    @Test void rechazadoNoActivaYReembolsoRevocaVigenciaSinBorrarSst(){
        String ref=iniciar(a);when(pasarela.consultar(paymentId)).thenReturn(pago(ref,"rejected"));service.notificacion(paymentId);
        assertThat(en(a,()->service.vigenteHasta())).isNull();
        when(pasarela.consultar(paymentId)).thenReturn(pago(ref,"approved"));service.notificacion(paymentId);assertThat(en(a,()->service.vigenteHasta())).isNotNull();
        when(pasarela.consultar(paymentId)).thenReturn(pago(ref,"refunded"));service.notificacion(paymentId);assertThat(en(a,()->service.vigenteHasta())).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE empresa_id=?",Integer.class,a)).isEqualTo(2);
    }
    @Test void soloAdminAbreMembresiaYRetornoNoActivaNada()throws Exception{
        var session=login(admin);String ref=iniciar(a);
        mvc.perform(get("/membresia").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString(ref)));
        mvc.perform(get("/membresia/retorno").session(session).param("status","approved").param("payment_id",paymentId)).andExpect(redirectedUrl("/membresia"));
        assertThat(en(a,()->service.vigenteHasta())).isNull();
        mvc.perform(post("/membresia/contratar").session(session).param("plan","SST_MENSUAL").param("solicitudId",UUID.randomUUID().toString())).andExpect(status().isForbidden());
        mvc.perform(get("/membresia").session(login(trabajador))).andExpect(status().isForbidden());
    }
    @Test void webhookSinSesionRequiereFirmaCorrectaYNoConfiaEnBody()throws Exception{
        String ref=iniciar(a);when(pasarela.consultar(paymentId)).thenReturn(pago(ref,"approved"));
        mvc.perform(post("/webhooks/mercadopago").param("data.id",paymentId)).andExpect(status().isUnauthorized());
        String ts=Long.toString(Instant.now().toEpochMilli()),requestId=UUID.randomUUID().toString();
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec("secreto-hmac-de-pruebas-2026".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String firma=HexFormat.of().formatHex(mac.doFinal(("id:"+paymentId+";request-id:"+requestId+";ts:"+ts+";").getBytes(StandardCharsets.UTF_8)));
        mvc.perform(post("/webhooks/mercadopago").param("data.id",paymentId).header("x-request-id",requestId).header("x-signature","ts="+ts+",v1="+firma)
                .contentType("application/json").content("{\"empresaId\":"+b+",\"status\":\"rejected\"}")).andExpect(status().isOk());
        assertThat(en(a,()->service.pagos().getFirst().estado())).isEqualTo("APROBADA");assertThat(en(b,()->service.vigenteHasta())).isNull();
    }
}
