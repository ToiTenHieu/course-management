package com.example.course_management;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class WeeklyGoalMigrationTests {
  @Test void backfillCountsEarliestCompletionOnceAndKeepsHistoryWithoutLessonForeignKey() throws Exception {
    try(var connection=DriverManager.getConnection("jdbc:h2:mem:legacy_goals_"+UUID.randomUUID()+";MODE=PostgreSQL","sa","")) {
      try(var s=connection.createStatement()) {
        s.execute("CREATE TABLE users(user_id INTEGER PRIMARY KEY)");s.execute("INSERT INTO users VALUES(1),(2)");
        s.execute("CREATE TABLE enrollments(enrollment_id INTEGER PRIMARY KEY,student_id INTEGER)");s.execute("INSERT INTO enrollments VALUES(1,1),(2,1),(3,2)");
        s.execute("CREATE TABLE lesson_progress(enrollment_id INTEGER,lesson_id INTEGER,is_completed BOOLEAN,completed_at TIMESTAMP)");
        s.execute("INSERT INTO lesson_progress VALUES(1,10,TRUE,TIMESTAMP '2026-10-05 12:00:00'),(2,10,TRUE,TIMESTAMP '2026-10-09 12:00:00'),(1,11,FALSE,NULL),(3,10,TRUE,TIMESTAMP '2026-10-09 12:00:00')");
      }
      ScriptUtils.executeSqlScript(connection,new ClassPathResource("db/migration/V19__weekly_goals_and_audit_history.sql"));
      try(var s=connection.createStatement();var r=s.executeQuery("SELECT student_id,lesson_id,completed_at FROM learning_completions ORDER BY student_id")) {
        assertTrue(r.next());assertEquals(1,r.getInt(1));assertEquals(10,r.getInt(2));assertEquals("2026-10-05T12:00",r.getTimestamp(3).toLocalDateTime().toString());
        assertTrue(r.next());assertEquals(2,r.getInt(1));assertFalse(r.next());
      }
      try(var s=connection.createStatement()) {
        s.execute("INSERT INTO audit_logs(occurred_at,actor_id,actor_username,actor_role,action,target_type,target_id,target_name,before_value,after_value) VALUES(CURRENT_TIMESTAMP,2,'admin_old','ADMIN','USER_ROLE_CHANGED','USER',1,'student_old','STUDENT','TEACHER')");
        s.execute("DELETE FROM users WHERE user_id=2");
        try(var r=s.executeQuery("SELECT actor_id,actor_username FROM audit_logs")) {assertTrue(r.next());assertNull(r.getObject(1));assertEquals("admin_old",r.getString(2));}
      }
    }
  }
}
