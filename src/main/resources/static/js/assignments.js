import { api, appConfig } from './api.js';
import { enhanceForms } from './experience.js';
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const date = value => value ? new Date(value).toLocaleString('vi-VN') : '';
window.addEventListener('beforeunload', event => {
  if (document.querySelector('.lesson-assignment form[data-dirty="true"],.lesson-assignment[data-pending="true"]')) {event.preventDefault();event.returnValue='';}
});
export function assignmentCanLeave(root = document) {
  if (root.querySelector('.lesson-assignment[data-pending="true"]')) return false;
  return !root.querySelector('.lesson-assignment form[data-dirty="true"]') || confirm('Bỏ nội dung bài tập chưa lưu/nộp để chuyển bài?');
}
function submissionMarkup(s) {
  return `<article class="assignment-submission"><div class="section-heading"><h4>${esc(s.studentName)}</h4><span class="badge ${s.score===null?'warm':'good'}">${s.score===null?'Chờ chấm':s.score+'/100'}</span></div><p class="hint">Nộp ${esc(date(s.submittedAt))} · Phiên bản đề ${s.assignmentRevision}</p><details><summary>Đề tại thời điểm nộp: ${esc(s.assignmentTitle)}</summary><p class="assignment-text">${esc(s.assignmentInstructions)}</p></details>${s.answer?`<p class="assignment-text">${esc(s.answer)}</p>`:''}${s.fileName?`<a class="text-link" href="/api/assignment-submissions/${s.submissionId}/file" target="_blank" rel="noopener noreferrer">Tải bài nộp: ${esc(s.fileName)}</a>`:''}${s.score!==null?`<p><strong>Nhận xét của ${esc(s.gradedByName)}</strong> · ${esc(date(s.gradedAt))}</p><p class="assignment-text">${esc(s.feedback || 'Chưa có nhận xét')}</p>`:''}</article>`;
}
export function mountAssignment(root, lessonId, manager = false) {
  root.className += ' lesson-assignment';
  let definition, own, limits, request=0, page=0, ungradedOnly=false, key=crypto.randomUUID();
  const pending = value => {root.dataset.pending=String(value);root.querySelectorAll('input,textarea,select,button').forEach(el=>{if(value){el.dataset.wasDisabled=String(el.disabled);el.disabled=true;}else if(el.dataset.wasDisabled!==undefined){el.disabled=el.dataset.wasDisabled==='true';delete el.dataset.wasDisabled;}});};
  const status = message => {root.querySelector('[data-status]').textContent=message;};
  const track = form => {form.addEventListener('input',()=>form.dataset.dirty='true');enhanceForms(form.parentElement);};
  function draw() {
    root.innerHTML=`<h3>Bài tập thực hành</h3><div data-body></div><p data-status role="status" aria-live="polite"></p>${manager?'<div data-submissions></div>':''}`;
    const body=root.querySelector('[data-body]');
    if(manager) {
      body.innerHTML=`<form data-definition><label>Tên bài tập<input name="title" required maxlength="255" value="${esc(definition?.title || '')}"></label><label>Đề bài và tiêu chí chấm<textarea name="instructions" required maxlength="10000" rows="5">${esc(definition?.instructions || '')}</textarea></label><label class="checkbox-label"><input type="checkbox" name="published" ${definition?.published?'checked':''}> Xuất bản bài tập</label><p class="hint">Mỗi học viên nộp một bài, điểm 0–100. Đề cũ được giữ trong bài nộp khi bạn sửa đề. Điểm bài tập không tự đổi tiến độ đọc bài.</p><button class="btn secondary compact">Lưu bài tập</button><button type="button" class="text-link" data-reload>Tải lại đề</button></form>`;
      const form=body.querySelector('form');track(form);
      form.querySelector('[data-reload]').onclick=()=>{if(assignmentCanLeave(root.parentElement))load();};
      form.onsubmit=async event=>{
        event.preventDefault();if(!form.reportValidity() || root.dataset.pending==='true')return;
        if (!assignmentCanLeave(root.querySelector('[data-submissions]'))) return;
        const data={expectedRevision:definition?.revision||0,title:form.elements.title.value,instructions:form.elements.instructions.value,published:form.elements.published.checked};
        pending(true);status('Đang lưu bài tập…');
        try{definition=await api(`/lessons/${lessonId}/assignment`,'PUT',data);form.dataset.dirty='false';status('Đã lưu bài tập');await loadSubmissions();}
        catch(error){status(error.message);}
        finally{pending(false);}
      };
      loadSubmissions();
    } else if(own) body.innerHTML=`<p class="hint">Bạn đã nộp bài. Bài nộp và điểm được lưu trên tài khoản.</p>${submissionMarkup(own)}<button type="button" class="btn secondary compact" data-reload>Tải lại điểm / nhận xét</button>`;
    else if(!definition) body.innerHTML='<p class="muted-text">Bài này chưa có bài tập được xuất bản.</p>';
    else {
      body.innerHTML=`<h4>${esc(definition.title)}</h4><p class="assignment-text">${esc(definition.instructions)}</p><form data-submit><label>Bài làm<textarea name="answer" rows="6" maxlength="10000" placeholder="Trình bày lời giải hoặc đính kèm tệp…"></textarea></label><label>Tệp bài làm (tùy chọn)<input name="file" type="file" accept=".pdf,.txt,.png,.jpg,.jpeg,.webp"></label><p class="hint">PDF, TXT UTF-8, PNG, JPG, WebP · Tối đa ${(limits.maxFileBytes/1024/1024).toLocaleString('vi-VN')} MB. Mỗi bài tập nộp một lần; kiểm tra nội dung trước khi nộp.</p><button class="btn compact">Nộp bài tập</button><button type="button" class="text-link" data-reload>Tải lại đề</button></form>`;
      const form=body.querySelector('form');track(form);
      form.querySelector('[data-reload]').onclick=()=>{if(assignmentCanLeave(root.parentElement))load();};
      form.onsubmit=async event=>{
        event.preventDefault();if(root.dataset.pending==='true')return;
        const file=form.elements.file.files[0];
        if(!form.elements.answer.value.trim() && !file){status('Nhập bài làm hoặc chọn tệp đính kèm');form.elements.answer.focus();return;}
        if(file && file.size>limits.maxFileBytes){status('Tệp vượt quá dung lượng cho phép');return;}
        if(!confirm('Nộp bài tập này? Bạn có thể xem lại bài và nhận xét sau khi nộp.'))return;
        const data=new FormData(form);if(!file)data.delete('file');data.append('expectedRevision',String(definition.revision));data.append('submissionKey',key);
        pending(true);status('Đang nộp bài…');
        try{own=await api(`/lessons/${lessonId}/assignment/submissions`,'POST',data);form.dataset.dirty='false';draw();status('Đã nộp bài, chờ giảng viên chấm');}
        catch(error){status(error.message+' Nội dung bài làm vẫn được giữ.');}
        finally{pending(false);}
      };
    }
    if(own && !manager) body.querySelector('[data-reload]').onclick=load;
  }
  async function loadSubmissions() {
    if(!manager || !root.isConnected)return;
    const target=root.querySelector('[data-submissions]');
    if(!definition){target.innerHTML='<p class="hint">Lưu bài tập để bắt đầu nhận bài nộp.</p>';return;}
    const current=++request;
    try{
      const list=await api(`/lessons/${lessonId}/assignment/submissions?`+new URLSearchParams({page:String(page),size:'5',ungradedOnly:String(ungradedOnly)}));
      if(current!==request || !root.isConnected)return;
      target.innerHTML=`<div class="section-heading"><h4>${list.totalElements} bài nộp ${ungradedOnly?'chờ chấm':''}</h4><label class="checkbox-label"><input type="checkbox" data-ungraded ${ungradedOnly?'checked':''}> Chỉ bài chưa chấm</label></div>${list.content.map(s=>`${submissionMarkup(s)}<form data-grade="${s.submissionId}" class="assignment-grade"><label>Điểm (0–100)<input type="number" name="score" required min="0" max="100" step="1" value="${s.score??''}"></label><label>Nhận xét<textarea name="feedback" maxlength="10000" rows="3">${esc(s.feedback||'')}</textarea></label><button class="btn secondary compact">${s.score===null?'Lưu điểm':'Cập nhật điểm'}</button><p data-grade-status role="status"></p></form>`).join('')||'<p class="hint">Chưa có bài nộp phù hợp.</p>'}<nav class="pagination" aria-label="Phân trang bài nộp"><button class="chip" data-prev ${page===0?'disabled':''}>← Trước</button><span>Trang ${page+1}/${Math.max(1,list.totalPages)}</span><button class="chip" data-next ${page+1>=list.totalPages?'disabled':''}>Sau →</button><button class="chip" data-refresh>Tải lại bài nộp</button></nav>`;
      target.querySelector('[data-ungraded]').onchange=event=>{if(!assignmentCanLeave(target)){event.target.checked=ungradedOnly;return;}ungradedOnly=event.target.checked;page=0;loadSubmissions();};
      for(const [attribute,delta] of [['prev',-1],['next',1],['refresh',0]]) target.querySelector(`[data-${attribute}]`).onclick=()=>{if(assignmentCanLeave(target)){page+=delta;loadSubmissions();}};
      target.querySelectorAll('[data-grade]').forEach(form=>{
        track(form);
        form.onsubmit=async event=>{
          event.preventDefault();if(!form.reportValidity() || root.dataset.pending==='true')return;
          const s=list.content.find(s=>s.submissionId===Number(form.dataset.grade));pending(true);status('Đang lưu điểm…');form.querySelector('[data-grade-status]').textContent='';
          try{const updated=await api(`/assignment-submissions/${s.submissionId}/grade`,'PUT',{expectedRevision:s.gradeRevision,score:Number(form.elements.score.value),feedback:form.elements.feedback.value});Object.assign(s,updated);form.previousElementSibling.outerHTML=submissionMarkup(updated);form.dataset.dirty='false';form.querySelector('button').textContent='Cập nhật điểm';form.querySelector('[data-grade-status]').textContent='';status('Đã lưu điểm');}
          catch(error){form.querySelector('[data-grade-status]').textContent=error.message;}
          finally{pending(false);}
        };
      });
    }catch(error){target.innerHTML='<p role="status"></p><button class="btn secondary compact">Thử tải lại bài nộp</button>';target.querySelector('p').textContent=error.message;target.querySelector('button').onclick=loadSubmissions;}
  }
  async function load() {
    root.innerHTML='<p class="hint" role="status">Đang tải bài tập…</p>';
    try{const [view,config]=await Promise.all([api(`/lessons/${lessonId}/assignment`),appConfig()]);if(!root.isConnected)return;definition=view.assignment;own=view.submission;limits=config.learning;key=crypto.randomUUID();draw();}
    catch(error){root.innerHTML='<p role="status"></p><button class="btn secondary compact">Thử tải lại bài tập</button>';root.querySelector('p').textContent=error.message;root.querySelector('button').onclick=load;}
  }
  load();
}
