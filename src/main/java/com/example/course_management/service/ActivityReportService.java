package com.example.course_management.service;

import com.example.course_management.dto.response.PageResponse;
import com.example.course_management.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class ActivityReportService {
  public record Row(int courseId,String title,long enrollments,long completed,BigDecimal averageProgress,
      long confirmedPayments,BigDecimal revenue) {}
  public record Summary(long enrollments,long completed,long confirmedPayments,BigDecimal revenue) {}
  public record Report(LocalDate from,LocalDate to,Summary summary,PageResponse<Row> courses) {}
  private final NamedParameterJdbcTemplate jdbc;
  public ActivityReportService(JdbcTemplate jdbc) { this.jdbc=new NamedParameterJdbcTemplate(jdbc); }
  private Map<String,Object> range(LocalDate from,LocalDate to) {
    if (from==null || to==null || from.isAfter(to) || ChronoUnit.DAYS.between(from,to)>365 || to.getYear()>9998)
      throw new BadRequestException("Chọn khoảng thời gian hợp lệ, tối đa 366 ngày");
    return new HashMap<>(Map.of("from",from.atStartOfDay(),"until",to.plusDays(1).atStartOfDay()));
  }
  private String query() { return """
      SELECT c.course_id,c.title,COALESCE(e.enrollments,0) AS enrollments,COALESCE(e.completed,0) AS completed,
        COALESCE(e.average_progress,0) AS average_progress,COALESCE(p.confirmed_payments,0) AS confirmed_payments,
        COALESCE(p.revenue,0) AS revenue
      FROM courses c LEFT JOIN (
        SELECT course_id,COUNT(*) AS enrollments,
          SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed,
          AVG(progress_percentage) AS average_progress
        FROM enrollments WHERE enrollment_date>=:from AND enrollment_date<:until GROUP BY course_id
      ) e ON e.course_id=c.course_id LEFT JOIN (
        SELECT course_id,COUNT(*) AS confirmed_payments,SUM(amount) AS revenue
        FROM payments WHERE status='CONFIRMED' AND confirmed_at>=:from AND confirmed_at<:until GROUP BY course_id
      ) p ON p.course_id=c.course_id WHERE e.course_id IS NOT NULL OR p.course_id IS NOT NULL
      """; }
  private Row map(java.sql.ResultSet r,int n) throws java.sql.SQLException {
    return new Row(r.getInt(1),r.getString(2),r.getLong(3),r.getLong(4),r.getBigDecimal(5),r.getLong(6),r.getBigDecimal(7));
  }
  public Report get(LocalDate from,LocalDate to,int page,int size) {
    var params=range(from,to);
    if (page<0 || page>=1000000 || size<1 || size>100) throw new BadRequestException("Trang không hợp lệ");
    long total=jdbc.queryForObject("SELECT COUNT(*) FROM ("+query()+") report_rows",params,Long.class);
    var summary=jdbc.queryForObject("SELECT COALESCE(SUM(enrollments),0),COALESCE(SUM(completed),0),COALESCE(SUM(confirmed_payments),0),COALESCE(SUM(revenue),0) FROM ("+query()+") report_rows",
        params,(r,n)->new Summary(r.getLong(1),r.getLong(2),r.getLong(3),r.getBigDecimal(4)));
    params.put("size",size);params.put("offset",(long)page*size);
    var rows=jdbc.query(query()+" ORDER BY revenue DESC,c.course_id LIMIT :size OFFSET :offset",params,this::map);
    return new Report(from,to,summary,new PageResponse<>(rows,page,size,total,(int)((total+size-1)/size)));
  }
  public byte[] export(LocalDate from,LocalDate to) {
    var params=range(from,to);
    long total=jdbc.queryForObject("SELECT COUNT(*) FROM ("+query()+") report_rows",params,Long.class);
    if (total>5000) throw new BadRequestException("Báo cáo vượt 5.000 khóa. Thu hẹp khoảng thời gian để xuất đầy đủ.");
    var rows=jdbc.query(query()+" ORDER BY revenue DESC,c.course_id",params,this::map);
    var csv=new StringBuilder("\uFEFFMã khóa,Tên khóa,Từ ngày,Đến ngày,Lượt đăng ký,Đã hoàn thành hiện tại,Tiến độ trung bình hiện tại (%),Thanh toán xác nhận,Doanh thu xác nhận (VND)\r\n");
    for (var r:rows) csv.append(r.courseId()).append(',').append(csvCell(r.title())).append(',').append(from).append(',').append(to)
        .append(',').append(r.enrollments()).append(',').append(r.completed()).append(',').append(r.averageProgress().setScale(2,java.math.RoundingMode.HALF_UP))
        .append(',').append(r.confirmedPayments()).append(',').append(r.revenue()).append("\r\n");
    return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }
  public static String csvCell(String value) {
    String trimmed=value.stripLeading();
    if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0))>=0 || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) value="'"+value;
    return "\""+value.replace("\"","\"\"")+"\"";
  }
}
