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

    logout: () => apiCall('/auth/logout', { method: 'POST' }),

    // --- User management (Phase 2) ---
    getUsers: (role, status) => {
        const params = new URLSearchParams();
        if (role) params.set('role', role);
        if (status) params.set('status', status);
        const qs = params.toString();
        return apiCall('/users' + (qs ? '?' + qs : ''));
    },

    createUser: (data) => apiCall('/users', { method: 'POST', body: data }),

    updateUserRole: (userId, role) =>
        apiCall(`/users/${userId}/role`, { method: 'PUT', body: { role } }),

    updateUserStatus: (userId, isActive) =>
        apiCall(`/users/${userId}/status`, { method: 'PUT', body: { isActive } }),

    deleteUser: (userId) => apiCall(`/users/${userId}`, { method: 'DELETE' }),

    updateUserProfile: (userId, fullName, email) =>
        apiCall(`/users/${userId}`, { method: 'PUT', body: { fullName, email } }),

    changePassword: (userId, oldPassword, newPassword) =>
        apiCall(`/users/${userId}/password`, { method: 'PUT', body: { oldPassword, newPassword } }),

    // --- Course management (Phase 3) ---
    getCourses: (search, teacherId, status) => {
        const params = new URLSearchParams();
        if (search) params.set('search', search);
        if (teacherId) params.set('teacherId', teacherId);
        if (status) params.set('status', status);
        const qs = params.toString();
        return apiCall('/courses' + (qs ? '?' + qs : ''));
    },

    getCourseById: (courseId) => apiCall(`/courses/${courseId}`),

    createCourse: (data) => apiCall('/courses', { method: 'POST', body: data }),

    updateCourse: (courseId, data) =>
        apiCall(`/courses/${courseId}`, { method: 'PUT', body: data }),

    updateCourseStatus: (courseId, status) =>
        apiCall(`/courses/${courseId}/status`, { method: 'PUT', body: { status } }),

    deleteCourse: (courseId) => apiCall(`/courses/${courseId}`, { method: 'DELETE' }),

    // --- Lesson management (Phase 4) ---
    getLessonsByCourse: (courseId) => apiCall(`/courses/${courseId}/lessons`),

    getLessonById: (lessonId) => apiCall(`/lessons/${lessonId}`),

    createLesson: (courseId, data) =>
        apiCall(`/courses/${courseId}/lessons`, { method: 'POST', body: data }),

    updateLesson: (lessonId, data) =>
        apiCall(`/lessons/${lessonId}`, { method: 'PUT', body: data }),

    updateLessonPublish: (lessonId, isPublished) =>
        apiCall(`/lessons/${lessonId}/publish`, { method: 'PUT', body: { isPublished } }),

    deleteLesson: (lessonId) => apiCall(`/lessons/${lessonId}`, { method: 'DELETE' }),

    // --- Enrollment (Phase 5) ---
    getMyEnrollments: () => apiCall('/enrollments'),

    enroll: (courseId) => apiCall('/enrollments', { method: 'POST', body: { courseId } }),

    getEnrollmentDetail: (enrollmentId) => apiCall(`/enrollments/${enrollmentId}`),

    completeLesson: (enrollmentId, lessonId) =>
        apiCall(`/enrollments/${enrollmentId}/complete_lesson/${lessonId}`, { method: 'PUT' }),

    // --- Notifications (Phase 6) ---
    getMyNotifications: () => apiCall('/notifications'),

    markNotificationRead: (notificationId) =>
        apiCall(`/notifications/${notificationId}/read`, { method: 'PUT' }),

    createNotification: (data) => apiCall('/notifications', { method: 'POST', body: data }),

    deleteNotification: (notificationId) =>
        apiCall(`/notifications/${notificationId}`, { method: 'DELETE' }),

    // --- Reviews (Phase 6) ---
    getReviews: (courseId) => apiCall(`/courses/${courseId}/reviews`),

    createReview: (courseId, rating, comment) =>
        apiCall(`/courses/${courseId}/reviews`, { method: 'POST', body: { rating, comment } }),

    updateReview: (reviewId, rating, comment) =>
        apiCall(`/reviews/${reviewId}`, { method: 'PUT', body: { rating, comment } }),

    deleteReview: (reviewId) => apiCall(`/reviews/${reviewId}`, { method: 'DELETE' }),

    // --- Reports (Phase 6, ADMIN) ---
    getTopCourses: (limit) => apiCall('/reports/top_courses?limit=' + (limit || 5)),

    getStudentProgress: (studentId) => apiCall(`/reports/student_progress/${studentId}`),

    getTeacherOverview: (teacherId) => apiCall(`/reports/teacher_courses_overview/${teacherId}`),

    // --- Content preview (Phase 6) ---
    getLessonPreview: (lessonId) => apiCall(`/lessons/${lessonId}/content_preview`),

    // --- Payments (chuyển khoản thủ công) ---
    getBankInfo: () => apiCall('/payments/bank-info'),

    createPayment: (courseId) => apiCall('/payments', { method: 'POST', body: { courseId } }),

    getMyPayments: () => apiCall('/payments/my'),

    getAllPayments: (status) => apiCall('/payments' + (status ? '?status=' + status : '')),

    confirmPayment: (paymentId) => apiCall(`/payments/${paymentId}/confirm`, { method: 'PUT' }),

    rejectPayment: (paymentId) => apiCall(`/payments/${paymentId}/reject`, { method: 'PUT' }),
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
    const profile = result.body.data;
    setupSidebarExtras(profile);
    return profile;
}

/**
 * Phần sidebar dùng chung cho mọi trang: hiện/ẩn mục theo role,
 * và hiển thị số thông báo chưa đọc. Được requireAuth() gọi tự động.
 */
async function setupSidebarExtras(profile) {
    const users = document.getElementById('usersNavItem');
    if (users && profile.role === 'ADMIN') users.style.display = 'flex';

    const mine = document.getElementById('myCoursesNavItem');
    if (mine && profile.role === 'STUDENT') mine.style.display = 'flex';

    const reports = document.getElementById('reportsNavItem');
    if (reports && profile.role === 'ADMIN') reports.style.display = 'flex';

    const payments = document.getElementById('paymentsNavItem');
    if (payments && profile.role === 'ADMIN') payments.style.display = 'flex';

    const myPayments = document.getElementById('myPaymentsNavItem');
    if (myPayments && profile.role === 'STUDENT') myPayments.style.display = 'flex';

    const badge = document.getElementById('notifBadge');
    if (badge) {
        const res = await Api.getMyNotifications();
        if (res.ok) {
            const unread = (res.body.data || []).filter(n => !n.isRead).length;
            if (unread > 0) {
                badge.textContent = unread > 99 ? '99+' : unread;
                badge.style.display = 'inline-block';
            }
        }
    }
}

function roleLabel(role) {
    switch (role) {
        case 'ADMIN': return 'Quản trị viên';
        case 'TEACHER': return 'Giảng viên';
        case 'STUDENT': return 'Học viên';
        default: return role;
    }
}

function courseStatusLabel(status) {
    switch (status) {
        case 'DRAFT': return 'Bản nháp';
        case 'PUBLISHED': return 'Đã xuất bản';
        case 'ARCHIVED': return 'Đã lưu trữ';
        default: return status;
    }
}

function formatMoney(value) {
    if (value === null || value === undefined) return '—';
    return Number(value).toLocaleString('vi-VN') + ' ₫';
}