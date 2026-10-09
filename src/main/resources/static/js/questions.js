import { api } from "./api.js";
import { enhanceForms } from "./experience.js";

const escape = (value) => String(value ?? "").replace(/[&<>"']/g,
  (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
const drafts = new Map();
const replyDrafts = new Map();
const date = (value) => new Date(value).toLocaleString("vi-VN");

export function replyMarkup(reply, manager) {
  const label = { STUDENT: "Học viên", TEACHER: "Giảng viên", ADMIN: "Quản trị viên" }[reply.authorRole] || "Thành viên";
  const official = ["TEACHER", "ADMIN"].includes(reply.authorRole);
  return `<article class="question-reply ${official ? "reply-official" : ""} ${reply.hidden ? "question-hidden" : ""}" data-reply="${Number(reply.replyId)}"><div class="question-meta"><strong>${escape(reply.authorName)}</strong><span class="badge ${official ? "good" : "muted"}">${label}</span><small>${escape(date(reply.createdAt))}</small>${reply.hidden ? '<span class="badge muted">Đã ẩn</span>' : ""}</div><p class="question-body">${escape(reply.body)}</p>${manager ? `<button class="text-link" type="button" data-hide-reply="${Number(reply.replyId)}" data-hidden="${!reply.hidden}">${reply.hidden ? "Hiện lại phản hồi" : "Ẩn phản hồi"}</button>` : ""}</article>`;
}

// Requests update only their own mounted question/thread. Drafts survive reload failures.
export function mountQuestions(root, lessonId, manager, questionId = null) {
  let current = 0, revision = 0, writing = false, targetShown = false;
  const openThreads = new Set(questionId ? [questionId] : []);
  const threadPages = new Map();
  const draftKey = `${lessonId}:${manager ? "manager" : "student"}`;
  root.innerHTML = `<div class="section-heading"><div><h3>Hỏi đáp theo bài</h3><p>${manager ? "Trao đổi nhiều lượt với học viên. Câu hỏi và phản hồi bị ẩn chỉ hiển thị với người quản lý khóa." : "Đặt câu hỏi, trả lời và học cùng các thành viên trong khóa. Phản hồi của giảng viên có nhãn riêng."}</p><p class="hint">Phản hồi mới thông báo cho người đặt câu hỏi và giảng viên phụ trách, trừ người vừa gửi.</p></div></div>${manager ? "" : `<form class="question-form"><label>Câu hỏi của bạn<textarea name="body" rows="3" required maxlength="5000" placeholder="Nêu phần bạn chưa hiểu và những gì đã thử…">${escape(drafts.get(draftKey) || "")}</textarea></label><button class="btn compact" type="submit">Gửi câu hỏi</button></form>`}<p class="question-status" role="status" aria-live="polite"></p><div class="question-list"></div><div class="question-pages"></div>`;
  const list = root.querySelector(".question-list");
  const pages = root.querySelector(".question-pages");
  const status = root.querySelector(".question-status");
  const active = () => root.isConnected && root.contains(list);

  function article(q) {
    const expanded = openThreads.has(q.questionId);
    return `<article class="lesson-question ${q.hidden ? "question-hidden" : ""} ${q.questionId === questionId ? "question-target" : ""}" data-question="${q.questionId}"><div class="question-meta"><strong>${escape(q.studentName)}</strong><small>${escape(date(q.createdAt))}</small><span class="badge ${q.hidden ? "muted" : q.answer ? "good" : "warm"}">${q.hidden ? "Đã ẩn" : q.answer ? "Giảng viên đã phản hồi" : "Chờ giảng viên"}</span></div><p class="question-body">${escape(q.body)}</p><div class="row-actions"><button type="button" class="text-link" data-thread-toggle aria-expanded="${expanded}" aria-controls="question-thread-${q.questionId}">${expanded ? "Thu gọn" : "Xem"} trao đổi (${q.replyCount})</button>${manager ? `<button class="text-link" type="button" data-hide="${!q.hidden}">${q.hidden ? "Hiện lại câu hỏi" : "Ẩn câu hỏi"}</button>` : ""}</div><section class="question-thread" id="question-thread-${q.questionId}" ${expanded ? "" : "hidden"} aria-label="Trao đổi về câu hỏi của ${escape(q.studentName)}"><p class="reply-status" role="status" aria-live="polite"></p><div class="reply-list"></div><nav class="question-pages reply-pages" aria-label="Phân trang phản hồi"></nav>${q.hidden ? '<p class="hint">Hiện lại câu hỏi để tiếp tục trao đổi.</p>' : `<form class="reply-form"><label>Phản hồi của bạn<textarea name="body" rows="3" required maxlength="5000" placeholder="Chia sẻ cách hiểu hoặc hỏi thêm…">${escape(replyDrafts.get(q.questionId)?.body || "")}</textarea></label><button class="btn secondary compact" type="submit">Gửi phản hồi</button></form>`}</section></article>`;
  }

  function bindQuestion(node) {
    const id = Number(node.dataset.question);
    const thread = node.querySelector(".question-thread");
    const toggle = node.querySelector("[data-thread-toggle]");
    const replyList = thread.querySelector(".reply-list");
    const replyPages = thread.querySelector(".reply-pages");
    const replyStatus = thread.querySelector(".reply-status");
    let threadRevision = 0;
    const threadActive = (request) => active() && node.isConnected && request === threadRevision;
    async function loadThread() {
      const request = ++threadRevision;
      replyList.setAttribute("aria-busy", "true");
      replyStatus.textContent = "Đang tải trao đổi…";
      replyPages.replaceChildren();
      try {
        const page = threadPages.get(id) || 0;
        const data = await api(`/questions/${id}/replies?page=${page}&size=10`);
        if (!threadActive(request)) return;
        if (page > 0 && page >= data.totalPages) {
          threadPages.set(id, Math.max(0, data.totalPages - 1));
          return loadThread();
        }
        replyList.innerHTML = [...data.content].reverse().map(r => replyMarkup(r, manager)).join("");
        replyStatus.textContent = data.totalElements
          ? `${data.totalElements} phản hồi${manager ? " (bao gồm phản hồi bị ẩn)" : ""} · Trang ${page + 1}/${data.totalPages}`
          : "Chưa có phản hồi. Hãy bắt đầu trao đổi.";
        replyPages.innerHTML = `${page + 1 < data.totalPages ? '<button class="btn secondary compact" type="button" data-reply-page="older">← Phản hồi cũ hơn</button>' : ""}${page > 0 ? '<button class="btn secondary compact" type="button" data-reply-page="newer">Phản hồi mới hơn →</button>' : ""}`;
        replyPages.querySelectorAll("[data-reply-page]").forEach(button => {
          button.onclick = () => {
            threadPages.set(id, page + (button.dataset.replyPage === "older" ? 1 : -1));
            loadThread();
          };
        });
        replyList.querySelectorAll("[data-hide-reply]").forEach(button => {
          button.onclick = () => write(button, `/question-replies/${button.dataset.hideReply}/visibility`, "PUT",
            { hidden: button.dataset.hidden === "true" }, () => {}, "Đã cập nhật hiển thị phản hồi.");
        });
      } catch (error) {
        if (!threadActive(request)) return;
        replyStatus.textContent = "Chưa tải được trao đổi: " + error.message;
        replyList.innerHTML = '<button type="button" class="btn secondary compact" data-retry-replies>Thử lại trao đổi</button>';
        replyList.querySelector("button").onclick = loadThread;
      } finally {
        if (threadActive(request)) replyList.setAttribute("aria-busy", "false");
      }
    }
    toggle.onclick = () => {
      const expanded = !openThreads.has(id);
      if (expanded) openThreads.add(id); else openThreads.delete(id);
      thread.hidden = !expanded;
      toggle.setAttribute("aria-expanded", String(expanded));
      toggle.textContent = toggle.textContent.replace(/^(Xem|Thu gọn)/, expanded ? "Thu gọn" : "Xem");
      if (expanded) loadThread();
    };
    const form = node.querySelector(".reply-form");
    if (form) {
      form.elements.body.oninput = () => {
        const body = form.elements.body.value;
        if (replyDrafts.get(id)?.body !== body) replyDrafts.set(id, { body, key: crypto.randomUUID() });
      };
      form.onsubmit = event => {
        event.preventDefault();
        const body = form.elements.body.value;
        const draft = replyDrafts.get(id) || { body, key: crypto.randomUUID() };
        replyDrafts.set(id, draft);
        write(form.querySelector("button"), `/questions/${id}/replies`, "POST",
          { body, clientRequestId: draft.key }, () => {
            replyDrafts.delete(id);
            threadPages.set(id, 0);
          }, "Đã gửi phản hồi.");
      };
    }
    const hide = node.querySelector("[data-hide]");
    if (hide) hide.onclick = () => write(hide, `/questions/${id}/visibility`, "PUT",
      { hidden: hide.dataset.hide === "true" }, () => {}, "Đã cập nhật hiển thị câu hỏi.");
    if (openThreads.has(id)) loadThread();
  }

  async function load() {
    const request = ++revision;
    list.setAttribute("aria-busy", "true");
    pages.replaceChildren();
    try {
      const data = await api(`/lessons/${lessonId}/questions?page=${current}&size=10`);
      if (!active() || request !== revision) return;
      if (current > 0 && current >= data.totalPages) {
        current = Math.max(0, data.totalPages - 1);
        return load();
      }
      let target = null, targetError = "";
      if (questionId) {
        target = data.content.find(q => q.questionId === questionId);
        if (!target) {
          try {
            target = await api(`/questions/${questionId}`);
            if (target.lessonId !== lessonId) target = null;
          } catch (error) { targetError = error.message; }
        }
      }
      if (!active() || request !== revision) return;
      list.innerHTML = (targetError ? `<p class="notice" role="alert">${escape(targetError)}</p>` : "")
        + (target && !data.content.some(q => q.questionId === target.questionId) ? article(target) : "")
        + data.content.map(article).join("")
        + (!data.content.length && !target ? '<p class="muted-text">Chưa có câu hỏi cho bài này.</p>' : "");
      pages.innerHTML = `<span>${data.totalElements} câu hỏi · Trang ${current + 1}/${Math.max(1, data.totalPages)}</span>${current > 0 ? '<button class="btn secondary compact" data-page="previous">← Trước</button>' : ""}${current + 1 < data.totalPages ? '<button class="btn secondary compact" data-page="next">Sau →</button>' : ""}`;
      pages.querySelectorAll("[data-page]").forEach(button => {
        button.onclick = () => {
          current += button.dataset.page === "next" ? 1 : -1;
          questionId = null;
          load();
        };
      });
      list.querySelectorAll("[data-question]").forEach(bindQuestion);
      enhanceForms(list);
      if (questionId && !targetShown) {
        list.querySelector(".question-target")?.scrollIntoView({ block: "center" });
        targetShown = true;
      }
    } catch (error) {
      if (!active() || request !== revision) return;
      list.innerHTML = `<p class="notice error" role="alert">${escape(error.message)}</p><button class="btn secondary compact" data-retry>Thử lại hỏi đáp</button>`;
      list.querySelector("[data-retry]").onclick = load;
    } finally {
      if (active() && request === revision) list.setAttribute("aria-busy", "false");
    }
  }

  async function write(button, path, method, data, success, message) {
    if (writing) return;
    if ("body" in data && !data.body?.trim()) {
      status.textContent = "Hãy nhập nội dung trước khi gửi; không dùng chỉ khoảng trắng.";
      return;
    }
    writing = true;
    button.disabled = true;
    const input = button.closest("form")?.elements.body;
    const question = Number(button.closest("[data-question]")?.dataset.question);
    const focusSelector = input
      ? (question ? `[data-question="${question}"] .reply-form textarea` : ".question-form textarea")
      : button.dataset.hideReply ? `[data-hide-reply="${Number(button.dataset.hideReply)}"]`
        : `[data-question="${question}"] [data-hide]`;
    if (input) input.disabled = true;
    status.textContent = "Đang lưu…";
    try {
      await api(path, method, data);
      success();
      if (!active()) return;
      status.textContent = message;
      await load();
    } catch (error) {
      if (active()) status.textContent = "Chưa lưu được: " + error.message.replace(/[.!?]+$/, "") + ". Nội dung vẫn được giữ để thử lại.";
    } finally {
      writing = false;
      button.disabled = false;
      if (input) input.disabled = false;
      if (active()) root.querySelector(focusSelector)?.focus({ preventScroll: true });
    }
  }
  const form = root.querySelector(".question-form");
  enhanceForms(root);
  if (form) {
    form.elements.body.oninput = () => drafts.set(draftKey, form.elements.body.value);
    form.onsubmit = event => {
      event.preventDefault();
      write(form.querySelector("button"), `/lessons/${lessonId}/questions`, "POST",
        { body: form.elements.body.value }, () => {
          form.elements.body.value = "";
          form.elements.body.dispatchEvent(new Event("input", { bubbles: true }));
          drafts.delete(draftKey);
          current = 0;
          questionId = null;
        }, "Đã gửi câu hỏi và thông báo cho giảng viên.");
    };
  }
  load();
}
