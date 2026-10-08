import { api } from "./api.js";
import { enhanceForms } from "./experience.js";

const escape = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const drafts = new Map();
const date = (value) => new Date(value).toLocaleString("vi-VN");

// Each mount owns its requests and DOM, so an old lesson cannot overwrite a new one.
export function mountQuestions(root, lessonId, manager, questionId = null) {
  let current = 0,
    revision = 0;
  const draftKey = `${lessonId}:${manager ? "manager" : "student"}`;
  root.innerHTML = `<div class="section-heading"><div><h3>Hỏi đáp theo bài</h3><p>${manager ? "Trả lời thắc mắc của học viên. Câu hỏi bị ẩn chỉ hiển thị với người quản lý khóa." : "Trao đổi với giảng viên và học viên trong khóa. Mọi học viên đã đăng ký đều có thể đọc câu hỏi."}</p></div></div>${manager ? "" : `<form class="question-form"><label>Câu hỏi của bạn<textarea name="body" rows="3" required maxlength="5000" placeholder="Nêu phần bạn chưa hiểu và những gì đã thử…">${escape(drafts.get(draftKey) || "")}</textarea></label><button class="btn compact" type="submit">Gửi câu hỏi</button></form>`}<p class="question-status" role="status" aria-live="polite"></p><div class="question-list"></div><div class="question-pages"></div>`;
  const list = root.querySelector(".question-list");
  const pages = root.querySelector(".question-pages");
  const status = root.querySelector(".question-status");
  const active = () => root.isConnected;
  function article(q, targeted = false) {
    return `<article class="lesson-question ${q.hidden ? "question-hidden" : ""} ${targeted ? "question-target" : ""}" data-question="${q.questionId}"><div class="question-meta"><strong>${escape(q.studentName)}</strong><small>${escape(date(q.createdAt))}</small><span class="badge ${q.hidden ? "muted" : q.answer ? "good" : "warm"}">${q.hidden ? "Đã ẩn" : q.answer ? "Đã phản hồi" : "Chờ phản hồi"}</span></div><p class="question-body">${escape(q.body)}</p>${q.answer ? `<div class="question-answer"><strong>Phản hồi · ${escape(q.answeredByName)}</strong><p>${escape(q.answer)}</p><small>${escape(date(q.answeredAt))}</small></div>` : ""}${manager ? `<div class="question-tools">${!q.hidden ? `<form class="answer-form"><label>Phản hồi của giảng viên<textarea name="body" rows="3" required maxlength="5000">${escape(drafts.get(`answer:${q.questionId}`) ?? q.answer ?? "")}</textarea></label><button class="btn secondary compact" type="submit">${q.answer ? "Cập nhật phản hồi" : "Gửi phản hồi"}</button></form>` : ""}<button class="text-link" type="button" data-hide="${q.hidden ? "false" : "true"}">${q.hidden ? "Hiện lại câu hỏi" : "Ẩn câu hỏi"}</button></div>` : ""}</article>`;
  }
  async function load() {
    const request = ++revision;
    list.setAttribute("aria-busy", "true");
    pages.innerHTML = "";
    try {
      const data = await api(
        `/lessons/${lessonId}/questions?page=${current}&size=10`,
      );
      if (!active() || request !== revision) return;
      if (current > 0 && current >= data.totalPages) {
        current = Math.max(0, data.totalPages - 1);
        return load();
      }
      let target = null,
        targetError = "";
      if (questionId) {
        target = data.content.find((q) => q.questionId === questionId);
        if (!target) {
          try {
            target = await api(`/questions/${questionId}`);
            if (target.lessonId !== lessonId) target = null;
          } catch (error) {
            targetError = error.message;
          }
        }
      }
      if (!active() || request !== revision) return;
      list.innerHTML =
        (targetError
          ? `<p class="notice" role="alert">${escape(targetError)}</p>`
          : "") +
        (target && !data.content.some((q) => q.questionId === target.questionId)
          ? article(target, true)
          : "") +
        data.content
          .map((q) => article(q, q.questionId === questionId))
          .join("") +
        (!data.content.length && !target
          ? '<p class="muted-text">Chưa có câu hỏi cho bài này.</p>'
          : "");
      pages.innerHTML = `<span>${data.totalElements} câu hỏi · Trang ${current + 1}/${Math.max(1, data.totalPages)}</span>${current > 0 ? '<button class="btn secondary compact" data-page="previous">← Trước</button>' : ""}${current + 1 < data.totalPages ? '<button class="btn secondary compact" data-page="next">Sau →</button>' : ""}`;
      pages.querySelectorAll("[data-page]").forEach(
        (b) =>
          (b.onclick = () => {
            current += b.dataset.page === "next" ? 1 : -1;
            questionId = null;
            load();
          }),
      );
      list.querySelectorAll("[data-question]").forEach((node) => {
        const id = Number(node.dataset.question);
        const form = node.querySelector(".answer-form");
        if (form) {
          form.elements.body.oninput = () =>
            drafts.set(`answer:${id}`, form.elements.body.value);
          form.onsubmit = (event) => {
            event.preventDefault();
            write(
              form.querySelector("button"),
              `/questions/${id}/answer`,
              "PUT",
              { body: form.elements.body.value },
              () => drafts.delete(`answer:${id}`),
              "Đã lưu phản hồi và gửi thông báo cho học viên.",
            );
          };
        }
        const hide = node.querySelector("[data-hide]");
        if (hide)
          hide.onclick = () =>
            write(
              hide,
              `/questions/${id}/visibility`,
              "PUT",
              { hidden: hide.dataset.hide === "true" },
              () => {},
              "Đã cập nhật hiển thị câu hỏi.",
            );
      });
      enhanceForms(list);
      if (questionId)
        list
          .querySelector(".question-target")
          ?.scrollIntoView({ block: "center" });
    } catch (error) {
      if (!active() || request !== revision) return;
      list.innerHTML = `<p class="notice error" role="alert">${escape(error.message)}</p><button class="btn secondary compact" data-retry>Thử lại hỏi đáp</button>`;
      list.querySelector("[data-retry]").onclick = load;
    } finally {
      if (active() && request === revision)
        list.setAttribute("aria-busy", "false");
    }
  }
  let writing = false;
  async function write(button, path, method, data, success, message) {
    if (writing) return;
    if ("body" in data && !data.body?.trim()) {
      status.textContent =
        "Hãy nhập nội dung trước khi gửi; không dùng chỉ khoảng trắng.";
      return;
    }
    writing = true;
    button.disabled = true;
    const input = button.closest("form")?.elements.body;
    if (input) input.disabled = true;
    status.textContent = "Đang lưu…";
    try {
      await api(path, method, data);
      success();
      if (!active()) return;
      status.textContent = message;
      await load();
    } catch (error) {
      if (active())
        status.textContent =
          "Chưa lưu được: " +
          error.message +
          ". Nội dung vẫn được giữ để thử lại.";
    } finally {
      writing = false;
      button.disabled = false;
      if (input) input.disabled = false;
    }
  }
  const form = root.querySelector(".question-form");
  enhanceForms(root);
  if (form) {
    form.elements.body.oninput = () =>
      drafts.set(draftKey, form.elements.body.value);
    form.onsubmit = (event) => {
      event.preventDefault();
      write(
        form.querySelector("button"),
        `/lessons/${lessonId}/questions`,
        "POST",
        { body: form.elements.body.value },
        () => {
          drafts.delete(draftKey);
          form.elements.body.value = "";
          current = 0;
          questionId = null;
        },
        "Đã gửi câu hỏi và thông báo cho giảng viên.",
      );
    };
  }
  load();
}
