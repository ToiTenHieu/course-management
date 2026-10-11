import { createSessionDraft } from './session-draft.js';

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function createTextDeliveryDraft(key, storage, newKey = () => crypto.randomUUID()) {
  const store = createSessionDraft(key, value => !!value && value.version === 1
    && typeof value.body === 'string' && value.body.length <= 5000
    && uuid.test(value.key) && typeof value.pending === 'boolean', storage);
  let durable = true;
  const save = value => { durable = store.write(value); return value; };
  return {
    read: () => store.read(),
    durable: () => durable,
    edit(body) {
      const previous = store.read();
      // Keep the exact payload and request key until an uncertain send is resolved.
      if (previous?.pending) return previous;
      if (!body) { store.clear(); durable = true; return null; }
      if (previous?.body === body) return previous;
      return save({ version: 1, body, key: newKey(), pending: false });
    },
    begin(body) {
      const previous = store.read();
      const value = previous?.pending ? previous : this.edit(body);
      if (!value?.body.trim()) throw new TypeError('Empty message');
      save({ ...value, pending: true });
      return { body: value.body, clientRequestId: value.key, retry: !!previous?.pending };
    },
    failed(error, retry) {
      const value = store.read();
      if (!value) return;
      const status = error?.status ?? 0;
      const uncertain = retry || status === 0 || status === 408 || status >= 500 || status >= 200 && status < 300;
      save({ ...value, pending: uncertain });
    },
    clear() { store.clear(); durable = true; },
  };
}
