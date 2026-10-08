CREATE INDEX ix_user_role_active ON users(role, is_active, user_id);
CREATE INDEX ix_user_name_page ON users(full_name, user_id);
CREATE INDEX ix_payment_status_page ON payments(status, created_at, payment_id);
CREATE INDEX ix_payment_owner_page ON payments(student_id, status, created_at, payment_id);
CREATE INDEX ix_notification_owner_read_page ON notifications(user_id, is_read, created_at, notification_id);
CREATE INDEX ix_enrollment_owner_status_page ON enrollments(student_id, status, enrollment_id);
CREATE INDEX ix_review_course_page ON reviews(course_id, created_at, review_id);
