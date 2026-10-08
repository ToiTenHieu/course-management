import { markdown, video } from "./lesson-content.js";
// Shared interaction details for forms and the mobile workspace.
let fieldSequence = 0;
const pendingContent = new WeakMap();
const escapeText = (value) => String(value ?? "").replace(/[&<>"']/g,
  (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

export function setPending(button, pending, text = "Đang xử lý…") {
  if (!button) return;
  if (pending) {
    pendingContent.set(button, button.innerHTML);
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    button.innerHTML = '<span class="button-spinner" aria-hidden="true"></span>' + escapeText(text);
  } else {
    button.disabled = false;
    button.removeAttribute("aria-busy");
    if (pendingContent.has(button)) button.innerHTML = pendingContent.get(button);
    pendingContent.delete(button);
  }
}

export function enhanceForms(root) {
  for (const form of root.querySelectorAll("form")) {
    if (form.dataset.enhanced) continue;
    form.dataset.enhanced = "true";
    form.noValidate = true;
    const controls = [...form.querySelectorAll("input, textarea, select")]
      .filter((input) => input.type !== "hidden");
    for (const input of controls) {
      const isPassword = input.type === "password";
      input.id ||= "form-field-" + ++fieldSequence;
      const error = document.createElement("small");
      error.id = input.id + "-error";
      error.className = "field-error";
      error.hidden = true;
      const label = input.closest("label");
      if (input.type === "password" && label) {
        const wrapper = document.createElement("div");
        wrapper.className = "password-field";
        label.before(wrapper);
        wrapper.append(label);
        const toggle = document.createElement("button");
        toggle.type = "button";
        toggle.className = "password-toggle";
        toggle.textContent = "Hiện";
        const name = label.textContent.trim();
        toggle.setAttribute("aria-label", "Hiện " + name.toLowerCase());
        toggle.setAttribute("aria-controls", input.id);
        toggle.setAttribute("aria-pressed", "false");
        toggle.onclick = () => {
          const shown = input.type === "password";
          input.type = shown ? "text" : "password";
          toggle.textContent = shown ? "Ẩn" : "Hiện";
          toggle.setAttribute("aria-label", (shown ? "Ẩn " : "Hiện ") + name.toLowerCase());
          toggle.setAttribute("aria-pressed", String(shown));
        };
        wrapper.append(toggle);
      }
      (input.closest(".password-field") || label || input).after(error);
      input.setAttribute("aria-describedby", [input.getAttribute("aria-describedby"), error.id].filter(Boolean).join(" "));
      let counter;
      if (input.tagName === "TEXTAREA" && input.maxLength > 0) {
        counter = document.createElement("small");
        counter.className = "character-count";
        counter.id = input.id + "-count";
        (label || input).after(counter);
        input.setAttribute("aria-describedby", input.getAttribute("aria-describedby") + " " + counter.id);
      }
      const validate = (reveal = false) => {
        // Passwords retain literal whitespace; text fields cannot be only whitespace.
        if (input.required && !input.readOnly && !isPassword && ["text", "textarea"].includes(input.type))
          input.setCustomValidity(input.value && !input.value.trim() ? "Vui lòng nhập nội dung, không chỉ khoảng trắng." : "");
        const v = input.validity;
        const message = v.valueMissing ? "Vui lòng điền mục này." : v.typeMismatch
          ? (input.type === "email" ? "Nhập email hợp lệ, ví dụ: ban@example.com." : "Nhập liên kết đầy đủ, bắt đầu bằng https:// hoặc http://.")
          : v.tooShort ? `Cần ít nhất ${input.minLength} ký tự.`
          : v.tooLong ? `Tối đa ${input.maxLength} ký tự.`
          : v.rangeUnderflow ? `Giá trị tối thiểu là ${input.min}.`
          : v.rangeOverflow ? `Giá trị tối đa là ${input.max}.`
          : v.stepMismatch ? "Nhập giá trị đúng bước tăng của mục này."
          : v.badInput ? "Vui lòng nhập một số hợp lệ."
          : v.patternMismatch ? "Nội dung chưa đúng định dạng yêu cầu."
          : v.customError ? input.validationMessage : "";
        if (reveal || input.getAttribute("aria-invalid") === "true") {
          input.setAttribute("aria-invalid", String(!v.valid));
          error.textContent = message;
          error.hidden = v.valid;
        }
        if (counter) counter.textContent = `${input.value.length.toLocaleString("vi-VN")} / ${input.maxLength.toLocaleString("vi-VN")} ký tự`;
        return v.valid;
      };
      input.addEventListener("input", () => validate());
      input.addEventListener("change", () => validate());
      input.addEventListener("blur", () => validate(true));
      input.validateExperience = validate;
      validate();
    }
    form.addEventListener("submit", (event) => {
      const invalid = controls.filter((input) => input.willValidate && !input.validateExperience(true));
      if (!invalid.length) return;
      event.preventDefault();
      event.stopImmediatePropagation();
      form.dispatchEvent(new Event("form-invalid"));
      invalid[0].focus();
    }, true);
  }
}

export function wireNavigation(root) {
  const sidebar = root.querySelector(".sidebar");
  const menu = root.querySelector("#menu");
  const close = root.querySelector("#closeNavigation");
  const overlay = root.querySelector(".navigation-overlay");
  const main = root.querySelector(".work-main");
  const mobile = matchMedia("(max-width: 700px)");
  let open = false;
  function draw(next, restoreFocus = false) {
    open = mobile.matches && next;
    sidebar.classList.toggle("open", open);
    sidebar.inert = mobile.matches && !open;
    main.inert = open;
    overlay.hidden = !open;
    menu.setAttribute("aria-expanded", String(open));
    document.body.classList.toggle("navigation-open", open);
    if (open) {
      sidebar.setAttribute("role", "dialog");
      sidebar.setAttribute("aria-modal", "true");
      sidebar.setAttribute("aria-label", "Điều hướng không gian làm việc");
      close.focus();
    } else {
      sidebar.removeAttribute("role");
      sidebar.removeAttribute("aria-modal");
      sidebar.removeAttribute("aria-label");
      if (restoreFocus && mobile.matches) menu.focus();
    }
  }
  menu.onclick = () => draw(!open);
  close.onclick = overlay.onclick = () => draw(false, true);
  document.onkeydown = (event) => {
    if (!open) return;
    if (event.key === "Escape") {
      event.preventDefault();
      draw(false, true);
    } else if (event.key === "Tab") {
      const focusable = [...sidebar.querySelectorAll("a[href], button:not(:disabled)")].filter((el) => el.getClientRects().length);
      const first = focusable[0], last = focusable.at(-1);
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault(); last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault(); first.focus();
      }
    }
  };
  // Re-rendering a shell replaces its previous responsive listener.
  root.navigationCleanup?.();
  const resize = () => draw(false, true);
  mobile.addEventListener("change", resize);
  root.navigationCleanup = () => mobile.removeEventListener("change", resize);
  draw(false);
}

export function lessonBody(text, resource, format = "TEXT", videoUrl = "") {
  let url = null;
  try {
    const parsed = new URL(resource);
    if (["https:", "http:"].includes(parsed.protocol)) url = parsed;
  } catch { /* Empty or invalid links are not rendered. */ }
  return `${video(videoUrl)}<div class="lesson-text ${format === "MARKDOWN" ? "structured-content" : ""}">${format === "MARKDOWN" ? markdown(text) : escapeText(text || "Bài học sử dụng tài liệu hoặc video đi kèm.")}</div>${url
    ? `<div class="lesson-resource"><span class="resource-symbol" aria-hidden="true">↗</span><div><strong>Tài liệu / video của bài học</strong><small>${escapeText(url.hostname)} · Mở trong thẻ mới</small></div><a class="btn secondary compact resource-link" href="${escapeText(url.href)}" target="_blank" rel="noopener noreferrer">Mở tài liệu ↗</a></div>`
    : ""}`;
}
