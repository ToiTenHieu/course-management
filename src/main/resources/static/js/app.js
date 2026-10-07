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
  String(v || "CM")
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
  if (typeof value !== "string" || !value.trim()) return null;
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
  document.title = title + " · Course Management";
}
function shell(title, subtitle, content, actions = "") {
  if (!user) return publicShell(title, subtitle, content, actions);
  setTitle(title);
  const admin = user.role === "ADMIN",
    teacher = user.role === "TEACHER";
  const items = [
    ["dashboard", "home", "Tổng quan"],
    [
      "courses",
      "book",
      teacher
        ? "Khóa phụ trách"
        : admin
          ? "Quản lý khóa học"
          : "Khám phá khóa học",
    ],
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
  app.innerHTML = `<div class="workspace ${admin || teacher ? "management" : "learner"}"><aside class="sidebar" id="workspaceSidebar"><a class="brand" href="/dashboard.html"><img src="/assets/mark.svg" alt=""><span>Course<span class="brand-sub">MANAGEMENT</span></span></a><p class="nav-label">${admin ? "QUẢN TRỊ" : teacher ? "GIẢNG DẠY" : "KHÔNG GIAN CỦA BẠN"}</p><nav>${items.map(([p, i, t]) => `<a class="nav-item ${page === p ? "active" : ""}" href="/${p}.html">${icon(i)}${t}</a>`).join("")}</nav><div class="sidebar-bottom"><div class="sidebar-note">Mỗi ngày một chút.<br><strong>Mỗi bước đều tiến xa.</strong></div><a class="account" href="/profile.html"><span class="avatar">${esc(initials(user.fullName))}</span><span><strong>${esc(user.fullName)}</strong><small>${role(user.role)}</small></span></a><button class="logout" id="logout">${icon("logout")} Đăng xuất</button></div></aside><div class="work-main"><header class="topbar"><button class="icon-button mobile-menu" aria-label="Mở điều hướng" id="menu" aria-expanded="false" aria-controls="workspaceSidebar">☰</button><span class="breadcrumb">Course Management <span>/</span> ${esc(title)}</span><div class="top-actions"><span class="role-chip">${role(user.role)}</span><a class="icon-button" aria-label="Thông báo" href="/notifications.html">${icon("bell")}</a><a class="avatar small" href="/profile.html" aria-label="Hồ sơ của tôi">${esc(initials(user.fullName))}</a></div></header><main id="main" class="main"><div class="page-heading"><div><span class="eyebrow">${admin ? "VẬN HÀNH COURSE MANAGEMENT" : teacher ? "KHÔNG GIAN GIẢNG DẠY" : "HÀNH TRÌNH HỌC TẬP"}</span><h1>${esc(title)}</h1><p>${esc(subtitle)}</p></div><div class="heading-actions">${actions}</div></div>${content}</main><footer class="footer">Course Management · Học để tiến xa <span>Kiến thức hôm nay. Cơ hội ngày mai.</span></footer></div></div>`;
  $("#logout").onclick = () =>
    action(async () => {
      if (saveBeforeLeaving && !(await saveBeforeLeaving())) return;
      await api("/auth/logout", "POST");
      location.href = "/login.html";
    });
  $("#menu").onclick = () => {
    const open = $(".sidebar").classList.toggle("open");
    $("#menu").setAttribute("aria-expanded", String(open));
  };
  document.onkeydown = (event) => {
    if (event.key === "Escape") {
      $(".sidebar")?.classList.remove("open");
      $("#menu")?.setAttribute("aria-expanded", "false");
    }
  };
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
  return `<div class="course-art ${style} ${large ? "large" : ""}"><span class="art-label">${label}</span><div class="art-orbit"></div><div class="art-symbol">${symbol}</div><span class="art-bottom">COURSE MANAGEMENT <span>↗</span></span></div>`;
}
function publicHeader() {
  return `<header class="public-header"><a class="brand" href="/"><img src="/assets/mark.svg" alt=""><span>Course<span class="brand-sub">MANAGEMENT</span></span></a><nav aria-label="Điều hướng chính"><a href="/courses.html" class="public-explore">Khám phá khóa học</a>${user ? link("Không gian của tôi " + icon("arrow"), "/dashboard.html", "btn compact") : link("Đăng nhập", "/login.html", "btn secondary compact") + link("Bắt đầu học", "/login.html?register=1", "btn compact")}</nav></header>`;
}
function publicShell(title, subtitle, content, actions = "") {
  setTitle(title);
  app.innerHTML = `<div class="public public-workspace">${publicHeader()}<main id="main"><div class="page-heading"><div><span class="eyebrow">KHÁM PHÁ · HỌC TẬP · TIẾN BỘ</span><h1>${esc(title)}</h1><p>${esc(subtitle)}</p></div>${actions}</div>${content}</main>${publicFooter()}</div>`;
}
function publicFooter() {
  return `<footer class="public-footer"><a class="brand" href="/">Course Management</a><span>Học theo tốc độ của bạn. Tiến bộ theo cách của bạn.</span><a href="/courses.html">Khám phá khóa học →</a></footer>`;
}
function authDestination(register = false) {
  return (
    "/login.html?" +
    new URLSearchParams({
      ...(register ? { register: "1" } : {}),
      next: location.pathname + location.search,
    })
  );
}
function outcomes(c) {
  const values = (c.learningOutcomes || "")
    .split("\n")
    .map((t) => t.trim())
    .filter(Boolean);
  return values.length
    ? `<ul class="outcomes">${values.map((t) => `<li>${icon("check")}${esc(t)}</li>`).join("")}</ul>`
    : '<p class="muted-text">Giảng viên đang cập nhật kết quả học tập của khóa học.</p>';
}
function readiness(c, lessons = null) {
  const checks = [
    ["Giới thiệu khóa học", !!c.description?.trim(), "overview"],
    ["Kết quả học tập", !!c.learningOutcomes?.trim(), "outcomes"],
    ["Bài học đã xuất bản", Number(c.lessonCount) > 0, "curriculum"],
  ];
  if (lessons)
    checks.push([
      "Nội dung cho mọi bài học",
      lessons.length > 0 &&
        lessons.every((l) => l.textContent?.trim() || l.contentUrl),
      "curriculum",
    ]);
  return `<section class="panel readiness"><div class="section-heading"><div><span class="eyebrow">CHUẨN BỊ KHÓA HỌC</span><h2>Sẵn sàng cho học viên</h2><p>${checks.filter((x) => x[1]).length}/${checks.length} mục đã chuẩn bị${lessons ? ` · ${lessons.filter((l) => !l.isPublished).length} bài còn ở bản nháp` : ""}</p></div>${badge(c.status)}</div><div class="readiness-items">${checks.map(([text, done, anchor]) => `<a href="${lessons ? "#" + anchor : "/course-detail.html?id=" + c.courseId + "#" + anchor}" class="readiness-item ${done ? "done" : ""}">${icon(done ? "check" : "clock")}<span>${text}</span><small>${done ? "Đã có" : "Cần bổ sung"}</small></a>`).join("")}</div><p class="hint">Kiểm tra nội dung, kết quả học tập và bài xuất bản trước khi mở khóa. Quản trị viên quyết định trạng thái xuất bản khóa học.</p></section>`;
}
function card(c, extra = "") {
  return `<article class="course-card"><a class="art-link" href="/course-detail.html?id=${c.courseId}" aria-label="${esc(c.title)}">${art(c)}</a><div class="card-body"><div class="card-tags"><span>${esc(c.category)}</span><span>${esc(c.level)}</span></div><a class="card-title" href="/course-detail.html?id=${c.courseId}">${esc(c.title)}</a><p class="teacher">${esc(c.teacherName)}</p><p class="card-description">${esc(c.description || "Khám phá chương trình và kết quả học tập.")}</p><div class="course-meta"><span>${icon("clock")}${c.durationHours || "—"} giờ</span><span>${icon("book")}${c.lessonCount || 0} bài học</span>${c.averageRating ? `<span class="rating">★ ${Number(c.averageRating).toFixed(1)}</span>` : `<span class="new-course">Chưa có đánh giá</span>`}</div>${extra || `<div class="card-foot"><strong>${money(c.price)}</strong>${link(icon("arrow"), "/course-detail.html?id=" + c.courseId, "round-link")}</div>`}</div></article>`;
}
const progress = (v) =>
  `<div class="progress" role="progressbar" aria-valuenow="${Number(v || 0)}" aria-valuemin="0" aria-valuemax="100"><span style="width:${Math.min(100, Math.max(0, Number(v || 0)))}%"></span></div>`;
const stat = (label, value, detail, i = "chart") =>
  `<div class="stat"><span class="stat-icon">${icon(i)}</span><span>${esc(label)}</span><strong>${esc(value)}</strong><small>${esc(detail)}</small></div>`;

async function landing() {
  user = await api("/auth/session");
  setTitle("Kỹ năng mới, cơ hội mới");
  app.innerHTML = `<div class="public">${publicHeader()}<main id="main"><section class="landing-hero"><div><span class="eyebrow">ĐẦU TƯ VÀO PHIÊN BẢN TIẾP THEO CỦA BẠN</span><h1>Học một kỹ năng.<br><span>Mở thêm cơ hội.</span></h1><p>Từ dòng code đầu tiên đến tư duy thiết kế và dữ liệu. Tìm khóa học phù hợp, học từng bước và nhìn thấy mình tiến bộ.</p><form class="hero-search" action="/courses.html"><label class="sr-only" for="heroSearch">Bạn muốn học gì?</label>${icon("search")}<input id="heroSearch" type="search" name="search" maxlength="255" placeholder="Bạn muốn học gì hôm nay?"><button class="btn">Tìm khóa học ${icon("arrow")}</button></form><div class="hero-proof"><span>${icon("check")} Xem chương trình trước khi đăng ký</span><span>${icon("check")} Học theo tốc độ của bạn</span></div></div><div class="landing-art"><div class="floating-card"><span class="mini-label">BIẾN KIẾN THỨC THÀNH KỸ NĂNG</span><h3>Bắt đầu nhỏ.<br>Tiến bộ mỗi ngày.</h3><div class="art-symbol">&lt;/&gt;</div><div class="art-subcard">${icon("book")} Lập trình · Thiết kế · Dữ liệu</div></div><span class="orbit-dot"></span></div></section><section class="discover-section"><div class="section-heading"><div><span class="eyebrow">TÌM HƯỚNG ĐI CỦA BẠN</span><h2>Bạn muốn phát triển kỹ năng nào?</h2></div>${link("Tất cả khóa học →", "/courses.html", "text-link")}</div><div class="topic-grid" id="homeTopics"><div class="boot" role="status">Đang tải chủ đề…</div></div></section><section class="discover-section"><div class="section-heading"><div><span class="eyebrow">KHÁM PHÁ ĐIỀU TIẾP THEO</span><h2>Khóa học mới nhất</h2><p>Xem kết quả học tập, chương trình và học phí trước khi chọn.</p></div>${link("Xem tất cả →", "/courses.html", "text-link")}</div><div class="course-grid" id="homeCourses"><div class="boot" role="status">Đang tải khóa học…</div></div></section><section id="approach" class="approach"><span class="eyebrow">MỖI BƯỚC ĐỀU CÓ Ý NGHĨA</span><h2>Từ tò mò đến làm được.</h2><div class="three-grid">${[
    [
      "01",
      "Chọn đúng điểm bắt đầu",
      "Đọc kết quả học tập, kiểm tra trình độ và xem chương trình trước khi quyết định.",
    ],
    [
      "02",
      "Học từng bài, ghi lại điều hay",
      "Quay lại bài gần nhất, lưu ghi chú riêng và đánh dấu bài đã hoàn thành.",
    ],
    [
      "03",
      "Nhìn thấy thành quả",
      "Theo dõi tiến độ từng khóa, ôn lại bài học và chia sẻ trải nghiệm của bạn.",
    ],
  ]
    .map(
      ([n, t, d]) =>
        `<article><span>${n}</span><h3>${t}</h3><p>${d}</p></article>`,
    )
    .join(
      "",
    )}</div></section><section class="join-banner"><div><span class="eyebrow">BƯỚC ĐẦU TIÊN CHỈ MẤT VÀI PHÚT</span><h2>Kỹ năng tiếp theo đang chờ bạn.</h2><p>Tạo tài khoản để đăng ký khóa học và lưu hành trình học tập.</p></div>${link("Tạo tài khoản miễn phí " + icon("arrow"), "/login.html?register=1", "btn lime")}</section></main>${publicFooter()}</div>`;
  const load = async () => {
    try {
      const [catalog, categories] = await Promise.all([
        api("/discovery/catalog?size=3"),
        api("/discovery/categories"),
      ]);
      $("#homeTopics").innerHTML =
        categories
          .map(
            (c, i) =>
              `<a class="topic-card" href="/courses.html?${new URLSearchParams({ category: c })}"><span class="topic-icon">${["&lt;/&gt;", "{ }", "✳", "⌘"][i % 4]}</span><strong>${esc(c)}</strong>${icon("arrow")}</a>`,
          )
          .join("") ||
        '<p class="muted-text">Các chủ đề sẽ xuất hiện khi khóa học được xuất bản.</p>';
      $("#homeCourses").innerHTML =
        catalog.content.map((c) => card(c)).join("") ||
        empty(
          "Khóa học đang được chuẩn bị",
          "Quay lại để khám phá nội dung mới.",
        );
    } catch (error) {
      $("#homeTopics").innerHTML = "";
      $("#homeCourses").innerHTML =
        errorBox(error) +
        '<button class="btn secondary" id="retryHome">Thử lại</button>';
      $("#retryHome").onclick = load;
    }
  };
  await load();
}
async function guestDetail() {
  const id = Number(params.get("id") || params.get("courseId"));
  if (!Number.isSafeInteger(id) || id < 1)
    throw new ApiError("Liên kết khóa học không hợp lệ.", 400);
  const c = await api("/discovery/courses/" + id);
  publicShell(
    c.title,
    "Tìm hiểu khóa học trước khi bắt đầu.",
    `<a class="back-link" href="/courses.html">← Trở lại khóa học</a><div class="detail-grid"><div><section class="course-overview" id="overview"><div class="chips"><span class="chip">${esc(c.category)}</span><span class="chip">${esc(c.level)}</span></div><h2>${esc(c.title)}</h2><p>${esc(c.description || "Giảng viên đang cập nhật giới thiệu.")}</p><div class="instructor"><span class="avatar">${esc(initials(c.teacherName))}</span><span>Giảng viên<strong>${esc(c.teacherName)}</strong></span></div><div class="course-meta"><span>${icon("clock")}${c.durationHours || "—"} giờ</span><span>${icon("book")}${c.lessonCount} bài học</span><span>${icon("users")}${c.enrollmentCount} lượt đăng ký</span>${c.averageRating ? `<span class="rating">★ ${Number(c.averageRating).toFixed(1)}</span>` : ""}</div></section><nav class="detail-tabs" aria-label="Thông tin khóa học"><a href="#outcomes">Kết quả học tập</a><a href="#curriculum">Chương trình</a><a href="#howToLearn">Cách học</a></nav><section class="panel" id="outcomes"><h2>Bạn sẽ học được gì?</h2>${outcomes(c)}</section><section class="panel" id="curriculum"><div class="section-heading"><div><h2>Chương trình học</h2><p>${c.lessons.length} bài học · Trình độ ${esc(c.level)}</p></div></div><div class="syllabus">${c.lessons.map((l) => `<div class="syllabus-row"><span class="lesson-number">${String(l.orderIndex).padStart(2, "0")}</span><div><strong>${esc(l.title)}</strong><small>Nội dung dành cho học viên đã đăng ký</small></div>${icon("lock")}</div>`).join("")}</div></section><section class="panel" id="howToLearn"><h2>Học theo cách của bạn</h2><div class="three-grid learning-benefits"><div>${icon("play")}<h3>Từng bước rõ ràng</h3><p>Đọc bài học và mở tài liệu hoặc video do giảng viên cung cấp.</p></div><div>${icon("book")}<h3>Ghi chú riêng</h3><p>Lưu lại ý tưởng và điều cần ôn trong từng bài học.</p></div><div>${icon("chart")}<h3>Theo dõi tiến độ</h3><p>Đánh dấu bài đã học, quay lại bài gần nhất và xem kết quả của bạn.</p></div></div></section></div><aside class="enroll-panel">${art(c, true)}<div class="enroll-body"><span class="mini-label">BẮT ĐẦU HÀNH TRÌNH</span><div class="price">${money(c.price)}</div>${link("Đăng nhập để đăng ký " + icon("arrow"), authDestination(), "btn full")}${link("Tạo tài khoản miễn phí", authDestination(true), "text-link full")}<p class="hint">${Number(c.price) > 0 ? "Thanh toán chuyển khoản. Quyền học được cấp sau khi quản trị viên xác nhận." : "Đăng ký miễn phí để truy cập các bài học."}</p><ul class="included"><li>${icon("check")} ${esc(c.level)} · ${c.durationHours || "—"} giờ học</li><li>${icon("check")} ${c.lessonCount} bài học trong chương trình</li><li>${icon("check")} Ghi chú và theo dõi tiến độ</li></ul></div></aside></div>`,
  );
}
async function login() {
  const config = await api("/auth/config");
  let register = params.has("register");
  function draw() {
    setTitle(register ? "Tạo tài khoản" : "Đăng nhập");
    app.innerHTML = `<div class="auth-page"><section class="auth-story"><a class="brand light" href="/"><img src="/assets/mark.svg" alt=""><span>Course<span class="brand-sub">MANAGEMENT</span></span></a><div><span class="eyebrow">KHÔNG GIAN CHO SỰ TIẾN BỘ</span><h1>Đi xa hơn,<br>bắt đầu từ<br><em>một bài học.</em></h1><p>Một nơi để khám phá kỹ năng mới, tiếp tục điều đang học và nhìn thấy mình tiến bộ mỗi ngày.</p><div class="story-art"><span>&lt;/&gt;</span><span>✳</span><span>{ }</span></div></div><small>Học để tiến xa.</small></section><section class="auth-form"><div class="auth-inner"><a class="back-link" href="/">← Trang chủ</a><h2>${register ? "Bắt đầu hành trình" : "Chào mừng trở lại"}</h2><p class="muted-text">${register ? "Tạo tài khoản học viên để khám phá các khóa học." : "Đăng nhập để tiếp tục hành trình học tập của bạn."}</p><form id="authForm">${register ? field("Họ và tên", "fullName", "", "text", true, 'maxlength="100"') : ""}${field("Tên đăng nhập", "username", "", "text", true, 'autocomplete="username" minlength="3" maxlength="40"')}${register ? field("Email", "email", "", "email", true, 'autocomplete="email" maxlength="100"') : ""}${field("Mật khẩu", "password", "", "password", true, `autocomplete="${register ? "new" : "current"}-password" ${register ? 'minlength="8" maxlength="64"' : ""}`)}${register ? '<small class="hint">Mật khẩu từ 8 đến 64 ký tự.</small>' : ""}<div id="authError" role="alert"></div><button class="btn full" id="authSubmit">${register ? "Tạo tài khoản" : "Đăng nhập"} ${icon("arrow")}</button></form><p class="auth-switch">${register ? "Đã có tài khoản?" : "Chưa có tài khoản?"} <button class="text-link" id="toggleAuth">${register ? "Đăng nhập" : "Đăng ký miễn phí"}</button></p>${
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
          const next = params.get("next");
          location.href =
            next &&
            /^\/(?:course-detail|courses|my-courses|learn)\.html(?:\?[^#]*)?$/.test(
              next,
            )
              ? next
              : "/dashboard.html";
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
  const student = user.role === "STUDENT",
    admin = user.role === "ADMIN";
  const [catalog, notices] = await Promise.all([
    api(
      "/courses" + (user.role === "TEACHER" ? "?teacherId=" + user.userId : ""),
    ),
    api("/notifications"),
  ]);
  const first = user.fullName.trim().split(/\s+/).slice(-1)[0];
  let body = "",
    actions = "";
  if (student) {
    const [enrollments, payments] = await Promise.all([
      api("/enrollments"),
      api("/payments/my"),
    ]);
    const active = enrollments.filter((e) => e.status === "ENROLLED");
    const pending = payments.filter((p) => p.status === "PENDING");
    const known = new Set(enrollments.map((e) => e.courseId));
    body = `<div class="stats">${stat("Đang học", active.length, "Khóa học đang tiếp tục", "play")}${stat("Đã hoàn thành", enrollments.filter((e) => e.status === "COMPLETED").length, "Thành quả của bạn", "check")}${stat("Đang chờ thanh toán", pending.length, "Yêu cầu cần đối chiếu", "card")}</div><section class="resume-section"><div class="section-heading"><div><span class="eyebrow">DÀNH MỘT CHÚT THỜI GIAN HÔM NAY</span><h2>Tiếp tục từ nơi bạn dừng lại</h2></div>${link("Việc học của tôi →", "/my-courses.html", "text-link")}</div>${
      active.length
        ? `<div class="resume-grid">${active
            .slice(0, 3)
            .map(
              (e) =>
                `<article class="resume-card"><span class="stat-icon">${icon("play")}</span><h3>${esc(e.courseTitle)}</h3><div class="progress-label"><span>Tiến độ khóa học</span><strong>${Number(e.progressPercentage)}%</strong></div>${progress(e.progressPercentage)}${link("Tiếp tục học " + icon("arrow"), "/learn.html?enrollmentId=" + e.enrollmentId, "btn full")}</article>`,
            )
            .join("")}</div>`
        : empty(
            "Bắt đầu một hành trình mới",
            "Chọn một kỹ năng bạn muốn phát triển. Các khóa đã đăng ký sẽ xuất hiện ở đây.",
            link("Khám phá khóa học", "/courses.html"),
          )
    }</section>${pending.length ? `<div class="notice payment-notice">${icon("clock")}<div><strong>${pending.length} yêu cầu thanh toán đang chờ xác nhận</strong><p>Kiểm tra số tiền và nội dung chuyển khoản. Quyền học mở sau khi admin xác nhận.</p></div>${link("Xem hướng dẫn", "/my-payments.html?status=PENDING", "btn secondary compact")}</div>` : ""}<section><div class="section-heading"><div><h2>Kỹ năng tiếp theo của bạn</h2><p>Khám phá các khóa bạn chưa đăng ký.</p></div>${link("Tất cả khóa học →", "/courses.html", "text-link")}</div><div class="course-grid">${
      catalog
        .filter((c) => !known.has(c.courseId))
        .slice(0, 3)
        .map((c) => card(c))
        .join("") ||
      empty(
        "Bạn đã khám phá hết các khóa hiện có",
        "Tiếp tục học hoặc quay lại khi có khóa mới.",
      )
    }</div></section>`;
  } else if (admin) {
    const [users, payments] = await Promise.all([
      api("/users"),
      api("/payments"),
    ]);
    const pending = payments
      .filter((p) => p.status === "PENDING")
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    const drafts = catalog.filter((c) => c.status === "DRAFT");
    actions = link("Quản lý khóa học " + icon("arrow"), "/courses.html", "btn");
    body = `<div class="stats">${stat("Thanh toán chờ duyệt", pending.length, "Ưu tiên yêu cầu cũ nhất", "card")}${stat("Khóa học bản nháp", drafts.length, "Cần kiểm tra để xuất bản", "book")}${stat("Tài khoản hoạt động", users.filter((u) => u.isActive).length, `${users.length} tài khoản toàn hệ thống`, "users")}</div><div class="operations-grid"><section class="panel"><div class="section-heading"><div><span class="eyebrow">CẦN XỬ LÝ</span><h2>Đối chiếu thanh toán</h2></div>${link("Xem tất cả →", "/payments.html?status=PENDING", "text-link")}</div>${
      pending
        .slice(0, 5)
        .map(
          (p) =>
            `<a class="task-row" href="/payments.html?status=PENDING"><span class="stat-icon">${icon("card")}</span><div><strong>${esc(p.courseTitle)}</strong><small>${esc(p.studentName)} · ${date(p.createdAt)}</small></div><strong>${money(p.amount)}</strong>${icon("arrow")}</a>`,
        )
        .join("") ||
      empty(
        "Không có yêu cầu chờ duyệt",
        "Các yêu cầu thanh toán mới sẽ xuất hiện tại đây.",
      )
    }</section><section class="panel"><div class="section-heading"><div><span class="eyebrow">CHẤT LƯỢNG NỘI DUNG</span><h2>Khóa chờ hoàn thiện</h2></div>${link("Các bản nháp →", "/courses.html?status=DRAFT", "text-link")}</div>${
      drafts
        .slice(0, 5)
        .map(
          (c) =>
            `<a class="task-row" href="/course-detail.html?id=${c.courseId}"><span class="stat-icon">${icon("book")}</span><div><strong>${esc(c.title)}</strong><small>${esc(c.teacherName)} · ${c.lessonCount} bài đã xuất bản</small></div>${badge(c.status)}${icon("arrow")}</a>`,
        )
        .join("") ||
      empty(
        "Không có khóa bản nháp",
        "Mở quản lý khóa học để chuẩn bị nội dung mới.",
      )
    }</section></div><div class="quick-actions">${link(icon("users") + " Quản lý người dùng", "/users.html", "btn secondary")}${link(icon("chart") + " Xem báo cáo", "/reports.html", "btn secondary")}${link(icon("bell") + " Thông báo", "/notifications.html", "btn secondary")}</div>`;
  } else {
    const drafts = catalog.filter((c) => c.status === "DRAFT");
    actions = link("Khóa học phụ trách →", "/courses.html", "btn");
    body = `<div class="stats">${stat("Khóa học phụ trách", catalog.length, "Nội dung do bạn giảng dạy", "book")}${stat("Đang chuẩn bị", drafts.length, "Khóa đang ở bản nháp", "clock")}${stat(
      "Lượt đăng ký",
      catalog.reduce((sum, c) => sum + Number(c.enrollmentCount || 0), 0),
      "Trên các khóa phụ trách",
      "users",
    )}</div><div class="section-heading"><div><span class="eyebrow">CHĂM CHÚT TRẢI NGHIỆM HỌC</span><h2>Không gian giảng dạy của bạn</h2><p>Hoàn thiện thông tin và chương trình trước khi xuất bản.</p></div></div><div class="teaching-grid">${
      catalog
        .map(
          (c) =>
            `<article class="panel teaching-card"><div class="section-heading"><div><span class="mini-label">${esc(c.category)} · ${esc(c.level)}</span><h3>${esc(c.title)}</h3></div>${badge(c.status)}</div><div class="teaching-metrics"><span>${c.lessonCount} bài đã xuất bản</span><span>${c.enrollmentCount} lượt đăng ký</span><span>${c.averageRating ? "★ " + Number(c.averageRating).toFixed(1) : "Chưa có đánh giá"}</span></div><div class="readiness-items">${[
              [
                "Giới thiệu và mục tiêu",
                !!c.description?.trim() && !!c.learningOutcomes?.trim(),
              ],
              ["Bài học đã xuất bản", Number(c.lessonCount) > 0],
            ]
              .map(
                ([label, done]) =>
                  `<div class="readiness-item ${done ? "done" : ""}">${icon(done ? "check" : "clock")}<span>${label}</span><small>${done ? "Đã có" : "Cần bổ sung"}</small></div>`,
              )
              .join(
                "",
              )}</div>${link("Soạn bài & quản lý khóa " + icon("arrow"), "/course-detail.html?id=" + c.courseId, "btn secondary full")}</article>`,
        )
        .join("") ||
      empty(
        "Bạn chưa được phân công khóa học",
        "Quản trị viên sẽ phân công khóa học để bạn bắt đầu soạn nội dung.",
      )
    }</div>`;
  }
  body += `<section class="panel activity"><div class="section-heading"><div><h2>Cập nhật gần đây</h2><p>${notices.filter((n) => !n.isRead).length} thông báo chưa đọc</p></div>${link("Xem thông báo →", "/notifications.html", "text-link")}</div>${
    notices
      .slice(0, 3)
      .map(
        (n) =>
          `<div class="activity-row"><span class="activity-dot"></span><p>${esc(n.message)}${safeUrl(n.targetUrl, true) ? " " + link("Xem chi tiết →", safeUrl(n.targetUrl, true), "text-link") : ""}</p><small>${date(n.createdAt)}</small></div>`,
      )
      .join("") || '<p class="muted-text">Bạn đã cập nhật hết thông báo.</p>'
  }</section>`;
  shell(
    admin
      ? "Trung tâm vận hành"
      : student
        ? "Không gian học tập"
        : "Không gian giảng dạy",
    `Chào ${first}, ${student ? "tiếp tục một bài học, tiến gần hơn đến mục tiêu của bạn." : admin ? "đây là những việc cần chú ý hôm nay." : "cùng tạo nên những bài học đáng học."}`,
    body,
    actions,
  );
}
async function courseEditor(course, done) {
  const teachers =
    user.role === "ADMIN"
      ? await api("/users?role=TEACHER&status=active")
      : [{ userId: user.userId, fullName: user.fullName }];
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
    )}<div class="form-grid">${field("Chủ đề", "category", course?.category || "Lập trình", "text", true, 'maxlength="100"')}${field("Trình độ", "level", course?.level || "Cơ bản", "text", true, 'maxlength="100"')}${field("Học phí (₫)", "price", course?.price || 0, "number", true, 'min="0" max="99999999" step="0.01"' + (user.role !== "ADMIN" ? ' readonly aria-describedby="priceHint"' : ""))}${field("Thời lượng (giờ)", "durationHours", course?.durationHours || 1, "number", true, 'min="1" max="10000"')}</div>${user.role !== "ADMIN" ? '<p class="hint" id="priceHint">Học phí và phân công giảng viên do quản trị viên quản lý.</p>' : ""}${textarea("Kết quả học tập (mỗi dòng một mục)", "learningOutcomes", course?.learningOutcomes || "", 'maxlength="10000"')}`,
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
  const admin = user?.role === "ADMIN",
    teacher = user?.role === "TEACHER";
  const catalogBase = user ? "/courses" : "/discovery";
  const categories = await api(
    catalogBase + "/categories" + (teacher ? "?teacherId=" + user.userId : ""),
  );
  shell(
    teacher
      ? "Khóa học phụ trách"
      : admin
        ? "Quản lý khóa học"
        : "Khám phá khóa học",
    teacher
      ? "Chăm chút từng bài học, đồng hành cùng học viên."
      : admin
        ? "Kiểm tra chương trình, phân công giảng viên và quản lý xuất bản."
        : "Chọn một kỹ năng mới. Bắt đầu một hành trình mới.",
    `<div class="catalog-intro"><div><h2>${admin || teacher ? "Một khóa học tốt.<br><em>Một trải nghiệm đáng học.</em>" : "Đầu tư vào điều<br>bạn <em>có thể trở thành.</em>"}</h2><p>${admin || teacher ? "Hoàn thiện nội dung, kiểm tra kết quả học tập và xuất bản khi sẵn sàng." : "Học theo từng bước, theo tốc độ của riêng bạn."}</p></div><span class="catalog-symbol">✳</span></div><div class="catalog-toolbar"><label class="search-box">${icon("search")}<input id="search" type="search" placeholder="Tìm khóa học hoặc giảng viên…" aria-label="Tìm khóa học"></label><select id="sort" aria-label="Sắp xếp"><option value="new">Mới nhất</option><option value="price">Học phí tăng dần</option><option value="title">Tên A–Z</option></select>${admin || teacher ? '<select id="status" aria-label="Trạng thái"><option value="">Mọi trạng thái</option><option value="DRAFT">Bản nháp</option><option value="PUBLISHED">Đã xuất bản</option><option value="ARCHIVED">Đã lưu trữ</option></select>' : ""}</div><div class="chips" id="categories"><button class="chip active" data-category="">Tất cả</button>${categories.map((c) => `<button class="chip" data-category="${esc(c)}">${esc(c)}</button>`).join("")}<button class="chip" id="freeFilter">Miễn phí</button></div><div class="result-count" id="count" role="status" aria-live="polite"></div><div class="course-grid" id="catalog"></div><nav class="pagination" id="pagination" aria-label="Phân trang khóa học"></nav>`,
    admin
      ? '<button class="btn" id="createCourse">' +
          icon("plus") +
          " Tạo khóa học</button>"
      : "",
  );
  let category = "",
    free = false,
    current = 0,
    revision = 0,
    timer;
  function restoreFilters() {
    const query = new URLSearchParams(location.search);
    $("#search").value = (query.get("search") || "").slice(0, 255);
    $("#search").maxLength = 255;
    $("#sort").value = ["new", "price", "title"].includes(query.get("sort"))
      ? query.get("sort")
      : "new";
    if ($("#status"))
      $("#status").value = ["DRAFT", "PUBLISHED", "ARCHIVED"].includes(
        query.get("status"),
      )
        ? query.get("status")
        : "";
    category = categories.includes(query.get("category"))
      ? query.get("category")
      : "";
    free = query.get("freeOnly") === "true";
    const requestedPage = Number(query.get("page") || 1);
    current =
      Number.isSafeInteger(requestedPage) &&
      requestedPage > 0 &&
      requestedPage < 1000000
        ? requestedPage - 1
        : 0;
    document
      .querySelectorAll("[data-category]")
      .forEach((b) =>
        b.classList.toggle("active", b.dataset.category === category),
      );
    $("#freeFilter").classList.toggle("active", free);
    $("#freeFilter").setAttribute("aria-pressed", String(free));
  }
  function queryForPage() {
    const query = new URLSearchParams({
      page: String(current),
      size: "9",
      sort: $("#sort").value,
    });
    if (teacher) query.set("teacherId", user.userId);
    if ($("#search").value.trim())
      query.set("search", $("#search").value.trim());
    if ($("#status")?.value) query.set("status", $("#status").value);
    if (category) query.set("category", category);
    if (free) query.set("freeOnly", "true");
    return query;
  }
  function saveFilters(replace = false) {
    const query = queryForPage();
    query.delete("size");
    query.delete("teacherId");
    query.set("page", String(current + 1));
    const url = location.pathname + "?" + query;
    if (url !== location.pathname + location.search)
      history[replace ? "replaceState" : "pushState"](null, "", url);
  }
  function drawPagination(result) {
    if (result.totalPages <= 1) {
      $("#pagination").innerHTML = "";
      return;
    }
    const pages = [
      ...new Set([
        0,
        result.totalPages - 1,
        current - 2,
        current - 1,
        current,
        current + 1,
        current + 2,
      ]),
    ]
      .filter((p) => p >= 0 && p < result.totalPages)
      .sort((a, b) => a - b);
    const button = (p, text, disabled = false) =>
      `<button class="chip ${p === current ? "active" : ""}" data-pagenum="${p}" ${disabled ? "disabled" : ""} ${p === current ? 'aria-current="page"' : ""}>${text}</button>`;
    $("#pagination").innerHTML =
      button(current - 1, "← Trước", current === 0) +
      pages
        .map(
          (p, i) =>
            (i > 0 && p - pages[i - 1] > 1
              ? '<span aria-hidden="true">…</span>'
              : "") + button(p, p + 1),
        )
        .join("") +
      button(current + 1, "Sau →", current === result.totalPages - 1);
    document.querySelectorAll("[data-pagenum]").forEach(
      (b) =>
        (b.onclick = () => {
          current = Number(b.dataset.pagenum);
          refresh();
        }),
    );
  }
  async function load(requestRevision) {
    try {
      const result = await api(catalogBase + "/catalog?" + queryForPage());
      if (requestRevision !== revision) return;
      if (current > 0 && current >= result.totalPages) {
        current = Math.max(0, result.totalPages - 1);
        saveFilters(true);
        return await load(requestRevision);
      }
      $("#count").textContent = result.totalElements + " khóa học phù hợp";
      $("#catalog").innerHTML =
        result.content
          .map((c) =>
            card(
              c,
              admin || teacher
                ? `<div class="card-foot">${badge(c.status)}<strong>${money(c.price)}</strong></div>`
                : "",
            ),
          )
          .join("") ||
        empty(
          "Chưa tìm thấy khóa học",
          "Thử từ khóa hoặc bộ lọc khác.",
          '<button class="btn secondary" id="resetFilters">Xóa bộ lọc</button>',
        );
      if ($("#resetFilters"))
        $("#resetFilters").onclick = () => {
          $("#search").value = "";
          $("#sort").value = "new";
          if ($("#status")) $("#status").value = "";
          category = "";
          free = false;
          current = 0;
          saveFilters();
          restoreFilters();
          refresh();
        };
      drawPagination(result);
    } catch (error) {
      if (requestRevision !== revision) return;
      $("#count").textContent = "Chưa tải được danh sách khóa học";
      $("#catalog").innerHTML =
        `<div>${errorBox(error)}<button class="btn secondary" id="retryCatalog">Thử lại</button></div>`;
      $("#retryCatalog").onclick = () => refresh(0, true);
      if (error.status === 401) location.href = "/login.html";
    } finally {
      if (requestRevision === revision)
        $("#catalog").setAttribute("aria-busy", "false");
    }
  }
  function refresh(delay = 0, replace = false) {
    clearTimeout(timer);
    const requestRevision = ++revision;
    saveFilters(replace);
    $("#count").textContent = "Đang tìm khóa học…";
    $("#catalog").setAttribute("aria-busy", "true");
    $("#catalog").innerHTML = "";
    $("#pagination").innerHTML = "";
    if (delay) timer = setTimeout(() => load(requestRevision), delay);
    else return load(requestRevision);
  }
  $("#search").oninput = () => {
    current = 0;
    refresh(300, true);
  };
  $("#sort").onchange = () => {
    current = 0;
    refresh();
  };
  if ($("#status"))
    $("#status").onchange = () => {
      current = 0;
      refresh();
    };
  document.querySelectorAll("[data-category]").forEach(
    (b) =>
      (b.onclick = () => {
        category = b.dataset.category;
        document
          .querySelectorAll("[data-category]")
          .forEach((x) => x.classList.toggle("active", x === b));
        current = 0;
        refresh();
      }),
  );
  $("#freeFilter").onclick = (e) => {
    free = !free;
    e.currentTarget.classList.toggle("active", free);
    e.currentTarget.setAttribute("aria-pressed", String(free));
    current = 0;
    refresh();
  };
  window.onpopstate = () => {
    restoreFilters();
    refresh(0, true);
  };
  if (admin)
    $("#createCourse").onclick = () =>
      action(() => courseEditor(null, courses));
  restoreFilters();
  await refresh(0, true);
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
    `<a class="back-link" href="/courses.html">← Trở lại khóa học</a><div class="detail-grid"><div>${manager ? readiness(c, lessons) : ""}<section class="course-overview" id="overview"><div class="chips"><span class="chip">${esc(c.category)}</span><span class="chip">${esc(c.level)}</span>${manager ? badge(c.status) : ""}</div><h2>${esc(c.title)}</h2><p>${esc(c.description || "Khám phá kiến thức qua các bài học và thực hành.")}</p><div class="instructor"><span class="avatar">${esc(initials(c.teacherName))}</span><span>Giảng viên<strong>${esc(c.teacherName)}</strong></span></div><div class="course-meta"><span>${icon("clock")}${c.durationHours || "—"} giờ học</span><span>${icon("book")}${lessons.length} bài học</span><span>${icon("users")}${c.enrollmentCount || 0} lượt đăng ký</span></div></section><nav class="detail-tabs" aria-label="Thông tin khóa học"><a href="#outcomes">Kết quả học tập</a><a href="#curriculum">Chương trình</a><a href="#reviews">Đánh giá</a></nav><section class="panel" id="outcomes"><h2>Bạn sẽ học được gì?</h2>${outcomes(c)}</section><section class="panel" id="curriculum"><div class="section-heading"><div><h2>Chương trình học</h2><p>${lessons.filter((l) => l.isPublished).length} bài đã xuất bản${manager ? ` · ${lessons.filter((l) => !l.isPublished).length} bài nháp` : ""}</p></div>${manager ? '<button class="btn secondary compact" id="addLesson">' + icon("plus") + " Thêm bài</button>" : ""}</div><div class="syllabus">${lessons.map((l) => `<div class="syllabus-row"><span class="lesson-number">${String(l.orderIndex).padStart(2, "0")}</span><div><strong>${esc(l.title)}</strong><small>${l.isPublished ? "Đã xuất bản" : "Bản nháp"}</small></div>${manager ? `<div class="row-actions"><button class="text-link" data-edit-lesson="${l.lessonId}">Sửa</button><button class="text-link" data-publish="${l.lessonId}">${l.isPublished ? "Ẩn" : "Xuất bản"}</button><button class="text-link danger" data-delete-lesson="${l.lessonId}">Xóa</button></div>` : `<button class="icon-button" data-preview="${l.lessonId}" aria-label="Xem trước ${esc(l.title)}">${icon(enrollment ? "play" : "lock")}</button>`}</div>`).join("") || '<p class="muted-text">Giảng viên đang chuẩn bị chương trình.</p>'}</div></section><section class="panel" id="reviews"><div class="section-heading"><h2>Đánh giá của học viên</h2>${enrollment ? `<button class="btn secondary compact" id="writeReview">${ownReview ? "Sửa đánh giá" : "Viết đánh giá"}</button>` : ""}</div>${reviews.map((r) => `<article class="review"><span class="avatar">${esc(initials(r.studentName))}</span><div><strong>${esc(r.studentName)}</strong><div class="stars" aria-label="${r.rating} trên 5 sao">${"★".repeat(r.rating)}${"☆".repeat(5 - r.rating)}</div><p>${esc(r.comment || "")}</p><small>${date(r.createdAt)}</small></div>${user.role === "ADMIN" || r.studentId === user.userId ? `<button class="text-link danger" data-delete-review="${r.reviewId}">Xóa</button>` : ""}</article>`).join("") || '<p class="muted-text">Chưa có đánh giá. Hãy chia sẻ trải nghiệm sau khi học.</p>'}</section></div><aside class="enroll-panel">${art(c, true)}<div class="enroll-body"><span class="mini-label">ĐẦU TƯ CHO KIẾN THỨC</span><div class="price">${money(c.price)}</div>${enrollment ? `${badge(enrollment.status)}${progress(enrollment.progressPercentage)}${link("Vào phòng học " + icon("arrow"), "/learn.html?enrollmentId=" + enrollment.enrollmentId, "btn full")}` : user.role === "STUDENT" ? `<button class="btn full" id="enrollButton">${pending ? "Xem hướng dẫn chuyển khoản" : Number(c.price) > 0 ? "Đăng ký & thanh toán" : "Đăng ký miễn phí"} ${icon("arrow")}</button>` : '<p class="hint">Bạn đang xem khóa học với vai trò ' + role(user.role) + ".</p>"}<ul class="included"><li>${icon("check")} Học theo tốc độ của bạn</li><li>${icon("check")} Theo dõi tiến độ từng bài</li><li>${icon("check")} Tài liệu trong chương trình</li></ul>${manager ? `<div class="manager-tools"><h3>Quản lý khóa học</h3>${user.role === "ADMIN" ? `<button class="btn secondary full" id="editCourse">Chỉnh sửa khóa học</button><label>Trạng thái xuất bản<select id="courseStatus">${["DRAFT", "PUBLISHED", "ARCHIVED"].map((v) => `<option value="${v}" ${v === c.status ? "selected" : ""}>${labels[v]}</option>`).join("")}</select></label><button class="btn secondary full" id="saveStatus">Lưu trạng thái</button><button class="text-link danger" id="deleteCourse">Xóa khóa học</button>` : '<button class="btn secondary full" id="editCourse">Chỉnh sửa thông tin khóa</button><p class="hint">Bạn có thể sửa giới thiệu, mục tiêu và soạn bài. Quản trị viên xuất bản khóa học.</p>'}</div>` : ""}</div></aside></div>`,
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
  if (manager)
    $("#editCourse").onclick = () => action(() => courseEditor(c, detail));
  if (user.role === "ADMIN") {
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
    "Tìm bài học đang dang dở, tiếp tục tiến bộ hoặc ôn lại điều đã học.",
    `<div class="stats">${stat("Đã đăng ký", es.length, "Các khóa học của bạn", "book")}${stat("Đang học", es.filter((e) => e.status === "ENROLLED").length, "Tiếp tục từ bài gần nhất", "play")}${stat("Hoàn thành", es.filter((e) => e.status === "COMPLETED").length, "Thành quả của sự kiên trì", "check")}</div><div class="catalog-toolbar"><label class="search-box">${icon("search")}<input id="learningSearch" type="search" placeholder="Tìm trong khóa học của bạn" aria-label="Tìm khóa đã đăng ký"></label><select id="learningSort" aria-label="Sắp xếp khóa đã đăng ký"><option value="new">Đăng ký gần nhất</option><option value="progress">Tiến độ giảm dần</option><option value="title">Tên A–Z</option></select></div><div class="chips" id="learningFilters">${[
      ["", "Tất cả"],
      ["ENROLLED", "Đang học"],
      ["COMPLETED", "Đã hoàn thành"],
    ]
      .map(
        ([value, label]) =>
          `<button class="chip" data-learning-status="${value}">${label}</button>`,
      )
      .join(
        "",
      )}</div><p class="result-count" id="learningCount" role="status" aria-live="polite"></p><div class="course-grid" id="learningCatalog"></div>`,
  );
  let status = new URLSearchParams(location.search).get("status") || "";
  if (!["", "ENROLLED", "COMPLETED"].includes(status)) status = "";
  function draw() {
    const found = es
      .map((e, i) => ({ e, c: cs[i] }))
      .filter(
        ({ e, c }) =>
          (!status || status === e.status) &&
          c.title
            .toLocaleLowerCase("vi")
            .includes(
              $("#learningSearch").value.trim().toLocaleLowerCase("vi"),
            ),
      );
    found.sort((a, b) =>
      $("#learningSort").value === "progress"
        ? Number(b.e.progressPercentage) - Number(a.e.progressPercentage)
        : $("#learningSort").value === "title"
          ? a.c.title.localeCompare(b.c.title, "vi")
          : b.e.enrollmentId - a.e.enrollmentId,
    );
    document.querySelectorAll("[data-learning-status]").forEach((b) => {
      b.classList.toggle("active", b.dataset.learningStatus === status);
      b.setAttribute(
        "aria-pressed",
        String(b.dataset.learningStatus === status),
      );
    });
    $("#learningCount").textContent = `${found.length} khóa học`;
    $("#learningCatalog").innerHTML =
      found
        .map(({ e, c }) =>
          card(
            c,
            `<div class="learning-progress"><div><span>${labels[e.status]}</span><strong>${Number(e.progressPercentage)}%</strong></div>${progress(e.progressPercentage)}</div>${e.status === "DROPPED" ? badge(e.status) : link((e.status === "COMPLETED" ? "Ôn lại khóa học " : "Tiếp tục học ") + icon("arrow"), "/learn.html?enrollmentId=" + e.enrollmentId, "btn full")}`,
          ),
        )
        .join("") ||
      empty(
        es.length
          ? "Không có khóa phù hợp"
          : "Hành trình của bạn bắt đầu tại đây",
        es.length
          ? "Thử đổi từ khóa hoặc chọn Tất cả."
          : "Đăng ký một khóa học và xây dựng kỹ năng đầu tiên.",
        link("Khám phá khóa học", "/courses.html"),
      );
  }
  $("#learningSearch").oninput = $("#learningSort").onchange = draw;
  document.querySelectorAll("[data-learning-status]").forEach(
    (b) =>
      (b.onclick = () => {
        status = b.dataset.learningStatus;
        history.replaceState(
          null,
          "",
          location.pathname + (status ? "?status=" + status : ""),
        );
        draw();
      }),
  );
  draw();
}
let dirtyNote = false;
let saveBeforeLeaving = null;
window.addEventListener("beforeunload", (event) => {
  if (dirtyNote) {
    event.preventDefault();
    event.returnValue = "";
  }
});
document.addEventListener(
  "click",
  async (event) => {
    const anchor = event.target.closest("a");
    if (
      !dirtyNote ||
      !saveBeforeLeaving ||
      !anchor ||
      anchor.target ||
      event.ctrlKey ||
      event.metaKey ||
      event.shiftKey ||
      event.altKey ||
      event.button !== 0
    )
      return;
    const url = new URL(anchor.href);
    if (
      url.pathname === location.pathname &&
      url.search === location.search &&
      url.hash
    )
      return;
    event.preventDefault();
    if (await saveBeforeLeaving()) location.href = anchor.href;
  },
  true,
);
async function learn() {
  if (user.role !== "STUDENT")
    throw new ApiError("Phòng học dành cho học viên đã đăng ký.", 403);
  const id = Number(params.get("enrollmentId"));
  let e = await api("/enrollments/" + id);
  const requested = Number(
    new URLSearchParams(location.search).get("lessonId"),
  );
  let current = e.lessons.some((l) => l.lessonId === requested)
    ? requested
    : e.lastLessonId ||
      e.lessons.find((l) => !l.isCompleted)?.lessonId ||
      e.lessons[0]?.lessonId;
  let revision = 0,
    saving = false,
    savedText = "";
  shell(
    "Phòng học",
    e.courseTitle,
    `<div class="player-heading"><a class="back-link" href="/course-detail.html?id=${e.courseId}">← Thông tin khóa học</a><button class="btn secondary compact" id="focusMode" aria-pressed="false">${icon("play")} Chế độ tập trung</button></div><div class="learning-layout"><aside class="lesson-sidebar"><details class="curriculum-disclosure" id="curriculumMenu" open><summary>Chương trình khóa học <span>${e.lessons.length} bài</span></summary><h3>${esc(e.courseTitle)}</h3><div class="progress-label"><span>Tiến độ của bạn</span><strong id="courseProgress">${Number(e.progressPercentage)}%</strong></div><div id="playerProgress">${progress(e.progressPercentage)}</div><p class="hint" id="lessonTotal"></p><div id="lessonNav"></div></details></aside><article class="lesson-article" id="lessonContent" aria-live="polite"></article></div>`,
  );
  $("#focusMode").onclick = () => {
    const active = $(".workspace").classList.toggle("focus-learning");
    $("#focusMode").setAttribute("aria-pressed", String(active));
    $("#focusMode").innerHTML =
      icon("play") + (active ? " Hiện điều hướng" : " Chế độ tập trung");
  };
  function drawNav() {
    $("#courseProgress").textContent = `${Number(e.progressPercentage)}%`;
    $("#playerProgress").innerHTML = progress(e.progressPercentage);
    $("#lessonTotal").textContent =
      `${e.lessons.filter((l) => l.isCompleted).length}/${e.lessons.length} bài đã hoàn thành`;
    $("#lessonNav").innerHTML = e.lessons
      .map(
        (l) =>
          `<button class="lesson-nav ${l.lessonId === current ? "active" : ""}" data-lesson="${l.lessonId}" ${l.lessonId === current ? 'aria-current="step"' : ""}><span>${icon(l.isCompleted ? "check" : "play")}</span><div><strong>${esc(l.title)}</strong><small>Bài ${l.orderIndex} ${l.isCompleted ? "· Đã hoàn thành" : ""}</small></div></button>`,
      )
      .join("");
    document
      .querySelectorAll("[data-lesson]")
      .forEach((b) => (b.onclick = () => load(Number(b.dataset.lesson))));
  }
  async function saveNotes() {
    if (!dirtyNote) return !saving;
    if (saving) return false;
    saving = true;
    const input = $("#lessonNote"),
      button = $("#saveNote"),
      status = $("#noteStatus"),
      note = input.value;
    input.disabled = button.disabled = true;
    status.textContent = "Đang lưu…";
    try {
      await api(`/enrollments/${id}/notes/${current}`, "PUT", { note });
      savedText = note;
      dirtyNote = false;
      status.textContent = "Đã lưu. Chỉ bạn có thể xem ghi chú này.";
      return true;
    } catch (error) {
      status.textContent = "Chưa lưu được: " + error.message + ". Hãy thử lại.";
      return false;
    } finally {
      saving = false;
      input.disabled = button.disabled = false;
    }
  }
  saveBeforeLeaving = saveNotes;
  async function load(lessonId, historyMode = "pushState") {
    if (!e.lessons.some((l) => l.lessonId === lessonId)) return;
    if (!(await saveNotes())) {
      history.replaceState(
        null,
        "",
        location.pathname +
          "?" +
          new URLSearchParams({
            enrollmentId: String(id),
            lessonId: String(current),
          }),
      );
      return;
    }
    const request = ++revision;
    const savedBeforeRequest = savedText;
    $("#lessonContent").setAttribute("aria-busy", "true");
    // Keep current content until the next lesson is ready so failed navigation cannot erase notes.
    try {
      const [l, note] = await Promise.all([
        api("/lessons/" + lessonId),
        api(`/enrollments/${id}/notes/${lessonId}`),
      ]);
      if (request !== revision) return;
      if (dirtyNote && !(await saveNotes())) return;
      if (request !== revision) return;
      if (current === lessonId && savedBeforeRequest !== savedText)
        note.note = savedText;
      current = lessonId;
      savedText = note.note;
      dirtyNote = false;
      const index = e.lessons.findIndex((x) => x.lessonId === lessonId),
        entry = e.lessons[index],
        next = e.lessons[index + 1],
        previous = e.lessons[index - 1],
        url = safeUrl(l.contentUrl);
      const query = new URLSearchParams({
        enrollmentId: String(id),
        lessonId: String(lessonId),
      });
      if (location.search !== "?" + query)
        history[historyMode](null, "", location.pathname + "?" + query);
      $("#lessonContent").innerHTML =
        `<div class="lesson-position"><span class="eyebrow">BÀI ${index + 1} / ${e.lessons.length}</span><span id="completionBadge">${entry.isCompleted ? badge("COMPLETED") : '<span class="badge muted">Chưa hoàn thành</span>'}</span></div><h2 tabindex="-1">${esc(l.title)}</h2><div class="lesson-text">${esc(l.textContent || "Bài học sử dụng tài liệu hoặc video bên dưới.")}</div>${url ? link("Mở tài liệu / video ↗", url, "btn secondary resource-link") : ""}<div class="lesson-footer"><div><button class="btn" id="completeLesson" ${entry.isCompleted ? "disabled" : ""}>${icon("check")} ${entry.isCompleted ? "Đã hoàn thành" : "Đánh dấu hoàn thành"}</button></div><div class="lesson-step-actions">${previous ? '<button class="btn secondary compact" id="previousLesson">← Bài trước</button>' : ""}${next ? '<button class="btn secondary compact" id="nextLesson">Bài tiếp theo →</button>' : link("Việc học của tôi", "/my-courses.html", "btn secondary compact")}</div></div><section class="private-notes"><div class="section-heading"><div><h3>Ghi chú của tôi</h3><p>Ghi lại ý tưởng, điều cần ôn hoặc câu hỏi cho chính bạn.</p></div><span class="badge muted">${icon("lock")} Riêng tư</span></div><label for="lessonNote" class="sr-only">Ghi chú cho bài học</label><textarea id="lessonNote" rows="6" maxlength="10000" placeholder="Điều mình học được từ bài này…">${esc(note.note)}</textarea><div class="note-actions"><span id="noteStatus" role="status" aria-live="polite">${note.note ? "Ghi chú đã lưu. Chỉ bạn có thể xem." : "Lưu ghi chú để xem lại trên các thiết bị của bạn."}</span><button class="btn secondary compact" id="saveNote">Lưu ghi chú</button></div></section>${Number(e.progressPercentage) === 100 ? '<div class="notice completion-notice">' + icon("check") + "<div><strong>Bạn đã hoàn thành khóa học!</strong><p>Ôn lại những điều quan trọng hoặc chia sẻ trải nghiệm để giúp học viên khác chọn khóa học.</p>" + link("Đánh giá khóa học →", "/course-detail.html?id=" + e.courseId + "#reviews", "text-link") + "</div></div>" : ""}`;
      drawNav();
      const resource = $(".resource-link");
      if (resource) {
        resource.target = "_blank";
        resource.rel = "noopener noreferrer";
      }
      $("#lessonNote").oninput = () => {
        dirtyNote = $("#lessonNote").value !== savedText;
        $("#noteStatus").textContent = dirtyNote
          ? "Chưa lưu · " + $("#lessonNote").value.length + "/10000 ký tự"
          : "Ghi chú đã lưu.";
      };
      $("#saveNote").onclick = saveNotes;
      $("#completeLesson").onclick = (event) =>
        action(async () => {
          if (!(await saveNotes())) return;
          const completionRevision = revision;
          e = await api(
            `/enrollments/${id}/complete_lesson/${lessonId}`,
            "PUT",
          );
          if (current !== lessonId || revision !== completionRevision) {
            drawNav();
            return;
          }
          toast(
            Number(e.progressPercentage) === 100
              ? "Chúc mừng! Bạn đã hoàn thành khóa học."
              : "Đã ghi nhận tiến độ của bạn",
          );
          await load(lessonId, "replaceState");
        }, event.currentTarget);
      if ($("#previousLesson"))
        $("#previousLesson").onclick = () => load(previous.lessonId);
      if ($("#nextLesson"))
        $("#nextLesson").onclick = () => load(next.lessonId);
      if (matchMedia("(max-width: 700px)").matches)
        $("#curriculumMenu").open = false;
      await api(`/enrollments/${id}/access_lesson/${lessonId}`, "PUT").catch(
        (error) => toast("Chưa lưu được vị trí học: " + error.message, true),
      );
    } catch (error) {
      if (request !== revision) return;
      toast(error.message, true);
      if (!$("#lessonContent h2")) {
        $("#lessonContent").innerHTML =
          errorBox(error) +
          '<button class="btn secondary" id="retryLesson">Thử lại</button>';
        $("#retryLesson").onclick = () => load(lessonId, "replaceState");
      }
    } finally {
      if (request === revision)
        $("#lessonContent").setAttribute("aria-busy", "false");
    }
  }
  window.onpopstate = () => {
    const lesson = Number(new URLSearchParams(location.search).get("lessonId"));
    load(
      e.lessons.some((l) => l.lessonId === lesson) ? lesson : current,
      "replaceState",
    );
  };
  drawNav();
  if (current) await load(current, "replaceState");
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
    "Quản lý tài khoản và quyền truy cập Course Management.",
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
  const requestedStatus = new URLSearchParams(location.search).get("status");
  if (["PENDING", "CONFIRMED", "REJECTED"].includes(requestedStatus))
    $("#paymentFilter").value = requestedStatus;
  data.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  $("#paymentFilter").onchange = () => {
    history.replaceState(
      null,
      "",
      location.pathname +
        ($("#paymentFilter").value
          ? "?status=" + $("#paymentFilter").value
          : ""),
    );
    draw();
  };
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
          )}${textarea("Nội dung", "message", "", "required")}${field("Liên kết trong Course Management", "targetUrl", "", "text", false, 'placeholder="/courses.html"')}`,
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
    "Báo cáo Course Management",
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
    if (page === "index") return await landing();
    if (page === "login") return await login();
    user = await api(
      ["courses", "course-detail"].includes(page)
        ? "/auth/session"
        : "/auth/me",
    );
    if (!user && page === "course-detail") return await guestDetail();
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
