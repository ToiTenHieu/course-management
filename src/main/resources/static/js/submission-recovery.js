// Keep the exact request while its write outcome is uncertain.
export function createSubmissionRecovery(send, lookup) {
  let pending = null;
  return {
    get unresolved() { return pending !== null; },
    reset() { pending = null; },
    async submit(payload) {
      if (pending) {
        const view = await lookup();
        if (view.submission) { pending = null; return view.submission; }
        payload = pending;
      }
      pending = payload;
      try {
        const saved = await send(pending);
        pending = null;
        return saved;
      } catch (error) {
        if (error.status >= 400 && error.status < 500 && error.status !== 408 && error.status !== 409) {
          pending = null;
          throw error;
        }
        try {
          const view = await lookup();
          if (view.submission) { pending = null; return view.submission; }
          if (error.status === 409) pending = null;
        } catch { /* Retain the original request for the next reconciliation. */ }
        throw error;
      }
    },
  };
}
