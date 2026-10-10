import { api } from './api.js';
import { enhanceForms, setPending } from './experience.js';
const date = value => new Date(value+'T00:00:00Z').toLocaleDateString('vi-VN',{timeZone:'UTC'});
export async function mountWeeklyGoal() {
  const root=document.createElement('section');root.className='panel weekly-goal';
  document.querySelector('#main .stats').after(root);
  let current=null,saving=false,navigationConfirmed=false;
  const dirty=()=>current&&root.querySelector('form')&&(Number(root.querySelector('[name=target]').value)!==current.lessonTarget||root.querySelector('[name=reminder]').checked!==current.dashboardReminder);
  window.addEventListener('beforeunload',event=>{if((dirty()||saving)&&!navigationConfirmed){event.preventDefault();event.returnValue='';}});
  document.addEventListener('click',event=>{
    const anchor=event.target.closest('a[href],#logout');
    if(anchor&&(dirty()||saving)&&!event.ctrlKey&&!event.metaKey&&!event.shiftKey&&anchor.target!=='_blank'&&!(anchor.getAttribute('href')||'').startsWith('#')) {
      if(saving||!confirm('Mục tiêu chưa lưu. Rời trang và bỏ thay đổi?')) {event.preventDefault();event.stopImmediatePropagation();}
      else {navigationConfirmed=true;setTimeout(()=>{navigationConfirmed=false;},3000);}
    }
  },true);
  function draw(message='') {
    const g=current,remaining=Math.max(0,g.lessonTarget-g.completed);
    root.innerHTML=`<div class="section-heading"><div><span class="eyebrow">MỖI TUẦN MỘT BƯỚC</span><h2>Mục tiêu tuần này</h2><p>${date(g.weekStart)} – ${date(g.weekEnd)} · Giờ Việt Nam</p></div><strong class="goal-count">${g.completed} <small>/ ${g.lessonTarget} bài</small></strong></div><div class="goal-progress" role="progressbar" aria-label="Tiến độ mục tiêu tuần" aria-valuemin="0" aria-valuemax="${g.lessonTarget}" aria-valuenow="${Math.min(g.lessonTarget,g.completed)}"><span style="width:${Math.min(100,100*g.completed/g.lessonTarget)}%"></span></div><p class="goal-nudge">${!g.configured?'Chọn số bài phù hợp và lưu để bắt đầu kế hoạch tuần này.':!remaining?'Bạn đã đạt mục tiêu tuần. Tiếp tục theo nhịp của bạn!':g.dashboardReminder?`Còn ${remaining} bài để đạt mục tiêu. Dành một khoảng thời gian cho bài tiếp theo nhé.`:'Mục tiêu đã lưu. Bạn có thể điều chỉnh theo lịch của mình.'}</p><form><div class="goal-controls"><label>Số bài mỗi tuần<input type="number" name="target" min="1" max="50" step="1" required value="${g.lessonTarget}"></label><label class="goal-checkbox"><input type="checkbox" name="reminder" ${g.dashboardReminder?'checked':''}>Nhắc mục tiêu tại tổng quan</label><button class="btn compact">Lưu mục tiêu</button><button type="button" class="btn secondary compact" data-reload>Tải lại mục tiêu</button></div><p data-status role="status" aria-live="polite">${message}</p></form><p class="hint">Mỗi bài được tính một lần vào tuần hoàn thành đầu tiên, kể cả khi khóa học thay đổi sau đó. Mục tiêu áp dụng riêng từng tuần; đầu tuần mới bạn chọn lại kế hoạch. Nhắc chỉ hiển thị tại đây.</p><details><summary>Hoạt động 8 tuần gần nhất</summary><div class="goal-history">${g.history.map(w=>`<div><span>${date(w.weekStart)} – ${date(w.weekEnd)}</span><strong>${w.completed} bài hoàn thành</strong><small>${w.lessonTarget===null?'Chưa đặt mục tiêu':`Mục tiêu ${w.lessonTarget} bài${w.completed>=w.lessonTarget?' · Đã đạt':''}`}</small></div>`).join('')}</div></details>`;
    enhanceForms(root);
    root.querySelector('form').addEventListener('submit',async event=>{
      event.preventDefault();if(saving) return;
      const button=root.querySelector('button'),status=root.querySelector('[data-status]');
      const request={weekStart:g.weekStart,expectedRevision:g.revision,lessonTarget:Number(root.querySelector('[name=target]').value),dashboardReminder:root.querySelector('[name=reminder]').checked};
      saving=true;setPending(button,true);root.querySelectorAll('input,[data-reload]').forEach(control=>control.disabled=true);status.textContent='Đang lưu mục tiêu…';
      try {current=await api('/learning-goal','PUT',request);draw('Đã lưu mục tiêu tuần.');root.querySelector('button').focus();}
      catch(error) {status.textContent=error.message;}
      finally {saving=false;if(button.isConnected){setPending(button,false);root.querySelectorAll('input,[data-reload]').forEach(control=>control.disabled=false);}}
    });
    root.querySelector('[data-reload]').onclick=()=>{if(!dirty()||confirm('Bỏ thay đổi chưa lưu và tải lại mục tiêu?')) load();};
  }
  async function load() {
    root.innerHTML='<p role="status">Đang tải mục tiêu tuần…</p>';
    try {current=await api('/learning-goal');draw();}
    catch(error) {
      root.innerHTML='<h2>Mục tiêu tuần</h2><p role="alert"></p><button class="btn secondary compact">Thử lại</button>';
      root.querySelector('p').textContent=error.message;root.querySelector('button').onclick=load;
    }
  }
  await load();
}
