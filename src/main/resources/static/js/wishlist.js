import { api } from "./api.js";

const heart = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8z"/></svg>';
const escape = value => String(value ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const revisions = new Map(), busy = new Set();
const intentKey = "course-management:wishlist-intent";
let getUser = () => null, notify = () => {};

export function wishlistButton(course, { detail = false, saved = false } = {}) {
  if (getUser() && getUser().role !== "STUDENT") return "";
  const loading = getUser()?.role === "STUDENT" && !saved;
  return `<button type="button" class="wishlist-toggle ${detail ? "wishlist-detail btn secondary full" : "wishlist-card"}" data-wishlist-id="${course.courseId}" data-course-title="${escape(course.title)}" data-saved="${loading ? "unknown" : saved}" ${loading ? "disabled" : ""} aria-pressed="${saved}" aria-label="${saved ? "Bỏ lưu" : "Lưu khóa"}: ${escape(course.title)}">${heart}<span${detail ? "" : ' class="sr-only"'}>${saved ? "Đã lưu khóa" : "Lưu khóa quan tâm"}</span></button>`;
}

function buttons(root = document) { return [...root.querySelectorAll("[data-wishlist-id]")]; }
function update(button, saved) {
  button.dataset.saved = String(saved);
  button.setAttribute("aria-pressed", String(saved));
  button.setAttribute("aria-label", `${saved ? "Bỏ lưu" : "Lưu khóa"}: ${button.dataset.courseTitle}`);
  button.querySelector("span").textContent = saved ? "Đã lưu khóa" : "Lưu khóa quan tâm";
  button.removeAttribute("title");
}
function paint(id, saved) {
  for (const button of buttons()) if (Number(button.dataset.wishlistId) === id) update(button, saved);
}
function clearIntent(id) {
  try { if (sessionStorage.getItem(intentKey) === String(id)) sessionStorage.removeItem(intentKey); } catch {}
}

export async function hydrateWishlist(root = document) {
  if (getUser()?.role !== "STUDENT") return;
  const nodes = buttons(root), ids = [...new Set(nodes.map(b => Number(b.dataset.wishlistId)))];
  if (!ids.length) return;
  const versions = new Map(ids.map(id => [id, revisions.get(id) || 0]));
  nodes.forEach(b => { b.disabled = true; });
  try {
    const saved = new Set(await api("/wishlist/status?" + new URLSearchParams({ ids: ids.join(",") })));
    for (const button of nodes) {
      const id = Number(button.dataset.wishlistId);
      if (button.isConnected && versions.get(id) === (revisions.get(id) || 0)) update(button, saved.has(id));
    }
  } catch (error) {
    for (const button of nodes) {
      if ((revisions.get(Number(button.dataset.wishlistId)) || 0) !== versions.get(Number(button.dataset.wishlistId))) continue;
      button.dataset.saved = "unknown";
      button.title = "Chưa tải được trạng thái lưu. Bấm để thử lại.";
    }
    notify("Chưa tải được trạng thái khóa quan tâm. Bấm nút lưu để thử lại.", true);
  } finally {
    nodes.forEach(b => { b.disabled = busy.has(Number(b.dataset.wishlistId)); });
  }
}

async function change(id, saved) {
  revisions.set(id, (revisions.get(id) || 0) + 1);
  await api("/wishlist/" + id, saved ? "PUT" : "DELETE");
  revisions.set(id, (revisions.get(id) || 0) + 1);
  paint(id, saved);
  clearIntent(id);
  notify(saved ? "Đã lưu vào khóa quan tâm" : "Đã bỏ lưu khóa học");
  document.dispatchEvent(new CustomEvent("wishlist:changed", { detail: { courseId: id, saved } }));
}

export function initWishlist(user, toast) {
  getUser = user;
  notify = toast;
  document.addEventListener("click", async event => {
    const button = event.target.closest?.("[data-wishlist-id]");
    if (!button || button.disabled) return;
    const id = Number(button.dataset.wishlistId);
    if (!getUser()) {
      try { sessionStorage.setItem(intentKey, String(id)); } catch {}
      const next = `/course-detail.html?id=${id}`;
      location.href = "/login.html?" + new URLSearchParams({ next });
      return;
    }
    if (getUser().role !== "STUDENT" || busy.has(id)) return;
    busy.add(id);
    buttons().filter(b => Number(b.dataset.wishlistId) === id).forEach(b => { b.disabled = true; });
    button.setAttribute("aria-busy", "true");
    try {
      let saved = button.dataset.saved === "true";
      if (button.dataset.saved === "unknown") {
        saved = (await api("/wishlist/status?ids=" + id)).includes(id);
        paint(id, saved);
      }
      await change(id, !saved);
    } catch (error) {
      notify(error.message, true);
      // The response may have been lost after a successful write; reconcile before toggling again.
      buttons().filter(b => Number(b.dataset.wishlistId) === id).forEach(b => { b.dataset.saved = "unknown"; });
      if (error.status === 401) location.href = "/login.html?" + new URLSearchParams({ next: location.pathname + location.search + location.hash });
    } finally {
      busy.delete(id);
      buttons().filter(b => Number(b.dataset.wishlistId) === id).forEach(b => { b.disabled = false; b.removeAttribute("aria-busy"); });
    }
  });
}

export async function resumeWishlistIntent() {
  if (!getUser() || location.pathname !== "/course-detail.html") return;
  let pending;
  try { pending = sessionStorage.getItem(intentKey); } catch { return; }
  const id = Number(pending), current = Number(new URLSearchParams(location.search).get("id"));
  if (!Number.isSafeInteger(id) || id < 1 || id !== current) return;
  if (getUser().role !== "STUDENT") {
    clearIntent(id);
    notify("Khóa quan tâm dành cho tài khoản học viên.", true);
    return;
  }
  if (busy.has(id)) return;
  busy.add(id);
  buttons().filter(b => Number(b.dataset.wishlistId) === id).forEach(b => { b.disabled = true; b.setAttribute("aria-busy", "true"); });
  try { await change(id, true); }
  catch (error) {
    clearIntent(id);
    notify("Chưa lưu được khóa vừa chọn: " + error.message + ". Bạn có thể bấm Lưu khóa quan tâm để thử lại.", true);
  }
  finally {
    busy.delete(id);
    buttons().filter(b => Number(b.dataset.wishlistId) === id).forEach(b => { b.disabled = false; b.removeAttribute("aria-busy"); });
  }
}
