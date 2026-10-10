import { api } from './api.js';
import { enhanceForms, setPending } from './experience.js';
const esc=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const accepted='Nếu email thuộc tài khoản đang hoạt động, bạn sẽ nhận được liên kết đặt lại mật khẩu. Vui lòng kiểm tra thư đến và thư rác.';
export async function mountPasswordRecovery(root,config) {
  let token=new URLSearchParams(location.hash.slice(1)).get('token')||'';
  if(location.hash)history.replaceState(null,'',location.pathname+location.search);
  const reset=!!token;
  root.innerHTML=`<main class="recovery-page" id="main"><a class="brand" href="/"><img src="/assets/mark.svg" alt=""><span>Course<span class="brand-sub">MANAGEMENT</span></span></a><section class="panel recovery-card"><a class="back-link" href="/login.html">← Về đăng nhập</a><span class="eyebrow">TRỞ LẠI HÀNH TRÌNH HỌC</span><h1>${reset?'Đặt mật khẩu mới':'Lấy lại mật khẩu'}</h1><div data-content></div></section></main>`;
  const content=root.querySelector('[data-content]');
  if(!config.passwordRecovery?.enabled) {content.innerHTML='<p>Lấy lại mật khẩu chưa được bật. Vui lòng liên hệ quản trị viên để được hỗ trợ.</p>';return;}
  if(reset&&!/^[A-Za-z0-9_-]{43}$/.test(token)) {content.innerHTML='<p role="alert">Liên kết không hợp lệ hoặc đã hết hạn.</p><a class="btn" href="/password-recovery.html">Yêu cầu liên kết mới</a>';return;}
  content.innerHTML=reset?`<p>Chọn mật khẩu từ 8–64 ký tự, tối đa 72 byte UTF-8. Sau khi lưu, các phiên đăng nhập cũ sẽ hết hiệu lực.</p><form><label>Mật khẩu mới<input type="password" name="newPassword" required minlength="8" maxlength="64" autocomplete="new-password"></label><label>Nhập lại mật khẩu mới<input type="password" name="confirmPassword" required minlength="8" maxlength="64" autocomplete="new-password"></label><p data-error role="alert"></p><button class="btn full">Đổi mật khẩu</button></form><a class="text-link recovery-new-link" href="/password-recovery.html">Yêu cầu một liên kết mới</a>`:`<p>Nhập email đã đăng ký. Liên kết dùng một lần và có hiệu lực trong 15 phút.</p><form><label>Email đã đăng ký<input type="email" name="email" required maxlength="100" autocomplete="email"></label><p data-error role="alert"></p><p data-status role="status" aria-live="polite"></p><button class="btn full">Gửi liên kết đặt lại</button></form>${config.passwordRecovery.demoMailbox?'<p class="hint recovery-demo-note">Bản demo lưu thư tại <strong>Hộp thư thử nghiệm</strong> của admin. Thư này không được gửi ra email bên ngoài.</p>':''}`;
  enhanceForms(content);
  const form=content.querySelector('form'),button=form.querySelector('button.btn'),error=form.querySelector('[data-error]');let pending=false,cooldown=0;
  const password=form.elements.newPassword,confirmation=form.elements.confirmPassword;
  if(password) {
    const validate=()=>{
      password.setCustomValidity(new TextEncoder().encode(password.value).length>72?'Mật khẩu tối đa 72 byte UTF-8.':'' );
      confirmation.setCustomValidity(confirmation.value&&confirmation.value!==password.value?'Mật khẩu nhập lại chưa khớp.':'');
    };
    password.addEventListener('input',validate);confirmation.addEventListener('input',validate);
    window.addEventListener('beforeunload',event=>{if(password.value||confirmation.value||pending){event.preventDefault();event.returnValue='';}});
  }
  form.addEventListener('submit',async event=>{
    event.preventDefault();if(pending||Date.now()<cooldown)return;
    error.textContent='';pending=true;setPending(button,true);
    const body=reset?{token,newPassword:password.value}:{email:form.elements.email.value.trim()};
    form.querySelectorAll('input').forEach(input=>input.disabled=true);
    try {
      await api(reset?'/auth/reset-password':'/auth/forgot-password','POST',body);
      if(reset) {
        password.value='';confirmation.value='';token='';
        content.innerHTML='<div class="recovery-success"><span class="eyebrow">ĐÃ LƯU MẬT KHẨU MỚI</span><h2>Bạn có thể đăng nhập lại</h2><p>Dùng mật khẩu mới để tiếp tục học. Các phiên đăng nhập cũ đã hết hiệu lực.</p><a class="btn full" href="/login.html">Đăng nhập</a></div>';
        content.querySelector('h2').tabIndex=-1;content.querySelector('h2').focus();
      } else {
        form.querySelector('[data-status]').textContent=accepted;cooldown=Date.now()+60000;
        const timer=setInterval(()=>{
          if(!button.isConnected){clearInterval(timer);return;}
          const remaining=Math.max(0,Math.ceil((cooldown-Date.now())/1000));button.disabled=remaining>0;button.textContent=remaining?`Gửi lại sau ${remaining} giây`:'Gửi lại liên kết';if(!remaining)clearInterval(timer);
        },1000);
      }
    } catch(failure) {error.textContent=failure.message;if(reset&&failure.status===400)error.textContent+=' Bạn có thể mở lại liên kết trong thư hoặc yêu cầu liên kết mới.';}
    finally {pending=false;if(button.isConnected){setPending(button,false);button.disabled=Date.now()<cooldown;form.querySelectorAll('input').forEach(input=>input.disabled=false);}}
  });
}

