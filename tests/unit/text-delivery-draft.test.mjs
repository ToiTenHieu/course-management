import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createTextDeliveryDraft } from '../../src/main/resources/static/js/text-delivery-draft.js';
const key1 = '10000000-0000-4000-8000-000000000001', key2 = '10000000-0000-4000-8000-000000000002';
function storage() {
  const data = new Map();
  return { getItem: k => data.get(k) ?? null, setItem: (k,v) => data.set(k,v), removeItem: k => data.delete(k) };
}
test('question and reply drafts recover the body and request key after reload', () => {
  const cache=storage(), draft=createTextDeliveryDraft('account:lesson:question',cache,()=>key1);
  draft.edit('  My question  ');
  assert.deepEqual(createTextDeliveryDraft('account:lesson:question',cache).read(), draft.read());
  assert.equal(createTextDeliveryDraft('other:lesson:question',cache).read(),null);
});
test('uncertain delivery locks the exact payload through edits and reload retries', () => {
  const cache=storage(), draft=createTextDeliveryDraft('draft',cache,()=>key1);
  const first=draft.begin('Original'); draft.failed({status:0},first.retry);
  draft.edit('Changed');
  const restored=createTextDeliveryDraft('draft',cache,()=>key2);
  const retry=restored.begin('Changed');
  assert.deepEqual(retry,{body:'Original',clientRequestId:key1,retry:true});
  // A later unauthorized response cannot prove that the original request failed.
  restored.failed({status:401},retry.retry);
  assert.equal(restored.read().pending,true);
});
test('server timeouts, failures and malformed success responses keep the pending key', () => {
  for(const status of [0,200,201,408,500,503]) {
    const draft=createTextDeliveryDraft('draft',storage(),()=>key1), first=draft.begin('Body');
    draft.failed({status},first.retry);assert.equal(draft.read().pending,true,`status ${status}`);
    assert.equal(draft.begin('Body').clientRequestId,key1);
  }
});
test('definite initial validation failures allow corrections with a new key', () => {
  const keys=[key1,key2], draft=createTextDeliveryDraft('draft',storage(),()=>keys.shift()), first=draft.begin('Rejected');
  draft.failed({status:400},first.retry);assert.equal(draft.read().pending,false);
  draft.edit('Corrected');assert.equal(draft.read().body,'Corrected');assert.equal(draft.read().key,key2);
});
test('a reload while the response is still outstanding recovers an uncertain send', () => {
  const cache=storage();createTextDeliveryDraft('draft',cache,()=>key1).begin('Body');
  const restored=createTextDeliveryDraft('draft',cache,()=>key2), request=restored.begin('Other');
  assert.equal(request.retry,true);assert.equal(request.body,'Body');assert.equal(request.clientRequestId,key1);
});
test('confirmed success or explicit discard clears the saved pending request', () => {
  const cache=storage(), draft=createTextDeliveryDraft('draft',cache,()=>key1);
  draft.begin('Body');draft.clear();assert.equal(createTextDeliveryDraft('draft',cache).read(),null);
});
test('blocked storage keeps pending content in memory and reports limited durability', () => {
  const blocked={getItem(){throw Error();},setItem(){throw Error();},removeItem(){throw Error();}};
  const draft=createTextDeliveryDraft('draft',blocked,()=>key1), first=draft.begin('Body');
  draft.failed({status:503},first.retry);assert.equal(draft.durable(),false);assert.equal(draft.read().body,'Body');
});
