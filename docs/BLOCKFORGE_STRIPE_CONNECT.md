# BlockForge + Stripe Connect

Estado: proveedor previsto para evaluación e integración inicial en modo de pruebas. Esta guía no activa pagos reales ni significa que Stripe haya aprobado BlockForge.

## Objetivo

Usar Stripe Connect para incorporar creadores como cuentas conectadas y procesar pagos del Marketplace y sus transferencias, cuando Stripe admita el país, tipo de entidad, edad y modelo comercial de BlockForge.

Documentación oficial: https://docs.stripe.com/connect

## Fases

1. **Diseño local:** implementar pantallas de billetera y estados de cuenta conectada sin solicitar datos bancarios.
2. **Sandbox:** backend de prueba, cuentas de prueba y pagos simulados; ninguna transacción real.
3. **Integración:** onboarding alojado por Stripe, creación de cuentas conectadas desde el servidor, cobros y transferencias según el modelo aprobado.
4. **Validación:** webhooks firmados, idempotencia, conciliación, reembolsos, disputas y pruebas de fallos.
5. **Producción:** solo después de aprobación de Stripe, revisión legal/fiscal y controles de seguridad y cumplimiento.

## Arquitectura requerida

- La aplicación Android nunca contiene la clave secreta de Stripe.
- Un backend de BlockForge crea sesiones/enlaces de onboarding y operaciones de pago.
- La app abre el flujo alojado por Stripe y recibe únicamente el estado necesario.
- El backend conserva identificadores de Stripe y los datos mínimos permitidos; Stripe gestiona la recopilación/verificación de información sensible.
- El libro contable de BlockForge es la fuente de verdad para los saldos mostrados. Un webhook no debe poder duplicar una operación.
- Las operaciones usan claves de idempotencia y cada evento recibido se valida mediante la firma oficial de Stripe.
- Los cambios de saldo se realizan en el servidor y quedan auditados.

## Modelo de comisiones

La distribución objetivo del Marketplace sigue siendo 95% para el creador y 5% para BlockForge, antes de impuestos, procesamiento, reembolsos, contracargos y otros ajustes. La implementación debe usar el modelo de cargos y transferencias que Stripe apruebe para la cuenta y el país; no asumir que el reparto exacto está disponible en todos los territorios.

## Eventos y estados

El backend debe procesar los eventos relevantes de Connect y pagos documentados por Stripe, verificar sus firmas, guardar el ID del evento y procesarlo una sola vez. La lista exacta de eventos se definirá al implementar el flujo elegido. Los estados internos mínimos son: onboarding pendiente, verificación pendiente, habilitado, restringido, pago pendiente, pagado, fallido y reembolsado.

## Protección de menores y requisitos comerciales

BlockForge no permitirá que un menor eluda requisitos de edad ni abra por su cuenta una cuenta comercial o de cobro si Stripe no lo permite. La configuración contractual y la cuenta de plataforma deben estar a cargo de una persona adulta responsable o entidad legal elegible. No pedir al usuario que comparta claves, documentos de identidad ni datos bancarios en el chat o dentro de la app.

## Variables de entorno del servidor

Usar secretos administrados por el entorno del backend, nunca guardarlos en Git ni en el APK:

- `STRIPE_SECRET_KEY`: clave secreta del entorno de prueba o producción.
- `STRIPE_WEBHOOK_SECRET`: secreto de firma del endpoint webhook.
- `STRIPE_PUBLISHABLE_KEY`: clave publicable, si la integración cliente la necesita.

Los valores reales no se escriben en este repositorio. No habilitar producción hasta completar aprobación y revisión.

## Criterios de aceptación

- La app no incluye claves secretas ni credenciales de cobro.
- Se puede probar el onboarding y el flujo de pago usando únicamente datos de prueba oficiales.
- Repetir un webhook no duplica ventas, comisiones, transferencias ni saldos.
- Un pago fallido o disputado no se presenta como retiro completado.
- Los países, monedas y métodos solo aparecen habilitados cuando el proveedor confirma que están disponibles.
- La división 95/5 y todos los cargos adicionales se muestran con claridad antes de la compra.
