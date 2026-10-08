CREATE INDEX ix_enrollment_course_status ON enrollments(course_id, status, enrollment_id);
CREATE INDEX ix_question_course_student ON lesson_questions(lesson_id, student_id, is_hidden);
