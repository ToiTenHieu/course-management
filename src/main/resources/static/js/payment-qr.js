// VietQR Quick Link: use the saved payment amount, never the current course price.
export function paymentQrUrl(bank, payment) {
  const bin = String(bank.bankBin ?? "");
  const account = String(bank.accountNumber ?? "");
  const amount = Number(payment.amount);
  const note = String(payment.transferNote ?? "");
  if (payment.status !== "PENDING" || !/^[0-9]{6}$/.test(bin)
      || !/^[A-Za-z0-9]{1,19}$/.test(account)
      || !Number.isSafeInteger(amount) || amount <= 0 || amount > 9999999999999
      || !/^[A-Za-z0-9 ]{1,50}$/.test(note)) return null;
  const url = new URL(`https://img.vietqr.io/image/${bin}-${account}-qr_only.png`);
  url.search = new URLSearchParams({ amount: String(amount), addInfo: note,
    accountName: String(bank.accountHolder ?? "") }).toString();
  return url.href;
}

export function mountPaymentQr(container, bank, payment) {
  const url = paymentQrUrl(bank, payment);
  const status = container.querySelector("[data-qr-status]");
  if (!url) {
    status.textContent = "Mã QR chưa khả dụng cho yêu cầu này. Bạn có thể sao chép thông tin chuyển khoản bên dưới.";
    return;
  }
  const image = container.querySelector("img");
  const retry = container.querySelector("button");
  let timer;
  const failed = () => {
    clearTimeout(timer);
    image.hidden = true;
    image.removeAttribute("src");
    status.textContent = "Chưa tải được mã QR. Thử lại hoặc sao chép thông tin chuyển khoản bên dưới.";
    retry.hidden = false;
  };
  const load = () => {
    clearTimeout(timer);
    status.textContent = "Đang tải mã QR…";
    image.hidden = true;
    retry.hidden = true;
    timer = setTimeout(failed, 15000);
    image.src = url;
  };
  image.onload = () => {
    clearTimeout(timer);
    image.hidden = false;
    status.textContent = "Quét bằng ứng dụng ngân hàng hoặc lưu ảnh QR để chọn từ thư viện ảnh. Kiểm tra người nhận, số tiền và nội dung trước khi chuyển.";
  };
  image.onerror = failed;
  retry.onclick = load;
  load();
  return () => {
    clearTimeout(timer);
    image.onload = image.onerror = null;
    image.removeAttribute("src");
  };
}
