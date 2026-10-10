const { test, expect } = require('@playwright/test');
const { randomUUID } = require('node:crypto');
const secret = 'Regression123!';

async function write(request, path, data, method = 'post') {
  const token = (await (await request.get('/api/auth/csrf')).json()).data;
  const response = await request[method](path, { data, headers: { [token.headerName]: token.token } });
  expect(response.ok(), `${method} ${path}: ${await response.text()}`).toBeTruthy();
  return (await response.json()).data;
}
async function demoLogin(request, role) {
  const config = (await (await request.get('/api/auth/config')).json()).data;
  const account = config.demoAccounts.find(a => a.role === role);
  expect(account).toBeTruthy();
  await write(request, '/api/auth/login', account);
  return account;
}
async function student(request) {
  const username = 'r_' + randomUUID().slice(0, 8);
  const account = { username, fullName: username, email: username + '@example.invalid', password: secret };
  const saved = await write(request, '/api/auth/register', account);
  return { ...account, userId: saved.userId };
}
async function login(page, account) {
  await page.goto('/login.html');
  await page.getByLabel('Tên đăng nhập').fill(account.username);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(account.password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/dashboard\.html|course-detail\.html/);
}
async function fixture(request) {
  await demoLogin(request, 'ADMIN');
  const teachers = (await (await request.get('/api/users?role=TEACHER')).json()).data;
  const teacher = teachers.find(a => a.username === 'teacher_demo');
  const course = await write(request, '/api/courses', { title: 'Regression ' + randomUUID().slice(0,8), teacherId: teacher.userId, price: 0 });
  const lessons = [];
  for (let i=1;i<=2;i++) {
    const lesson = await write(request, `/api/courses/${course.courseId}/lessons`, { title: `Regression lesson ${i}`, orderIndex: i, textContent: 'Nội dung kiểm thử' });
    await write(request, `/api/lessons/${lesson.lessonId}/publish`, { isPublished: true }, 'put');
    lessons.push(lesson);
  }
  await write(request, `/api/courses/${course.courseId}/status`, { status: 'PUBLISHED' }, 'put');
  return { course, lessons };
}

test.beforeEach(async ({ page, request }) => {
  const ready = await request.get('/api/auth/ready');
  expect(ready.status()).toBe(200);
  expect((await ready.json()).data.ready).toBe(true);
  page.unhandledErrors = [];
  page.on('pageerror', error => page.unhandledErrors.push(error.message));
});
test.afterEach(async ({ page }) => expect(page.unhandledErrors).toEqual([]));

test('Mật khẩu Unicode có lỗi cụ thể và đăng ký được tại giới hạn 72 byte', async ({ page, request }) => {
  const account = { username: 'unicode_' + randomUUID().slice(0,8), fullName: 'Unicode', email: randomUUID() + '@example.invalid', password: 'ắ'.repeat(25) };
  const csrf = (await (await request.get('/api/auth/csrf')).json()).data;
  const rejected = await request.post('/api/auth/register', { data: account, headers: { [csrf.headerName]: csrf.token } });
  expect(rejected.status()).toBe(400);
  expect((await rejected.json()).message).toContain('72 byte');
  await page.goto('/login.html?register=1');
  await page.getByLabel('Họ và tên').fill(account.fullName);
  await page.getByLabel('Tên đăng nhập').fill(account.username);
  await page.getByLabel('Email', { exact: true }).fill(account.email);
  const password = page.getByLabel('Mật khẩu', { exact: true });
  await password.fill(account.password);
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.locator('.field-error:visible')).toContainText('72 byte');
  account.password = 'ắ'.repeat(24);
  await password.fill(account.password);
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Chào mừng trở lại' })).toBeVisible();
  await login(page, account);
  await expect(page).toHaveURL(/dashboard\.html/);
});

test('Thứ tự chương nhất quán, khách thấy chương và giữ khóa quan tâm qua đăng nhập', async ({ page, request }) => {
  const { course, lessons } = await fixture(request);
  const plan = (await (await request.get(`/api/courses/${course.courseId}/curriculum`)).json()).data;
  const saved = await write(request, `/api/courses/${course.courseId}/curriculum`, { expectedRevision: plan.revision, groups: [
    { chapterId: null, title: '', lessonIds: [lessons[0].lessonId] },
    { chapterId: null, title: 'Chương công khai', lessonIds: [lessons[1].lessonId] },
  ] }, 'put');
  const persisted = (await (await request.get(`/api/courses/${course.courseId}/lessons`)).json()).data;
  expect(saved.groups.flatMap(g => g.lessonIds)).toEqual(persisted.map(l => l.lessonId));
  const learner = await student(request);
  await page.goto(`/course-detail.html?id=${course.courseId}`);
  await expect(page.locator('#curriculum .chapter-label')).toHaveText('Chương công khai');
  await page.locator('.wishlist-detail').click();
  await expect(page).toHaveURL(/login\.html/);
  await page.getByLabel('Tên đăng nhập').fill(learner.username);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(learner.password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/course-detail\.html/);
  await expect(page.locator('.wishlist-detail')).toHaveAttribute('aria-pressed', 'true');
  await page.goto('/wishlist.html');
  await expect(page.locator('.card-title')).toHaveText(course.title);
  await page.reload();
  await expect(page.locator('.card-title')).toHaveText(course.title);
});

