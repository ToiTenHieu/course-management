const { test, expect } = require("@playwright/test");
const password = "Journey123!";
let sequence = 0;
test.beforeEach(async ({ page }) => {
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.unhandledErrors = errors;
});
test.afterEach(async ({ page }) => {
  expect(
    page.unhandledErrors,
    "Không có lỗi JavaScript chưa được xử lý",
  ).toEqual([]);
});
function account() {
  const username = `e2e_${Date.now()}_${sequence++}`;
  return {
    username,
    email: `${username}@example.invalid`,
    fullName: username,
    password,
  };
}
async function seed(request) {
  const user = account();
  const token = (await (await request.get("/api/auth/csrf")).json()).data;
  const response = await request.post("/api/auth/register", {
    data: user,
    headers: { [token.headerName]: token.token },
  });
  expect(response.ok()).toBeTruthy();
  return user;
}
async function login(page, username, secret = password) {
  await page.goto("/login.html");
  await page.getByLabel("Tên đăng nhập").fill(username);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(secret);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(/dashboard\.html/);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    username === "admin_demo"
      ? "Trung tâm vận hành"
      : username === "teacher_demo"
        ? "Không gian giảng dạy"
        : "Không gian học tập",
  );
}
async function course(page, title) {
  await page.goto("/courses.html");
  await page.getByLabel("Tìm khóa học", { exact: true }).fill(title);
  await page.locator(".card-title").filter({ hasText: title }).click();
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(title);
}

test("Đăng nhập sai báo lỗi, sau đó vẫn đăng nhập thành công", async ({
  page,
  request,
}) => {
  const user = await seed(request);
  await page.goto("/login.html");
  await page.getByLabel("Tên đăng nhập").fill(user.username);
  await page.getByLabel("Mật khẩu", { exact: true }).fill("Wrong123!");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.locator("#authError")).not.toBeEmpty();
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(/dashboard\.html/);
});

test("Đăng ký, học khóa miễn phí đến 100% và giữ tiến độ sau tải lại", async ({
  page,
}) => {
  const user = account();
  await page.goto("/login.html?register=1");
  await page.getByLabel("Họ và tên").fill(user.fullName);
  await page.getByLabel("Tên đăng nhập").fill(user.username);
  await page.getByLabel("Email", { exact: true }).fill(user.email);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await page
    .getByRole("button", { name: "Tạo tài khoản", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Chào mừng trở lại" }),
  ).toBeVisible();
  await login(page, user.username);
  await course(page, "Java từ nền tảng đến ứng dụng");
  await page.getByRole("button", { name: "Đăng ký miễn phí" }).click();
  await page.getByRole("link", { name: /Vào phòng học/ }).click();
  const titles = [
    "Bắt đầu và chuẩn bị môi trường",
    "Các khái niệm cốt lõi",
    "Thực hành qua ví dụ",
    "Tổng kết và dự án nhỏ",
  ];
  for (let i = 0; i < titles.length; i++) {
    await page
      .locator("#lessonNav")
      .getByRole("button", { name: new RegExp(titles[i]) })
      .click();
    await expect(page.locator("#lessonContent h2")).toHaveText(titles[i]);
    await page.getByRole("button", { name: "Đánh dấu hoàn thành" }).click();
    await expect(page.locator(".progress-label strong")).toHaveText(
      `${(i + 1) * 25}%`,
    );
  }
  await page.reload();
  await expect(page.locator(".progress-label strong")).toHaveText("100%");
  await page.goto("/my-courses.html");
  await expect(page.locator(".learning-progress")).toContainText("Hoàn thành");
  await expect(page.locator(".learning-progress")).toContainText("100%");
});

test("Khóa trả phí chỉ mở sau khi admin duyệt thanh toán", async ({
  page,
  request,
  browser,
}) => {
  const user = await seed(request);
  await login(page, user.username);
  await course(page, "Thiết kế giao diện với tư duy sản phẩm");
  await page.getByRole("button", { name: "Đăng ký & thanh toán" }).click();
  await expect(page.getByRole("dialog")).toContainText("Ngân hàng demo");
  await page.getByRole("button", { name: "Đã hiểu" }).click();
  await expect(page.getByRole("link", { name: /Vào phòng học/ })).toHaveCount(
    0,
  );
  const context = await browser.newContext({
    baseURL:
      test.info().project.use.baseURL ||
      process.env.E2E_BASE_URL ||
      "http://127.0.0.1:8080",
  });
  try {
    const admin = await context.newPage();
    await login(admin, "admin_demo", "Demo123!");
    await admin.goto("/payments.html");
    await admin.getByLabel("Tìm thanh toán").fill(user.fullName);
    const row = admin.getByRole("row").filter({ hasText: user.fullName });
    await row.getByRole("button", { name: "Xác nhận", exact: true }).click();
    await admin
      .getByRole("dialog")
      .getByRole("button", { name: "Xác nhận", exact: true })
      .click();
    await expect(row).toContainText("Đã xác nhận");
    await page.reload();
    await page.getByRole("link", { name: /Vào phòng học/ }).click();
    await expect(page.locator(".lesson-text")).toContainText(
      "Đây là nội dung mẫu",
    );
  } finally {
    await context.close();
  }
});

test("Tên chứa HTML được giữ như văn bản khi lưu và tải lại hồ sơ", async ({
  page,
  request,
}) => {
  const user = await seed(request);
  await login(page, user.username);
  await page.goto("/profile.html");
  const payload = '<img src=x onerror="document.body.remove()">';
  await page.getByLabel("Họ và tên").fill(payload);
  await page.getByRole("button", { name: "Lưu thông tin" }).click();
  await expect(page.locator(".profile-summary h2")).toHaveText(payload);
  await page.reload();
  await expect(page.getByLabel("Họ và tên")).toHaveValue(payload);
  await expect(page.locator('img[src="x"]')).toHaveCount(0);
  await expect(page.locator("[onerror]")).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Lưu thông tin" }),
  ).toBeVisible();
});

