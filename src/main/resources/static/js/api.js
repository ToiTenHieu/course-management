/**
 * api.js — lớp gọi API dùng chung cho toàn bộ giao diện.
 * Mọi request đều gắn credentials: 'include' để trình duyệt gửi/nhận
 * cookie session (thay cho JWT).
 */

const API_BASE = '/api';

/**
 * Gọi API chung. Trả về { ok, status, body } thay vì ném lỗi,
 * để trang gọi tự quyết định xử lý (redirect, hiện thông báo...).
 */
async function apiCall(path, { method = 'GET', body = null } = {}) {
    const options = {
        method,
        credentials: 'include',
        headers: {}
    };
    if (body !== null) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let res, data;
    try {
        res = await fetch(API_BASE + path, options);
    } catch (networkErr) {
        return { ok: false, status: 0, body: { success: false, message: 'Không kết nối được tới server' } };
    }

    try {
        data = await res.json();
    } catch (parseErr) {
        data = { success: false, message: 'Phản hồi không hợp lệ từ server' };
    }

    return { ok: res.ok, status: res.status, body: data };
}

const Api = {
    login: (username, password) =>
        apiCall('/auth/login', { method: 'POST', body: { username, password } }),

    me: () => apiCall('/auth/me'),

    verify: () => apiCall('/auth/verify', { method: 'POST' }),
};

/**
 * Bảo vệ trang: nếu chưa đăng nhập, đá về login.html.
 * Gọi ở đầu mỗi trang cần đăng nhập (trừ login.html).
 * Trả về profile nếu hợp lệ.
 */
async function requireAuth() {
    const result = await Api.me();
    if (!result.ok) {
        window.location.href = '/login.html';
        return null;
    }
    return result.body.data;
}

function roleLabel(role) {
    switch (role) {
        case 'ADMIN': return 'Quản trị viên';
        case 'TEACHER': return 'Giảng viên';
        case 'STUDENT': return 'Học viên';
        default: return role;
    }
}