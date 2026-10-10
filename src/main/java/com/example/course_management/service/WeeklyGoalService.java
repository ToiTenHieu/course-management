package com.example.course_management.service;

import com.example.course_management.exception.*;
import com.example.course_management.repository.UserRepository;
import com.example.course_management.security.CustomUserDetails;
import jakarta.validation.constraints.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class WeeklyGoalService {
  public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  public record Save(@NotNull LocalDate weekStart,@Min(0) int expectedRevision,
      @Min(1) @Max(50) int lessonTarget,boolean dashboardReminder) {}
  public record Week(LocalDate weekStart,LocalDate weekEnd,Integer lessonTarget,long completed) {}
  public record Goal(LocalDate weekStart,LocalDate weekEnd,int lessonTarget,boolean configured,
      boolean dashboardReminder,int revision,long completed,List<Week> history) {}
  private record Setting(int target,boolean reminder,int revision) {}
  private final JdbcTemplate jdbc;
  private final UserRepository users;
  public WeeklyGoalService(JdbcTemplate jdbc,UserRepository users) {this.jdbc=jdbc;this.users=users;}
  private LocalDate currentWeek() {
    return LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }
  @Transactional(readOnly=true)
  public Goal get(CustomUserDetails actor) {
    int id=actor.getUser().getUserId();LocalDate start=currentWeek();
    var settings=jdbc.query("SELECT week_start,lesson_target,dashboard_reminder,revision FROM weekly_learning_goals WHERE student_id=? AND week_start>=? AND week_start<=?",
        (r,n)->Map.entry(r.getDate(1).toLocalDate(),new Setting(r.getInt(2),r.getBoolean(3),r.getInt(4))),id,start.minusWeeks(7),start);
    Map<LocalDate,Setting> byWeek=new HashMap<>();settings.forEach(row->byWeek.put(row.getKey(),row.getValue()));
    var dates=jdbc.query("SELECT CAST(completed_at AS DATE),COUNT(*) FROM learning_completions WHERE student_id=? AND completed_at>=? AND completed_at<? GROUP BY CAST(completed_at AS DATE)",
        (r,n)->Map.entry(r.getDate(1).toLocalDate(),r.getLong(2)),id,start.minusWeeks(7).atStartOfDay(),start.plusWeeks(1).atStartOfDay());
    Map<LocalDate,Long> counts=new HashMap<>();dates.forEach(row->counts.merge(row.getKey().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),row.getValue(),Long::sum));
    var history=new ArrayList<Week>();
    for(int i=0;i<8;i++) {var week=start.minusWeeks(i);var setting=byWeek.get(week);
      history.add(new Week(week,week.plusDays(6),setting==null?null:setting.target(),counts.getOrDefault(week,0L)));}
    var current=byWeek.get(start);
    return new Goal(start,start.plusDays(6),current==null?3:current.target(),current!=null,
        current==null||current.reminder(),current==null?0:current.revision(),counts.getOrDefault(start,0L),history);
  }
  public Goal save(Save request,CustomUserDetails actor) {
    if(request.weekStart()==null||!request.weekStart().equals(currentWeek()))
      throw new ConflictException("Tuần học đã đổi. Tải lại mục tiêu trước khi lưu.");
    if(request.lessonTarget()<1||request.lessonTarget()>50||request.expectedRevision()<0)
      throw new BadRequestException("Mục tiêu từ 1 đến 50 bài mỗi tuần");
    int id=actor.getUser().getUserId();
    users.findLockedById(id).orElseThrow(()->new ForbiddenException("Tài khoản không tồn tại"));
    var revisions=jdbc.queryForList("SELECT revision FROM weekly_learning_goals WHERE student_id=? AND week_start=?",Integer.class,id,request.weekStart());
    int revision=revisions.isEmpty()?0:revisions.getFirst();
    if(revision!=request.expectedRevision()) throw new ConflictException("Mục tiêu đã thay đổi ở một phiên khác. Tải lại để xem bản mới.");
    if(revision==0) jdbc.update("INSERT INTO weekly_learning_goals(student_id,week_start,lesson_target,dashboard_reminder,revision) VALUES(?,?,?,?,1)",id,request.weekStart(),request.lessonTarget(),request.dashboardReminder());
    else jdbc.update("UPDATE weekly_learning_goals SET lesson_target=?,dashboard_reminder=?,revision=revision+1 WHERE student_id=? AND week_start=?",request.lessonTarget(),request.dashboardReminder(),id,request.weekStart());
    return get(actor);
  }
  // Called while the course/enrollment locks are held. Retained when course content changes.
  public void recordCompletion(int studentId,int lessonId) {
    jdbc.update("INSERT INTO learning_completions(student_id,lesson_id,completed_at) SELECT ?,?,? WHERE NOT EXISTS (SELECT 1 FROM learning_completions WHERE student_id=? AND lesson_id=?)",
        studentId,lessonId,LocalDateTime.now(ZONE),studentId,lessonId);
  }
}
