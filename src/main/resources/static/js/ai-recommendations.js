import { api, appConfig } from "./api.js";

const escape = (value) => String(value ?? "").replace(/[&<>"']/g,
  (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

export async function mountAiRecommendations() {
  let config;
  try { config = await appConfig(); } catch { return; }
  const anchor = document.querySelector("#main .weekly-goal") || document.querySelector("#main .stats");
  if (!config?.ai?.enabled || !anchor) return;
  const root = document.createElement("section");
  root.className = "panel ai-recommendations";
  root.innerHTML = `<div class="section-heading"><div><span class="eyebrow">GỢI Ý TỪ AI</span><h2>Nên học gì tiếp theo?</h2><p>AI dựa vào khóa bạn đang học, khóa quan tâm và kết quả kiểm tra đầu vào để chọn tối đa 3 khóa đang mở.</p></div></div><form><label>Mục tiêu của bạn (không bắt buộc)<input name="goal" maxlength="500" placeholder="Ví dụ: Đi thực tập backend trong 6 tháng"></label><button class="btn secondary compact">Gợi ý khóa học</button></form><p role="status" aria-live="polite"></p><div class="ai-recommendation-list"></div>`;
  anchor.after(root);
  const form = root.querySelector("form"), status = root.querySelector("[role=status]"), list = root.querySelector(".ai-recommendation-list");
  form.onsubmit = async (event) => {
    event.preventDefault();
    const button = form.querySelector("button");
    if (button.disabled) return;
    button.disabled = true;
    status.textContent = "AI đang chọn khóa học phù hợp…";
    try {
      const courses = await api("/ai/course-recommendations", "POST", { goal: form.elements.goal.value.trim() });
      list.innerHTML = courses.map((c) => `<article><span class="badge muted">${escape(c.category)} · ${escape(c.level)}</span><h3>${escape(c.title)}</h3><p>${escape(c.reason)}</p><a class="text-link" href="/course-detail.html?id=${Number(c.courseId)}">Xem khóa học →</a></article>`).join("");
      status.textContent = "Gợi ý chỉ mang tính tham khảo; xem chi tiết khóa trước khi đăng ký.";
    } catch (error) {
      status.textContent = error.message;
    } finally {
      button.disabled = false;
    }
  };
}
