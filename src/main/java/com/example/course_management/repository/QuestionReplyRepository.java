package com.example.course_management.repository;

import com.example.course_management.entity.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface QuestionReplyRepository extends JpaRepository<QuestionReply, Integer> {
  @EntityGraph(attributePaths = {"author"})
  @Query("SELECT r FROM QuestionReply r WHERE r.question.questionId = :questionId AND (:manager = true OR r.isHidden = false)")
  Page<QuestionReply> findVisible(Integer questionId, boolean manager, Pageable pageable);

  interface ReplyCount {
    Integer getQuestionId();
    long getTotal();
  }

  @Query("SELECT r.question.questionId AS questionId, COUNT(r) AS total FROM QuestionReply r WHERE r.question.questionId IN :ids AND (:manager = true OR r.isHidden = false) GROUP BY r.question.questionId")
  List<ReplyCount> countVisible(Collection<Integer> ids, boolean manager);

  @Query("SELECT COUNT(r) FROM QuestionReply r WHERE r.question.questionId = :id AND (:manager = true OR r.isHidden = false)")
  long countVisible(Integer id, boolean manager);

  @Query("SELECT r.question.lesson.course.courseId FROM QuestionReply r WHERE r.replyId = :id")
  Optional<Integer> findCourseId(Integer id);

  @EntityGraph(attributePaths = {"author"})
  Optional<QuestionReply> findFirstByQuestion_QuestionIdAndIsHiddenFalseAndAuthorRoleInOrderByReplyIdDesc(
      Integer questionId, Collection<Role> roles);

  Optional<QuestionReply> findByAuthor_UserIdAndClientRequestId(Integer authorId, String clientRequestId);
}
