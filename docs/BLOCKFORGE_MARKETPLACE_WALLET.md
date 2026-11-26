# BlockForge Marketplace y Wallet — especificación

Estado: diseño aprobado para implementar por etapas. Esto NO es todavía un sistema de pagos activo ni mueve dinero real.

## Modelo del Marketplace

- Publicar recursos será gratis.
- En cada venta, la distribución objetivo es 95% para el creador y 5% para BlockForge.
- Ejemplo antes de impuestos, comisiones del procesador, reembolsos y ajustes: venta de 10,00 CAD = 9,50 CAD para el creador y 0,50 CAD para BlockForge.
- La pantalla de compra debe mostrar el precio, moneda, impuestos aplicables y cualquier cargo antes de confirmar.
- No cobrar por subir, actualizar o mantener un recurso publicado.
- Admitir recursos gratuitos y de pago; los recursos gratuitos no generan reparto.

## BlockForge Wallet

Cada cuenta tendrá una vista de billetera con:
- Saldo pendiente: ventas que todavía no se pueden retirar.
- Saldo disponible: fondos liberados para retiro.
- Saldo en revisión: operaciones retenidas por seguridad, disputas o verificación.
- Historial detallado: venta, comisión, reembolso, ajuste, retiro y estado.
- Moneda y conversión claramente identificadas; no mezclar importes de monedas distintas.
- Recibos y referencias de transacción.

## Retiros automáticos

Objetivo de producto: el creador solicita el retiro y un proveedor de pagos compatible procesa automáticamente el pago. El equipo de BlockForge no envía cada transferencia manualmente.

Flujo:
1. El creador añade un método de cobro mediante el flujo seguro del proveedor.
2. El proveedor realiza las verificaciones de identidad, titularidad y elegibilidad que exija la ley.
3. El creador solicita un retiro o configura pagos automáticos al alcanzar un mínimo.
4. El servidor comprueba saldo disponible, moneda, límites, retenciones, disputas y riesgo.
5. El servidor crea una orden de pago idempotente con el proveedor.
6. El proveedor procesa la transferencia y comunica el resultado mediante webhook firmado.
7. BlockForge actualiza el libro contable y muestra completado, pendiente o fallido.
8. Si falla, los fondos vuelven al saldo disponible cuando el proveedor confirme que no se pagaron.

Métodos potenciales, sujetos a país, proveedor y elegibilidad: transferencia bancaria, PayPal y proveedores de pagos/cripto compatibles. Binance no se promete como método hasta que exista integración oficial, aprobación legal y soporte real en los países admitidos.

## Reglas de seguridad y contabilidad

- Nunca guardar contraseñas bancarias, claves privadas, frases semilla ni credenciales de PayPal/Binance.
- Usar onboarding alojado/tokenizado del proveedor; almacenar solo identificadores tokenizados y datos mínimos permitidos.
- El servidor, no la app Android, es la autoridad del saldo.
- Libro contable inmutable de doble entrada; no calcular el saldo sumando valores manipulables del cliente.
- Verificar firma de webhooks, evitar pagos duplicados, usar claves de idempotencia y reconciliar con informes del proveedor.
- Límites de retiro, detección de fraude, revisión de operaciones anómalas y registro de auditoría.
- Gestionar contracargos, reembolsos, reservas y saldo negativo según reglas publicadas.
- Mostrar plazos, mínimos, comisiones, tipos de cambio y restricciones antes de confirmar.
- No prometer disponibilidad mundial: métodos y países se habilitan solo tras comprobar requisitos legales y del proveedor.
- No custodiar fondos directamente ni lanzar pagos reales hasta contar con proveedor autorizado, asesoría legal/contable, controles fiscales y cumplimiento de KYC/AML y protección al consumidor aplicables.

## Protección de menores

Como BlockForge puede ser utilizado por menores, no permitir que una persona menor abra por sí sola una cuenta de cobro o eluda restricciones de edad. El proveedor debe determinar si hace falta un padre/tutor o una entidad legal adulta. No solicitar ni almacenar documentos sensibles en la app si el proveedor puede verificarlos directamente.

## Panel del creador

Mostrar:
- Ventas brutas.
- Parte del creador (95% objetivo).
- Comisión de BlockForge (5% objetivo).
- Impuestos, procesamiento, reembolsos y otros ajustes por separado.
- Saldo pendiente, disponible y en revisión.
- Retiros con estado y referencia del proveedor.

## Etapas de implementación

1. Diseñar las pantallas y el modelo de datos de la billetera en modo demostración, sin dinero real.
2. Implementar libro contable de prueba y simulador de compras/reembolsos/retiros.
3. Crear backend seguro y pruebas de idempotencia, webhooks, reconciliación y fraude.
4. Elegir proveedor(es) de pagos según país, edad, moneda y métodos admitidos.
5. Completar revisión legal, fiscal, privacidad y seguridad.
6. Habilitar pagos reales gradualmente, primero en países y métodos aprobados.

## Criterios de aceptación

- Una compra de prueba de 10,00 CAD refleja 9,50 CAD al creador y 0,50 CAD a BlockForge antes de otros cargos.
- Repetir el mismo webhook no duplica el saldo ni el retiro.
- Un retiro fallido no se marca como pagado.
- La app no puede modificar directamente el saldo.
- Todas las comisiones y ajustes se explican al usuario.
- Ningún método aparece como disponible si no está realmente integrado y habilitado.
