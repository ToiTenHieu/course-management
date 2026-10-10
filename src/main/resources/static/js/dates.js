const zone = 'Asia/Ho_Chi_Minh';
export function serverDate(value) {
  if (!value) return null;
  const text = String(value);
  return new Date(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(text)
    && !/(Z|[+-]\d{2}:?\d{2})$/i.test(text) ? text + '+07:00' : text);
}
export const dateTime = value => value ? serverDate(value).toLocaleString('vi-VN', { timeZone: zone }) : '';
export const dateOnly = value => value ? serverDate(value).toLocaleDateString('vi-VN', { timeZone: zone }) : '—';
export const timeOnly = value => value ? serverDate(value).toLocaleTimeString('vi-VN', { timeZone: zone }) : '';
export function today(offsetDays = 0, instant = new Date()) {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(instant);
  const get = type => parts.find(p => p.type === type).value;
  const date = new Date(`${get('year')}-${get('month')}-${get('day')}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}
