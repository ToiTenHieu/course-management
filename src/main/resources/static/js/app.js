import { api, ApiError } from "./api.js";
const $ = (s) => document.querySelector(s);
const app = $("#app"),
  modal = $("#modal");
const page = document.body.dataset.page,
  params = new URLSearchParams(location.search);
let user = null;
const esc = (v) =>
  String(v ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const money = (v) =>
  Number(v || 0) === 0 ? "Miễn phí" : Number(v).toLocaleString("vi-VN") + " ₫";
const date = (v) => (v ? new Date(v).toLocaleDateString("vi-VN") : "—");
const role = (v) =>
  ({ ADMIN: "Quản trị viên", TEACHER: "Giảng viên", STUDENT: "Học viên" })[v] ||
  v;
const labels = {
  DRAFT: "Bản nháp",
  PUBLISHED: "Đã xuất bản",
  ARCHIVED: "Đã lưu trữ",
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  REJECTED: "Đã từ chối",
  ENROLLED: "Đang học",
  COMPLETED: "Hoàn thành",
  DROPPED: "Đã ngừng học",
};
const badge = (v) =>
  `<span class="badge ${["PUBLISHED", "CONFIRMED", "COMPLETED"].includes(v) ? "good" : v === "PENDING" || v === "DRAFT" ? "warm" : "muted"}">${esc(labels[v] || v)}</span>`;
const initials = (v) =>
  String(v || "HV")
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((s) => s[0])
    .join("")
    .toUpperCase();
const icons = {
  home: "M3 10 12 3l9 7v10H3z M9 20v-7h6v7",
  book: "M3 4h7l2 2 2-2h7v16h-7l-2 2-2-2H3z M12 6v16",
  bell: "M5 17h14l-2-4V9a5 5 0 0 0-10 0v4z M10 21h4",
  users:
    "M8 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M1 21v-3a7 7 0 0 1 14 0v3 M17 4a4 4 0 0 1 0 8 M18 15a5 5 0 0 1 5 5",
  chart: "M4 20V10 M12 20V4 M20 20v-7",
  card: "M2 5h20v14H2z M2 9h20 M5 15h5",
  arrow: "M5 12h14 M13 6l6 6-6 6",
  check: "m5 12 4 4L19 6",
  play: "m8 4 13 8-13 8z",
  search: "M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16 M16 16l6 6",
  logout: "M10 4H3v16h7 M9 12h12 M16 7l5 5-5 5",
  clock: "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20 M12 6v6l4 2",
  plus: "M12 4v16 M4 12h16",
  close: "m5 5 14 14 M19 5 5 19",
  lock: "M5 10h14v11H5z M8 10V6a4 4 0 0 1 8 0v4",
};
const icon = (n) =>
  `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${icons[n] || icons.book}"/></svg>`;
function toast(message, bad = false) {
  const node = document.createElement("div");
  node.className = "toast" + (bad ? " error" : "");
  node.textContent = message;
  $("#toasts").append(node);
  setTimeout(() => node.remove(), 5000);
}
function errorBox(error) {
  return `<div class="notice error" role="alert">${esc(error.message)}</div>`;
}
function empty(title, description, link = "") {
  return `<div class="empty">${icon("book")}<h3>${esc(title)}</h3><p>${esc(description)}</p>${link}</div>`;
}
function safeUrl(value, internal = false) {
  try {
    const u = new URL(value, location.origin);
    if (!["http:", "https:"].includes(u.protocol)) return null;
    if (
      internal &&
      (u.origin !== location.origin ||
        !/^\/[a-zA-Z0-9_.-]+\.html$/.test(u.pathname))
    )
      return null;
    return u.href;
  } catch {
    return null;
  }
}
function link(text, url, style = "btn") {
  return `<a class="${style}" href="${esc(url)}">${text}</a>`;
}
function setTitle(title) {
  document.title = title + " · Học viện";
}
function shell(title, subtitle, content, actions = "") {
  setTitle(title);
  const admin = user.role === "ADMIN",
    teacher = user.role === "TEACHER";
  const items = [
    ["dashboard", "home", "Tổng quan"],
    ["courses", "book", teacher ? "Khóa phụ trách" : "Khám phá khóa học"],
    ...(user.role === "STUDENT"
      ? [
          ["my-courses", "play", "Việc học của tôi"],
          ["my-payments", "card", "Thanh toán"],
        ]
      : []),
    ...(admin
      ? [
          ["users", "users", "Người dùng"],
          ["payments", "card", "Duyệt thanh toán"],
          ["reports", "chart", "Báo cáo"],
        ]
      : []),
    ["notifications", "bell", "Thông báo"],
  ];
  app.innerHTML = `<div class="workspace ${admin || teacher ? "management" : "learner"}"><aside class="sidebar"><a class="brand" href="/dashboard.html"><img src="/assets/mark.svg" alt=""><span>học viện<span class="brand-sub">HỌC ĐỂ TIẾN XA</span></span></a><p class="nav-label">${admin ? "QUẢN TRỊ" : teacher ? "GIẢNG DẠY" : "KHÔNG GIAN CỦA BẠN"}</p><nav>${items.map(([p, i, t]) => `<a class="nav-item ${page === p ? "active" : ""}" href="/${p}.html">${icon(i)}${t}</a>`).join("")}</nav><div class="sidebar-bottom"><div class="sidebar-note">Mỗi ngày một chút.<br><strong>Mỗi bước đều tiến xa.</strong></div><a class="account" href="/profile.html"><span class="avatar">${esc(initials(user.fullName))}</span><span><strong>${esc(user.fullName)}</strong><small>${role(user.role)}</small></span></a><button class="logout" id="logout">${icon("logout")} Đăng xuất</button></div></aside><div class="work-main"><header class="topbar"><button class="icon-button mobile-menu" aria-label="Mở điều hướng" id="menu">☰</button><span class="breadcrumb">Học viện <span>/</span> ${esc(title)}</span><div class="top-actions"><span class="role-chip">${role(user.role)}</span><a class="icon-button" aria-label="Thông báo" href="/notifications.html">${icon("bell")}</a><a class="avatar small" href="/profile.html" aria-label="Hồ sơ của tôi">${esc(initials(user.fullName))}</a></div></header><main id="main" class="main"><div class="page-heading"><div><span class="eyebrow">${admin ? "VẬN HÀNH HỌC VIỆN" : teacher ? "KHÔNG GIAN GIẢNG DẠY" : "HÀNH TRÌNH HỌC TẬP"}</span><h1>${esc(title)}</h1><p>${esc(subtitle)}</p></div><div class="heading-actions">${actions}</div></div>${content}</main><footer class="footer">Học viện · Học để tiến xa <span>Kiến thức hôm nay. Cơ hội ngày mai.</span></footer></div></div>`;
  $("#logout").onclick = () =>
    action(async () => {
      await api("/auth/logout", "POST");
      location.href = "/login.html";
    });
  $("#menu").onclick = () => $(".sidebar").classList.toggle("open");
}
async function action(fn, button) {
  if (button) button.disabled = true;
  try {
    await fn();
  } catch (e) {
    toast(e.message, true);
    if (e.status === 401)
      setTimeout(() => (location.href = "/login.html"), 1200);
  } finally {
    if (button) button.disabled = false;
  }
}
function showDialog(title, body, onSubmit, submit = "Lưu thay đổi") {
  modal.innerHTML = `<form id="dialogForm"><div class="dialog-head"><h2>${esc(title)}</h2><button type="button" class="icon-button" id="closeModal" aria-label="Đóng">${icon("close")}</button></div><div class="dialog-body">${body}<div id="dialogError" role="alert"></div></div><div class="dialog-footer"><button type="button" class="btn secondary" id="cancelModal">Hủy</button><button class="btn" id="saveModal">${esc(submit)}</button></div></form>`;
  $("#closeModal").onclick = $("#cancelModal").onclick = () => modal.close();
  $("#dialogForm").onsubmit = async (e) => {
    e.preventDefault();
    const button = $("#saveModal");
    button.disabled = true;
    $("#dialogError").innerHTML = "";
    try {
      await onSubmit(Object.fromEntries(new FormData(e.currentTarget)));
      modal.close();
    } catch (error) {
      $("#dialogError").innerHTML = errorBox(error);
    } finally {
      button.disabled = false;
    }
  };
  modal.showModal();
}
function confirmAction(title, text, fn) {
  showDialog(title, `<p>${esc(text)}</p>`, fn, "Xác nhận");
}
function field(
  label,
  name,
  value = "",
  type = "text",
  required = true,
  extra = "",
) {
  return `<label>${esc(label)}<input name="${name}" type="${type}" value="${esc(value)}" ${required ? "required" : ""} ${extra}></label>`;
}
function textarea(label, name, value = "", extra = "") {
  return `<label>${esc(label)}<textarea name="${name}" rows="4" ${extra}>${esc(value)}</textarea></label>`;
}
function select(label, name, options, value) {
  return `<label>${esc(label)}<select name="${name}">${options.map(([v, t]) => `<option value="${esc(v)}" ${String(v) === String(value) ? "selected" : ""}>${esc(t)}</option>`).join("")}</select></label>`;
}
function art(c, large = false) {
  const styles = {
    "Thiết kế": ["design", "✳", "DESIGN"],
    "Dữ liệu": ["data", "{ }", "DATA"],
    "Công cụ": ["tools", "⌘", "WORKFLOW"],
    "Lập trình": ["code", "&lt;/&gt;", "DEVELOPMENT"],
  };
  const [style, symbol, label] = styles[c.category] || styles["Lập trình"];
  return `<div class="course-art ${style} ${large ? "large" : ""}"><span class="art-label">${label}</span><div class="art-orbit"></div><div class="art-symbol">${symbol}</div><span class="art-bottom">HỌC VIỆN <span>↗</span></span></div>`;
}
function card(c, extra = "") {
  return `<article class="course-card"><a class="art-link" href="/course-detail.html?id=${c.courseId}" aria-label="${esc(c.title)}">${art(c)}</a><div class="card-body"><div class="card-tags"><span>${esc(c.category)}</span><span>${esc(c.level)}</span></div><a class="card-title" href="/course-detail.html?id=${c.courseId}">${esc(c.title)}</a><p class="teacher">${esc(c.teacherName)}</p><div class="course-meta"><span>${icon("clock")}${c.durationHours || "—"} giờ</span><span>${icon("book")}${c.lessonCount || 0} bài học</span>${c.averageRating ? `<span>★ ${Number(c.averageRating).toFixed(1)}</span>` : ""}</div>${extra || `<div class="card-foot"><strong>${money(c.price)}</strong>${link(icon("arrow"), "/course-detail.html?id=" + c.courseId, "round-link")}</div>`}</div></article>`;
}
const progress = (v) =>
  `<div class="progress" role="progressbar" aria-valuenow="${Number(v || 0)}" aria-valuemin="0" aria-valuemax="100"><span style="width:${Math.min(100, Math.max(0, Number(v || 0)))}%"></span></div>`;
const stat = (label, value, detail, i = "chart") =>
  `<div class="stat"><span class="stat-icon">${icon(i)}</span><span>${esc(label)}</span><strong>${esc(value)}</strong><small>${esc(detail)}</small></div>`;

async function landing() {
  setTitle("Học để tiến xa");
  app.innerHTML = `<div class="public"><header class="public-header"><a class="brand" href="/"><img src="/assets/mark.svg" alt=""><span>học viện</span></a><nav><a href="#approach">Cách học</a>${link("Đăng nhập", "/login.html", "btn secondary")}${link("Bắt đầu học", "/login.html?register=1")}</nav></header><main><section class="landing-hero"><div><span class="eyebrow">HỌC HÔM NAY. TIẾN XA NGÀY MAI.</span><h1>Kiến thức mới.<br><span>Khả năng mới.</span><br>Phiên bản mới của bạn.</h1><p>Khám phá kỹ năng số, học theo từng bước và biến kiến thức thành điều bạn thực sự làm được.</p><div class="hero-actions">${link("Khám phá khóa học " + icon("arrow"), "/courses.html")}${link("Tạo tài khoản miễn phí", "/login.html?register=1", "text-link")}</div><div class="hero-proof"><span>${icon("check")} Lộ trình rõ ràng</span><span>${icon("check")} Theo dõi tiến độ</span></div></div><div class="landing-art"><div class="floating-card"><span class="mini-label">BƯỚC TIẾP THEO CỦA BẠN</span><h3>Học một điều mới.<br>Làm một điều hay.</h3><div class="art-symbol">&lt;/&gt;</div><div class="art-subcard">${icon("book")} Lập trình · Thiết kế · Dữ liệu</div></div><span class="orbit-dot"></span></div></section><section id="approach" class="approach"><span class="eyebrow">HỌC THEO CÁCH CỦA BẠN</span><h2>Một hành trình. Từng bước rõ ràng.</h2><div class="three-grid">${[
    [
      "01",
      "Tìm điều bạn muốn học",
      "Khám phá các khóa học theo kỹ năng, trình độ và mục tiêu.",
    ],
    [
      "02",
      "Học và thực hành",
      "Theo dõi bài học, đọc nội dung và áp dụng qua các ví dụ.",
    ],
    [
      "03",
      "Nhìn thấy sự tiến bộ",
      "Đánh dấu hoàn thành và theo dõi hành trình của chính bạn.",
    ],
  ]
    .map(
      ([n, t, d]) =>
        `<article><span>${n}</span><h3>${t}</h3><p>${d}</p></article>`,
    )
    .join(
      "",
    )}</div></section></main><footer class="public-footer">Học viện · Học để tiến xa <span>Nền tảng học trực tuyến</span></footer></div>`;
}
async function login() {
  const config = await api("/auth/config");
  let register = params.has("register");
  function draw() {
    setTitle(register ? "Tạo tài khoản" : "Đăng nhập");
    app.innerHTML = `<div class="auth-page"><section class="auth-story"><a class="brand light" href="/"><img src="/assets/mark.svg" alt=""><span>học viện</span></a><div><span class="eyebrow">KHÔNG GIAN CHO SỰ TIẾN BỘ</span><h1>Đi xa hơn,<br>bắt đầu từ<br><em>một bài học.</em></h1><p>Một nơi để khám phá kỹ năng mới, tiếp tục điều đang học và nhìn thấy mình tiến bộ mỗi ngày.</p><div class="story-art"><span>&lt;/&gt;</span><span>✳</span><span>{ }</span></div></div><small>Học để tiến xa.</small></section><section class="auth-form"><div class="auth-inner"><a class="back-link" href="/">← Trang chủ</a><h2>${register ? "Bắt đầu hành trình" : "Chào mừng trở lại"}</h2><p class="muted-text">${register ? "Tạo tài khoản học viên để khám phá các khóa học." : "Đăng nhập để tiếp tục hành trình học tập của bạn."}</p><form id="authForm">${register ? field("Họ và tên", "fullName", "", "text", true, 'maxlength="100"') : ""}${field("Tên đăng nhập", "username", "", "text", true, 'autocomplete="username" minlength="3" maxlength="40"')}${register ? field("Email", "email", "", "email", true, 'autocomplete="email" maxlength="100"') : ""}${field("Mật khẩu", "password", "", "password", true, `autocomplete="${register ? "new" : "current"}-password" ${register ? 'minlength="8" maxlength="64"' : ""}`)}${register ? '<small class="hint">Mật khẩu từ 8 đến 64 ký tự.</small>' : ""}<div id="authError" role="alert"></div><button class="btn full" id="authSubmit">${register ? "Tạo tài khoản" : "Đăng nhập"} ${icon("arrow")}</button></form><p class="auth-switch">${register ? "Đã có tài khoản?" : "Chưa có tài khoản?"} <button class="text-link" id="toggleAuth">${register ? "Đăng nhập" : "Đăng ký miễn phí"}</button></p>${
      config.demo && !register
        ? `<div class="demo-box"><span class="mini-label">KHÁM PHÁ BẢN DEMO</span><div>${[
            ["student_demo", "Học viên"],
            ["teacher_demo", "Giảng viên"],
            ["admin_demo", "Admin"],
          ]
            .map(
              ([u, t]) =>
                `<button class="btn secondary compact" data-demo="${u}">${t}</button>`,
            )
            .join(
              "",
            )}</div><small>Chọn vai trò để điền tài khoản mẫu. Mật khẩu: Demo123!</small></div>`
        : ""
    }</div></section></div>`;
    $("#toggleAuth").onclick = () => {
      register = !register;
      draw();
    };
    document.querySelectorAll("[data-demo]").forEach(
      (b) =>
        (b.onclick = () => {
          const f = $("#authForm");
          f.elements.username.value = b.dataset.demo;
          f.elements.password.value = "Demo123!";
        }),
    );
    $("#authForm").onsubmit = async (e) => {
      e.preventDefault();
      const data = Object.fromEntries(new FormData(e.currentTarget)),
        b = $("#authSubmit");
      b.disabled = true;
      $("#authError").innerHTML = "";
      try {
        if (register) {
          await api("/auth/register", "POST", data);
          register = false;
          draw();
          toast("Tài khoản đã sẵn sàng. Hãy đăng nhập.");
        } else {
          await api("/auth/login", "POST", data);
          location.href = "/dashboard.html";
        }
      } catch (error) {
        $("#authError").innerHTML = errorBox(error);
      } finally {
        b.disabled = false;
      }
    };
  }
  draw();
}
async function dashboard() {
  const [courses, notifications] = await Promise.all([
    api("/courses"),
    api("/notifications"),
  ]);
  const student = user.role === "STUDENT",
    admin = user.role === "ADMIN";
  let enrollments = [],
    payments = [],
    users = [];
  if (student)
    [enrollments, payments] = await Promise.all([
      api("/enrollments"),
      api("/payments/my"),
    ]);
  if (admin)
    [users, payments] = await Promise.all([api("/users"), api("/payments")]);
  const own = courses.filter((c) => c.teacherId === user.userId),
    first = user.fullName.trim().split(/\s+/).slice(-1)[0];
  const stats = student
    ? [
        stat(
          "Khóa đang học",
          enrollments.filter((e) => e.status === "ENROLLED").length,
          "Tiếp tục hành trình",
          "book",
        ),
        stat(
          "Đã hoàn thành",
          enrollments.filter((e) => e.status === "COMPLETED").length,
          "Từng bước tiến bộ",
          "check",
        ),
        stat(
          "Thanh toán chờ duyệt",
          payments.filter((p) => p.status === "PENDING").length,
          "Xem trạng thái chuyển khoản",
          "card",
        ),
      ]
    : admin
      ? [
          stat("Người dùng", users.length, "Tài khoản trong học viện", "users"),
          stat(
            "Khóa học",
            courses.length,
            courses.filter((c) => c.status === "PUBLISHED").length +
              " khóa đã xuất bản",
            "book",
          ),
          stat(
            "Thanh toán chờ duyệt",
            payments.filter((p) => p.status === "PENDING").length,
            "Yêu cầu cần xử lý",
            "card",
          ),
        ]
      : [
          stat(
            "Khóa phụ trách",
            own.length,
            "Nội dung bạn đang giảng dạy",
            "book",
          ),
          stat(
            "Khóa đã xuất bản",
            own.filter((c) => c.status === "PUBLISHED").length,
            "Sẵn sàng cho học viên",
            "check",
          ),
          stat(
            "Lượt đăng ký",
            own.reduce((s, c) => s + Number(c.enrollmentCount || 0), 0),
            "Trên các khóa phụ trách",
            "users",
          ),
        ];
  const ongoing = enrollments.find((e) => e.status === "ENROLLED"),
    featured = (user.role === "TEACHER" ? own : courses).slice(0, 3);
  shell(
    "Tổng quan",
    "Chào " + first + ", hôm nay bạn muốn tiến thêm một bước ở đâu?",
    `<section class="welcome"><div><span class="eyebrow">${student ? "MỘT CHÚT MỖI NGÀY" : admin ? "HỌC VIỆN TRONG TẦM TAY" : "TRUYỀN ĐẠT KIẾN THỨC"}</span><h2>${student ? "Hôm nay là một ngày đẹp<br>để học điều mới." : admin ? "Cùng xây dựng một<br>không gian học tốt hơn." : "Một bài học hay.<br>Nhiều cơ hội mới."}</h2><p>${student ? "Tiếp tục bài học còn dang dở hoặc khám phá kỹ năng tiếp theo." : "Theo dõi hoạt động và chăm chút trải nghiệm của học viên."}</p>${link(ongoing ? "Tiếp tục học " + icon("arrow") : admin ? "Duyệt thanh toán " + icon("arrow") : "Khám phá khóa học " + icon("arrow"), ongoing ? "/learn.html?enrollmentId=" + ongoing.enrollmentId : admin ? "/payments.html" : "/courses.html", "btn lime")}</div><div class="welcome-art" aria-hidden="true"><div class="circle one"></div><div class="circle two"></div><span>↗</span></div></section><div class="stats">${stats.join("")}</div><div class="section-heading"><div><h2>${user.role === "TEACHER" ? "Khóa học của bạn" : "Khám phá tiếp theo"}</h2><p>${user.role === "TEACHER" ? "Mở khóa học để soạn bài và quản lý nội dung." : "Kiến thức mới cho mục tiêu tiếp theo của bạn."}</p></div>${link("Xem tất cả →", "/courses.html", "text-link")}</div><div class="course-grid">${featured.map((c) => card(c)).join("") || empty("Chưa có khóa học", "Các khóa học sẽ xuất hiện tại đây.")}</div><section class="panel activity"><div class="section-heading"><h2>Thông báo gần đây</h2>${link("Xem tất cả →", "/notifications.html", "text-link")}</div>${
      notifications
        .slice(0, 3)
        .map(
          (n) =>
            `<div class="activity-row"><span class="activity-dot"></span><p>${esc(n.message)}</p><small>${date(n.createdAt)}</small></div>`,
        )
        .join("") || '<p class="muted-text">Bạn đã cập nhật hết thông báo.</p>'
    }</section>`,
  );
}
async function courseEditor(course, done) {
  const teachers = await api("/users?role=TEACHER&status=active");
  if (!teachers.length) {
    toast("Hãy tạo một tài khoản giảng viên đang hoạt động trước.", true);
    return;
  }
  showDialog(
    course ? "Chỉnh sửa khóa học" : "Tạo khóa học mới",
    `${field("Tên khóa học", "title", course?.title || "", "text", true, 'maxlength="255"')}${textarea("Giới thiệu", "description", course?.description || "", 'maxlength="10000"')}${select(
      "Giảng viên",
      "teacherId",
      teachers.map((t) => [t.userId, t.fullName]),
      course?.teacherId,
    )}<div class="form-grid">${field("Chủ đề", "category", course?.category || "Lập trình", "text", true, 'maxlength="100"')}${field("Trình độ", "level", course?.level || "Cơ bản", "text", true, 'maxlength="100"')}${field("Học phí (₫)", "price", course?.price || 0, "number", true, 'min="0" max="99999999" step="0.01"')}${field("Thời lượng (giờ)", "durationHours", course?.durationHours || 1, "number", true, 'min="1" max="10000"')}</div>${textarea("Kết quả học tập (mỗi dòng một mục)", "learningOutcomes", course?.learningOutcomes || "", 'maxlength="10000"')}`,
    async (d) => {
      d.teacherId = Number(d.teacherId);
      d.price = Number(d.price);
      d.durationHours = Number(d.durationHours);
      await api(
        "/courses" + (course ? "/" + course.courseId : ""),
        course ? "PUT" : "POST",
        d,
      );
      toast("Đã lưu khóa học");
      await done();
    },
  );
}
async function courses() {
  const data = await api("/courses"),
    admin = user.role === "ADMIN",
    teacher = user.role === "TEACHER";
  const all = teacher ? data.filter((c) => c.teacherId === user.userId) : data;
  shell(
    teacher ? "Khóa học phụ trách" : "Khám phá khóa học",
    teacher
      ? "Chăm chút từng bài học, đồng hành cùng học viên."
      : "Chọn một kỹ năng mới. Bắt đầu một hành trình mới.",
    `<div class="catalog-intro"><div><h2>Đầu tư vào điều<br>bạn <em>có thể trở thành.</em></h2><p>Học theo từng bước, theo tốc độ của riêng bạn.</p></div><span class="catalog-symbol">✳</span></div><div class="catalog-toolbar"><label class="search-box">${icon("search")}<input id="search" type="search" placeholder="Tìm khóa học hoặc giảng viên…" aria-label="Tìm khóa học"></label><select id="sort" aria-label="Sắp xếp"><option value="new">Mới nhất</option><option value="price">Học phí tăng dần</option><option value="title">Tên A–Z</option></select>${admin || teacher ? '<select id="status" aria-label="Trạng thái"><option value="">Mọi trạng thái</option><option value="DRAFT">Bản nháp</option><option value="PUBLISHED">Đã xuất bản</option><option value="ARCHIVED">Đã lưu trữ</option></select>' : ""}</div><div class="chips" id="categories"><button class="chip active" data-category="">Tất cả</button>${[...new Set(all.map((c) => c.category))].map((c) => `<button class="chip" data-category="${esc(c)}">${esc(c)}</button>`).join("")}<button class="chip" id="freeFilter">Miễn phí</button></div><div class="result-count" id="count"></div><div class="course-grid" id="catalog"></div><div class="pagination" id="pagination"></div>`,
    admin
      ? '<button class="btn" id="createCourse">' +
          icon("plus") +
          " Tạo khóa học</button>"
      : "",
  );
  let category = "",
    free = false,
    current = 1;
  function draw() {
    let found = all.filter(
      (c) =>
        (!category || c.category === category) &&
        (!free || Number(c.price) === 0) &&
        (!$("#status")?.value || c.status === $("#status").value) &&
        [c.title, c.teacherName]
          .join(" ")
          .toLocaleLowerCase("vi")
          .includes($("#search").value.trim().toLocaleLowerCase("vi")),
    );
    if ($("#sort").value === "price") found.sort((a, b) => a.price - b.price);
    if ($("#sort").value === "title")
      found.sort((a, b) => a.title.localeCompare(b.title, "vi"));
    $("#count").textContent = found.length + " khóa học phù hợp";
    $("#catalog").innerHTML =
      found
        .slice((current - 1) * 9, current * 9)
        .map((c) =>
          card(
            c,
            admin || teacher
              ? `<div class="card-foot">${badge(c.status)}<strong>${money(c.price)}</strong></div>`
              : "",
          ),
        )
        .join("") ||
      empty("Chưa tìm thấy khóa học", "Thử từ khóa hoặc bộ lọc khác.");
    $("#pagination").innerHTML = Array.from(
      { length: Math.ceil(found.length / 9) },
      (_, i) =>
        `<button class="chip ${current === i + 1 ? "active" : ""}" data-pagenum="${i + 1}">${i + 1}</button>`,
    ).join("");
    document.querySelectorAll("[data-pagenum]").forEach(
      (b) =>
        (b.onclick = () => {
          current = Number(b.dataset.pagenum);
          draw();
        }),
    );
  }
  $("#search").oninput = $("#sort").onchange = () => {
    current = 1;
    draw();
  };
  if ($("#status"))
    $("#status").onchange = () => {
      current = 1;
      draw();
    };
  document.querySelectorAll("[data-category]").forEach(
    (b) =>
      (b.onclick = () => {
        category = b.dataset.category;
        document
          .querySelectorAll("[data-category]")
          .forEach((x) => x.classList.toggle("active", x === b));
        current = 1;
        draw();
      }),
  );
  $("#freeFilter").onclick = (e) => {
    free = !free;
    e.currentTarget.classList.toggle("active", free);
    current = 1;
    draw();
  };
  if (admin)
    $("#createCourse").onclick = () =>
      action(() => courseEditor(null, courses));
  draw();
}
async function lessonEditor(courseId, lesson, done) {
  showDialog(
    lesson ? "Chỉnh sửa bài học" : "Thêm bài học",
    `${field("Tiêu đề bài học", "title", lesson?.title || "", "text", true, 'maxlength="255"')}${field("Thứ tự", "orderIndex", lesson?.orderIndex || 1, "number", true, 'min="1"')}${field("Liên kết tài liệu hoặc video", "contentUrl", lesson?.contentUrl || "", "url", false, 'maxlength="500" placeholder="https://…"')}${textarea("Nội dung bài học", "textContent", lesson?.textContent || "", 'maxlength="100000"')}`,
    async (d) => {
      d.orderIndex = Number(d.orderIndex);
      d.contentUrl = d.contentUrl || null;
      await api(
        lesson
          ? "/lessons/" + lesson.lessonId
          : "/courses/" + courseId + "/lessons",
        lesson ? "PUT" : "POST",
        d,
      );
      toast("Đã lưu bài học");
      await done();
    },
  );
}
async function paymentInfo(payment) {
  const bank = await api("/payments/bank-info");
  showDialog(
    "Hướng dẫn chuyển khoản",
    `<p>Yêu cầu của bạn đang chờ xác nhận. Quyền học sẽ được cấp sau khi quản trị viên đối chiếu chuyển khoản.</p><div class="bank-info">${[
      ["Ngân hàng", bank.bankName],
      ["Số tài khoản", bank.accountNumber],
      ["Chủ tài khoản", bank.accountHolder],
      ["Số tiền", money(payment.amount)],
      ["Nội dung chuyển khoản", payment.transferNote],
    ]
      .map(
        ([t, v]) =>
          `<div><span>${esc(t)}</span><strong>${esc(v)}</strong></div>`,
      )
      .join(
        "",
      )}</div><p class="hint">Chuyển đúng số tiền và nội dung để việc đối chiếu thuận tiện hơn.</p>`,
    async () => {},
    "Đã hiểu",
  );
}
async function detail() {
  const id = Number(params.get("id") || params.get("courseId"));
  if (!id) throw new ApiError("Liên kết khóa học không hợp lệ.", 400);
  const [c, lessons, reviews] = await Promise.all([
    api("/courses/" + id),
    api("/courses/" + id + "/lessons"),
    api("/courses/" + id + "/reviews"),
  ]);
  const manager =
    user.role === "ADMIN" ||
    (user.role === "TEACHER" && c.teacherId === user.userId);
  let enrollment = null,
    pending = null;
  if (user.role === "STUDENT") {
    const [es, ps] = await Promise.all([
      api("/enrollments"),
      api("/payments/my"),
    ]);
    enrollment = es.find((e) => e.courseId === id && e.status !== "DROPPED");
    pending = ps.find((p) => p.courseId === id && p.status === "PENDING");
  }
  const ownReview = reviews.find((r) => r.studentId === user.userId);
  shell(
    c.title,
    "Một kỹ năng mới, từng bước rõ ràng.",
    `<a class="back-link" href="/courses.html">← Trở lại khóa học</a><div class="detail-grid"><div><section class="course-overview"><div class="chips"><span class="chip">${esc(c.category)}</span><span class="chip">${esc(c.level)}</span>${manager ? badge(c.status) : ""}</div><h2>${esc(c.title)}</h2><p>${esc(c.description || "Khám phá kiến thức qua các bài học và thực hành.")}</p><div class="instructor"><span class="avatar">${esc(initials(c.teacherName))}</span><span>Giảng viên<strong>${esc(c.teacherName)}</strong></span></div><div class="course-meta"><span>${icon("clock")}${c.durationHours || "—"} giờ học</span><span>${icon("book")}${lessons.length} bài học</span><span>${icon("users")}${c.enrollmentCount || 0} lượt đăng ký</span></div></section><section class="panel"><h2>Bạn sẽ học được gì?</h2><ul class="outcomes">${(
      c.learningOutcomes ||
      "Hiểu kiến thức nền tảng\nÁp dụng vào các ví dụ thực hành"
    )
      .split("\n")
      .filter(Boolean)
      .map((t) => `<li>${icon("check")}${esc(t)}</li>`)
      .join(
        "",
      )}</ul></section><section class="panel"><div class="section-heading"><div><h2>Chương trình học</h2><p>${lessons.length} bài học · Lộ trình từng bước</p></div>${manager ? '<button class="btn secondary compact" id="addLesson">' + icon("plus") + " Thêm bài</button>" : ""}</div><div class="syllabus">${lessons.map((l) => `<div class="syllabus-row"><span class="lesson-number">${String(l.orderIndex).padStart(2, "0")}</span><div><strong>${esc(l.title)}</strong><small>${l.isPublished ? "Đã xuất bản" : "Bản nháp"}</small></div>${manager ? `<div class="row-actions"><button class="text-link" data-edit-lesson="${l.lessonId}">Sửa</button><button class="text-link" data-publish="${l.lessonId}">${l.isPublished ? "Ẩn" : "Xuất bản"}</button><button class="text-link danger" data-delete-lesson="${l.lessonId}">Xóa</button></div>` : `<button class="icon-button" data-preview="${l.lessonId}" aria-label="Xem trước ${esc(l.title)}">${icon(enrollment ? "play" : "lock")}</button>`}</div>`).join("") || '<p class="muted-text">Giảng viên đang chuẩn bị chương trình.</p>'}</div></section><section class="panel"><div class="section-heading"><h2>Đánh giá của học viên</h2>${enrollment ? `<button class="btn secondary compact" id="writeReview">${ownReview ? "Sửa đánh giá" : "Viết đánh giá"}</button>` : ""}</div>${reviews.map((r) => `<article class="review"><span class="avatar">${esc(initials(r.studentName))}</span><div><strong>${esc(r.studentName)}</strong><div class="stars" aria-label="${r.rating} trên 5 sao">${"★".repeat(r.rating)}${"☆".repeat(5 - r.rating)}</div><p>${esc(r.comment || "")}</p><small>${date(r.createdAt)}</small></div>${user.role === "ADMIN" || r.studentId === user.userId ? `<button class="text-link danger" data-delete-review="${r.reviewId}">Xóa</button>` : ""}</article>`).join("") || '<p class="muted-text">Chưa có đánh giá. Hãy chia sẻ trải nghiệm sau khi học.</p>'}</section></div><aside class="enroll-panel">${art(c, true)}<div class="enroll-body"><span class="mini-label">ĐẦU TƯ CHO KIẾN THỨC</span><div class="price">${money(c.price)}</div>${enrollment ? `${badge(enrollment.status)}${progress(enrollment.progressPercentage)}${link("Vào phòng học " + icon("arrow"), "/learn.html?enrollmentId=" + enrollment.enrollmentId, "btn full")}` : user.role === "STUDENT" ? `<button class="btn full" id="enrollButton">${pending ? "Xem hướng dẫn chuyển khoản" : Number(c.price) > 0 ? "Đăng ký & thanh toán" : "Đăng ký miễn phí"} ${icon("arrow")}</button>` : '<p class="hint">Bạn đang xem khóa học với vai trò ' + role(user.role) + ".</p>"}<ul class="included"><li>${icon("check")} Học theo tốc độ của bạn</li><li>${icon("check")} Theo dõi tiến độ từng bài</li><li>${icon("check")} Tài liệu trong chương trình</li></ul>${manager ? `<div class="manager-tools"><h3>Quản lý khóa học</h3>${user.role === "ADMIN" ? `<button class="btn secondary full" id="editCourse">Chỉnh sửa khóa học</button><label>Trạng thái xuất bản<select id="courseStatus">${["DRAFT", "PUBLISHED", "ARCHIVED"].map((v) => `<option value="${v}" ${v === c.status ? "selected" : ""}>${labels[v]}</option>`).join("")}</select></label><button class="btn secondary full" id="saveStatus">Lưu trạng thái</button><button class="text-link danger" id="deleteCourse">Xóa khóa học</button>` : '<p class="hint">Bạn có thể soạn và xuất bản bài học của khóa này.</p>'}</div>` : ""}</div></aside></div>`,
  );
  if ($("#enrollButton"))
    $("#enrollButton").onclick = (e) =>
      action(async () => {
        if (pending) return paymentInfo(pending);
        if (Number(c.price) > 0) {
          const p = await api("/payments", "POST", { courseId: id });
          await detail();
          await paymentInfo(p);
        } else {
          await api("/enrollments", "POST", { courseId: id });
          toast("Đăng ký thành công. Bắt đầu học ngay!");
          await detail();
        }
      }, e.currentTarget);
  if (manager) {
    $("#addLesson").onclick = () =>
      action(() => lessonEditor(id, null, detail));
    document.querySelectorAll("[data-edit-lesson]").forEach(
      (b) =>
        (b.onclick = () =>
          action(() =>
            lessonEditor(
              id,
              lessons.find((l) => l.lessonId === Number(b.dataset.editLesson)),
              detail,
            ),
          )),
    );
    document.querySelectorAll("[data-publish]").forEach(
      (b) =>
        (b.onclick = () =>
          action(async () => {
            const l = lessons.find(
              (l) => l.lessonId === Number(b.dataset.publish),
            );
            await api("/lessons/" + l.lessonId + "/publish", "PUT", {
              isPublished: !l.isPublished,
            });
            toast("Đã cập nhật xuất bản và tiến độ học viên");
            await detail();
          }, b)),
    );
    document.querySelectorAll("[data-delete-lesson]").forEach(
      (b) =>
        (b.onclick = () =>
          confirmAction(
            "Xóa bài học",
            "Bài học và tiến độ liên quan sẽ được xóa. Tiến độ khóa học sẽ được tính lại.",
            async () => {
              await api("/lessons/" + b.dataset.deleteLesson, "DELETE");
              await detail();
            },
          )),
    );
  }
  if (user.role === "ADMIN") {
    $("#editCourse").onclick = () => action(() => courseEditor(c, detail));
    $("#saveStatus").onclick = (e) =>
      action(async () => {
        await api("/courses/" + id + "/status", "PUT", {
          status: $("#courseStatus").value,
        });
        toast("Đã cập nhật trạng thái");
        await detail();
      }, e.currentTarget);
    $("#deleteCourse").onclick = () =>
      confirmAction(
        "Xóa khóa học",
        "Chỉ có thể xóa khóa học chưa có bài học, học viên hoặc thanh toán.",
        async () => {
          await api("/courses/" + id, "DELETE");
          location.href = "/courses.html";
        },
      );
  }
  document.querySelectorAll("[data-preview]").forEach(
    (b) =>
      (b.onclick = () =>
        action(async () => {
          if (enrollment) {
            location.href =
              "/learn.html?enrollmentId=" +
              enrollment.enrollmentId +
              "&lessonId=" +
              b.dataset.preview;
            return;
          }
          const p = await api(
            "/lessons/" + b.dataset.preview + "/content_preview",
          );
          showDialog(
            p.title,
            `<p class="lesson-text">${esc(p.preview || "Bài học có tài liệu hoặc video. Đăng ký để truy cập nội dung.")}</p><div class="notice">Đây là bản xem trước. Đăng ký khóa học để xem đầy đủ.</div>`,
            async () => {},
            "Đã hiểu",
          );
        })),
  );
  if ($("#writeReview"))
    $("#writeReview").onclick = () =>
      showDialog(
        "Chia sẻ trải nghiệm",
        `${select(
          "Điểm đánh giá",
          "rating",
          [5, 4, 3, 2, 1].map((v) => [v, v + " sao"]),
          ownReview?.rating || 5,
        )}${textarea("Nhận xét", "comment", ownReview?.comment || "", 'maxlength="5000"')}`,
        async (d) => {
          d.rating = Number(d.rating);
          await api(
            ownReview
              ? "/reviews/" + ownReview.reviewId
              : "/courses/" + id + "/reviews",
            ownReview ? "PUT" : "POST",
            d,
          );
          toast("Đã lưu đánh giá");
          await detail();
        },
      );
  document.querySelectorAll("[data-delete-review]").forEach(
    (b) =>
      (b.onclick = () =>
        confirmAction(
          "Xóa đánh giá",
          "Bạn muốn xóa đánh giá này?",
          async () => {
            await api("/reviews/" + b.dataset.deleteReview, "DELETE");
            await detail();
          },
        )),
  );
}
async function myCourses() {
  if (user.role !== "STUDENT")
    throw new ApiError("Trang này dành cho học viên.", 403);
  const es = await api("/enrollments");
  const cs = await Promise.all(es.map((e) => api("/courses/" + e.courseId)));
  shell(
    "Việc học của tôi",
    "Từng bài học nhỏ tạo nên những bước tiến lớn.",
    `<div class="stats">${stat("Đã đăng ký", es.length, "Các khóa học của bạn", "book")}${stat("Đang học", es.filter((e) => e.status === "ENROLLED").length, "Hãy dành thời gian hôm nay", "play")}${stat("Hoàn thành", es.filter((e) => e.status === "COMPLETED").length, "Thành quả của sự kiên trì", "check")}</div><div class="course-grid">${es.map((e, i) => card(cs[i], `<div class="learning-progress"><div><span>${labels[e.status]}</span><strong>${Number(e.progressPercentage)}%</strong></div>${progress(e.progressPercentage)}</div>${link("Tiếp tục học " + icon("arrow"), "/learn.html?enrollmentId=" + e.enrollmentId, "btn full")}`)).join("") || empty("Hành trình của bạn bắt đầu tại đây", "Đăng ký một khóa học và xây dựng kỹ năng đầu tiên.", link("Khám phá khóa học", "/courses.html"))}</div>`,
  );
}
async function learn() {
  if (user.role !== "STUDENT")
    throw new ApiError("Phòng học dành cho học viên đã đăng ký.", 403);
  const id = Number(params.get("enrollmentId"));
  const e = await api("/enrollments/" + id);
  let current =
    Number(params.get("lessonId")) ||
    e.lessons.find((l) => !l.isCompleted)?.lessonId ||
    e.lessons[0]?.lessonId;
  shell(
    "Phòng học",
    e.courseTitle,
    `<a class="back-link" href="/course-detail.html?id=${e.courseId}">← Thông tin khóa học</a><div class="learning-layout"><aside class="lesson-sidebar"><h3>${esc(e.courseTitle)}</h3><div class="progress-label"><span>Tiến độ của bạn</span><strong>${Number(e.progressPercentage)}%</strong></div>${progress(e.progressPercentage)}<div id="lessonNav">${e.lessons.map((l) => `<button class="lesson-nav ${l.lessonId === current ? "active" : ""}" data-lesson="${l.lessonId}"><span>${icon(l.isCompleted ? "check" : "play")}</span><div><strong>${esc(l.title)}</strong><small>Bài ${l.orderIndex} ${l.isCompleted ? "· Đã hoàn thành" : ""}</small></div></button>`).join("")}</div></aside><article class="lesson-article" id="lessonContent"></article></div>`,
  );
  async function load(lessonId) {
    current = lessonId;
    document
      .querySelectorAll("[data-lesson]")
      .forEach((b) =>
        b.classList.toggle("active", Number(b.dataset.lesson) === lessonId),
      );
    $("#lessonContent").innerHTML = '<div class="boot">Đang tải bài học…</div>';
    try {
      const l = await api("/lessons/" + lessonId),
        entry = e.lessons.find((x) => x.lessonId === lessonId),
        url = safeUrl(l.contentUrl);
      const next =
        e.lessons[e.lessons.findIndex((x) => x.lessonId === lessonId) + 1];
      $("#lessonContent").innerHTML =
        `<span class="eyebrow">BÀI ${l.orderIndex} · ${esc(e.courseTitle)}</span><h2>${esc(l.title)}</h2><div class="lesson-text">${esc(l.textContent || "Bài học sử dụng tài liệu hoặc video bên dưới.")}</div>${url ? link("Mở tài liệu / video ↗", url, "btn secondary resource-link") : ""}<div class="lesson-footer"><div>${entry.isCompleted ? badge("COMPLETED") : '<button class="btn" id="completeLesson">' + icon("check") + " Đánh dấu hoàn thành</button>"}</div>${next ? `<button class="btn secondary" id="nextLesson">Bài tiếp theo ${icon("arrow")}</button>` : link("Về việc học của tôi", "/my-courses.html", "btn secondary")}</div>`;
      const resource = $(".resource-link");
      if (resource) {
        resource.target = "_blank";
        resource.rel = "noopener noreferrer";
      }
      if ($("#completeLesson"))
        $("#completeLesson").onclick = (b) =>
          action(async () => {
            await api(
              "/enrollments/" + id + "/complete_lesson/" + lessonId,
              "PUT",
            );
            toast("Đã ghi nhận tiến độ của bạn");
            params.set("lessonId", lessonId);
            await learn();
          }, b.currentTarget);
      if ($("#nextLesson"))
        $("#nextLesson").onclick = () => load(next.lessonId);
    } catch (error) {
      $("#lessonContent").innerHTML = errorBox(error);
    }
  }
  document
    .querySelectorAll("[data-lesson]")
    .forEach((b) => (b.onclick = () => load(Number(b.dataset.lesson))));
  if (current) await load(current);
  else
    $("#lessonContent").innerHTML = empty(
      "Chưa có bài học",
      "Giảng viên đang chuẩn bị nội dung.",
    );
}
async function users() {
  if (user.role !== "ADMIN")
    throw new ApiError("Bạn không có quyền quản lý người dùng.", 403);
  const data = await api("/users");
  shell(
    "Người dùng",
    "Quản lý tài khoản và quyền truy cập học viện.",
    `<div class="catalog-toolbar"><label class="search-box">${icon("search")}<input id="search" type="search" placeholder="Tìm theo tên, email hoặc tài khoản" aria-label="Tìm người dùng"></label><select id="roleFilter" aria-label="Lọc vai trò"><option value="">Mọi vai trò</option>${["STUDENT", "TEACHER", "ADMIN"].map((v) => `<option value="${v}">${role(v)}</option>`).join("")}</select></div><div class="panel table-panel"><div class="table-wrap"><table><thead><tr><th>Người dùng</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody id="userRows"></tbody></table></div></div>`,
    '<button class="btn" id="addUser">' +
      icon("plus") +
      " Tạo tài khoản</button>",
  );
  function draw() {
    const found = data.filter(
      (u) =>
        (!$("#roleFilter").value || u.role === $("#roleFilter").value) &&
        [u.fullName, u.username, u.email]
          .join(" ")
          .toLowerCase()
          .includes($("#search").value.toLowerCase()),
    );
    $("#userRows").innerHTML =
      found
        .map(
          (u) =>
            `<tr><td><div class="user-cell"><span class="avatar">${esc(initials(u.fullName))}</span><div><strong>${esc(u.fullName)}</strong><small>${esc(u.username)} · ${esc(u.email)}</small></div></div></td><td>${role(u.role)}</td><td><span class="badge ${u.isActive ? "good" : "muted"}">${u.isActive ? "Đang hoạt động" : "Đã khóa"}</span></td><td><div class="row-actions"><button class="text-link" data-edit-user="${u.userId}">Chỉnh sửa</button><button class="text-link" data-toggle-user="${u.userId}">${u.isActive ? "Khóa" : "Mở khóa"}</button><button class="text-link danger" data-remove-user="${u.userId}">Xóa</button></div></td></tr>`,
        )
        .join("") || '<tr><td colspan="4">Không tìm thấy người dùng.</td></tr>';
    document.querySelectorAll("[data-edit-user]").forEach(
      (b) =>
        (b.onclick = () => {
          const u = data.find((x) => x.userId === Number(b.dataset.editUser));
          showDialog(
            "Chỉnh sửa người dùng",
            `${field("Họ và tên", "fullName", u.fullName, "text", true, 'maxlength="100"')}${field("Email", "email", u.email, "email", true, 'maxlength="100"')}${
              u.role === "ADMIN"
                ? '<p class="hint">Vai trò của quản trị viên được giữ nguyên.</p>'
                : select(
                    "Vai trò",
                    "role",
                    ["STUDENT", "TEACHER", "ADMIN"].map((v) => [v, role(v)]),
                    u.role,
                  )
            }`,
            async (d) => {
              await api("/users/" + u.userId, "PUT", {
                fullName: d.fullName,
                email: d.email,
              });
              if (d.role && d.role !== u.role)
                await api("/users/" + u.userId + "/role", "PUT", {
                  role: d.role,
                });
              toast("Đã cập nhật tài khoản");
              await users();
            },
          );
        }),
    );
    document.querySelectorAll("[data-toggle-user]").forEach(
      (b) =>
        (b.onclick = () => {
          const u = data.find((x) => x.userId === Number(b.dataset.toggleUser));
          confirmAction(
            u.isActive ? "Khóa tài khoản" : "Mở khóa tài khoản",
            u.isActive
              ? "Phiên đăng nhập hiện tại của tài khoản này sẽ mất hiệu lực."
              : "Người dùng có thể đăng nhập trở lại.",
            async () => {
              await api("/users/" + u.userId + "/status", "PUT", {
                isActive: !u.isActive,
              });
              await users();
            },
          );
        }),
    );
    document.querySelectorAll("[data-remove-user]").forEach(
      (b) =>
        (b.onclick = () =>
          confirmAction(
            "Xóa tài khoản",
            "Chỉ có thể xóa tài khoản chưa có dữ liệu liên quan. Hãy dùng chức năng khóa nếu cần giữ lịch sử.",
            async () => {
              await api("/users/" + b.dataset.removeUser, "DELETE");
              await users();
            },
          )),
    );
  }
  $("#search").oninput = $("#roleFilter").onchange = draw;
  $("#addUser").onclick = () =>
    showDialog(
      "Tạo tài khoản",
      `${field("Họ và tên", "fullName", "", "text", true, 'maxlength="100"')}${field("Tên đăng nhập", "username", "", "text", true, 'maxlength="40"')}${field("Email", "email", "", "email", true, 'maxlength="100"')}${field("Mật khẩu", "password", "", "password", true, 'minlength="8" maxlength="64"')}${select(
        "Vai trò",
        "role",
        ["STUDENT", "TEACHER", "ADMIN"].map((v) => [v, role(v)]),
        "STUDENT",
      )}`,
      async (d) => {
        await api("/users", "POST", d);
        toast("Đã tạo tài khoản");
        await users();
      },
      "Tạo tài khoản",
    );
  draw();
}
async function payments() {
  const admin = page === "payments";
  if ((admin && user.role !== "ADMIN") || (!admin && user.role !== "STUDENT"))
    throw new ApiError("Bạn không có quyền truy cập trang này.", 403);
  const data = await api(admin ? "/payments" : "/payments/my");
  shell(
    admin ? "Duyệt thanh toán" : "Thanh toán của tôi",
    admin
      ? "Đối chiếu chuyển khoản và cấp quyền học cho học viên."
      : "Theo dõi yêu cầu và trạng thái đăng ký khóa học.",
    `<div class="stats">${stat("Đang chờ", data.filter((p) => p.status === "PENDING").length, "Chờ đối chiếu chuyển khoản", "clock")}${stat("Đã xác nhận", data.filter((p) => p.status === "CONFIRMED").length, "Đã cấp quyền học", "check")}${stat("Đã từ chối", data.filter((p) => p.status === "REJECTED").length, "Có thể tạo yêu cầu mới", "card")}</div><div class="catalog-toolbar"><select id="paymentFilter" aria-label="Lọc trạng thái"><option value="">Mọi trạng thái</option>${["PENDING", "CONFIRMED", "REJECTED"].map((v) => `<option value="${v}">${labels[v]}</option>`).join("")}</select><p class="hint">Thanh toán chuyển khoản được quản trị viên xác nhận thủ công.</p></div><div class="panel table-panel"><div class="table-wrap"><table><thead><tr><th>Khóa học${admin ? " / học viên" : ""}</th><th>Số tiền</th><th>Nội dung CK</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody id="paymentRows"></tbody></table></div></div>`,
  );
  function draw() {
    const list = data.filter(
      (p) =>
        !$("#paymentFilter").value || p.status === $("#paymentFilter").value,
    );
    $("#paymentRows").innerHTML =
      list
        .map(
          (p) =>
            `<tr><td><strong>${esc(p.courseTitle)}</strong><small>${admin ? esc(p.studentName) + " · " : ""}${date(p.createdAt)}</small></td><td><strong>${money(p.amount)}</strong></td><td><code>${esc(p.transferNote)}</code></td><td>${badge(p.status)}</td><td><div class="row-actions">${p.status === "PENDING" ? (admin ? `<button class="text-link" data-confirm="${p.paymentId}">Xác nhận</button><button class="text-link danger" data-reject="${p.paymentId}">Từ chối</button>` : `<button class="text-link" data-bank="${p.paymentId}">Hướng dẫn CK</button>`) : link("Xem khóa học", "/course-detail.html?id=" + p.courseId, "text-link")}</div></td></tr>`,
        )
        .join("") ||
      '<tr><td colspan="5">Chưa có yêu cầu thanh toán.</td></tr>';
    document.querySelectorAll("[data-confirm]").forEach(
      (b) =>
        (b.onclick = () =>
          confirmAction(
            "Xác nhận đã nhận tiền",
            "Chỉ xác nhận khi số tiền và nội dung chuyển khoản đã khớp. Học viên sẽ được cấp quyền học.",
            async () => {
              await api("/payments/" + b.dataset.confirm + "/confirm", "PUT");
              toast("Đã xác nhận và cấp quyền học");
              await payments();
            },
          )),
    );
    document.querySelectorAll("[data-reject]").forEach(
      (b) =>
        (b.onclick = () =>
          confirmAction(
            "Từ chối yêu cầu",
            "Học viên có thể tạo yêu cầu mới sau khi yêu cầu này bị từ chối.",
            async () => {
              await api("/payments/" + b.dataset.reject + "/reject", "PUT");
              await payments();
            },
          )),
    );
    document
      .querySelectorAll("[data-bank]")
      .forEach(
        (b) =>
          (b.onclick = () =>
            action(() =>
              paymentInfo(
                data.find((p) => p.paymentId === Number(b.dataset.bank)),
              ),
            )),
      );
  }
  $("#paymentFilter").onchange = draw;
  draw();
}
async function notifications() {
  const data = await api("/notifications");
  const unread = data.filter((n) => !n.isRead).length;
  shell(
    "Thông báo",
    unread
      ? `Bạn có ${unread} thông báo chưa đọc.`
      : "Bạn đã cập nhật tất cả thông báo.",
    `<div class="panel notification-list">${
      data
        .map((n) => {
          const url = safeUrl(n.targetUrl, true);
          return `<article class="notification ${n.isRead ? "" : "unread"}"><span class="notification-icon">${icon("bell")}</span><div><p>${esc(n.message)}</p><small>${date(n.createdAt)} · ${n.isRead ? "Đã đọc" : "Chưa đọc"}</small>${url ? link("Xem chi tiết →", url, "text-link") : ""}</div><div class="row-actions">${!n.isRead ? `<button class="text-link" data-read="${n.notificationId}">Đánh dấu đã đọc</button>` : ""}${user.role === "ADMIN" ? `<button class="text-link danger" data-delete-notification="${n.notificationId}">Xóa</button>` : ""}</div></article>`;
        })
        .join("") ||
      empty(
        "Không có thông báo mới",
        "Những cập nhật về khóa học và thanh toán sẽ xuất hiện tại đây.",
      )
    }</div>`,
    user.role === "ADMIN"
      ? '<button class="btn" id="newNotification">' +
          icon("plus") +
          " Tạo thông báo</button>"
      : "",
  );
  document.querySelectorAll("[data-read]").forEach(
    (b) =>
      (b.onclick = () =>
        action(async () => {
          await api("/notifications/" + b.dataset.read + "/read", "PUT");
          await notifications();
        }, b)),
  );
  document.querySelectorAll("[data-delete-notification]").forEach(
    (b) =>
      (b.onclick = () =>
        confirmAction(
          "Xóa thông báo",
          "Bạn muốn xóa thông báo này?",
          async () => {
            await api(
              "/notifications/" + b.dataset.deleteNotification,
              "DELETE",
            );
            await notifications();
          },
        )),
  );
  if ($("#newNotification"))
    $("#newNotification").onclick = () =>
      action(async () => {
        const us = await api("/users");
        showDialog(
          "Tạo thông báo",
          `${select(
            "Người nhận",
            "userId",
            us.map((u) => [u.userId, u.fullName]),
            us[0]?.userId,
          )}${textarea("Nội dung", "message", "", "required")}${field("Liên kết trong học viện", "targetUrl", "", "text", false, 'placeholder="/courses.html"')}`,
          async (d) => {
            d.userId = Number(d.userId);
            d.type = "GENERAL";
            await api("/notifications", "POST", d);
            toast("Đã tạo thông báo");
            await notifications();
          },
        );
      });
}
async function profile() {
  shell(
    "Hồ sơ của tôi",
    "Cập nhật thông tin cá nhân và bảo vệ tài khoản.",
    `<div class="profile-grid"><section class="panel"><div class="profile-summary"><span class="avatar big">${esc(initials(user.fullName))}</span><h2>${esc(user.fullName)}</h2><p>${role(user.role)} · ${esc(user.username)}</p></div><form id="profileForm">${field("Họ và tên", "fullName", user.fullName, "text", true, 'maxlength="100"')}${field("Email", "email", user.email, "email", true, 'maxlength="100"')}<div id="profileError" role="alert"></div><button class="btn">Lưu thông tin</button></form></section><section class="panel"><h2>Đổi mật khẩu</h2><p class="muted-text">Các phiên đăng nhập hiện tại sẽ hết hiệu lực sau khi đổi mật khẩu.</p><form id="passwordForm">${field("Mật khẩu hiện tại", "oldPassword", "", "password", true, 'autocomplete="current-password"')}${field("Mật khẩu mới", "newPassword", "", "password", true, 'minlength="8" maxlength="64" autocomplete="new-password"')}${field("Nhập lại mật khẩu mới", "confirmPassword", "", "password", true, 'minlength="8" maxlength="64"')}<div id="passwordError" role="alert"></div><button class="btn secondary">Đổi mật khẩu</button></form></section></div>`,
  );
  $("#profileForm").onsubmit = (e) => {
    e.preventDefault();
    action(async () => {
      user = {
        ...user,
        ...(await api(
          "/users/" + user.userId,
          "PUT",
          Object.fromEntries(new FormData(e.target)),
        )),
      };
      toast("Đã cập nhật hồ sơ");
      await profile();
    }, e.submitter);
  };
  $("#passwordForm").onsubmit = (e) => {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.target));
    if (d.newPassword !== d.confirmPassword) {
      $("#passwordError").innerHTML = errorBox(
        new Error("Hai mật khẩu mới chưa khớp."),
      );
      return;
    }
    action(async () => {
      await api("/users/" + user.userId + "/password", "PUT", {
        oldPassword: d.oldPassword,
        newPassword: d.newPassword,
      });
      toast("Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
      setTimeout(() => (location.href = "/login.html"), 1000);
    }, e.submitter);
  };
}
async function reports() {
  if (user.role !== "ADMIN")
    throw new ApiError("Báo cáo dành cho quản trị viên.", 403);
  const [top, us] = await Promise.all([
    api("/reports/top_courses?limit=10"),
    api("/users"),
  ]);
  const max = Math.max(1, ...top.map((c) => c.enrollmentCount));
  shell(
    "Báo cáo học viện",
    "Dữ liệu hoạt động, tiến độ học tập và hiệu quả giảng dạy.",
    `<section class="panel"><h2>Khóa học được đăng ký nhiều nhất</h2><div class="bar-chart">${top.map((c) => `<div><span>${esc(c.title)}</span><div class="bar-track"><i style="width:${(c.enrollmentCount / max) * 100}%"></i></div><strong>${c.enrollmentCount}</strong></div>`).join("") || '<p class="muted-text">Chưa có lượt đăng ký để thống kê.</p>'}</div></section><div class="profile-grid"><section class="panel"><h2>Tiến độ học viên</h2><label>Chọn học viên<select id="studentReport"><option value="">Chọn tài khoản</option>${us
      .filter((u) => u.role === "STUDENT")
      .map((u) => `<option value="${u.userId}">${esc(u.fullName)}</option>`)
      .join(
        "",
      )}</select></label><div id="studentResult"></div></section><section class="panel"><h2>Hoạt động giảng viên</h2><label>Chọn giảng viên<select id="teacherReport"><option value="">Chọn tài khoản</option>${us
      .filter((u) => u.role === "TEACHER")
      .map((u) => `<option value="${u.userId}">${esc(u.fullName)}</option>`)
      .join(
        "",
      )}</select></label><div id="teacherResult"></div></section></div>`,
  );
  $("#studentReport").onchange = () =>
    action(async () => {
      if (!$("#studentReport").value) {
        $("#studentResult").innerHTML = "";
        return;
      }
      const r = await api(
        "/reports/student_progress/" + $("#studentReport").value,
      );
      $("#studentResult").innerHTML =
        `<div class="report-summary">${r.totalEnrollments} khóa đăng ký · ${r.completedCount} hoàn thành · ${r.averageProgress}% trung bình</div>${r.courses.map((c) => `<div class="report-row"><strong>${esc(c.courseTitle)}</strong>${progress(c.progressPercentage)}<small>${c.progressPercentage}%</small></div>`).join("")}`;
    });
  $("#teacherReport").onchange = () =>
    action(async () => {
      if (!$("#teacherReport").value) {
        $("#teacherResult").innerHTML = "";
        return;
      }
      const r = await api(
        "/reports/teacher_courses_overview/" + $("#teacherReport").value,
      );
      $("#teacherResult").innerHTML =
        `<div class="report-summary">${r.totalCourses} khóa phụ trách · ${r.totalEnrollments} lượt đăng ký</div>${r.courses.map((c) => `<div class="report-row"><strong>${esc(c.title)}</strong><small>${c.lessonCount} bài · ${c.enrollmentCount} lượt đăng ký</small>${badge(c.status)}</div>`).join("")}`;
    });
}
async function start() {
  try {
    if (page === "index") return landing();
    if (page === "login") return await login();
    user = await api("/auth/me");
    const routes = {
      dashboard,
      courses,
      "course-detail": detail,
      learn,
      "my-courses": myCourses,
      users,
      payments,
      "my-payments": payments,
      notifications,
      profile,
      reports,
    };
    await (routes[page] || dashboard)();
  } catch (error) {
    if (error.status === 401) {
      location.replace("/login.html");
      return;
    }
    if (user) {
      shell(
        "Không thể mở trang",
        "Vui lòng kiểm tra và thử lại.",
        errorBox(error) +
          link("Về tổng quan", "/dashboard.html", "btn secondary"),
      );
    } else
      app.innerHTML = `<main class="public-error">${errorBox(error)}${link("Thử lại", location.pathname)}${link("Trang chủ", "/", "btn secondary")}</main>`;
  }
}
start();
