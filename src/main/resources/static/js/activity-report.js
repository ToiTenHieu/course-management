import { api } from './api.js';
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const number = value => Number(value).toLocaleString('vi-VN',{maximumFractionDigits:2});
const localDate = date => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
export async function mountActivityReport() {
  const root=document.createElement('section');root.className='panel activity-report';
  const end=new Date(),start=new Date();start.setDate(start.getDate()-29);
  const params=new URLSearchParams(location.search);
  const from=/^\d{4}-\d{2}-\d{2}$/.test(params.get('from')||'')?params.get('from'):localDate(start);
  const to=/^\d{4}-\d{2}-\d{2}$/.test(params.get('to')||'')?params.get('to'):localDate(end);
  root.innerHTML=`<h2>Hoạt động theo thời gian</h2><form class="report-filters"><label>Từ ngày<input type="date" name="from" required value="${from}"></label><label>Đến ngày<input type="date" name="to" required value="${to}"></label><button class="btn compact">Xem báo cáo</button><button type="button" class="btn secondary compact" data-export disabled>Xuất CSV / Excel</button></form><p class="hint">Tối đa 366 ngày, gồm cả ngày đầu và cuối. Doanh thu tính theo ngày xác nhận, chỉ gồm thanh toán đã xác nhận. Đăng ký tính theo ngày đăng ký; hoàn thành và tiến độ là trạng thái hiện tại của nhóm đăng ký đó. CSV UTF-8 mở được bằng Excel.</p><div class="student-summary" data-summary></div><p data-status role="status"></p><div data-rows></div><nav class="pagination" aria-label="Phân trang báo cáo hoạt động" data-pager></nav>`;
  document.querySelector('.main-content')?.prepend(root);
  if (!root.isConnected) document.querySelector('#app .panel').before(root);
  const form=root.querySelector('form');let page=0,revision=0,applied=null;
  async function load() {
    const request=++revision,query=new URLSearchParams({from:form.elements.from.value,to:form.elements.to.value,page:String(page),size:'20'});
    const status=root.querySelector('[data-status]');status.textContent='Đang tải báo cáo…';root.querySelector('[data-export]').disabled=true;
    try {
      const report=await api('/reports/activity?'+query);
      if (request!==revision) return;
      applied={from:report.from,to:report.to};
      const url=new URL(location.href);url.searchParams.set('from',report.from);url.searchParams.set('to',report.to);history.replaceState(null,'',url);
      const s=report.summary;
      root.querySelector('[data-summary]').innerHTML=[['Đăng ký',s.enrollments],['Đã hoàn thành hiện tại',s.completed],['Thanh toán xác nhận',s.confirmedPayments],['Doanh thu xác nhận',number(s.revenue)+' ₫']].map(([label,value])=>`<div><strong>${typeof value==='number'?number(value):value}</strong><span>${label}</span></div>`).join('');
      const list=report.courses;status.textContent=`${list.totalElements} khóa có hoạt động từ ${report.from} đến ${report.to}`;
      root.querySelector('[data-rows]').innerHTML=list.content.length?`<div class="table-scroll"><table><thead><tr><th>Khóa học</th><th>Đăng ký</th><th>Hoàn thành hiện tại</th><th>Tiến độ TB</th><th>Thanh toán xác nhận</th><th>Doanh thu</th></tr></thead><tbody>${list.content.map(r=>`<tr><td><a href="/course-detail.html?id=${r.courseId}">${esc(r.title)}</a></td><td>${number(r.enrollments)}</td><td>${number(r.completed)}</td><td>${number(r.averageProgress)}%</td><td>${number(r.confirmedPayments)}</td><td>${number(r.revenue)} ₫</td></tr>`).join('')}</tbody></table></div>`:'<p class="muted-text">Chưa có hoạt động trong khoảng này.</p>';
      const nav=root.querySelector('[data-pager]');nav.innerHTML=`<button class="chip" data-prev ${page===0?'disabled':''}>← Trước</button><span>Trang ${page+1}/${Math.max(1,list.totalPages)}</span><button class="chip" data-next ${page+1>=list.totalPages?'disabled':''}>Sau →</button>`;
      nav.querySelector('[data-prev]').onclick=()=>{page--;load();};nav.querySelector('[data-next]').onclick=()=>{page++;load();};
      root.querySelector('[data-export]').disabled=false;
    } catch(error) { if(request===revision) {status.textContent=error.message;applied=null;root.querySelector('[data-summary]').replaceChildren();root.querySelector('[data-rows]').replaceChildren();root.querySelector('[data-pager]').replaceChildren();} }
  }
  form.addEventListener('input',()=>{
    revision++;applied=null;root.querySelector('[data-export]').disabled=true;
    root.querySelectorAll('[data-pager] button').forEach(button=>button.disabled=true);
    root.querySelector('[data-status]').textContent='Khoảng ngày đã đổi. Bấm Xem báo cáo để áp dụng.';
  });
  form.onsubmit=event=>{event.preventDefault();page=0;load();};
  root.querySelector('[data-export]').onclick=async()=>{
    if(!applied) return;
    const range={...applied},button=root.querySelector('[data-export]');button.disabled=true;
    try {
      const response=await fetch('/api/reports/activity/export?'+new URLSearchParams(range),{credentials:'same-origin'});
      if(!response.ok) {let message='Không xuất được báo cáo';try{message=(await response.json()).message||message;}catch{}throw new Error(message);}
      const url=URL.createObjectURL(await response.blob()),a=document.createElement('a');a.href=url;a.download=`bao-cao-${range.from}-${range.to}.csv`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
    }catch(error){root.querySelector('[data-status]').textContent=error.message;}
    finally{button.disabled=!applied;}
  };
  await load();
}
