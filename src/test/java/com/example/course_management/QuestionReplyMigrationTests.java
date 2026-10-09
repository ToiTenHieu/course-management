package com.example.course_management;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class QuestionReplyMigrationTests {
  @Test
  void migrationPreservesLegacyAnswersIncludingHiddenQuestionsWithoutChangingThem() throws Exception {
    try (var connection = DriverManager.getConnection("jdbc:h2:mem:legacy_replies_" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "")) {
      try (var statement = connection.createStatement()) {
        statement.execute("CREATE TABLE users (user_id INTEGER PRIMARY KEY, role VARCHAR(20))");
        statement.execute("CREATE TABLE lesson_questions (question_id INTEGER PRIMARY KEY, answered_by INTEGER, answer TEXT, answered_at TIMESTAMP, created_at TIMESTAMP, is_hidden BOOLEAN)");
        statement.execute("INSERT INTO users VALUES (1,'TEACHER'),(2,'ADMIN')");
        statement.execute("INSERT INTO lesson_questions VALUES (1,1,'Original teacher answer',TIMESTAMP '2026-01-02 03:04:05',TIMESTAMP '2026-01-01 00:00:00',TRUE),(2,2,'Admin answer',NULL,TIMESTAMP '2026-01-01 00:00:00',FALSE),(3,NULL,NULL,NULL,TIMESTAMP '2026-01-01 00:00:00',FALSE)");
      }
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V15__question_replies.sql"));
      try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT question_id,author_role,body,created_at,client_request_id FROM question_replies ORDER BY question_id")) {
        assertTrue(rows.next()); assertEquals(1, rows.getInt(1)); assertEquals("TEACHER", rows.getString(2));
        assertEquals("Original teacher answer", rows.getString(3));
        assertEquals("2026-01-02T03:04:05", rows.getTimestamp(4).toLocalDateTime().toString()); assertNull(rows.getString(5));
        assertTrue(rows.next()); assertEquals(2, rows.getInt(1)); assertEquals("ADMIN", rows.getString(2));
        assertEquals("2026-01-01T00:00", rows.getTimestamp(4).toLocalDateTime().toString()); assertFalse(rows.next());
      }
      try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT answer,is_hidden FROM lesson_questions WHERE question_id=1")) {
        assertTrue(rows.next()); assertEquals("Original teacher answer", rows.getString(1)); assertTrue(rows.getBoolean(2));
      }
    }
  }
}
