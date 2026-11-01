# Chạy bản demo trên macOS

Làm một lần các bước 1–3, những lần sau chỉ cần bước 4.

## 1. Cài công cụ (một lần)

Cần [Homebrew](https://brew.sh). Mở **Terminal** rồi chạy:

```bash
brew install openjdk@21 postgresql@17 git
sudo ln -sfn "$(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-21.jdk
brew services start postgresql@17
```

Kiểm tra: `java -version` phải in ra phiên bản 21 trở lên.

## 2. Tạo database (một lần)

PostgreSQL cài bằng Homebrew dùng tên đăng nhập là tên tài khoản Mac và không có mật khẩu:

```bash
"$(brew --prefix)/opt/postgresql@17/bin/createdb" course_management_demo
```

Lưu cấu hình để các lần sau không phải đặt lại (thay `~/.zshrc` bằng `~/.bash_profile` nếu dùng bash):

```bash
echo "export DB_USERNAME=$(whoami)" >> ~/.zshrc
source ~/.zshrc
```

Nếu dùng PostgreSQL có tài khoản `postgres` và mật khẩu (ví dụ Postgres.app hoặc bản cài từ trang chủ), đặt `DB_USERNAME=postgres` và `DB_PASSWORD=<mật khẩu>` thay cho dòng trên.

## 3. Lấy mã nguồn (một lần)

```bash
git clone https://github.com/ToiTenHieu/course-management.git
cd course-management
```

## 4. Chạy

Trong thư mục dự án:

```bash
./scripts/start-demo.sh
```

Lần đầu Maven tải thư viện và build nên mất vài phút. Khi thấy `Demo ready` thì mở <http://127.0.0.1:8080>. Tài khoản `admin_demo`, `teacher_demo`, `student_demo`, cùng mật khẩu `Demo123!`. Dữ liệu mẫu được tạo ở lần chạy đầu và giữ lại cho các lần sau.

- AI luôn bật. Key Gemini để trong file `gemini-key.txt` ở thư mục dự án (dán key vào rồi lưu). Chưa có file thì lần chạy đầu script hỏi key (không hiện khi dán) và tự lưu vào file; file bị Git bỏ qua.
- Đã build rồi, chỉ chạy lại: thêm `--skip-build`.
- Đổi cổng: `--port 8081`. Máy chậm: `--timeout 180`.
- Dừng: `./scripts/stop-demo.sh`. Log ở `target/local-demo/server.log`.

Ví dụ chạy lại nhanh:

```bash
./scripts/start-demo.sh --skip-build
```

## Lỗi thường gặp

| Thông báo | Cách xử lý |
|---|---|
| `permission denied: ./scripts/start-demo.sh` | Chạy `bash ./scripts/start-demo.sh` hoặc `chmod +x scripts/*.sh mvnw` |
| `JDK 21 or newer is required` | Làm lại bước 1, mở Terminal mới rồi kiểm tra `java -version` |
| `Demo exited before becoming ready`, log có `Connection refused` | PostgreSQL chưa chạy: `brew services start postgresql@17` |
| Log có `database "course_management_demo" does not exist` | Làm lại bước 2 |
| Log có `role "postgres" does not exist` hoặc `password authentication failed` | Đặt đúng `DB_USERNAME` / `DB_PASSWORD` như bước 2 |
| `Port 8080 is already in use` | Đã có ứng dụng khác dùng cổng 8080: thêm `--port 8081` |
| Nút AI báo "Dịch vụ AI tạm thời không khả dụng" | Xem `target/local-demo/server.log`; HTTP 403 "free tier is not available" thì tạo key mới trong project mới tại [AI Studio](https://aistudio.google.com/apikey) |
