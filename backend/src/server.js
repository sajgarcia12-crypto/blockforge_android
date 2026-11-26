require("dotenv").config();
const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const Stripe = require("stripe");

const app = express();
app.disable("x-powered-by");
app.use(helmet());

const port = Number(process.env.PORT || 4242);
const secretKey = process.env.STRIPE_SECRET_KEY || "";
const apiToken = process.env.BLOCKFORGE_API_TOKEN || "";
const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET || "";
const baseUrl = process.env.PUBLIC_BASE_URL || "";
const successUrl = process.env.SUCCESS_URL || "";
const cancelUrl = process.env.CANCEL_URL || "";

if (!secretKey.startsWith("sk_test_")) {
  throw new Error("BlockForge sandbox only: STRIPE_SECRET_KEY must start with sk_test_.");
}
if (apiToken.length < 32) {
  throw new Error("BLOCKFORGE_API_TOKEN must be a server-only random token of at least 32 characters.");
}
if (!baseUrl.startsWith("https://") && !["localhost", "127.0.0.1"].includes(new URL(baseUrl).hostname)) {
  throw new Error("PUBLIC_BASE_URL must use HTTPS except for localhost testing.");
}

const stripe = new Stripe(secretKey);
const products = JSON.parse(process.env.BLOCKFORGE_TEST_PRODUCTS_JSON || "{}");
const connectedAccounts = new Map(); // Sandbox only; replace with durable DB storage before deployment.

const limiter = rateLimit({ windowMs: 60 * 1000, limit: 30, standardHeaders: "draft-7", legacyHeaders: false });

// Stripe requires the raw request body for webhook signature verification.
app.post("/webhooks/stripe", express.raw({ type: "application/json" }), (req, res) => {
  let event;
  try {
    event = stripe.webhooks.constructEvent(req.body, req.headers["stripe-signature"], webhookSecret);
  } catch (err) {
    return res.status(400).send("Invalid webhook signature");
  }

  // Sandbox acknowledgement only. Production must persist event IDs and apply
  // idempotent ledger transitions in a transactional database.
  console.log("Verified Stripe test event:", event.id, event.type);
  return res.json({ received: true });
});

app.use(express.json({ limit: "32kb" }));
app.use(limiter);

function requireSandboxToken(req, res, next) {
  const supplied = (req.headers.authorization || "").replace(/^Bearer\s+/i, "");
  if (!apiToken || supplied !== apiToken) return res.status(401).json({ error: "Unauthorized" });
  next();
}

app.get("/health", (_req, res) => res.json({ ok: true, mode: "test", livePayments: false }));

app.post("/api/connect/onboarding", requireSandboxToken, async (req, res) => {
  try {
    const { creatorRef, email, country } = req.body || {};
    if (typeof creatorRef !== "string" || !/^[a-zA-Z0-9_-]{3,64}$/.test(creatorRef)) {
      return res.status(400).json({ error: "creatorRef inválido" });
    }
    if (typeof email !== "string" || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ error: "Email de prueba inválido" });
    }
    if (typeof country !== "string" || !/^[A-Z]{2}$/.test(country)) {
      return res.status(400).json({ error: "Usa un código de país ISO de dos letras" });
    }

    let accountId = connectedAccounts.get(creatorRef);
    if (!accountId) {
      const account = await stripe.accounts.create({
        type: "express",
        country,
        email,
        capabilities: {
          card_payments: { requested: true },
          transfers: { requested: true }
        },
        metadata: { blockforge_creator_ref: creatorRef, environment: "sandbox" }
      });
      accountId = account.id;
      connectedAccounts.set(creatorRef, accountId);
    }

    const link = await stripe.accountLinks.create({
      account: accountId,
      refresh_url: baseUrl + "/connect/refresh",
      return_url: baseUrl + "/connect/return",
      type: "account_onboarding"
    });
    return res.json({ mode: "test", accountId, onboardingUrl: link.url });
  } catch (err) {
    console.error("Connect onboarding error:", err.message);
    return res.status(502).json({ error: "Stripe test onboarding could not be created" });
  }
});

app.post("/api/checkout", requireSandboxToken, async (req, res) => {
  try {
    const { productId, creatorRef, requestId } = req.body || {};
    if (typeof requestId !== "string" || !/^[a-f0-9-]{36}$/i.test(requestId)) {
      return res.status(400).json({ error: "requestId UUID requerido para idempotencia" });
    }
    const product = typeof productId === "string" ? products[productId] : null;
    const accountId = typeof creatorRef === "string" ? connectedAccounts.get(creatorRef) : null;

    if (!product || !Number.isSafeInteger(product.amount_cad) || product.amount_cad < 100) {
      return res.status(400).json({ error: "Producto de prueba no configurado en el servidor" });
    }
    if (!accountId || product.connected_account_id !== accountId) {
      return res.status(400).json({ error: "El creador no tiene una cuenta de prueba vinculada a este producto" });
    }
    if (!successUrl.startsWith("https://") && !successUrl.startsWith("http://localhost")) {
      return res.status(500).json({ error: "SUCCESS_URL debe ser HTTPS (excepto localhost)" });
    }
    if (!cancelUrl.startsWith("https://") && !cancelUrl.startsWith("http://localhost")) {
      return res.status(500).json({ error: "CANCEL_URL debe ser HTTPS (excepto localhost)" });
    }

    const amount = product.amount_cad;
    const applicationFee = Math.floor(amount * 0.05);
    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      line_items: [{
        price_data: {
          currency: "cad",
          unit_amount: amount,
          product_data: { name: product.name || productId }
        },
        quantity: 1
      }],
      payment_intent_data: {
        application_fee_amount: applicationFee,
        transfer_data: { destination: accountId }
      },
      success_url: successUrl + "?session_id={CHECKOUT_SESSION_ID}",
      cancel_url: cancelUrl,
      metadata: { blockforge_product_id: productId, creator_ref: creatorRef }
    }, { idempotencyKey: "bf_test_" + requestId });

    return res.json({
      mode: "test",
      checkoutUrl: session.url,
      amountCad: amount,
      creatorShareBeforeOtherFeesCad: amount - applicationFee,
      blockForgeFeeCad: applicationFee
    });
  } catch (err) {
    console.error("Checkout error:", err.message);
    return res.status(502).json({ error: "Stripe test checkout could not be created" });
  }
});

app.get("/success", (_req, res) => res.type("text").send("Pago de prueba completado. No se ha realizado ningún cobro real."));
app.get("/cancel", (_req, res) => res.type("text").send("Pago de prueba cancelado."));
app.get("/connect/refresh", (_req, res) => res.type("text").send("Vuelve a iniciar el onboarding desde BlockForge."));
app.get("/connect/return", (_req, res) => res.type("text").send("Onboarding de prueba devuelto. El backend debe consultar el estado de la cuenta."));

app.listen(port, () => console.log(`BlockForge Stripe sandbox listening on port ${port}; test mode only.`));
