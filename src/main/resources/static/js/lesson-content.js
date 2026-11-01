import { api, appConfig } from "./api.js";
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
function safe(value) {
  try {
    const u = new URL(value, location.origin);
    if (!["http:", "https:"].includes(u.protocol) || u.username || u.password) return null;
    if (!/^https?:\/\//i.test(value) && !(u.origin === location.origin && /^\/api\/lesson-resources\/\d+\/content$/.test(u.pathname) && !u.search && !u.hash)) return null;
    return u;
  } catch { return null; }
}
function inline(text) {
  const tokens = /!\[([^\]\n]*)\]\(([^\s)]+)\)|\[([^\]\n]+)\]\(([^\s)]+)\)|`([^`\n]+)`|\*\*([^*\n]+)\*\*/g;
  let html = "", last = 0;
  for (const m of text.matchAll(tokens)) {
    html += esc(text.slice(last, m.index));
    if (m[2]) {
      const url = safe(m[2]);
      html += url ? `<img src="${esc(url.href)}" alt="${esc(m[1])}" loading="lazy" referrerpolicy="no-referrer">` : esc(m[0]);
    } else if (m[4]) {
      const url = safe(m[4]);
      html += url ? `<a href="${esc(url.href)}" target="_blank" rel="noopener noreferrer">${esc(m[3])}</a>` : esc(m[0]);
    } else html += m[5] ? `<code>${esc(m[5])}</code>` : `<strong>${esc(m[6])}</strong>`;
    last = m.index + m[0].length;
  }
  return html + esc(text.slice(last));
}
export function markdown(text) {
  const lines = String(text || "").replace(/\r\n?/g, "\n").split("\n");
  let html = "", paragraph = [], list = "", code = null;
  const flush = () => { if (paragraph.length) { html += `<p>${paragraph.map(inline).join("<br>")}</p>`; paragraph = []; } };
  const closeList = () => { if (list) { html += `</${list}>`; list = ""; } };
  for (const line of lines) {
    if (/^```/.test(line)) {
      flush(); closeList();
      if (code !== null) { html += `<pre><code>${esc(code.join("\n"))}</code></pre>`; code = null; }
      else code = [];
    } else if (code !== null) code.push(line);
    else if (!line.trim()) { flush(); closeList(); }
    else if (/^#{1,3} /.test(line)) {
      flush(); closeList();
      const level = line.match(/^#+/)[0].length + 2;
      html += `<h${level}>${inline(line.replace(/^#+ /, ""))}</h${level}>`;
    } else if (/^(?:[-*] |\d+\. )/.test(line)) {
      flush(); const type = /^\d/.test(line) ? "ol" : "ul";
      if (list !== type) { closeList(); list = type; html += `<${type}>`; }
      html += `<li>${inline(line.replace(/^(?:[-*] |\d+\. )/, ""))}</li>`;
    } else { closeList(); paragraph.push(line); }
  }
  flush(); closeList();
  if (code !== null) html += `<pre><code>${esc(code.join("\n"))}</code></pre>`;
  return html;
}
// AI answers use the lesson Markdown subset. Images the model names become plain links so an
// answer never makes the browser load a URL by itself; indented fences and list items are unindented.
export function aiMarkdown(text) {
  return markdown(String(text ?? "").replace(/!\[/g, "[").replace(/^[ \t]+(?=```|[-*] |\d+\. )/gm, "")
    .replace(/^([-*]|\d+\.)[ \t]+/gm, "$1 "));
}
function videoStart(url) {
  const value = url.searchParams.get("start") ?? url.searchParams.get("t") ?? (url.hash.startsWith("#t=") ? url.hash.slice(3) : "");
  let seconds = 0;
  if (/^\d+s?$/.test(value)) seconds = Number(value.replace(/s$/, ""));
  else {
    const parts = value.match(/^(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$/);
    if (parts) seconds = Number(parts[1] || 0) * 3600 + Number(parts[2] || 0) * 60 + Number(parts[3] || 0);
  }
  return Number.isSafeInteger(seconds) && seconds > 0 && seconds <= 604800 ? seconds : 0;
}
export function video(value) {
  const url = safe(value);
  if (!url) return "";
  let id;
  if (["www.youtube.com", "youtube.com", "m.youtube.com", "www.youtube-nocookie.com", "youtube-nocookie.com"].includes(url.hostname)) {
    id = url.pathname === "/watch" ? url.searchParams.get("v") : url.pathname.match(/^\/(?:embed|shorts)\/([^/]+)$/)?.[1];
  } else if (url.hostname === "youtu.be") id = url.pathname.slice(1);
  if (/^[a-zA-Z0-9_-]{11}$/.test(id || "")) {
    const start = videoStart(url);
    const embed = `https://www.youtube-nocookie.com/embed/${id}${start ? `?start=${start}` : ""}`;
    const original = `https://www.youtube.com/watch?v=${id}${start ? `&amp;t=${start}s` : ""}`;
    return `<div class="lesson-video"><iframe title="Video bài học" src="${embed}" loading="lazy" referrerpolicy="strict-origin-when-cross-origin" allow="fullscreen; picture-in-picture" allowfullscreen></iframe></div><p class="hint">Nguồn video: YouTube · Nếu video không phát tại đây, <a href="${original}" target="_blank" rel="noopener noreferrer">mở trên YouTube ↗</a>.</p>`;
  }
  if (/\.(mp4|webm)$/i.test(url.pathname))
    return `<video class="lesson-player" controls preload="metadata" aria-label="Video bài học" src="${esc(url.href)}"></video><p class="hint">Nội dung văn bản của bài học có thể dùng để ôn lại video.</p>`;
  return `<p><a href="${esc(url.href)}" target="_blank" rel="noopener noreferrer">Mở video của bài học ↗</a></p>`;
}
export async function mountResources(root, lessonId, manager = false, insertImage) {
  root.setAttribute("aria-busy", "true");
  root.innerHTML = '<p class="hint" role="status">Đang tải tài liệu đính kèm…</p>';
  let limits, busy = false;
  const begin = message => {
    if (busy || !root.isConnected) return false;
    busy = true; root.dataset.pending = "true"; root.setAttribute("aria-busy", "true");
    root.querySelectorAll("button,input").forEach(control => control.disabled = true);
    root.querySelector("[data-status]").textContent = message;
    return true;
  };
  const end = () => { busy = false; root.dataset.pending = "false"; root.setAttribute("aria-busy", "false"); };
  const recover = error => {
    if (!root.isConnected) return;
    const status = root.querySelector("[data-status]");
    status.textContent = `${error.message} Tải lại danh sách để kiểm tra kết quả trước khi thao tác tiếp.`;
    const retry = document.createElement("button");
    retry.type = "button"; retry.className = "text-link"; retry.textContent = "Thử tải lại tài liệu";
    retry.onclick = () => mountResources(root, lessonId, manager, insertImage);
    status.after(retry);
  };
  const sizeLabel = () => {
    const units = ["byte", "KB", "MB", "GB"];
    let size = limits.maxFileBytes, unit = 0;
    while (size >= 1024 && unit < units.length - 1) { size /= 1024; unit++; }
    return `${size.toLocaleString("vi-VN", {maximumFractionDigits: 2})} ${units[unit]}`;
  };
  const render = (items) => {
    if (!root.isConnected) return;
    root.innerHTML = `<h3>Tài liệu đính kèm</h3>${manager ? `<p class="hint">PDF, TXT, PNG, JPG, WebP · Tối đa ${sizeLabel()}/file, ${limits.maxResourcesPerLesson} file/bài. Tải lên và xóa tài liệu có hiệu lực ngay.</p><label>Tải tài liệu<input type="file" accept=".pdf,.txt,.png,.jpg,.jpeg,.webp" data-upload ${items.length >= limits.maxResourcesPerLesson ? "disabled" : ""}></label>` : '<p class="hint">Mở tài liệu để xem hoặc tải xuống và thực hành cùng bài học.</p>'}<div class="resource-items">${items.map(r => `<div class="resource-item"><a href="/api/lesson-resources/${r.resourceId}/content" target="_blank" rel="noopener noreferrer">${esc(r.name)}</a><small>${Math.ceil(r.size / 1024)} KB</small>${manager ? `<button type="button" class="text-link danger" data-remove="${r.resourceId}">Xóa</button>${r.mediaType.startsWith("image/") ? `<button type="button" class="text-link" data-image="${r.resourceId}">Chèn ảnh vào bài</button>` : ""}` : ""}</div>`).join("") || '<p class="muted-text">Chưa có tài liệu đính kèm.</p>'}</div><p data-status role="status"></p>`;
    if (!manager) return;
    root.querySelector("[data-upload]").onchange = async (e) => {
      const file = e.target.files[0];
      if (!file || busy) return;
      const status = root.querySelector("[data-status]");
      if (file.size > limits.maxFileBytes) { status.textContent = `Mỗi tài liệu tối đa ${sizeLabel()}`; e.target.value = ""; return; }
      const body = new FormData(); body.append("file", file);
      if (!begin("Đang tải tài liệu…")) return;
      try { render(await api(`/lessons/${lessonId}/resources`, "POST", body)); }
      catch (error) { e.target.value = ""; recover(error); }
      finally { end(); }
    };
    root.querySelectorAll("[data-remove]").forEach(button => { button.onclick = async () => {
      if (busy || !window.confirm("Xóa tài liệu này? Ảnh đã chèn từ tài liệu sẽ không còn hiển thị.")) return;
      if (!begin("Đang xóa tài liệu…")) return;
      try { await api(`/lesson-resources/${button.dataset.remove}`, "DELETE"); render(items.filter(item => String(item.resourceId) !== button.dataset.remove)); }
      catch (error) { recover(error); }
      finally { end(); }
    }; });
    root.querySelectorAll("[data-image]").forEach(button => { button.onclick = () => { if (!busy) insertImage?.(`![Mô tả ảnh](/api/lesson-resources/${button.dataset.image}/content)`); }; });
  };
  try {
    limits = (await appConfig()).learning;
    render(await api(`/lessons/${lessonId}/resources`));
  }
  catch (error) { if (root.isConnected) { root.innerHTML = `<p role="alert">${esc(error.message)}</p><button type="button" class="text-link">Thử tải lại tài liệu</button>`; root.querySelector("button").onclick = () => mountResources(root, lessonId, manager, insertImage); } }
  finally { root.setAttribute("aria-busy", "false"); }
}
