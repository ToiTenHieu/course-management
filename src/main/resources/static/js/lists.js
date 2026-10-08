import { api } from "./api.js";

let listInstance = 0;

export function statusTabs(items, parameter = "status") {
  return `<div class="chips list-tabs" role="group" aria-label="Lọc danh sách">${items.map(([value, label]) => `<button class="chip" data-list-filter="${parameter}" data-value="${value}" aria-pressed="false">${label}</button>`).join("")}</div>`;
}

export async function mountPagedList({
  endpoint,
  container,
  render,
  controls = [],
  size = 10,
  label = "mục",
  columns = 1,
  countSelector = "#listCount",
  pagerSelector = "#listPager",
  syncUrl = true,
  unwrap = (result) => result,
}) {
  const rows = document.querySelector(container);
  const instance = String(++listInstance);
  rows.dataset.listInstance = instance;
  const count = document.querySelector(countSelector);
  const pager = document.querySelector(pagerSelector);
  let current = 0,
    revision = 0,
    timer;
  const values = {};
  const filters = [...controls];
  for (const button of document.querySelectorAll("[data-list-filter]")) {
    const param = button.dataset.listFilter;
    if (!filters.some((f) => f.param === param))
      filters.push({
        param,
        allowed: [
          ...document.querySelectorAll(`[data-list-filter="${param}"]`),
        ].map((b) => b.dataset.value),
        default: "",
      });
  }
  function restore() {
    const query = new URLSearchParams(syncUrl ? location.search : "");
    const page = Number(query.get("page") || 1);
    current =
      Number.isSafeInteger(page) && page > 0 && page < 1000000 ? page - 1 : 0;
    for (const f of filters) {
      let value = query.get(f.param) ?? f.default ?? "";
      if (f.allowed && !f.allowed.includes(value)) value = f.default || "";
      values[f.param] = value.slice(0, 255);
      if (f.id) document.querySelector(f.id).value = values[f.param];
    }
    updateTabs();
  }
  function updateTabs() {
    for (const f of controls)
      if (f.id) document.querySelector(f.id).value = values[f.param] || "";
    document.querySelectorAll("[data-list-filter]").forEach((b) => {
      const selected = b.dataset.value === values[b.dataset.listFilter];
      b.classList.toggle("active", selected);
      b.setAttribute("aria-pressed", String(selected));
    });
  }
  function query(apiRequest = false) {
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(values))
      if (value) query.set(key, value.trim());
    query.set("page", String(current + (apiRequest ? 0 : 1)));
    if (apiRequest) query.set("size", size);
    return query;
  }
  function save(replace) {
    if (!syncUrl) return;
    const next = location.pathname + "?" + query() + location.hash;
    if (next !== location.pathname + location.search + location.hash)
      history[replace ? "replaceState" : "pushState"](null, "", next);
  }
  function pagination(result) {
    pager.replaceChildren();
    if (result.totalPages < 2) return;
    const pages = [
      ...new Set([0, current - 1, current, current + 1, result.totalPages - 1]),
    ]
      .filter((p) => p >= 0 && p < result.totalPages)
      .sort((a, b) => a - b);
    const add = (page, text, disabled = false) => {
      const b = document.createElement("button");
      b.className = "chip";
      b.textContent = text;
      b.disabled = disabled;
      if (page === current && /^\d+$/.test(String(text))) {
        b.classList.add("active");
        b.setAttribute("aria-current", "page");
      }
      b.onclick = () => {
        current = page;
        refresh();
        rows
          .closest(".table-panel, .course-grid, .notification-list")
          ?.scrollIntoView({ block: "start" });
      };
      pager.append(b);
    };
    add(current - 1, "← Trước", current === 0);
    pages.forEach((p, i) => {
      if (i && p - pages[i - 1] > 1) pager.append(document.createTextNode("…"));
      add(p, String(p + 1));
    });
    add(current + 1, "Sau →", current === result.totalPages - 1);
  }
  async function load(request) {
    try {
      const response = await api(
        endpoint + (endpoint.includes("?") ? "&" : "?") + query(true),
      );
      if (
        request !== revision ||
        !rows.isConnected ||
        rows.dataset.listInstance !== instance
      )
        return;
      const result = unwrap(response);
      if (current > 0 && current >= result.totalPages) {
        current = Math.max(0, result.totalPages - 1);
        save(true);
        return load(request);
      }
      render(result.content, result);
      count.textContent = result.totalElements
        ? `${current * size + 1}–${current * size + result.content.length} trong ${result.totalElements} ${label}`
        : `Không có ${label} phù hợp.`;
      pagination(result);
    } catch (error) {
      if (
        request !== revision ||
        !rows.isConnected ||
        rows.dataset.listInstance !== instance
      )
        return;
      count.textContent = error.message + " ";
      const retry = document.createElement("button");
      retry.className = "text-link";
      retry.textContent = "Thử lại";
      retry.onclick = () => refresh(0, true);
      count.append(retry);
      if (error.status === 401) location.href = "/login.html";
    } finally {
      if (request === revision && rows.dataset.listInstance === instance)
        rows.setAttribute("aria-busy", "false");
    }
  }
  function refresh(delay = 0, replace = false) {
    clearTimeout(timer);
    const request = ++revision;
    updateTabs();
    save(replace);
    count.textContent = "Đang tải danh sách…";
    rows.setAttribute("aria-busy", "true");
    pager.replaceChildren();
    rows.innerHTML =
      rows.tagName === "TBODY"
        ? `<tr><td colspan="${columns}">Đang tải…</td></tr>`
        : '<p class="muted-text">Đang tải…</p>';
    if (delay) timer = setTimeout(() => load(request), delay);
    else return load(request);
  }
  for (const f of controls) {
    const input = document.querySelector(f.id);
    if (input.tagName === "INPUT") input.maxLength = 255;
    input[input.tagName === "INPUT" ? "oninput" : "onchange"] = () => {
      values[f.param] = input.value;
      current = 0;
      refresh(input.tagName === "INPUT" ? 250 : 0, input.tagName === "INPUT");
    };
  }
  document.querySelectorAll("[data-list-filter]").forEach((b) => {
    b.onclick = () => {
      values[b.dataset.listFilter] = b.dataset.value;
      current = 0;
      refresh();
    };
  });
  if (syncUrl)
    window.onpopstate = () => {
      restore();
      refresh(0, true);
    };
  restore();
  await refresh(0, true);
  return { refresh };
}

