import test from 'node:test';
import assert from 'node:assert/strict';
import { newPasswordError } from '../../src/main/resources/static/js/password-policy.js';
import { serverDate, dateTime, today } from '../../src/main/resources/static/js/dates.js';

test('new passwords accept the exact bcrypt byte boundary and reject Unicode overflow', () => {
  assert.equal(newPasswordError('ắ'.repeat(24)), '');
  assert.match(newPasswordError('ắ'.repeat(25)), /72 byte/);
  assert.equal(newPasswordError('a'.repeat(64)), '');
  assert.notEqual(newPasswordError('a'.repeat(65)), '');
  assert.notEqual(newPasswordError('short'), '');
  assert.notEqual(newPasswordError('        '), '');
  assert.equal(newPasswordError(' leading and trailing '), '');
});

test('Vietnam timestamps retain their instant and display consistently outside Vietnam', () => {
  assert.equal(serverDate('2026-10-12T00:00:00').toISOString(), '2026-10-11T17:00:00.000Z');
  assert.equal(serverDate('2026-10-11T17:00:00Z').toISOString(), '2026-10-11T17:00:00.000Z');
  assert.equal(serverDate('2026-10-12T00:00:00+07:00').toISOString(), '2026-10-11T17:00:00.000Z');
  assert.equal(dateTime('2026-10-12T00:00:00'), dateTime('2026-10-11T17:00:00Z'));
});

test('report date defaults cross the Vietnam day boundary independently of the host', () => {
  assert.equal(today(0, new Date('2026-10-11T16:59:59Z')), '2026-10-11');
  assert.equal(today(0, new Date('2026-10-11T17:00:00Z')), '2026-10-12');
  assert.equal(today(-29, new Date('2026-10-11T17:00:00Z')), '2026-09-13');
});
