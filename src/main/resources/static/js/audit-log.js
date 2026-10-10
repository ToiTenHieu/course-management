import { dateTime, today } from './dates.js';
import { api } from './api.js';
const esc=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const actions={PAYMENT_CONFIRMED:'Xác nhận thanh toán',PAYMENT_REJECTED:'Từ chối thanh toán',USER_ROLE_CHANGED:'Đổi vai trò',USER_STATUS_CHANGED:'Khóa / mở tài khoản',COURSE_STATUS_CHANGED:'Đổi trạng thái khóa học'};
const states={PENDING:'Chờ duyệt',CONFIRMED:'Đã xác nhận',REJECTED:'Đã từ chối',DRAFT:'Bản nháp',PUBLISHED:'Đã xuất bản',ARCHIVED:'Đã lưu trữ',ADMIN:'Quản trị viên',TEACHER:'Giảng viên',STUDENT:'Học viên',true:'Hoạt động',false:'Đã khóa',SYSTEM:'Hệ thống'};
export async function mountAuditLog(root) {
  const params=new URLSearchParams(location.search),now=today(),start=today(-29);
  const validDate=value=>/^\d{4}-\d{2}-\d{2}$/.test(value||'');
  root.innerHTML=`<form class="report-filters audit-filters"><label>Từ ngày<input type="date" name="from" required value="${validDate(params.get('from'))?params.get('from'):start}"></label><label>Đến ngày<input type="date" name="to" required value="${validDate(params.get('to'))?params.get('to'):now}"></label><label>Thao tác<select name="action"><option value="">Tất cả thao tác</option>${Object.entries(actions).map(([value,label])=>`<option value="${value}" ${params.get('action')===value?'selected':''}>${label}</option>`).join('')}</select></label><label>Người thực hiện / đối tượng<input type="search" name="search" maxlength="100" placeholder="Tên tài khoản hoặc khóa học" value="${esc((params.get('search')||'').slice(0,100))}"></label><button class="btn compact">Lọc lịch sử</button></form><p class="hint">Thời gian theo giờ Việt Nam. Lưu thao tác thành công từ khi tính năng được bật; tối đa 366 ngày mỗi lần tra cứu. Tên và trạng thái được giữ tại thời điểm thao tác.</p><p data-status role="status" aria-live="polite"></p><div data-rows></div><nav class="pagination" aria-label="Phân trang lịch sử thao tác" data-pager></nav>`;
  const form=root.querySelector('form');let page=0,version=0;
  async function load() {
    const request=++version,query=new URLSearchParams(new FormData(form));query.set('page',page);query.set('size','20');
    const status=root.querySelector('[data-status]');status.textContent='Đang tải lịch sử…';root.querySelectorAll('[data-pager] button').forEach(button=>button.disabled=true);
    try {
      const result=await api('/audit-logs?'+query);if(request!==version) return;
      const url=new URL(location.href);url.search=query.toString();url.searchParams.delete('page');url.searchParams.delete('size');history.replaceState(null,'',url);
      status.textContent=`${result.totalElements} thao tác phù hợp`;
      root.querySelector('[data-rows]').innerHTML=result.content.length?`<div class="table-scroll"><table class="audit-table"><thead><tr><th>Thời điểm</th><th>Người thực hiện</th><th>Thao tác / đối tượng</th><th>Thay đổi</th></tr></thead><tbody>${result.content.map(r=>`<tr><td>${esc(dateTime(r.occurredAt))}</td><td><strong>${esc(r.actorUsername)}</strong><small>${esc(states[r.actorRole]||r.actorRole)}</small></td><td><strong>${esc(actions[r.action]||r.action)}</strong><span>${esc(r.targetName)}</span><small>${({USER:'Tài khoản',COURSE:'Khóa học',PAYMENT:'Thanh toán'})[r.targetType]||esc(r.targetType)} #${r.targetId}</small></td><td>${esc(states[r.beforeValue]||r.beforeValue)} → ${esc(states[r.afterValue]||r.afterValue)}</td></tr>`).join('')}</tbody></table></div>`:'<p class="muted-text">Chưa có thao tác phù hợp với bộ lọc.</p>';
      const nav=root.querySelector('[data-pager]');nav.innerHTML=`<button class="chip" data-prev ${page===0?'disabled':''}>← Trước</button><span>Trang ${page+1}/${Math.max(1,result.totalPages)}</span><button class="chip" data-next ${page+1>=result.totalPages?'disabled':''}>Sau →</button>`;
      nav.querySelector('[data-prev]').onclick=()=>{page--;load();};nav.querySelector('[data-next]').onclick=()=>{page++;load();};
    }catch(error){if(request===version){status.textContent=error.message;root.querySelector('[data-rows]').replaceChildren();root.querySelector('[data-pager]').replaceChildren();}}
  }
  form.addEventListener('input',()=>{version++;root.querySelectorAll('[data-pager] button').forEach(button=>button.disabled=true);root.querySelector('[data-status]').textContent='Bộ lọc đã đổi. Bấm Lọc lịch sử để áp dụng.';});
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity()) return;page=0;load();};
  await load();
}
