import { test } from "node:test";
import assert from "node:assert/strict";

let sequence = 0;
async function setup(user = { role: "STUDENT" }) {
  const nodes = [], handlers = new Map(), notices = [], storage = new Map(), calls = [];
  globalThis.document = {
    querySelectorAll: () => nodes,
    addEventListener: (name, callback) => handlers.set(name, callback),
    dispatchEvent: () => {},
  };
  globalThis.CustomEvent = class { constructor(type, options) { this.type = type; this.detail = options.detail; } };
  globalThis.location = { pathname: "/courses.html", search: "", hash: "", href: "" };
  globalThis.sessionStorage = { getItem: key => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value), removeItem: key => storage.delete(key) };
  let respond = async () => [];
  globalThis.fetch = async (path, options = {}) => {
    calls.push([path, options.method]);
    const data = path === "/api/auth/csrf" ? { headerName: "X-CSRF-TOKEN", token: "test" } : await respond(path, options);
    return { ok: true, status: 200, json: async () => ({ success: true, data }) };
  };
  const module = await import(`../../src/main/resources/static/js/wishlist.js?case=${++sequence}`);
  module.initWishlist(() => user, (...message) => notices.push(message));
  function button(id = 7, saved = "false") {
    const attrs = new Map(), span = { textContent: "Lưu khóa quan tâm" };
    const node = { dataset: { wishlistId: String(id), courseTitle: "Java", saved }, disabled: false, isConnected: true,
      setAttribute: (key, value) => attrs.set(key, value), removeAttribute: key => attrs.delete(key),
      querySelector: () => span, attrs, span };
    nodes.push(node);
    return node;
  }
  return { module, button, storage, calls, notices, setResponder: callback => { respond = callback; },
    click: node => handlers.get("click")({ target: { closest: () => node } }) };
}

test("guest save keeps the selected course through login, then saves only in the same tab", async () => {
  const guest = await setup(null);
  await guest.click(guest.button(7));
  assert.equal(guest.calls.length, 0);
  const next = new URL(location.href, "http://localhost").searchParams.get("next");
  assert.equal(next, "/course-detail.html?id=7");
  const member = await setup();
  location.pathname = "/course-detail.html"; location.search = "?id=7";
  await member.module.resumeWishlistIntent();
  assert.equal(member.calls.length, 0, "A URL alone cannot cause a save");
  member.storage.set("course-management:wishlist-intent", "7");
  await member.module.resumeWishlistIntent();
  assert.equal(member.calls.filter(([path, method]) => path === "/api/wishlist/7" && method === "PUT").length, 1);
  assert.equal(member.storage.size, 0);
});

test("a stale status response cannot undo a completed save on duplicate buttons", async () => {
  const env = await setup(), first = env.button(), second = env.button();
  let resolveStatus;
  env.setResponder(path => path.includes("/status") ? new Promise(resolve => { resolveStatus = resolve; }) : null);
  const status = env.module.hydrateWishlist();
  // Another already-loaded representation of this course remains available.
  first.disabled = false;
  await env.click(first);
  resolveStatus([]); await status;
  for (const node of [first, second]) {
    assert.equal(node.dataset.saved, "true"); assert.equal(node.attrs.get("aria-pressed"), "true");
    assert.equal(node.disabled, false);
  }
});

test("lost mutation responses reconcile server state before the next toggle", async () => {
  const env = await setup(), node = env.button();
  let savedOnServer = false, lost = true;
  env.setResponder((path, options) => {
    if (path.includes("/status")) return savedOnServer ? [7] : [];
    if (options.method === "PUT") { savedOnServer = true; if (lost) { lost = false; throw new Error("lost response"); } }
    if (options.method === "DELETE") savedOnServer = false;
    return null;
  });
  await env.click(node);
  assert.equal(node.dataset.saved, "unknown"); assert.equal(node.disabled, false);
  await env.click(node);
  assert.equal(savedOnServer, false); assert.equal(node.dataset.saved, "false");
  assert.equal(env.calls.filter(([, method]) => method === "PUT").length, 1);
  assert.equal(env.calls.filter(([, method]) => method === "DELETE").length, 1);
});

test("failed status loading enables retry and does not blindly overwrite an existing saved state", async () => {
  const env = await setup(), node = env.button();
  let failed = false;
  env.setResponder(path => {
    if (path.includes("/status")) { if (!failed) { failed = true; throw new Error("offline"); } return [7]; }
    return null;
  });
  await env.module.hydrateWishlist();
  assert.equal(node.dataset.saved, "unknown"); assert.equal(node.disabled, false);
  await env.click(node);
  assert.equal(env.calls.filter(([, method]) => method === "DELETE").length, 1);
  assert.equal(node.dataset.saved, "false");
});
