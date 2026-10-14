import { test } from 'node:test';
import assert from 'node:assert/strict';
import { setPending } from '../../src/main/resources/static/js/experience.js';
const button = (disabled=false) => ({innerHTML:'Lưu <strong>thay đổi</strong>',disabled,attributes:new Map(),
  setAttribute(key,value){this.attributes.set(key,value);},removeAttribute(key){this.attributes.delete(key);}});
test('repeated pending updates restore the original label rather than another spinner',()=>{
  const control=button();setPending(control,true);setPending(control,true,'Đang tải…');
  assert.equal(control.disabled,true);assert.equal(control.attributes.get('aria-busy'),'true');
  setPending(control,false);
  assert.equal(control.innerHTML,'Lưu <strong>thay đổi</strong>');assert.equal(control.disabled,false);
  assert.equal(control.attributes.has('aria-busy'),false);
});
test('ending pending state preserves a control that was already unavailable',()=>{
  const control=button(true);setPending(control,true);setPending(control,false);assert.equal(control.disabled,true);
  setPending(control,false);assert.equal(control.disabled,true);
});
test('pending labels escape markup and independent buttons keep their own state',()=>{
  const first=button(),second=button();second.innerHTML='Tải lại';
  setPending(first,true,'<img src=x>');setPending(second,true);assert.doesNotMatch(first.innerHTML,/<img/);
  setPending(first,false);assert.equal(second.disabled,true);
  setPending(second,false);assert.equal(second.innerHTML,'Tải lại');
});
