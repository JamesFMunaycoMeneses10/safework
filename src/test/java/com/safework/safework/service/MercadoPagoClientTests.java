package com.safework.safework.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MercadoPagoClientTests {
    private String pago(String modo,String vendedor,String comprador) {
        return """
            {"id":98765,"external_reference":"109cac7e-baf0-4631-8395-80694dfa9a73",
             "transaction_amount":49,"currency_id":"PEN","status":"approved",
             "live_mode":%s,"collector_id":%s,"payer":{"id":%s},
             "date_approved":"2026-10-28T20:42:14.000-04:00","date_last_updated":"2026-10-28T20:42:19.000-04:00"}
            """.formatted(modo,vendedor,comprador);
    }
    private void vendedorDePrueba(MockRestServiceServer server) {
        server.expect(requestTo("https://api.mercadolibre.com/users/me"))
            .andRespond(withSuccess("{\"id\":12345,\"tags\":[\"test_user\"]}",MediaType.APPLICATION_JSON));
    }
    @Test void pagoSandboxSigueDisponibleSinConfigurarComprador() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var client=new MercadoPagoClient(true,"token-falso",12345,"https://safework.example","secreto-solo-para-http-2026",builder);
        vendedorDePrueba(server);
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/98765"))
            .andRespond(withSuccess(pago("false","12345","67890"),MediaType.APPLICATION_JSON));
        var resultado=client.consultar("98765");
        assertThat(resultado.real()).isFalse();assertThat(resultado.comprador()).isEqualTo(67890);
        assertThat(resultado.aprobadoEn()).isEqualTo(java.time.LocalDateTime.of(2026,10,28,19,42,14));server.verify();
    }
    @Test void saldoFicticioConLiveModeTrueExigeVendedorVerificadoYCompradorExplicito() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var client=new MercadoPagoClient(true,"token-falso",12345,"https://safework.example","secreto-solo-para-http-2026",builder,67890);
        vendedorDePrueba(server);
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/98765"))
            .andRespond(withSuccess(pago("true","12345","67890"),MediaType.APPLICATION_JSON));
        var resultado=client.consultar("98765");
        assertThat(resultado.real()).isTrue();assertThat(resultado.comprador()).isEqualTo(67890);server.verify();
    }
    @Test void liveModeTrueRechazaCompradorNoConfiguradoAjenoAusenteOIgualAlVendedor() {
        for(long[] caso:new long[][] {{0,67890},{67890,99999},{67890,0},{12345,12345}}) {
            var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
            var client=new MercadoPagoClient(true,"token-falso",12345,"https://safework.example","secreto-solo-para-http-2026",builder,caso[0]);
            vendedorDePrueba(server);
            String comprador=caso[1]==0?"null":Long.toString(caso[1]);
            server.expect(requestTo("https://api.mercadopago.com/v1/payments/98765"))
                .andRespond(withSuccess(pago("true","12345",comprador),MediaType.APPLICATION_JSON));
            assertThatThrownBy(()->client.consultar("98765")).isInstanceOf(IllegalArgumentException.class);server.verify();
        }
    }
    @Test void compradorConfiguradoNoAutorizaUnVendedorReal() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var client=new MercadoPagoClient(true,"token-falso",12345,"https://safework.example","secreto-solo-para-http-2026",builder,67890);
        server.expect(requestTo("https://api.mercadolibre.com/users/me"))
            .andRespond(withSuccess("{\"id\":12345,\"tags\":[\"normal\"]}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->client.consultar("98765")).isInstanceOf(IllegalStateException.class);server.verify();
    }
    @Test void noConfundeOtroPagoNiOtroVendedorConLaSolicitud() {
        for(String json:new String[] {pago("false","99999","67890"),pago("false","12345","67890").replace("\"id\":98765","\"id\":11111")}) {
            var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
            var client=new MercadoPagoClient(true,"token-falso",12345,"https://safework.example","secreto-solo-para-http-2026",builder);
            vendedorDePrueba(server);
            server.expect(requestTo("https://api.mercadopago.com/v1/payments/98765")).andRespond(withSuccess(json,MediaType.APPLICATION_JSON));
            assertThatThrownBy(()->client.consultar("98765")).isInstanceOf(IllegalArgumentException.class);server.verify();
        }
    }
    @Test void banderaDeEntornoAusenteOConTipoIncorrectoNoSeInterpretaComoSandbox() {
        for(String modo:new String[] {"null","\"false\"","\"desconocido\""}) {
            var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
            var client=new MercadoPagoClient(true,"token-falso",12345,"https://safework.example","secreto-solo-para-http-2026",builder,67890);
            vendedorDePrueba(server);
            server.expect(requestTo("https://api.mercadopago.com/v1/payments/98765")).andRespond(withSuccess(pago(modo,"12345","67890"),MediaType.APPLICATION_JSON));
            assertThatThrownBy(()->client.consultar("98765")).isInstanceOf(IllegalStateException.class);server.verify();
        }
    }
    @Test void soloCuentaDePruebaObtieneCheckoutYEnviaImporteDelServidor() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var client=new MercadoPagoClient(true,"token-falso-para-http",12345,"https://safework.example","secreto-solo-para-http-2026",builder);
        server.expect(requestTo("https://api.mercadolibre.com/users/me")).andRespond(withSuccess("{\"id\":12345,\"tags\":[\"test_user\"]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.items[0].unit_price").value(49.0)).andExpect(jsonPath("$.items[0].currency_id").value("PEN"))
                .andExpect(jsonPath("$.external_reference").value("referencia-segura"))
                .andRespond(withSuccess("{\"id\":\"pref-1\",\"init_point\":\"https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-1\"}",MediaType.APPLICATION_JSON));
        assertThat(client.crear("referencia-segura","Plan mensual",new BigDecimal("49.00"),"PEN").id()).isEqualTo("pref-1");server.verify();
    }
    @Test void cuentaRealSeRechazaAntesDeCrearPreferencia() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var client=new MercadoPagoClient(true,"token-falso",12345,"https://safework.example","secreto-solo-para-http-2026",builder);
        server.expect(requestTo("https://api.mercadolibre.com/users/me")).andRespond(withSuccess("{\"id\":12345,\"tags\":[\"normal\"]}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->client.crear("referencia","Plan",BigDecimal.TEN,"PEN")).hasMessageContaining("prueba");server.verify();
    }
    @Test void checkoutNoRedirigeAUnDominioAjeno() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var client=new MercadoPagoClient(true,"token-falso",12345,"https://safework.example","secreto-solo-para-http-2026",builder);
        server.expect(requestTo("https://api.mercadolibre.com/users/me")).andRespond(withSuccess("{\"id\":12345,\"tags\":[\"test_user\"]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadopago.com/checkout/preferences")).andRespond(withSuccess("{\"id\":\"p\",\"init_point\":\"https://atacante.example/\"}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->client.crear("ref","Plan",BigDecimal.TEN,"PEN")).hasMessageContaining("reconocida");server.verify();
    }
}
