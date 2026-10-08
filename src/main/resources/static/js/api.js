let csrf = null;
let configRequest = null;
export function appConfig(refresh = false) {
  if (refresh) configRequest = null;
  if (!configRequest) configRequest = api("/auth/config").catch(error => {
    configRequest = null;
    throw error;
  });
  return configRequest;
}
export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}
async function readResponse(response) {
  let data;
  try { data = await response.json(); }
  catch {
    throw new ApiError(
      response.status === 413 ? "Tài liệu vượt quá giới hạn dung lượng cho phép."
        : response.status === 401 ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        : "Máy chủ chưa trả được kết quả hợp lệ. Vui lòng thử lại.", response.status);
  }
  if (!data || typeof data !== "object" || typeof data.success !== "boolean")
    throw new ApiError("Máy chủ chưa trả được kết quả hợp lệ. Vui lòng thử lại.", response.status);
  return data;
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
        const result = await readResponse(r);
        const token = result.data;
        if (!result.success || !token || typeof token.headerName !== "string" || !token.headerName
            || typeof token.token !== "string" || !token.token)
          throw new ApiError("Không thể tạo phiên bảo vệ. Vui lòng tải lại trang.", r.status);
        csrf = token;
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
    if (response.status === 401 || response.status === 403) csrf = null;
    const data = await readResponse(response);
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
