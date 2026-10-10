import { api } from './api.js';
import { assignmentCanLeave } from './assignments.js';
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

export function curriculumCanLeave() {
  if (document.querySelector('.curriculum-editor[data-pending="true"]')) return false;
  return !document.querySelector('.curriculum-editor[data-dirty="true"]') || confirm('Bỏ bản sắp xếp chương trình chưa lưu để cập nhật trang?');
}

export async function mountCurriculumEditor(courseId, lessons, refresh) {
  const root = document.createElement('details');
  root.className = 'curriculum-editor';
  root.innerHTML = '<summary>Chia chương & sắp xếp bài</summary><div data-editor></div>';
  document.querySelector('#curriculum .syllabus').before(root);
  const content = root.querySelector('[data-editor]');
  let plan, dirty = false, busy = false, dragged = null;
  const titles = new Map(lessons.map(l => [l.lessonId, l.title]));
  const guard = event => {
    if (!root.isConnected) { window.removeEventListener('beforeunload', guard); return; }
    if (dirty) { event.preventDefault(); event.returnValue = ''; }
  };
  window.addEventListener('beforeunload', guard);
  function mark() { dirty = true; root.dataset.dirty='true'; content.querySelector('[data-status]').textContent = 'Chưa lưu thay đổi'; }
  function move(groupIndex, itemIndex, targetGroup, targetIndex) {
    const from = plan.groups[groupIndex].lessonIds;
    const [id] = from.splice(itemIndex, 1);
    const to = plan.groups[targetGroup].lessonIds;
    if (groupIndex === targetGroup && targetIndex > itemIndex) targetIndex--;
    to.splice(targetIndex, 0, id); draw(); mark();
  }
  function draw() {
    content.innerHTML = `<p class="hint">Kéo bằng biểu tượng ⠿ để đưa bài tới vị trí hoặc chương khác. Dùng ↑ ↓ và ô Chuyển chương trên điện thoại/bàn phím. Xóa chương chuyển bài về nhóm chưa chia chương. Chọn Lưu chương trình để áp dụng.</p><div data-groups>${plan.groups.map((g,gi) => `<section class="curriculum-group" data-group="${gi}"><div class="curriculum-group-heading">${g.title ? `<label>Tên chương<input data-title="${gi}" value="${esc(g.title)}" maxlength="255" required></label><div class="row-actions"><button type="button" class="chip" data-chapter-up="${gi}" ${gi===0?'disabled':''} aria-label="Đưa chương lên">↑</button><button type="button" class="chip" data-chapter-down="${gi}" ${gi===plan.groups.length-2?'disabled':''} aria-label="Đưa chương xuống">↓</button><button type="button" class="text-link danger" data-delete="${gi}">Xóa chương</button></div>` : '<h3>Bài chưa chia chương</h3>'}</div><div class="curriculum-drop" data-drop-group="${gi}">${g.lessonIds.map((id,i) => `<div class="curriculum-item" draggable="true" data-item="${i}" data-gi="${gi}"><span class="drag-handle" aria-hidden="true">⠿</span><strong>${esc(titles.get(id))}</strong><div class="row-actions"><button type="button" class="chip" data-up="${gi},${i}" ${i===0?'disabled':''} aria-label="Đưa ${esc(titles.get(id))} lên">↑</button><button type="button" class="chip" data-down="${gi},${i}" ${i===g.lessonIds.length-1?'disabled':''} aria-label="Đưa ${esc(titles.get(id))} xuống">↓</button><select data-transfer="${gi},${i}" aria-label="Chuyển chương cho ${esc(titles.get(id))}">${plan.groups.map((t,ti) => `<option value="${ti}" ${ti===gi?'selected':''}>${esc(t.title || 'Chưa chia chương')}</option>`).join('')}</select></div></div>`).join('') || '<p class="hint">Thả bài vào đây</p>'}</div></section>`).join('')}</div><div class="note-actions"><button type="button" class="btn secondary compact" data-add>Thêm chương</button><button type="button" class="btn compact" data-save>Lưu chương trình</button><button type="button" class="text-link" data-reload>Tải lại chương trình</button></div><p data-status role="status">${dirty?'Chưa lưu thay đổi':''}</p>`;
    content.querySelectorAll('[data-title]').forEach(input => input.oninput = () => { plan.groups[Number(input.dataset.title)].title = input.value; mark(); });
    content.querySelector('[data-add]').onclick = () => { if (plan.groups.length>=101) return; plan.groups.splice(plan.groups.length-1,0,{chapterId:null,title:'Chương mới',lessonIds:[]});draw();mark(); };
    for (const direction of ['up','down']) content.querySelectorAll(`[data-${direction}]`).forEach(b => b.onclick = () => { const [g,i]=b.dataset[direction].split(',').map(Number);move(g,i,g,direction==='up'?i-1:i+2); });
    content.querySelectorAll('[data-transfer]').forEach(select => select.onchange = () => { const [g,i]=select.dataset.transfer.split(',').map(Number);const target=Number(select.value);move(g,i,target,plan.groups[target].lessonIds.length); });
    for (const direction of ['up','down']) content.querySelectorAll(`[data-chapter-${direction}]`).forEach(b => b.onclick = () => { const i=Number(b.dataset[direction==='up'?'chapterUp':'chapterDown']),j=i+(direction==='up'?-1:1);[plan.groups[i],plan.groups[j]]=[plan.groups[j],plan.groups[i]];draw();mark(); });
    content.querySelectorAll('[data-delete]').forEach(b => b.onclick = () => { const i=Number(b.dataset.delete);plan.groups.at(-1).lessonIds.push(...plan.groups[i].lessonIds);plan.groups.splice(i,1);draw();mark(); });
    content.querySelectorAll('[data-item]').forEach(row => {
      row.ondragstart = event => { dragged=[Number(row.dataset.gi),Number(row.dataset.item)];event.dataTransfer.effectAllowed='move';event.dataTransfer.setData('text/plain','lesson'); };
      row.ondragend = () => { dragged=null; };
    });
    content.querySelectorAll('[data-drop-group]').forEach(group => {
      group.ondragover = event => { if (dragged) { event.preventDefault();event.dataTransfer.dropEffect='move'; } };
      group.ondrop = event => { if (!dragged) return;event.preventDefault();const row=event.target.closest('[data-item]'),gi=Number(group.dataset.dropGroup);move(...dragged,gi,row?Number(row.dataset.item):plan.groups[gi].lessonIds.length);dragged=null; };
    });
    content.querySelector('[data-reload]').onclick = async () => { if (dirty && !confirm('Bỏ thay đổi đang sắp xếp và tải lại?')) return;await load(); };
    content.querySelector('[data-save]').onclick = async () => {
      if (busy || !assignmentCanLeave()) return;
      const invalid=[...content.querySelectorAll('[data-title]')].find(i => !i.value.trim());
      if (invalid) { invalid.focus();content.querySelector('[data-status]').textContent='Tên chương không được để trống';return; }
      busy=true;root.dataset.pending='true';content.querySelectorAll('button,input,select').forEach(el=>el.disabled=true);
      content.querySelectorAll('[draggable]').forEach(el=>el.draggable=false);
      const status=content.querySelector('[data-status]');status.textContent='Đang lưu…';
      try {
        const result=await api(`/courses/${courseId}/curriculum`,'PUT',{expectedRevision:plan.revision,groups:plan.groups});
        plan=result;dirty=false;root.dataset.dirty='false';status.textContent='Đã lưu chương trình';
        try { await refresh(); } catch (error) { draw();content.querySelector('[data-status]').textContent='Đã lưu. Chưa tải lại trang được: '+error.message; }
      } catch(error) { draw();content.querySelector('[data-status]').textContent=error.message; }
      finally { busy=false;root.dataset.pending='false'; }
    };
  }
  async function load() {
    try { plan=await api(`/courses/${courseId}/curriculum`);dirty=false;root.dataset.dirty='false';draw(); }
    catch(error) { content.innerHTML='<p role="status"></p><button class="btn secondary compact" type="button">Thử tải lại</button>';content.querySelector('p').textContent=error.message;content.querySelector('button').onclick=load; }
  }
  root.ontoggle = () => { if(root.open && !plan) load(); };
}
