import { dateTime, timeOnly } from './dates.js';
import { api } from './api.js';
const fields = ['title', 'orderIndex', 'contentUrl', 'textContent', 'contentFormat', 'videoUrl'];
const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

export function mountLessonDraft(form, courseId, lesson, draft) {
  const path = `/courses/${courseId}/lesson-drafts/${lesson?.lessonId || 0}`;
  const initialContent = Object.fromEntries(fields.map(key => [key, form.elements[key].value]));
  let revision = draft?.revision || 0;
  let baseRevision = draft?.content ? draft.baseRevision : lesson?.contentRevision ?? null;
  let timer, pending = Promise.resolve(), blocked = false, stopped = false, resetting = false;
  const box = document.createElement('section');
  box.className = 'draft-status';
  box.innerHTML = '<p role="status" aria-live="polite"></p><div class="row-actions"><button type="button" class="text-link" data-retry hidden>Thử lưu nháp lại</button><button type="button" class="text-link" data-current>Dùng nội dung hiện tại</button></div>';
  form.querySelector('.editor-intro').after(box);
  const status = box.querySelector('p'), retry = box.querySelector('[data-retry]');
  function collect() {
    const data = Object.fromEntries(fields.map(key => [key, form.elements[key].value]));
    data.orderIndex = data.orderIndex ? Number(data.orderIndex) : null;
    return data;
  }
  function apply(content) {
    for (const key of fields) form.elements[key].value = content[key] ?? '';
    for (const key of fields) form.elements[key].dispatchEvent(new Event('input', {bubbles:true}));
  }
  if (draft?.content) apply(draft.content);
  let saved = JSON.stringify(collect());
  status.textContent = draft?.content ? 'Đã khôi phục bản nháp trên máy chủ. Chưa cập nhật bài học.' : 'Thay đổi được tự lưu thành bản nháp trên máy chủ.';
  async function save() {
    if (blocked) throw new Error('Bản nháp có xung đột. Mở lại trình soạn hoặc dùng nội dung hiện tại.');
    const content = collect(), snapshot = JSON.stringify(content);
    if (snapshot === saved) return;
    status.textContent = 'Đang lưu bản nháp…';
    try {
      const result = await api(path, 'PUT', {...content, expectedRevision:revision, baseRevision});
      revision = result.revision; saved = snapshot;
      status.textContent = 'Đã lưu bản nháp trên máy chủ · ' + timeOnly(result.updatedAt);
      retry.hidden = true;
    } catch (error) {
      blocked = error.status === 409;
      status.textContent = error.message + ' Nội dung trong trình soạn vẫn được giữ.';
      retry.hidden = blocked;
      throw error;
    }
  }
  function enqueue() {
    pending = pending.catch(() => {}).then(save);
    return pending;
  }
  function changed(event) {
    if (stopped || resetting || !fields.includes(event.target.name)) return;
    clearTimeout(timer);
    status.textContent = 'Có thay đổi đang chờ lưu nháp…';
    timer = setTimeout(() => enqueue().catch(() => {}), 900);
  }
  form.addEventListener('input', changed);
  form.addEventListener('change', changed);
  retry.onclick = () => enqueue().catch(() => {});
  box.querySelector('[data-current]').onclick = async () => {
    if (!confirm('Bỏ nội dung trong trình soạn và bản nháp của bạn để dùng bài hiện tại?')) return;
    if (resetting) return;
    resetting = true;
    const controls = [...form.querySelectorAll('input,textarea,select,button')].filter(input => !input.disabled);
    controls.forEach(input => { input.disabled = true; });
    clearTimeout(timer);
    try {
      await pending.catch(() => {});
      const current = await api(path);
      // Fetch the live content before removing a draft; network failure must not destroy it.
      const live = lesson ? await api('/lessons/' + lesson.lessonId) : initialContent;
      if (current?.content) await api(path + '?revision=' + current.revision, 'DELETE');
      revision = current?.content ? current.revision + 1 : current?.revision || 0;
      baseRevision = live.contentRevision ?? null; blocked = false;
      apply(live); saved = JSON.stringify(collect()); retry.hidden = true;
      status.textContent = 'Đã dùng nội dung hiện tại. Bản nháp cũ đã được bỏ.';
    } catch (error) { status.textContent = error.message + ' Nội dung đang soạn vẫn được giữ.'; }
    finally { resetting = false; controls.forEach(input => { input.disabled = false; }); }
  };
  if (lesson) {
    const history = document.createElement('details'); history.className = 'lesson-history';
    history.innerHTML = '<summary>Lịch sử nội dung (30 phiên bản gần nhất)</summary><div class="history-list"></div>';
    form.querySelector('#lessonEditPanel').append(history);
    let loaded = false;
    history.ontoggle = async () => {
      if (!history.open || loaded) return;
      const list = history.querySelector('div');
      list.textContent = 'Đang tải lịch sử…';
      try {
        const versions = await api('/lessons/' + lesson.lessonId + '/content-versions');
        loaded = true;
        list.innerHTML = versions.length ? versions.map((v,i) => `<div class="history-row"><span>Phiên bản ${v.revision} · ${escape(v.editorName)}<small>${escape(dateTime(v.createdAt))} · ${escape(v.content.title)}</small></span><button type="button" class="btn secondary compact" data-version="${i}">Đưa vào bản nháp</button></div>`).join('') : '<p class="hint">Chưa có phiên bản trước đó.</p>';
        list.querySelectorAll('[data-version]').forEach(button => button.onclick = () => {
          if (!confirm('Thay nội dung đang soạn bằng phiên bản này? Bài học chỉ cập nhật sau khi bạn lưu thay đổi.')) return;
          apply(versions[Number(button.dataset.version)].content);
          enqueue().catch(() => {});
        });
      } catch (error) { list.textContent = error.message; }
    };
  }
  form.closest('dialog').addEventListener('close', () => {
    stopped = true; clearTimeout(timer);
    // Keep unsent edits in the server draft when the editor is closed.
    enqueue().catch(() => {});
  }, {once:true});
  return {
    async flush() {
      if (resetting) throw new Error('Đang tải nội dung hiện tại. Hãy chờ trước khi đóng.');
      clearTimeout(timer);
      await enqueue();
    },
    async prepare(data) {
      if (resetting) throw new Error('Đang tải nội dung hiện tại. Hãy chờ trước khi lưu.');
      clearTimeout(timer);
      await enqueue();
      data.expectedRevision = baseRevision;
      const current = await api(path);
      if ((current?.revision || 0) !== revision) {
        blocked = true;
        throw new Error('Bản nháp đã thay đổi ở tab khác. Mở lại trình soạn trước khi lưu.');
      }
      if (current?.content) data.draftRevision = revision;
    }
  };
}
