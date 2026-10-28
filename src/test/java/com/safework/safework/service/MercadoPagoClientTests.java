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
