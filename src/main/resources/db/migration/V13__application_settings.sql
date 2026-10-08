CREATE TABLE application_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    revision INTEGER NOT NULL DEFAULT 0,
    max_file_bytes BIGINT NOT NULL CHECK (max_file_bytes > 0),
    max_resources_per_lesson INTEGER NOT NULL CHECK (max_resources_per_lesson > 0),
    max_quiz_questions INTEGER NOT NULL CHECK (max_quiz_questions > 0),
    default_pass_percentage INTEGER NOT NULL CHECK (default_pass_percentage BETWEEN 1 AND 100),
    default_category VARCHAR(100) NOT NULL,
    default_level VARCHAR(100) NOT NULL,
    bank_name VARCHAR(255) NOT NULL,
    bank_account VARCHAR(255) NOT NULL,
    bank_holder VARCHAR(255) NOT NULL
);

INSERT INTO application_settings
    (id, max_file_bytes, max_resources_per_lesson, max_quiz_questions,
     default_pass_percentage, default_category, default_level, bank_name, bank_account, bank_holder)
VALUES (1, 5242880, 20, 20, 70, 'Lập trình', 'Cơ bản', 'Ngân hàng demo', 'DEMO-000001', 'HOC VIEN DEMO');

CREATE TABLE demo_login_accounts (
    user_id INTEGER PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE
);
