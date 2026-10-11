const pages = new Set(['dashboard','courses','course-detail','teacher','learn','my-courses','wishlist',
  'learning-assessment','users','payments','my-payments','notifications','profile','reports','audit',
  'settings','recovery-mailbox']);

export function loginDestination(value) {
  if (typeof value !== 'string' || /[\\\u0000-\u001f\u007f]/.test(value)) return '/dashboard.html';
  const match = /^\/([a-z-]+)\.html(?:\?[^#]*)?(?:#[a-zA-Z0-9_-]+)?$/.exec(value);
  return match && pages.has(match[1]) ? value : '/dashboard.html';
}

export function loginUrl(current = location) {
  const next = loginDestination(current.pathname + current.search + current.hash);
  return '/login.html?' + new URLSearchParams({ next });
}
