export const passwordMessage = 'Mật khẩu phải từ 8 đến 64 ký tự và tối đa 72 byte UTF-8';
export function newPasswordError(value) {
  return !value.trim() || value.length < 8 || value.length > 64
    || new TextEncoder().encode(value).length > 72 ? passwordMessage : '';
}
