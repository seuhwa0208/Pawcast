package com.project.pawcast.qna.repository;

import com.project.pawcast.qna.entity.Qna;
import com.project.pawcast.qna.entity.QnaStatus;
import com.project.pawcast.qna.entity.QnaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QnaRepository extends JpaRepository<Qna, Long> {

    List<Qna> findAllByMember_IdOrderByCreatedAtDesc(Long memberId);
    Optional<Qna> findByIdAndMember_Id(Long qnaId, Long memberId);
    Optional<Qna> findByIdAndDeletedAtIsNull(Long qnaId);

    @Query("""
        SELECT q
        FROM Qna q
        WHERE q.deletedAt IS NULL
          AND q.status = :status
        ORDER BY q.createdAt DESC
        """)
    List<Qna> findAllWaitingForAdmin(@Param("status") QnaStatus status);

    @Query("""
        SELECT q
        FROM Qna q
        WHERE q.deletedAt IS NULL
          AND (:type IS NULL OR q.type = :type)
          AND (
              :keyword IS NULL
              OR :keyword = ''
              OR q.title LIKE CONCAT('%', :keyword, '%')
              OR q.content LIKE CONCAT('%', :keyword, '%')
          )
        ORDER BY q.createdAt DESC
        """)
    List<Qna> searchQnas(
            @Param("type") QnaType type,
            @Param("keyword") String keyword
    );
}