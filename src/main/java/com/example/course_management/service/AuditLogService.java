package com.example.course_management.service;

import com.example.course_management.time.ApplicationTime;
import com.example.course_management.dto.response.PageResponse;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.security.CustomUserDetails;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class AuditLogService {
  public static final Set<String> ACTIONS=Set.of("PAYMENT_CONFIRMED","PAYMENT_REJECTED","USER_ROLE_CHANGED","USER_STATUS_CHANGED","COURSE_STATUS_CHANGED");
  public record Row(long auditId,LocalDateTime occurredAt,Integer actorId,String actorUsername,
      String actorRole,String action,String targetType,int targetId,String targetName,String beforeValue,String afterValue) {}
  private final JdbcTemplate jdbc;
  public AuditLogService(JdbcTemplate jdbc) {this.jdbc=jdbc;}
  public void recordCurrent(String action,String type,int id,String name,String before,String after) {
    var auth=SecurityContextHolder.getContext().getAuthentication();
    var actor=auth!=null&&auth.getPrincipal() instanceof CustomUserDetails details?details:null;
    record(actor,action,type,id,name,before,after);
  }
  public void record(CustomUserDetails actor,String action,String type,int id,String name,String before,String after) {
    if(Objects.equals(before,after)) return;
    jdbc.update("INSERT INTO audit_logs(occurred_at,actor_id,actor_username,actor_role,action,target_type,target_id,target_name,before_value,after_value) VALUES(?,?,?,?,?,?,?,?,?,?)",
        ApplicationTime.now(),actor==null?null:actor.getUser().getUserId(),
        actor==null?"system":actor.getUsername(),actor==null?"SYSTEM":actor.getUser().getRole().name(),action,type,id,name,before,after);
  }
  @Transactional(readOnly=true)
  public PageResponse<Row> list(LocalDate from,LocalDate to,String action,String search,int page,int size) {
    if(from==null||to==null||from.isAfter(to)||ChronoUnit.DAYS.between(from,to)>365||to.getYear()>9998)
      throw new BadRequestException("Chọn khoảng thời gian hợp lệ, tối đa 366 ngày");
    if(page<0||page>=1000000||size<1||size>100||search.length()>100||!action.isEmpty()&&!ACTIONS.contains(action))
      throw new BadRequestException("Bộ lọc lịch sử không hợp lệ");
    var params=new HashMap<String,Object>();params.put("from",from.atStartOfDay());params.put("until",to.plusDays(1).atStartOfDay());
    params.put("action",action);params.put("term","%"+search.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");
    String where=" FROM audit_logs WHERE occurred_at>=:from AND occurred_at<:until AND (:action='' OR action=:action) AND (LOWER(actor_username) LIKE :term ESCAPE '!' OR LOWER(target_name) LIKE :term ESCAPE '!')";
    var named=new NamedParameterJdbcTemplate(jdbc);long total=named.queryForObject("SELECT COUNT(*)"+where,params,Long.class);
    params.put("size",size);params.put("offset",(long)page*size);
    var rows=named.query("SELECT *"+where+" ORDER BY occurred_at DESC,audit_id DESC LIMIT :size OFFSET :offset",params,
        (r,n)->new Row(r.getLong("audit_id"),r.getTimestamp("occurred_at").toLocalDateTime(),(Integer)r.getObject("actor_id"),r.getString("actor_username"),r.getString("actor_role"),r.getString("action"),r.getString("target_type"),r.getInt("target_id"),r.getString("target_name"),r.getString("before_value"),r.getString("after_value")));
    return new PageResponse<>(rows,page,size,total,(int)((total+size-1)/size));
  }
}