export async function wireUserPicker(
  select,
  { role = "", activeOnly = false, label = "Tìm tài khoản" } = {},
) {
  const input = document.createElement("input");
  input.type = "search";
  input.maxLength = 255;
  input.placeholder = label;
  input.setAttribute("aria-label", label);
  select.before(input);
  const count = document.createElement("small");
  count.className = "hint";
  count.setAttribute("role", "status");
  select.after(count);
  let request = 0,
    timer,
    chosen = select.value;
  let chosenText = select.selectedOptions[0]?.textContent || "";
  select.onchange = ((original) => (event) => {
    chosen = select.value;
    chosenText = select.selectedOptions[0]?.textContent || "";
    original?.call(select, event);
  })(select.onchange);
  async function load() {
    const version = ++request;
    try {
      const query = new URLSearchParams({
        search: input.value,
        size: "20",
        page: "0",
      });
      if (role) query.set("role", role);
      if (activeOnly) query.set("status", "active");
      const result = await api("/lists/users?" + query);
      if (version !== request || !select.isConnected) return;
      select.replaceChildren(new Option("Chọn tài khoản", ""));
      for (const u of result.content)
        select.add(
          new Option(`${u.fullName} · ${u.username}`, String(u.userId)),
        );
      if (chosen && !result.content.some((u) => String(u.userId) === chosen))
        select.add(new Option(chosenText, chosen));
      select.value = chosen;
      count.textContent =
        result.totalElements > 20
          ? `Hiển thị 20/${result.totalElements} tài khoản. Nhập tên để tìm thêm.`
          : `${result.totalElements} tài khoản phù hợp.`;
    } catch (error) {
      if (version === request) count.textContent = error.message;
    }
  }
  input.oninput = () => {
    ++request;
    clearTimeout(timer);
    timer = setTimeout(load, 250);
  };
  await load();
}

// For metadata already needed by the page, keep long sections bounded too.
export function paginateElements(container, selector, size = 8) {
  const items = [...container.querySelectorAll(selector)];
  if (items.length <= size) return;
  const nav = document.createElement("nav");
  nav.className = "pagination";
  nav.setAttribute("aria-label", "Phân trang nội dung");
  container.append(nav);
  let current = Math.max(
    0,
    Math.floor(items.findIndex((item) => item.matches(".active")) / size),
  );
  function draw() {
    items.forEach((item, i) => {
      item.hidden = i < current * size || i >= (current + 1) * size;
    });
    nav.replaceChildren();
    const pages = Math.ceil(items.length / size);
    const prev = document.createElement("button"),
      next = document.createElement("button"),
      info = document.createElement("span");
    prev.className = next.className = "chip";
    prev.textContent = "← Trước";
    next.textContent = "Sau →";
    prev.disabled = current === 0;
    next.disabled = current === pages - 1;
    info.textContent = `Trang ${current + 1}/${pages} · ${items.length} mục`;
    prev.onclick = () => {
      current--;
      draw();
    };
    next.onclick = () => {
      current++;
      draw();
    };
    nav.append(prev, info, next);
  }
  draw();
}

export function wireDetailTabs(initial = "outcomes") {
  const nav = document.querySelector(".detail-tabs");
  if (!nav) return;
  const tabs = [...nav.querySelectorAll('a[href^="#"]')].filter((a) =>
    document.querySelector(a.getAttribute("href")),
  );
  nav.setAttribute("role", "tablist");
  for (const [i, tab] of tabs.entries()) {
    const panel = document.querySelector(tab.getAttribute("href"));
    tab.id = `course-tab-${i}`;
    tab.setAttribute("role", "tab");
    tab.setAttribute("aria-controls", panel.id);
    panel.setAttribute("role", "tabpanel");
    panel.setAttribute("aria-labelledby", tab.id);
    tab.onkeydown = (event) => {
      let index;
      if (event.key === "ArrowRight") index = (i + 1) % tabs.length;
      else if (event.key === "ArrowLeft")
        index = (i + tabs.length - 1) % tabs.length;
      else if (event.key === "Home") index = 0;
      else if (event.key === "End") index = tabs.length - 1;
      else return;
      event.preventDefault();
      tabs[index].focus();
      tabs[index].click();
    };
  }
  function select() {
    const requested =
      location.hash ||
      (new URLSearchParams(location.search).has("questionId")
        ? "#questions"
        : "#" + initial);
    const current =
      tabs.find((t) => t.getAttribute("href") === requested) || tabs[0];
    for (const tab of tabs) {
      const selected = tab === current;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      document.querySelector(tab.getAttribute("href")).hidden = !selected;
    }
    const practice = document.querySelector("#practice");
    if (practice)
      practice.hidden = !["#quiz", "#questions"].includes(
        current.getAttribute("href"),
      );
  }
  window.onhashchange = select;
  for (const tab of tabs)
    tab.onclick = () => {
      history.pushState(null, "", tab.getAttribute("href"));
      select();
    };
  select();
}
