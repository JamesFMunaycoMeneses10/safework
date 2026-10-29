# Verificacion del piloto de Mercado Pago

SafeWork utiliza Checkout Pro con Preferences API y renovacion manual. No hay cobros recurrentes ni activacion a partir de parametros del navegador.

## Caso observado y correccion

Durante el piloto del 28 de octubre de 2026, una compra con saldo ficticio entre las cuentas mostradas en el panel de pruebas devolvio `approved`, el vendedor esperado y `live_mode=true`. El filtro original rechazaba cualquier `live_mode=true`, incluso en este caso. No se debe convertir esa bandera a `false` ni eliminar la comprobacion de entorno sin otra restriccion.

La consulta autenticada `/users/me` identifico al vendedor configurado con la etiqueta `test_user`. En cambio, la consulta del perfil del comprador con el token del vendedor no devolvio esa etiqueta. Por tanto, su ausencia en ese perfil no se usa como prueba de que el comprador sea real ni como mecanismo de autorizacion.

Para admitir la simulacion concreta, el operador debe registrar expresamente el ID de la cuenta COMPRADOR de prueba de su panel de Mercado Pago mediante `SAFEWORK_MP_TEST_BUYER_ID`. No debe registrar cuentas personales o comerciales reales. El valor predeterminado es `0`: sin esta configuracion se conserva el rechazo de `live_mode=true`.

El adaptador conserva la bandera original y obtiene el comprador desde el pago consultado al proveedor. Un pago con esa bandera solo se admite cuando se cumplen todos estos controles:

- El token identifica por API al vendedor configurado y este tiene la etiqueta `test_user`.
- El ID del pago recibido coincide exactamente con el solicitado.
- El vendedor del pago coincide con el vendedor configurado.
- El comprador del pago coincide con el comprador de prueba registrado expresamente, es distinto del vendedor y no es cero.
- La referencia existe y, en la consulta de un administrador, pertenece a su empresa.
- El importe y la moneda coinciden con el catalogo almacenado en el servidor.
- La fecha de aprobacion es valida y la conciliacion no duplica eventos ni dias de vigencia.

Un pago sandbox con `live_mode=false` sigue el flujo anterior. Un valor de entorno ausente o que no sea booleano se rechaza. Una cuenta vendedora real sigue rechazada antes de crear una preferencia o consultar un pago.

## Configuracion

| Variable | Valor esperado |
| --- | --- |
| `SAFEWORK_PAGOS_ENABLED` | `true` solo al completar la configuracion |
| `SAFEWORK_MP_TEST_ACCESS_TOKEN` | Token privado del vendedor de prueba |
| `SAFEWORK_MP_TEST_SELLER_ID` | ID del vendedor de prueba |
| `SAFEWORK_MP_TEST_BUYER_ID` | ID del comprador de prueba del panel; `0` desactiva la excepcion controlada |
| `SAFEWORK_MP_WEBHOOK_SECRET` | Firma privada del webhook |
| `SAFEWORK_PUBLIC_URL` | URL publica HTTPS de SafeWork |

No publicar tokens, contrasenas ni firmas. Los IDs de comprador y vendedor no son numeros de aplicacion. La seleccion del comprador es una provision manual de confianza para este piloto, no una validacion general de compradores comerciales.

## Comprobar una operacion ya aprobada

Despues de desplegar el cambio y configurar el comprador, entrar como administrador de la empresa que origino el pago. En Membresia, usar el ID de la operacion existente en Consultar estado al proveedor. Esta consulta no crea otro pago ni fuerza una aprobacion: vuelve a leer y validar la operacion en Mercado Pago.

Verificar APROBADA, el mismo ID externo y la fecha de vigencia. Repetir la consulta no debe extender otros 30 dias. El retorno del navegador, por si solo, no debe cambiar ninguna suscripcion. La entrega automatica por webhook se verifica por separado; el exito de una consulta manual no demuestra que el webhook haya llegado.

## Referencias

- [Cuentas de prueba de Checkout Pro](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-preferences/test-accounts): cuentas de comprador/vendedor y saldo ficticio.
- [Compras de prueba](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-preferences/integration-test/test-purchases): sesion de comprador de prueba y comprobacion del resultado.
- [Notificaciones de pago](https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-preferences/payment-notifications): firma y consulta del estado al proveedor.

La condicion `live_mode=true` descrita arriba procede de la respuesta observada en este piloto; no se presenta como una garantia general del proveedor para todas las integraciones o medios de pago.
