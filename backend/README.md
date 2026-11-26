# BlockForge Stripe sandbox backend

This is a **test-mode starter**, not a production payment service. It cannot move real money.

## Run locally

1. Install Node.js 20 or newer.
2. Copy `.env.example` to `.env`.
3. Use a Stripe **test-mode** secret key and test webhook secret from an adult-managed, authorized Stripe developer account. Never put secrets in Git, chat, the APK, or client-side code.
4. Set a random `BLOCKFORGE_API_TOKEN` (32+ characters).
5. Configure `BLOCKFORGE_TEST_PRODUCTS_JSON` with a fixed test product and the connected test account ID.
6. Run `npm install`, then `npm run check`, then `npm start`.
7. Forward Stripe test webhooks to `/webhooks/stripe` using the Stripe CLI and set the resulting signing secret.

## Endpoints

- `GET /health`: confirms the service is in test mode.
- `POST /api/connect/onboarding`: creates/reuses a test Express connected account and returns a hosted onboarding link.
- `POST /api/checkout`: creates a test Checkout Session for a product in the server-side catalog. The server calculates the 5% platform fee; Android cannot supply a price or destination account.
- `POST /webhooks/stripe`: verifies Stripe's signature and acknowledges test events.

Authenticated API calls require `Authorization: Bearer <BLOCKFORGE_API_TOKEN>`.

## Deliberate limitations

- This sandbox rejects live Stripe secret keys.
- Connected-account mappings are held in memory and disappear on restart.
- Webhooks are signature-verified but are **not** yet persisted or applied to a double-entry ledger.
- There is no app authentication, creator database, order database, payout ledger, dispute/refund workflow, or Android connection.
- Automatic payout schedules and payout eligibility must be configured and approved through Stripe Connect for the relevant account/country; this starter does not promise withdrawals.
- Product/account configuration is server-side and must not be accepted from an untrusted Android client.

## Before any production launch

Implement durable database storage and transactions, authenticated user sessions, authorization checks, idempotent ledger entries, webhook event deduplication, reconciliation, refunds/disputes, audit logs, rate limits and monitoring. Complete legal, tax, privacy, age, KYC/AML and Stripe platform approval reviews. A responsible adult/legal entity must handle any account and contractual requirements applicable to a minor. Only then should a separate production deployment be considered.
