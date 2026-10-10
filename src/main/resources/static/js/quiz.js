import { dateTime } from './dates.js';
import { api, appConfig } from "./api.js";
import { enhanceForms } from "./experience.js";

const esc = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
let letters = [];
const editorDrafts = new Map();
const selections = new Map();
const date = dateTime;
const blankQuestion = () => ({
  prompt: "",
  options: letters.map(() => ""),
  correctIndex: 0,
  explanation: "",
});

export function mountQuiz(root, lessonId, manager, userId) {
  const draftKey = `${userId}:${lessonId}`;
  const pendingKey = `cm-quiz-pending:${draftKey}`;
  let settings;
  let quiz = null,
    revision = 0,
    resultRevision = 0,
    statisticsRevision = 0,
    historyPage = 0,
    busy = false;
  const active = () => root.isConnected;
  function pending() {
    try {
      const saved = JSON.parse(sessionStorage.getItem(pendingKey) || "null");
      if (!saved) return null;
      if (!saved.quiz || !Array.isArray(saved.quiz.questions)
          || !saved.quiz.questions.length
          || !saved.quiz.questions.every(q => Array.isArray(q.options) && q.options.length === letters.length)
          || !saved.payload || !Array.isArray(saved.payload.answers)
          || saved.payload.answers.length !== saved.quiz.questions.length
          || !saved.payload.answers.every(answer => Number.isInteger(answer) && answer >= 0 && answer < letters.length)
          || saved.payload.quizVersionId !== saved.quiz.quizVersionId
          || typeof saved.payload.submissionKey !== "string") {
        sessionStorage.removeItem(pendingKey);
        return null;
      }
      return saved;
    } catch {
      return null;
    }
  }
  function setPending(value) {
    try {
      if (value) sessionStorage.setItem(pendingKey, JSON.stringify(value));
      else sessionStorage.removeItem(pendingKey);
    } catch {
      /* The same mounted form still retains the submission key if storage is unavailable. */
    }
  }
  const header = `<div class="section-heading"><div><span class="eyebrow">THỰC HÀNH SAU BÀI HỌC</span><h3>Kiểm tra kiến thức</h3><p>${manager ? "Soạn quiz trắc nghiệm và xem câu hỏi học viên hay sai." : "Làm bài, nhận giải thích và xem lại các lần làm. Điểm quiz được theo dõi riêng với tiến độ đọc bài."}</p></div></div>`;
  async function load() {
    const request = ++revision;
    ++resultRevision;
    root.innerHTML = header + '<p role="status">Đang tải quiz…</p>';
    try {
      settings = (await appConfig()).learning;
      letters = Array.from({length: settings.quizOptionCount}, (_, index) => String.fromCharCode(65 + index));
      const loaded = await api(`/lessons/${lessonId}/quiz`);
      if (!active() || request !== revision) return;
      quiz = loaded;
      if (manager) renderManager();
      else renderStudent();
    } catch (error) {
      if (!active() || request !== revision) return;
      root.innerHTML =
        header +
        `<p class="notice error" role="alert">${esc(error.message)}</p><button class="btn secondary compact" data-retry>Thử lại quiz</button>`;
      root.querySelector("[data-retry]").onclick = load;
    }
  }
  function feedback(data) {
    const s = data.summary;
    return `<div class="quiz-score ${s.passed ? "quiz-passed" : "quiz-retry"}"><strong>${s.score}% · ${s.correctCount}/${s.questionCount} câu đúng</strong><span>${s.passed ? "Đạt" : "Cần ôn thêm"} · Mức đạt ${s.passPercentage}% · Phiên bản ${s.revision}</span><small>${esc(s.title)} · ${esc(date(s.submittedAt))}</small></div><div class="quiz-feedback">${data.feedback.map((q, i) => `<article><h4>Câu ${i + 1}: ${esc(q.prompt)}</h4><p class="badge ${q.correct ? "good" : "warm"}">${q.correct ? "Chính xác" : "Chưa đúng"}</p><p>Bạn chọn ${letters[q.selectedIndex]}: ${esc(q.options[q.selectedIndex])}</p>${q.correct ? "" : `<p><strong>Đáp án ${letters[q.correctIndex]}: ${esc(q.options[q.correctIndex])}</strong></p>`}<p class="quiz-explanation">${esc(q.explanation)}</p></article>`).join("")}</div>`;
  }
  function renderStudent() {
    const saved = pending();
    const shown = saved?.quiz || quiz;
    const values =
      saved?.payload.answers ||
      selections.get(`${draftKey}:${shown?.quizVersionId}`) ||
      [];
    let inFlight = saved;
    root.innerHTML =
      header +
      (!shown
        ? '<p class="muted-text">Giảng viên chưa xuất bản quiz cho bài này.</p>'
        : `<h4>${esc(shown.title)}</h4><p class="hint">${shown.questions.length} câu · Chọn một đáp án mỗi câu · Mức đạt ${shown.passPercentage}% · Phiên bản ${shown.revision}</p>${saved ? '<p class="notice" data-pending-notice>Có lần nộp chưa nhận được kết quả. Thử nhận kết quả trước khi làm lượt mới.</p>' : ""}<form class="quiz-form">${shown.questions.map((q, i) => `<fieldset><legend>Câu ${i + 1}: ${esc(q.prompt)}</legend>${q.options.map((option, index) => `<label class="quiz-option"><input type="radio" name="q${i}" value="${index}" required ${values[i] === index ? "checked" : ""} ${saved ? "disabled" : ""}><span>${letters[index]}. ${esc(option)}</span></label>`).join("")}</fieldset>`).join("")}<button class="btn compact" type="submit">${saved ? "Thử nhận kết quả" : "Nộp quiz"}</button></form>`) +
      '<p class="quiz-status" role="status" aria-live="polite"></p><div class="quiz-result"></div><section class="quiz-history"><h4>Lịch sử làm bài của tôi</h4><div class="quiz-history-list"></div></section>';
    const form = root.querySelector(".quiz-form");
    const status = root.querySelector(".quiz-status");
    if (form) {
      form.onchange = () =>
        selections.set(
          `${draftKey}:${shown.quizVersionId}`,
          shown.questions.map((_, i) =>
            form.querySelector(`input[name=q${i}]:checked`)
              ? Number(form.querySelector(`input[name=q${i}]:checked`).value)
              : null,
          ),
        );
      form.onsubmit = async (event) => {
        event.preventDefault();
        if (busy) return;
        busy = true;
        ++resultRevision;
        if (!inFlight) {
          inFlight = {
            quiz: shown,
            payload: {
              quizVersionId: shown.quizVersionId,
              submissionKey: crypto.randomUUID(),
              answers: shown.questions.map((_, i) =>
                Number(new FormData(form).get(`q${i}`)),
              ),
            },
          };
          setPending(inFlight);
        }
        form
          .querySelectorAll("input,button")
          .forEach((n) => (n.disabled = true));
        status.textContent = "Đang chấm bài…";
        try {
          const result = await api(
            `/lessons/${lessonId}/quiz/attempts`,
            "POST",
            inFlight.payload,
          );
          setPending(null);
          selections.delete(`${draftKey}:${shown.quizVersionId}`);
          if (!active()) return;
          status.textContent =
            "Đã lưu kết quả. Bạn có thể xem lại trong lịch sử.";
          root.querySelector(".quiz-result").innerHTML =
            feedback(result) +
            '<button class="btn secondary compact" data-again>Làm lại quiz</button>';
          form.remove();
          root.querySelector("[data-pending-notice]")?.remove();
          root.querySelector("[data-again]").onclick = load;
          historyPage = 0;
          await loadHistory();
        } catch (error) {
          if (!active()) return;
          status.textContent =
            error.message +
            (error.status === 409
              ? " — Hãy tải đề mới; câu trả lời đã chọn được giữ trong phiên này."
              : " — Câu trả lời được giữ. Thử nhận kết quả lại để tránh tạo lần làm trùng.");
          form.querySelector("button").textContent = "Thử nhận kết quả";
          if ([400, 404, 409].includes(error.status)) {
            const button = document.createElement("button");
            button.className = "btn secondary compact";
            button.textContent = "Tải đề hiện tại";
            button.onclick = () => {
              setPending(null);
              load();
            };
            root.querySelector(".quiz-result").replaceChildren(button);
          }
        } finally {
          busy = false;
          const button = form.querySelector("button");
          if (button) button.disabled = false;
        }
      };
    }
    loadHistory();
  }
  let historyRevision = 0;
  async function loadHistory() {
    const request = ++historyRevision;
    const container = root.querySelector(".quiz-history-list");
    if (!container) return;
    container.innerHTML = '<p role="status">Đang tải lịch sử…</p>';
    try {
      const data = await api(
        `/lessons/${lessonId}/quiz/attempts?page=${historyPage}&size=5`,
      );
      if (!active() || !container.isConnected || request !== historyRevision) return;
      if (historyPage > 0 && historyPage >= data.totalPages) {
        historyPage = Math.max(0, data.totalPages - 1);
        return loadHistory();
      }
      container.innerHTML =
        data.content
          .map(
            (a) =>
              `<button class="quiz-history-item" data-attempt="${a.attemptId}"><span><strong>${a.score}% · ${a.passed ? "Đạt" : "Cần ôn thêm"}</strong><small>${esc(date(a.submittedAt))} · Phiên bản ${a.revision}</small></span><span>Xem lại →</span></button>`,
          )
          .join("") ||
        '<p class="muted-text">Chưa có lần làm nào. Kết quả sẽ được lưu khi bạn nộp bài.</p>';
      if (data.totalPages > 1)
        container.insertAdjacentHTML(
          "beforeend",
          `<div class="quiz-pages"><span>${data.totalElements} lần làm · Trang ${historyPage + 1}/${data.totalPages}</span>${historyPage > 0 ? '<button class="btn secondary compact" data-history-page="previous">← Trước</button>' : ""}${historyPage + 1 < data.totalPages ? '<button class="btn secondary compact" data-history-page="next">Sau →</button>' : ""}</div>`,
        );
      container.querySelectorAll("[data-history-page]").forEach(
        (button) =>
          (button.onclick = () => {
            historyPage += button.dataset.historyPage === "next" ? 1 : -1;
            loadHistory();
          }),
      );
      container.querySelectorAll("[data-attempt]").forEach(
        (button) =>
          (button.onclick = async () => {
            if (busy) return;
            const request = ++resultRevision;
            const resultContainer = root.querySelector(".quiz-result");
            button.disabled = true;
            try {
              const result = await api(
                `/quiz-attempts/${button.dataset.attempt}`,
              );
              if (!active() || !resultContainer.isConnected || request !== resultRevision) return;
              resultContainer.innerHTML = feedback(result);
              if (!root.querySelector(".quiz-form")) {
                const again = document.createElement("button");
                again.className = "btn secondary compact";
                again.textContent = "Làm lại quiz";
                again.onclick = load;
                resultContainer.append(again);
              }
              resultContainer.scrollIntoView({ block: "start" });
            } catch (error) {
              if (active() && request === resultRevision && resultContainer.isConnected)
                root.querySelector(".quiz-status").textContent = error.message;
            } finally {
              button.disabled = false;
            }
          }),
      );
    } catch (error) {
      if (!active() || !container.isConnected || request !== historyRevision) return;
      container.innerHTML = `<p role="alert">${esc(error.message)}</p><button class="btn secondary compact" data-retry-history>Thử lại lịch sử</button>`;
      container.querySelector("[data-retry-history]").onclick = loadHistory;
    }
  }
  function renderManager() {
    root.innerHTML =
      header +
      `<div class="quiz-management-summary"><span class="badge ${quiz?.published ? "good" : "muted"}">${quiz ? (quiz.published ? "Quiz đã xuất bản" : "Quiz đang nháp") + ` · Phiên bản ${quiz.revision}` : "Chưa có quiz"}</span><button class="btn secondary compact" data-edit>Soạn quiz</button></div><div class="quiz-editor"></div><div class="quiz-statistics"></div>`;
    root.querySelector("[data-edit]").onclick = renderEditor;
    if (editorDrafts.has(draftKey)) renderEditor();
    if (quiz) loadStatistics();
  }
  async function loadStatistics() {
    const container = root.querySelector(".quiz-statistics");
    const request = ++statisticsRevision;
    try {
      const data = await api(`/lessons/${lessonId}/quiz/statistics`);
      if (!active() || !container.isConnected || request !== statisticsRevision) return;
      container.innerHTML = `<h4>Kết quả phiên bản ${data.revision}</h4><p>${data.attempts} lần làm · ${data.students} học viên · ${data.passedAttempts} lần đạt · Điểm trung bình ${data.averageScore === null ? "—" : data.averageScore.toFixed(1) + "%"}</p><p class="hint">Thống kê tính tất cả lần làm của phiên bản hiện tại, gồm cả các lần làm lại.</p>${data.questions.map((q, i) => `<div class="quiz-stat-row"><strong>Câu ${i + 1}: ${esc(q.prompt)}</strong><span>${q.incorrect}/${q.answered} lần trả lời sai</span></div>`).join("")}`;
    } catch (error) {
      if (!active() || !container.isConnected || request !== statisticsRevision) return;
      container.innerHTML = `<p role="alert">${esc(error.message)}</p><button class="btn secondary compact" data-retry-stats>Thử lại thống kê</button>`;
      container.querySelector("[data-retry-stats]").onclick = loadStatistics;
    }
  }
  function renderEditor() {
    const container = root.querySelector(".quiz-editor");
    const model = editorDrafts.get(draftKey) || {
      expectedRevision: quiz?.revision || 0,
      title: quiz?.title || "Kiểm tra sau bài học",
      passPercentage: quiz?.passPercentage || settings.defaultPassPercentage,
      published: quiz?.published || false,
      questions: quiz?.questions.map((q) => ({
        ...q,
        options: [...q.options],
      })) || [blankQuestion()],
    };
    container.innerHTML = `<p class="hint">1–${settings.maxQuizQuestions} câu, mỗi câu có ${settings.quizOptionCount} lựa chọn và một đáp án đúng. Giải thích giúp học viên hiểu vì sao. Mỗi lần lưu tạo phiên bản mới và giữ lịch sử cũ. Bỏ xuất bản sẽ ẩn đề hiện tại với học viên.</p><form class="quiz-editor-form"><label>Tên quiz<input name="title" required maxlength="255" value="${esc(model.title)}"></label><label>Mức đạt (%)<input name="passPercentage" type="number" required min="1" max="100" value="${model.passPercentage}"></label><label class="quiz-option"><input type="checkbox" name="published" ${model.published ? "checked" : ""}><span>Xuất bản quiz cho học viên</span></label><div class="quiz-editor-questions">${model.questions.map((q, i) => `<fieldset data-question-index="${i}"><legend>Câu ${i + 1}</legend><label>Nội dung câu ${i + 1}<textarea name="prompt${i}" required rows="3" maxlength="5000">${esc(q.prompt)}</textarea></label>${q.options.map((o, j) => `<label>Lựa chọn ${letters[j]} của câu ${i + 1}<input name="option${i}_${j}" required maxlength="1000" value="${esc(o)}"></label>`).join("")}<label>Đáp án đúng của câu ${i + 1}<select name="correct${i}">${letters.map((letter, j) => `<option value="${j}" ${q.correctIndex === j ? "selected" : ""}>${letter}</option>`).join("")}</select></label><label>Giải thích câu ${i + 1}<textarea name="explanation${i}" required maxlength="5000" rows="3">${esc(q.explanation)}</textarea></label>${model.questions.length > 1 ? `<button type="button" class="text-link danger" data-remove="${i}">Bỏ câu ${i + 1}</button>` : ""}</fieldset>`).join("")}</div><div class="quiz-editor-actions"><button type="button" class="btn secondary compact" data-add ${model.questions.length >= settings.maxQuizQuestions ? "disabled" : ""}>Thêm câu hỏi</button><button type="submit" class="btn compact">Lưu quiz</button><button type="button" class="text-link" data-cancel>Đóng trình soạn</button></div><p class="quiz-editor-status" role="status" aria-live="polite"></p></form>`;
    const form = container.querySelector("form");
    enhanceForms(container);
    function collect() {
      const data = new FormData(form);
      return {
        expectedRevision: model.expectedRevision,
        title: data.get("title"),
        passPercentage: Number(data.get("passPercentage")),
        published: data.has("published"),
        questions: model.questions.map((_, i) => ({
          prompt: data.get(`prompt${i}`),
          options: letters.map((_, j) => data.get(`option${i}_${j}`)),
          correctIndex: Number(data.get(`correct${i}`)),
          explanation: data.get(`explanation${i}`),
        })),
      };
    }
    form.oninput = () => editorDrafts.set(draftKey, collect());
    form.onchange = form.oninput;
    container.querySelector("[data-add]").onclick = () => {
      const value = collect();
      if (value.questions.length >= settings.maxQuizQuestions) return;
      value.questions.push(blankQuestion());
      editorDrafts.set(draftKey, value);
      renderEditor();
    };
    container.querySelectorAll("[data-remove]").forEach(
      (b) =>
        (b.onclick = () => {
          const value = collect();
          value.questions.splice(Number(b.dataset.remove), 1);
          editorDrafts.set(draftKey, value);
          renderEditor();
        }),
    );
    container.querySelector("[data-cancel]").onclick = () => {
      editorDrafts.set(draftKey, collect());
      container.innerHTML = "";
    };
    form.onsubmit = async (event) => {
      event.preventDefault();
      if (busy) return;
      const value = collect();
      editorDrafts.set(draftKey, value);
      busy = true;
      const status = form.querySelector(".quiz-editor-status");
      form
        .querySelectorAll("input,textarea,select,button")
        .forEach((n) => (n.disabled = true));
      status.textContent = "Đang lưu phiên bản mới…";
      try {
        quiz = await api(`/lessons/${lessonId}/quiz`, "PUT", value);
        editorDrafts.delete(draftKey);
        if (active()) renderManager();
      } catch (error) {
        if (!active()) return;
        status.textContent =
          error.message + ". Nội dung đang soạn vẫn được giữ.";
        if (error.status === 409) {
          status.insertAdjacentHTML(
            "beforeend",
            '<br><button type="button" class="btn secondary compact" data-reload-quiz>Tải đề mới nhất</button>',
          );
          status.querySelector("[data-reload-quiz]").onclick = async () => {
            let latest;
            const reload = status.querySelector("[data-reload-quiz]");
            reload.disabled = true;
            try { latest = await api(`/lessons/${lessonId}/quiz`); }
            catch (error) {
              if (active() && status.isConnected) {
                status.firstChild.textContent = error.message + '. Bản soạn vẫn được giữ; hãy thử tải lại.';
                reload.disabled = false;
              }
              return;
            }
            if (!active() || !status.isConnected || !latest) return;
            quiz = latest;
            status.textContent = `Đề mới là phiên bản ${latest.revision}. Bản soạn của bạn vẫn được giữ; dùng nút dưới nếu muốn lưu đè thành phiên bản kế tiếp.`;
            const button = document.createElement("button");
            button.type = "button";
            button.className = "btn secondary compact";
            button.textContent = "Dùng phiên bản mới làm cơ sở lưu";
            button.onclick = () => {
              model.expectedRevision = latest.revision;
              editorDrafts.set(draftKey, collect());
              status.textContent =
                "Có thể lưu bản soạn thành phiên bản kế tiếp.";
            };
            status.append(button);
          };
        }
      } finally {
        busy = false;
        form
          .querySelectorAll("input,textarea,select,button")
          .forEach((n) => (n.disabled = false));
        form.querySelector("[data-add]").disabled =
          model.questions.length >= settings.maxQuizQuestions;
      }
    };
  }
  load();
}