test('Nộp tệp, chấm bài, mục tiêu tuần và lịch sử quản trị giữ đúng dữ liệu', async ({ page, request, browser, baseURL }) => {
  const { course, lessons } = await fixture(request);
  const learner = await student(request);
  await write(request, `/api/lessons/${lessons[0].lessonId}/assignment`, { expectedRevision: 0, title: 'Bài tập hồi quy', instructions: 'Nộp văn bản và tệp', published: true }, 'put');
  await login(page, learner);
  const enrollment = await write(page.request, '/api/enrollments', { courseId: course.courseId });
  await page.goto(`/learn.html?enrollmentId=${enrollment.enrollmentId}`);
  await expect(page.locator('.lesson-assignment')).toContainText('Bài tập hồi quy');
  await page.getByLabel('Bài làm', { exact: true }).fill('Lời giải đã lưu');
  const bytes = Buffer.from('Tệp hồi quy UTF-8');
  await page.getByLabel('Tệp bài làm (tùy chọn)').setInputFiles({ name: 'regression.txt', mimeType: 'text/plain', buffer: bytes });
  page.once('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Nộp bài tập', exact: true }).click();
  await expect(page.locator('.assignment-submission')).toContainText('Lời giải đã lưu');
  const submitted = (await (await page.request.get(`/api/lessons/${lessons[0].lessonId}/assignment`)).json()).data.submission;
  expect(Math.abs(Date.now() - Date.parse(submitted.submittedAt + '+07:00'))).toBeLessThan(60000);
  const link = page.getByRole('link', { name: /Tải bài nộp: regression.txt/ });
  const downloaded = await page.request.get(await link.getAttribute('href'));
  expect(await downloaded.body()).toEqual(bytes);
  const managerContext = await browser.newContext({ baseURL, timezoneId: 'America/Los_Angeles' });
  const manager = await managerContext.newPage();
  try {
    await demoLogin(managerContext.request, 'TEACHER');
    await manager.goto(`/course-detail.html?id=${course.courseId}#assignment`);
    await manager.locator('[name=score]').fill('85');
    await manager.locator('[name=feedback]').fill('Đã kiểm tra tệp');
    await manager.getByRole('button', { name: 'Lưu điểm', exact: true }).click();
    await expect(manager.locator('.assignment-submission')).toContainText('85/100');
    await page.getByRole('button', { name: 'Tải lại điểm / nhận xét' }).click();
    await expect(page.locator('.assignment-submission')).toContainText('Đã kiểm tra tệp');
    await page.getByRole('button', { name: 'Đánh dấu hoàn thành' }).click();
    await expect(page.locator('.progress-label strong')).toHaveText('50%');
    await page.goto('/dashboard.html');
    await page.locator('.weekly-goal [name=target]').fill('2');
    await page.getByRole('button', { name: 'Lưu mục tiêu', exact: true }).click();
    await expect(page.locator('.goal-count')).toHaveText('1 / 2 bài');
    await page.reload();
    await expect(page.locator('.weekly-goal [name=target]')).toHaveValue('2');
    await write(request, `/api/courses/${course.courseId}/status`, { status: 'ARCHIVED' }, 'put');
    await demoLogin(managerContext.request, 'ADMIN');
    await manager.goto('/audit.html');
    await manager.locator('[name=search]').fill(course.title);
    await manager.getByRole('button', { name: 'Lọc lịch sử', exact: true }).click();
    await expect(manager.locator('.audit-table')).toContainText(course.title);
    await expect(manager.locator('.audit-table')).toContainText('Đã lưu trữ');
  } finally { await managerContext.close(); }
});

test('Lấy lại mật khẩu qua hộp thư demo thu hồi phiên cũ và token dùng một lần', async ({ page, request, browser, baseURL }) => {
  const learner = await student(request);
  const previousContext = await browser.newContext({ baseURL, timezoneId: 'America/Los_Angeles' });
  try {
    const previous = await previousContext.newPage();
    await login(previous, learner);
    await page.goto('/password-recovery.html');
    await page.getByLabel('Email đã đăng ký').fill(learner.email);
    await page.getByRole('button', { name: 'Gửi liên kết đặt lại', exact: true }).click();
    await expect(page.locator('[data-status]')).toContainText('Nếu email thuộc');
    await demoLogin(request, 'ADMIN');
    let url;
    await expect.poll(async () => {
      const messages = (await (await request.get('/api/demo/recovery-mailbox')).json()).data;
      url = messages.find(message => message.recipient === learner.email)?.resetUrl;
      return !!url;
    }).toBe(true);
    await page.goto(new URL(url).pathname + new URL(url).hash);
    await expect(page).not.toHaveURL(/#token=/);
    await page.getByLabel('Mật khẩu mới', { exact: true }).fill('Changed123!');
    await page.getByLabel('Nhập lại mật khẩu mới', { exact: true }).fill('Changed123!');
    await page.getByRole('button', { name: 'Đổi mật khẩu', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Bạn có thể đăng nhập lại' })).toBeVisible();
    expect((await previousContext.request.get('/api/auth/me')).status()).toBe(401);
    await login(page, { ...learner, password: 'Changed123!' });
    await page.goto(new URL(url).pathname + new URL(url).hash);
    await page.getByLabel('Mật khẩu mới', { exact: true }).fill('Again123!');
    await page.getByLabel('Nhập lại mật khẩu mới', { exact: true }).fill('Again123!');
    await page.getByRole('button', { name: 'Đổi mật khẩu', exact: true }).click();
    await expect(page.locator('[data-error]')).toContainText('Liên kết không hợp lệ');
  } finally { await previousContext.close(); }
});
