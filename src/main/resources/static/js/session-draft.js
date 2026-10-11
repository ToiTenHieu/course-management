// Drafts belong to one browser tab. Callers include the account and content version in the key.
export function createSessionDraft(key, validate, storage) {
  let memory = null, fallback = false;
  const copy = value => JSON.parse(JSON.stringify(value));
  const cache = () => storage ?? globalThis.sessionStorage;
  return {
    read() {
      if (fallback) return memory === null ? null : copy(memory);
      try {
        const raw = cache().getItem(key);
        if (raw !== null) {
          let value;
          try { value = JSON.parse(raw); } catch { this.clear(); return null; }
          if (!validate(value)) { this.clear(); return null; }
          memory = copy(value);
        } else memory = null;
      } catch { /* Keep the mounted draft when storage cannot be read. */ }
      return memory === null ? null : copy(memory);
    },
    write(value) {
      if (!validate(value)) throw new TypeError('Invalid draft');
      memory = copy(value);
      try { cache().setItem(key, JSON.stringify(memory)); fallback=false; return true; }
      catch { fallback=true; return false; }
    },
    clear() {
      memory = null;
      try { cache().removeItem(key); fallback=false; } catch { fallback=true; }
    },
  };
}
