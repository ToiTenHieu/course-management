import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createSessionDraft } from '../../src/main/resources/static/js/session-draft.js';
const valid = value => !!value && value.version===1 && Array.isArray(value.answers) && value.answers.every(a=>a===null || Number.isInteger(a)&&a>=-1&&a<=3);
function storage() {
  const data=new Map();
  return {getItem:key=>data.get(key)??null,setItem:(key,value)=>data.set(key,value),removeItem:key=>data.delete(key)};
}
test('a partially answered draft survives a new mounted session, including unknown answers',()=>{
  const cache=storage(),draft={version:1,answers:[null,-1,2]};
  assert.equal(createSessionDraft('student:1',valid,cache).write(draft),true);
  assert.deepEqual(createSessionDraft('student:1',valid,cache).read(),draft);
});
test('account and question-version keys cannot restore another draft',()=>{
  const cache=storage();createSessionDraft('student:1:version:1',valid,cache).write({version:1,answers:[3]});
  assert.equal(createSessionDraft('student:2:version:1',valid,cache).read(),null);
  assert.equal(createSessionDraft('student:1:version:2',valid,cache).read(),null);
});
test('stale or tampered drafts are discarded before use',()=>{
  const cache=storage();cache.setItem('draft',JSON.stringify({version:2,answers:[0]}));
  assert.equal(createSessionDraft('draft',valid,cache).read(),null);
  assert.equal(cache.getItem('draft'),null);
  cache.setItem('draft','{broken');
  assert.equal(createSessionDraft('draft',valid,cache).read(),null);
  assert.equal(cache.getItem('draft'),null);
});
test('blocked storage retains an isolated in-memory copy and reports that reload recovery is unavailable',()=>{
  const blocked={getItem(){throw new Error('blocked');},setItem(){throw new Error('quota');},removeItem(){throw new Error('blocked');}};
  const store=createSessionDraft('draft',valid,blocked),draft={version:1,answers:[2]};
  assert.equal(store.write(draft),false);draft.answers[0]=0;
  const restored=store.read();restored.answers[0]=1;
  assert.deepEqual(store.read(),{version:1,answers:[2]});
  store.clear();assert.equal(store.read(),null);
});
test('clearing a saved result removes its draft from a later page load',()=>{
  const cache=storage(),store=createSessionDraft('draft',valid,cache);
  store.write({version:1,answers:[0]});store.clear();
  assert.equal(createSessionDraft('draft',valid,cache).read(),null);
});
test('a full storage quota cannot replace the current draft with the older saved copy',()=>{
  const cache=storage(),store=createSessionDraft('draft',valid,cache);
  store.write({version:1,answers:[0]});cache.setItem=()=>{throw new Error('full');};
  assert.equal(store.write({version:1,answers:[2]}),false);
  assert.deepEqual(store.read(),{version:1,answers:[2]});
});
