import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createLessonNoteDraft } from '../../src/main/resources/static/js/lesson-note-draft.js';
const cache = () => {
  const data = new Map();
  return { getItem: key => data.get(key) ?? null, setItem: (key, value) => data.set(key, value), removeItem: key => data.delete(key) };
};
test('unsaved notes, including deleting all text, recover against the same server version', () => {
  const storage = cache();
  createLessonNoteDraft(1, 2, 3, storage).write('', 'Old note');
  assert.deepEqual(createLessonNoteDraft(1, 2, 3, storage).read('Old note'), { note: '', baseNote: 'Old note', conflict: false });
});
test('recovery detects a newer server note without changing either version', () => {
  const storage = cache(), draft = createLessonNoteDraft(1, 2, 3, storage);
  draft.write('Unsaved edit', 'Original');
  assert.deepEqual(draft.read('Other device'), { note: 'Unsaved edit', baseNote: 'Original', conflict: true });
  assert.equal(draft.read('Original').conflict, false);
});
test('a lost save response clears the draft when the server already has its contents', () => {
  const storage = cache(), draft = createLessonNoteDraft(1, 2, 3, storage);
  draft.write('Saved edit', 'Original');
  assert.equal(draft.read('Saved edit'), null);
  assert.equal(createLessonNoteDraft(1, 2, 3, storage).read('Newer note'), null);
});
test('note drafts are isolated by account, enrollment and lesson and discard explicitly', () => {
  const storage = cache(), draft = createLessonNoteDraft(1, 2, 3, storage);
  draft.write('Private draft', '');
  for (const ids of [[4,2,3], [1,4,3], [1,2,4]]) assert.equal(createLessonNoteDraft(...ids, storage).read(''), null);
  draft.clear(); assert.equal(draft.read(''), null);
});
test('blocked storage keeps the latest mounted note and signals limited durability', () => {
  const storage = cache(), draft = createLessonNoteDraft(1, 2, 3, storage);
  draft.write('Old edit', '');
  storage.setItem = () => { throw new Error('quota'); };
  assert.equal(draft.write('Latest edit', ''), false);
  assert.equal(draft.read('').note, 'Latest edit');
  assert.throws(() => draft.write('x'.repeat(10001), ''), TypeError);
});