test("Điện thoại không tràn ngang và mở được menu điều hướng", async ({
  page,
  request,
}) => {
  const user = await seed(request);
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, user.username);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await page.getByRole("button", { name: "Mở điều hướng" }).click();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Khám phá khóa học", exact: true })
    .click();
  await expect(page).toHaveURL(/courses\.html/);
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
});

test("Giảng viên xem khóa nháp và soạn, xuất bản bài học", async ({
  page,
  request,
}) => {
  async function write(path, data) {
    const csrf = (await (await request.get("/api/auth/csrf")).json()).data;
    const response = await request.post(path, {
      data,
      headers: { [csrf.headerName]: csrf.token },
    });
    expect(response.ok()).toBeTruthy();
    return (await response.json()).data;
  }
  await write("/api/auth/login", {
    username: "admin_demo",
    password: "Demo123!",
  });
  const users = (await (await request.get("/api/users")).json()).data;
  const teacher = users.find((u) => u.username === "teacher_demo");
  const draft = await write("/api/courses", {
    title: `Khóa nháp E2E ${Date.now()}`,
    teacherId: teacher.userId,
    price: 0,
  });
  expect(draft.status).toBe("DRAFT");
  await login(page, "teacher_demo", "Demo123!");
  await page.goto(`/course-detail.html?id=${draft.courseId}`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(draft.title);
  await page.getByRole("button", { name: "Thêm bài", exact: true }).click();
  await page
    .getByLabel("Tiêu đề bài học")
    .fill("Bài thử nghiệm của giảng viên");
  await page
    .getByLabel("Nội dung bài học", { exact: true })
    .fill("Nội dung thử nghiệm: lưu và xuất bản bài học.");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Lưu thay đổi" })
    .click();
  const row = page
    .locator(".syllabus-row")
    .filter({ hasText: "Bài thử nghiệm của giảng viên" });
  await expect(row).toContainText("Bản nháp");
  await row.getByRole("button", { name: "Xuất bản", exact: true }).click();
  await expect(row).toContainText("Đã xuất bản");
  await page.reload();
  await expect(row).toContainText("Đã xuất bản");
});
