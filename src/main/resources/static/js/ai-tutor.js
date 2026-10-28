import { api, appConfig } from "./api.js";

const escape = (value) => String(value ?? "").replace(/[&<>"']/g,
  (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
// Conversations stay in memory per lesson; they are not stored on the server.
const conversations = new Map();

export async function mountAiTutor(root, lessonId) {
  let config;
  try { config = await appConfig(); } catch { return; }
  if (!config?.ai?.enabled || !root.isConnected) return;
  const turns = conversations.get(lessonId) || [];
  conversations.set(lessonId, turns);
  let busy = false;
  root.innerHTML = `<div class="section-heading"><div><h3>Trợ giảng AI</h3><p>Hỏi nhanh về nội dung bài này. AI chỉ dựa vào phần văn bản của bài, có thể sai; câu hỏi cần giảng viên hãy đăng ở mục Hỏi đáp.</p></div><span class="badge muted">Thử nghiệm</span></div><div class="ai-tutor-log" aria-live="polite"></div><form class="ai-tutor-form"><label>Câu hỏi cho trợ giảng AI<textarea name="question" rows="2" required maxlength="2000" placeholder="Ví dụ: Giải thích lại ý chính của bài bằng ví dụ đơn giản"></textarea></label><div class="note-actions"><span class="ai-tutor-status" role="status" aria-live="polite"></span><button class="btn secondary compact" type="button" data-summary>Tóm tắt bài</button><button class="btn secondary compact" type="submit">Hỏi AI</button></div></form>`;
  const log = root.querySelector(".ai-tutor-log");
  const form = root.querySelector("form");
  const status = root.querySelector(".ai-tutor-status");
  const draw = () => {
    log.innerHTML = turns.map((t) => `<div class="ai-turn ${t.fromAssistant ? "ai-turn-assistant" : "ai-turn-user"}"><strong>${t.fromAssistant ? "Trợ giảng AI" : "Bạn"}</strong><p>${escape(t.text)}</p></div>`).join("");
    log.hidden = !turns.length;
    log.scrollTop = log.scrollHeight;
  };
  draw();
  form.querySelector("[data-summary]").onclick = async () => {
    if (busy) return;
    busy = true;
    form.querySelectorAll("textarea,button").forEach((n) => (n.disabled = true));
    status.textContent = "AI đang tóm tắt bài…";
    try {
      const data = await api(`/lessons/${lessonId}/ai/summary`, "POST", {});
      turns.push({ fromAssistant: false, text: "Tóm tắt bài này giúp mình." }, { fromAssistant: true, text: data.answer });
      if (!root.isConnected) return;
      status.textContent = "";
      draw();
    } catch (error) {
      if (root.isConnected) status.textContent = error.message;
    } finally {
      busy = false;
      form.querySelectorAll("textarea,button").forEach((n) => (n.disabled = false));
    }
  };
  form.onsubmit = async (event) => {
    event.preventDefault();
    const question = form.elements.question.value.trim();
    if (busy) return;
    if (!question) { status.textContent = "Hãy nhập câu hỏi trước khi gửi."; return; }
    busy = true;
    form.querySelectorAll("textarea,button").forEach((n) => (n.disabled = true));
    status.textContent = "AI đang trả lời…";
    try {
      const data = await api(`/lessons/${lessonId}/ai/tutor`, "POST", { question, history: turns.slice(-6) });
      turns.push({ fromAssistant: false, text: question }, { fromAssistant: true, text: data.answer });
      if (!root.isConnected) return;
      form.elements.question.value = "";
      status.textContent = "";
      draw();
    } catch (error) {
      if (root.isConnected) status.textContent = error.message + ". Câu hỏi vẫn được giữ để thử lại.";
    } finally {
      busy = false;
      form.querySelectorAll("textarea,button").forEach((n) => (n.disabled = false));
    }
  };
}
