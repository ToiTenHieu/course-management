CREATE TABLE learning_assessments (
  student_id INTEGER PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
  version INTEGER NOT NULL,
  goal VARCHAR(20) NOT NULL,
  hours_per_week INTEGER NOT NULL CHECK (hours_per_week BETWEEN 1 AND 20),
  answers VARCHAR(100) NOT NULL,
  completed_at TIMESTAMP NOT NULL
);
