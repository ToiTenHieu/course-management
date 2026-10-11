import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createSubmissionRecovery } from '../../src/main/resources/static/js/submission-recovery.js';
const failure = status => Object.assign(new Error('Request failed'), { status });

test('a lost write response reconciles an existing submission without another POST', async () => {
  const saved = {submissionId: 3, answer: 'original'}; let sends = 0;
  const recovery = createSubmissionRecovery(async () => {sends++;throw failure(0);}, async () => ({submission:saved}));
  assert.equal(await recovery.submit({answer:'original'}),saved);
  assert.equal(sends,1);assert.equal(recovery.unresolved,false);
});

test('unreachable reconciliation preserves the exact payload and verifies before retrying', async () => {
  const original = {answer:'original',file:new Uint8Array([1,2]),submissionKey:'same-key'};
  let sends=0,lookups=0;
  const recovery=createSubmissionRecovery(async payload=>{
    assert.equal(payload,original);if(++sends===1)throw failure(503);return {submissionId:4};
  },async()=>{if(++lookups===1)throw failure(0);return {submission:null};});
  await assert.rejects(recovery.submit(original));assert.equal(recovery.unresolved,true);
  assert.deepEqual(await recovery.submit({answer:'edited'}),{submissionId:4});
  assert.equal(lookups,2);assert.equal(recovery.unresolved,false);
});

test('a definite validation failure permits editing and a fresh submission',async()=>{
  let sends=0;
  const recovery=createSubmissionRecovery(async payload=>{if(++sends===1)throw failure(400);return payload;},async()=>{throw new Error('Unexpected lookup');});
  await assert.rejects(recovery.submit({answer:'bad'}));assert.equal(recovery.unresolved,false);
  assert.deepEqual(await recovery.submit({answer:'fixed'}),{answer:'fixed'});
});

test('a submission conflict opens the already saved result',async()=>{
  const saved={submissionId:8};
  const recovery=createSubmissionRecovery(async()=>{throw failure(409);},async()=>({submission:saved}));
  assert.equal(await recovery.submit({answer:'second attempt'}),saved);
});

test('a malformed successful response reconciles instead of unlocking a saved answer',async()=>{
  const saved={submissionId:9};let sends=0;
  const recovery=createSubmissionRecovery(async()=>{sends++;throw failure(200);},async()=>({submission:saved}));
  assert.equal(await recovery.submit({answer:'original'}),saved);
  assert.equal(sends,1);assert.equal(recovery.unresolved,false);
});

test('a confirmed version conflict without a submission permits editing and reloading the prompt',async()=>{
  const recovery=createSubmissionRecovery(async()=>{throw failure(409);},async()=>({submission:null}));
  await assert.rejects(recovery.submit({answer:'old prompt'}));
  assert.equal(recovery.unresolved,false);
});
