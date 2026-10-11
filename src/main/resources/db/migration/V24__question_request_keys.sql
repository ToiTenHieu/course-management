ALTER TABLE lesson_questions ADD COLUMN client_request_id VARCHAR(36);
ALTER TABLE lesson_questions ADD CONSTRAINT uq_question_student_request UNIQUE (student_id, client_request_id);
