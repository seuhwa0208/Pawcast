package com.project.pawcast.qna.repository;

import com.project.pawcast.notice.entity.Notice;
import com.project.pawcast.notice.entity.NoticeType;
import com.project.pawcast.qna.entity.Qna;
import com.project.pawcast.qna.entity.QnaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QnaRepository extends JpaRepository<Qna, Long> {
    //List<Qna> findAllByIsPrivateFalseOrderByCreatedAtDesc();
    List<Qna> findAllByMember_IdOrderByCreatedAtDesc(Long memberId);
    Optional<Qna> findByIdAndMember_Id(Long qnaId, Long memberId);
    Optional<Qna> findByIdAndDeletedAtIsNull(Long qnaId);
    List<Qna> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
    @Query("""
    SELECT q FROM Qnas q WHERE q.deletedAt IS NULL
      AND (:type IS NULL OR q.qnaType = :type)
      AND (
          :keyword IS NULL
          OR LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ) ORDER BY q.createdAt DESC
    """)
    List<Qna> searchQnas(@Param("type") QnaType type, @Param("keyword") String keyword);
}
