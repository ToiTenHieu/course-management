import { test } from 'node:test';
import assert from 'node:assert/strict';
import { loginDestination, loginUrl } from '../../src/main/resources/static/js/navigation.js';

test('login preserves protected destinations, filters and the active tab for every role', () => {
  for (const destination of ['/reports.html?page=2','/settings.html','/payments.html?status=PENDING&page=3',
    '/course-detail.html?id=9&lessonId=2#assignment','/learn.html?enrollmentId=8&lessonId=2',
    '/my-payments.html?status=REJECTED','/learning-assessment.html','/profile.html','/recovery-mailbox.html'])
    assert.equal(loginDestination(destination),destination);
  const current = {pathname:'/payments.html',search:'?status=PENDING&page=3',hash:''};
  assert.equal(new URLSearchParams(loginUrl(current).split('?')[1]).get('next'), '/payments.html?status=PENDING&page=3');
});

test('login rejects external, unknown and malformed destinations', () => {
  for (const value of [null,'','https://example.com','//example.com','/unknown.html','/login.html',
    '/reports.html/anything','/\\example.html','/reports.html\n','/%72eports.html','/reports.html#bad/slash','javascript:alert(1)'])
    assert.equal(loginDestination(value),'/dashboard.html');
});
