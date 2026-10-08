let csrf = null;
export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}
export async function api(path, method = "GET", body) {
  try {
    const options = { method, credentials: "same-origin", headers: {} };
    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
      if (!csrf) {
        const r = await fetch("/api/auth/csrf", { credentials: "same-origin" });
        if (!r.ok)
          throw new ApiError(
            "Không thể tạo phiên bảo vệ. Vui lòng tải lại trang.",
            r.status,
          );
        csrf = (await r.json()).data;
      }
      options.headers[csrf.headerName] = csrf.token;
    }
    if (body !== undefined) {
      if (body instanceof FormData) options.body = body;
      else {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
      }
    }
    const response = await fetch("/api" + path, options);
    const data = await response.json();
    if (!response.ok || !data.success) {
      if (response.status === 401 || response.status === 403) csrf = null;
      throw new ApiError(
        data.message || "Yêu cầu chưa được xử lý. Vui lòng thử lại.",
        response.status,
      );
    }
    if (path === "/auth/login" || path === "/auth/logout") csrf = null;
    return data.data;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(
      "Không kết nối được tới máy chủ. Kiểm tra kết nối và thử lại.",
      0,
    );
  }
}
