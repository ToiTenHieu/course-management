import {test} from 'node:test';
import assert from 'node:assert/strict';

let sequence=0;
const moduleForTest=()=>import(`../../src/main/resources/static/js/api.js?test=${++sequence}`);
const json=(body,status=200)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});

test('HTML error responses keep HTTP status instead of reporting a network failure',async()=>{
  const {api,ApiError}=await moduleForTest();
  for(const status of [401,413,503]){
    globalThis.fetch=async()=>new Response('<html>Server error</html>',{status});
    await assert.rejects(api('/test'),error=>error instanceof ApiError&&error.status===status);
  }
});

test('invalid successful response envelopes are rejected',async()=>{
  const {api}=await moduleForTest();
  for(const body of [null,{},'text',{success:'yes'}]){
    globalThis.fetch=async()=>json(body);
    await assert.rejects(api('/test'),error=>error.status===200);
  }
});

test('a malformed CSRF response never sends a write request',async()=>{
  const {api}=await moduleForTest();
  let calls=0;
  globalThis.fetch=async()=>{calls++;return json({success:true,data:{token:'missing-header'}});};
  await assert.rejects(api('/test','POST',{value:1}));
  assert.equal(calls,1);
});

test('an HTML authorization error clears the cached CSRF token before retry',async()=>{
  const {api}=await moduleForTest();
  let tokens=0,writes=0;
  globalThis.fetch=async(path,options)=>{
    if(path==='/api/auth/csrf')return json({success:true,data:{headerName:'X-CSRF-TOKEN',token:`token-${++tokens}`}});
    writes++;
    if(writes===1)return new Response('Forbidden',{status:403});
    assert.equal(options.headers['X-CSRF-TOKEN'],'token-2');
    return json({success:true,data:{saved:true}});
  };
  await assert.rejects(api('/test','PUT',{}),error=>error.status===403);
  assert.deepEqual(await api('/test','PUT',{}),{saved:true});
  assert.equal(tokens,2);
});
