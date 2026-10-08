import { test } from "node:test";
import assert from "node:assert/strict";
globalThis.location = { origin: "http://localhost:8080" };
const { markdown, video } = await import("../../src/main/resources/static/js/lesson-content.js");
test("structured lessons render headings, lists and literal code", () => {
  const html = markdown("# Mục tiêu\n\n- **Học** Java\n- `code`\n\n```java\n<script>alert(1)</script>\n```");
  assert.match(html, /<h3>Mục tiêu<\/h3>/);
  assert.match(html, /<ul><li><strong>Học<\/strong> Java<\/li>/);
  assert.match(html, /<pre><code>&lt;script&gt;/);
  assert.doesNotMatch(html, /<script>/);
});
test("unsafe links, images and raw HTML cannot execute", () => {
  const html = markdown('[x](javascript:alert) ![x](data:text/html,bad) <img src=x onerror=alert(1)> [x](https://a.example/\"onload=bad)');
  assert.doesNotMatch(html, /<img |<script|href="javascript:|src="data:|"onload=/);
  assert.match(html, /%22onload=bad/);
  assert.match(html, /&lt;img/);
});
test("protected image URLs remain usable and require exact API path", () => {
  assert.match(markdown("![Ví dụ](/api/lesson-resources/12/content)"), /alt="Ví dụ"/);
  assert.doesNotMatch(markdown("![x](//evil.example/image.png) ![x](/login.html)"), /<img /);
});
test("video player embeds only known video hosts and direct formats", () => {
  assert.match(video("https://youtu.be/abcdefghijk"), /youtube-nocookie.com\/embed\/abcdefghijk/);
  assert.match(video("https://example.com/lesson.mp4"), /<video /);
  assert.doesNotMatch(video("https://youtube.com.evil.example/watch?v=abcdefghijk"), /iframe/);
  assert.equal(video("javascript:alert(1)"), "");
});

test("YouTube chapters preserve their timestamp in the player and fallback link", () => {
  for (const link of [
    "https://www.youtube.com/watch?v=abcdefghijk&t=1h2m3s",
    "https://youtu.be/abcdefghijk?t=3723",
    "https://www.youtube-nocookie.com/embed/abcdefghijk?start=3723",
    "https://www.youtube.com/shorts/abcdefghijk#t=62m3s"
  ]) {
    const html = video(link);
    assert.match(html, /src="https:\/\/www.youtube-nocookie.com\/embed\/abcdefghijk\?start=3723"/);
    assert.match(html, /href="https:\/\/www.youtube.com\/watch\?v=abcdefghijk&amp;t=3723s"/);
    assert.match(html, /mở trên YouTube/);
    assert.doesNotMatch(html, /autoplay/);
  }
  for (const value of ["-1", "Infinity", "90bad", "999999999999999999", "0"]) {
    assert.doesNotMatch(video(`https://youtu.be/abcdefghijk?t=${value}`), /\?start=/);
  }
  assert.doesNotMatch(video("https://youtube-nocookie.com.evil.example/embed/abcdefghijk"), /iframe/);
});
