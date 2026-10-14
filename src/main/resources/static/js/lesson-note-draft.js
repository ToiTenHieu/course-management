import { createSessionDraft } from './session-draft.js';

// The original server note lets recovery distinguish an unsaved edit from a change on another device.
export function createLessonNoteDraft(userId, enrollmentId, lessonId, storage) {
  const valid = value => value && typeof value.note === 'string' && value.note.length <= 10000
    && typeof value.baseNote === 'string' && value.baseNote.length <= 10000;
  const draft = createSessionDraft(`cm-lesson-note:${userId}:${enrollmentId}:${lessonId}`, valid, storage);
  return {
    read(serverNote) {
      const saved = draft.read();
      if (!saved) return null;
      if (saved.note === serverNote) { draft.clear(); return null; }
      return { ...saved, conflict: saved.baseNote !== serverNote };
    },
    write(note, baseNote) {
      if (note === baseNote) { draft.clear(); return true; }
      return draft.write({ note, baseNote });
    },
    clear() { draft.clear(); },
  };
}
