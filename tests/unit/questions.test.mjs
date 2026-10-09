import { test } from "node:test";
import assert from "node:assert/strict";
import { replyMarkup } from "../../src/main/resources/static/js/questions.js";

const reply = { replyId: 123, authorName: '<img src=x onerror="alert(1)">',
  authorRole: "STUDENT", body: "<script>alert(1)</script>\nSecond line", hidden: false, createdAt: "2026-10-09T10:00:00" };

test("discussion messages escape member names and body content", () => {
  const html = replyMarkup(reply, false);
  assert.doesNotMatch(html, /<img|<script/);
  assert.match(html, /&lt;script&gt;/);
  assert.match(html, /&lt;img/);
  assert.match(html, /Học viên/);
  assert.doesNotMatch(html, /reply-official|data-hide-reply/);
});

test("official replies have role labels and moderation controls are limited to managers", () => {
  for (const [authorRole, label] of [["TEACHER", "Giảng viên"], ["ADMIN", "Quản trị viên"]]) {
    const studentView = replyMarkup({ ...reply, authorRole }, false);
    assert.match(studentView, /reply-official/); assert.ok(studentView.includes(label));
    assert.doesNotMatch(studentView, /data-hide-reply/);
  }
  assert.match(replyMarkup(reply, true), /Ẩn phản hồi/);
  assert.match(replyMarkup({ ...reply, hidden: true }, true), /Hiện lại phản hồi/);
});
