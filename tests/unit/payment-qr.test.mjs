import { test } from "node:test";
import assert from "node:assert/strict";
import { paymentQrUrl } from "../../src/main/resources/static/js/payment-qr.js";

const bank = { bankBin: "970415", accountNumber: "001133666888", accountHolder: "NGUYEN VAN A & B" };
const payment = { status: "PENDING", amount: 790000, transferNote: "TT000123" };

test("QR preserves the payment reference, amount and account leading zeros", () => {
  const url = new URL(paymentQrUrl(bank, { ...payment, amount: "790000.00", coursePrice: 999999 }));
  assert.equal(url.origin, "https://img.vietqr.io");
  assert.equal(url.pathname, "/image/970415-001133666888-qr_only.png");
  assert.equal(url.searchParams.get("amount"), "790000");
  assert.equal(url.searchParams.get("addInfo"), "TT000123");
  assert.equal(url.searchParams.get("accountName"), bank.accountHolder);
  assert.equal(url.searchParams.size, 3);
});

test("manual payment remains available when QR configuration is missing or invalid", () => {
  for (const invalid of [{ bankBin: "" }, { bankBin: null }, { bankBin: "97041" },
    { bankBin: "../../evil" }, { accountNumber: "DEMO-000001" },
    { accountNumber: "123?amount=1" }, { accountNumber: "1".repeat(20) }]) {
    assert.equal(paymentQrUrl({ ...bank, ...invalid }, payment), null);
  }
});

test("QR never rounds money, invents a reference or suggests paying a processed request", () => {
  for (const invalid of [{ amount: 0 }, { amount: -1 }, { amount: 790000.25 },
    { amount: 10000000000000 }, { amount: "invalid" }, { transferNote: "" },
    { transferNote: "TT123&amount=1" }, { status: "CONFIRMED" }, { status: "REJECTED" }]) {
    assert.equal(paymentQrUrl(bank, { ...payment, ...invalid }), null);
  }
});
