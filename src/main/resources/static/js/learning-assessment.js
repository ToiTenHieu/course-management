import { api } from './api.js';
import { dateTime } from './dates.js';
import { createSessionDraft } from './session-draft.js';
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

export async function mountLearningAssessment(root, userId) {
  const [definition, initialLatest] = await Promise.all([api('/learning-assessment'), api('/learning-assessment/latest')]);
  if (!root.isConnected) return;
  let latest=initialLatest, dirty = false, submitting = false, durable = true;
  const draft = createSessionDraft(`cm-assessment-draft:${userId}`, value => {
    const goal=definition.goals.find(g=>g.id===value?.goal);
    return !!goal && value.version===definition.version && Number.isInteger(value.hoursPerWeek)
      && value.hoursPerWeek>=1 && value.hoursPerWeek<=20 && Array.isArray(value.answers)
      && value.answers.length===goal.questions.length && value.answers.every(a=>a===null || Number.isInteger(a)&&a>=-1&&a<goal.questions[0].options.length);
  });
  window.addEventListener('beforeunload', event => {
    if (root.isConnected && (submitting || dirty&&!durable)) { event.preventDefault(); event.returnValue = ''; }
  });
  function resume(saved) {
    dirty=false;
    const goal=definition.goals.find(g=>g.id===saved.goal);
    root.innerHTML=`<section class="panel assessment-intro"><span class="eyebrow">BÀI KIỂM TRA ĐANG LÀM</span><h2 tabindex="-1">Tiếp tục từ câu đã chọn</h2><p>${esc(goal.title)} · ${saved.hoursPerWeek} giờ / tuần · Đã trả lời ${saved.answers.filter(a=>a!==null).length} / ${saved.answers.length} câu.</p><p class="hint">Bản đang làm được giữ riêng cho tài khoản này trong tab hiện tại. Kết quả gần nhất chỉ thay đổi khi nộp thành công.</p><div class="row-actions"><button class="btn" data-resume>Tiếp tục bài kiểm tra</button><button class="btn secondary" data-discard>Xóa bản đang làm</button>${latest?'<button class="text-link" data-result>Xem kết quả gần nhất</button>':''}</div></section>`;
    root.querySelector('[data-resume]').onclick=()=>questions(goal,saved.hoursPerWeek,saved.answers);
    root.querySelector('[data-discard]').onclick=()=>{
      if(!confirm('Xóa các câu đã chọn của bài đang làm? Kết quả đã lưu vẫn được giữ.'))return;
      draft.clear();setup();
    };
    if(latest)root.querySelector('[data-result]').onclick=()=>showResult(latest);
    root.querySelector('h2').focus();
  }
  function setup() {
    root.innerHTML = `<section class="panel assessment-intro"><span class="eyebrow">CHỌN ĐIỂM BẮT ĐẦU CỦA BẠN</span><h2>Học đúng phần bạn đang cần</h2><p>8 câu trắc nghiệm · khoảng 5–10 phút · không giới hạn thời gian. Kết quả giúp chọn nội dung nên ôn và thứ tự thực hành.</p><form id="assessmentSetup"><label>Bạn muốn học theo hướng nào?<select name="goal">${definition.goals.map(g=>`<option value="${esc(g.id)}">${esc(g.title)}</option>`).join('')}</select></label><label>Số giờ học mỗi tuần<select name="hours">${[1,2,3,4,5,6,8,10,15,20].map(h=>`<option value="${h}" ${h===4?'selected':''}>${h} giờ / tuần</option>`).join('')}</select></label><p class="hint">Chọn “Chưa biết” khi chưa chắc đáp án. Bạn có thể điều chỉnh lộ trình sau khi thực hành. Làm lại sẽ thay kết quả gần nhất.</p><button class="btn" type="submit">Bắt đầu kiểm tra</button></form></section>`;
    root.querySelector('form').onsubmit = event => {
      event.preventDefault(); const data = new FormData(event.currentTarget);
      if(draft.read() && !confirm('Bắt đầu bài mới và thay bản đang làm trong tab này?'))return;
      questions(definition.goals.find(g=>g.id===data.get('goal')), Number(data.get('hours')));
    };
  }
  function questions(goal, hours, restored=[]) {
    root.innerHTML = `<form id="assessmentQuestions"><section class="panel"><span class="eyebrow">${esc(goal.title)}</span><h2 tabindex="-1">Kiểm tra kiến thức đầu vào</h2><p>${hours} giờ học mỗi tuần · Chọn một đáp án cho từng câu.</p><p id="assessmentProgress" role="status" aria-live="polite">Đã trả lời 0 / 8 câu</p></section>${goal.questions.map((q,index)=>`<fieldset class="panel assessment-question"><legend>Câu ${index+1} · ${esc(q.topic)}</legend><h3>${esc(q.text)}</h3>${[...q.options,'Chưa biết'].map((option,i)=>`<label class="assessment-option"><input type="radio" name="answer${index}" value="${i===4?-1:i}"><span>${i===4?'?':String.fromCharCode(65+i)}. ${esc(option)}</span></label>`).join('')}</fieldset>`).join('')}<p class="field-error" id="assessmentError" role="alert"></p><button class="btn" type="submit">Xem lộ trình gợi ý</button></form>`;
    const form = root.querySelector('form');
    const hint=document.createElement('p');hint.className='hint';hint.setAttribute('role','status');
    root.querySelector('#assessmentProgress').after(hint);
    restored.forEach((answer,i)=>{if(answer!==null)form.querySelector(`[name="answer${i}"][value="${answer}"]`)?.setAttribute('checked','');});
    const saveDraft = () => {
      const data=new FormData(form),answers=goal.questions.map((_,i)=>data.has('answer'+i)?Number(data.get('answer'+i)):null);
      dirty=true;durable=draft.write({version:definition.version,goal:goal.id,hoursPerWeek:hours,answers});
      root.querySelector('#assessmentProgress').textContent=`Đã trả lời ${answers.filter(a=>a!==null).length} / ${answers.length} câu`;
      hint.textContent=durable?'Đã giữ bản đang làm trong tab này. Bạn có thể rời trang và quay lại.':'Trình duyệt không lưu được bản đang làm. Giữ trang này mở để tránh mất các câu đã chọn.';
    };
    form.onchange=saveDraft;saveDraft();
    const back=document.createElement('button');back.type='button';back.className='btn secondary';back.textContent='Đổi hướng học / thời gian';
    back.onclick=()=>{if(submitting)return;if(confirm('Đổi lựa chọn sẽ xóa các câu đang làm. Tiếp tục?')){draft.clear();dirty=false;setup();}};
    form.append(back);
    form.onsubmit = async event => {
      event.preventDefault(); if(submitting) return;
      const data = new FormData(form), answers = goal.questions.map((_,i)=>data.has('answer'+i)?Number(data.get('answer'+i)):null);
      const error = root.querySelector('#assessmentError'), missing = answers.indexOf(null);
      if(missing>=0) {
        error.textContent = `Bạn chưa trả lời câu ${missing+1}. Chọn đáp án hoặc “Chưa biết”.`;
        root.querySelector(`[name="answer${missing}"]`).focus(); return;
      }
      submitting=true; const button=form.querySelector('[type="submit"]'); button.disabled=true;back.disabled=true; button.textContent='Đang tạo lộ trình…';
      form.querySelectorAll('fieldset').forEach(f=>f.disabled=true);
      try {
        const result=await api('/learning-assessment','POST',{version:definition.version,goal:goal.id,hoursPerWeek:hours,answers});
        dirty=false;draft.clear();latest=result;if(root.isConnected)showResult(result);
      } catch(e) {
        error.textContent=e.message+' Các câu đã chọn vẫn được giữ.'; button.disabled=false;back.disabled=false; button.textContent='Xem lộ trình gợi ý';
        form.querySelectorAll('fieldset').forEach(f=>f.disabled=false);
      } finally {submitting=false;}
    };
    root.querySelector('h2').focus();
  }
  function showResult(result) {
    const bank=definition.goals.find(g=>g.id===result.goal)?.questions;
    root.innerHTML=`<section class="panel assessment-result"><div class="section-heading"><div><span class="eyebrow">LỘ TRÌNH DÀNH CHO BẠN</span><h2 tabindex="-1">${esc(result.goalTitle)}</h2><p>Kết quả gần nhất · ${esc(dateTime(result.completedAt))}</p></div><span class="assessment-score">${result.score} / ${result.total}</span></div><span class="badge good">${esc(result.level)}</span><p>${esc(result.summary)}</p><div class="assessment-metrics"><span>Kiến thức chung <strong>${result.foundationScore} / 4</strong></span><span>Theo hướng học <strong>${result.subjectScore} / 4</strong></span><span>Thời gian học <strong>${result.hoursPerWeek} giờ / tuần</strong></span></div><p class="hint">Khoảng ${result.steps.reduce((sum,s)=>sum+s.weeks,0)} tuần cho các bước dưới đây. Thời gian gồm học và thực hành, chỉ là ước tính. Kết quả không thay thế việc hoàn thành khóa học.</p></section><ol class="assessment-roadmap">${result.steps.map((s,i)=>`<li class="panel"><span class="assessment-step-number" aria-hidden="true">${i+1}</span><div><span class="mini-label">${s.estimatedHours} giờ · khoảng ${s.weeks} tuần</span><h3>${esc(s.title)}</h3><p>${esc(s.description)}</p>${s.courses.length?`<ul class="assessment-courses">${s.courses.map(c=>`<li><a class="text-link" href="/course-detail.html?id=${c.courseId}">${esc(c.title)} →</a><small>${esc(c.level || 'Chưa ghi cấp độ')}</small></li>`).join('')}</ul>`:'<p class="hint">Chưa có khóa phù hợp đang xuất bản cho bước này. Bạn có thể thực hành theo mục tiêu trên.</p>'}</div></li>`).join('')}</ol><section class="panel"><h2>Đối chiếu đáp án</h2><p>Mở từng câu để xem phần cần ôn.</p>${result.reviews.map((r,i)=>`<details class="assessment-review"><summary>Câu ${i+1} · ${esc(r.topic)} · ${r.selected===r.correct?'Đúng':r.selected===-1?'Chưa biết':'Cần ôn'}</summary><p>${esc(r.question)}</p><p>Bạn chọn: <strong>${esc(r.selected===-1?'Chưa biết':bank?.[i]?.options[r.selected] ?? '—')}</strong></p><p>Đáp án đúng: <strong>${esc(bank?.[i]?.options[r.correct] ?? '—')}</strong></p><p>${esc(r.explanation)}</p></details>`).join('')}</section><div class="row-actions"><button class="btn secondary" id="retakeAssessment">Làm lại bài kiểm tra</button><a class="btn" href="/courses.html">Khám phá khóa học</a></div>`;
    root.querySelector('#retakeAssessment').onclick=setup;
    const saved=draft.read();
    if(saved){const button=document.createElement('button');button.className='btn secondary';button.textContent='Tiếp tục bài đang làm';button.onclick=()=>resume(saved);root.querySelector('.row-actions').prepend(button);}
    root.querySelector('h2').focus();
  }
  const saved=draft.read();
  if(saved)resume(saved);else if(latest) showResult(latest); else setup();
}