export async function mountRecoveryMailbox(root) {
  root.innerHTML='<p>Thư thử nghiệm chỉ dành cho admin, giữ trong bộ nhớ máy chủ tối đa 15 phút. Khởi động lại sẽ xóa hộp thư; thư đã dùng có thể vẫn còn hiển thị đến lúc hết hạn. Không gửi email bên ngoài.</p><button class="btn secondary compact" data-refresh>Làm mới hộp thư</button><p data-status role="status" aria-live="polite"></p><div class="recovery-mails" data-mails></div>';
  const button=root.querySelector('[data-refresh]');
  async function load() {
    const status=root.querySelector('[data-status]');setPending(button,true);status.textContent='Đang tải thư…';
    try {
      const messages=await api('/demo/recovery-mailbox');status.textContent=`${messages.length} thư còn trong hộp thư thử nghiệm`;
      root.querySelector('[data-mails]').innerHTML=messages.length?messages.map(message=>{
        let link='';try{const url=new URL(message.resetUrl);if(['http:','https:'].includes(url.protocol)&&url.pathname==='/password-recovery.html'&&/^#token=[A-Za-z0-9_-]{43}$/.test(url.hash))link=url.href;}catch{}
        return `<article class="recovery-mail"><div class="section-heading"><div><span class="eyebrow">${esc(message.recipient)}</span><h2>${esc(message.subject)}</h2></div><small>Hết hạn ${esc(new Date(message.expiresAt+'+07:00').toLocaleString('vi-VN',{timeZone:'Asia/Ho_Chi_Minh'}))}</small></div><p>Liên kết dùng một lần. Sao chép liên kết hoặc mở trong tab mới để thử quy trình đặt lại mật khẩu.</p>${link?`<a class="btn compact" href="${esc(link)}" target="_blank" rel="noopener noreferrer">Mở liên kết đặt lại ↗</a><button class="btn secondary compact" data-copy="${esc(link)}">Sao chép liên kết</button>`:''}<p data-copy-status role="status" aria-live="polite"></p></article>`;
      }).join(''):'<p class="muted-text">Chưa có thư. Gửi yêu cầu tại trang Lấy lại mật khẩu rồi làm mới tại đây.</p>';
      root.querySelectorAll('[data-copy]').forEach(copy=>copy.onclick=async()=>{
        const status=copy.closest('article').querySelector('[data-copy-status]');
        try {await navigator.clipboard.writeText(copy.dataset.copy);status.textContent='Đã sao chép liên kết.';}
        catch {status.replaceChildren();const input=document.createElement('input');input.readOnly=true;input.value=copy.dataset.copy;input.setAttribute('aria-label','Liên kết để sao chép');status.append(input);input.focus();input.select();}
      });
    }catch(failure){status.textContent=failure.message;root.querySelector('[data-mails]').replaceChildren();}
    finally{setPending(button,false);}
  }
  button.onclick=load;await load();
}
