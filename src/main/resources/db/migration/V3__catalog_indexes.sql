CREATE INDEX IF NOT EXISTS ix_course_status_id ON courses(status, course_id);
CREATE INDEX IF NOT EXISTS ix_course_teacher_id ON courses(teacher_id, course_id);
CREATE INDEX IF NOT EXISTS ix_course_category_id ON courses(category, course_id);
CREATE INDEX IF NOT EXISTS ix_enrollment_course ON enrollments(course_id);
CREATE INDEX IF NOT EXISTS ix_review_course ON reviews(course_id);
