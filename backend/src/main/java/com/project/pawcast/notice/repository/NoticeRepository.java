package com.project.pawcast.notice.repository;

import com.project.pawcast.notice.entity.Notice;
import com.project.pawcast.notice.entity.NoticeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 삭제되지 않은 단건 공지사항 조회
    Optional<Notice> findByIdAndDeletedAtIsNull(Long id);

    // 삭제되지 않은 전체 공지사항을 상단 고정(isPinned) 및 최신순으로 조회
    List<Notice> findByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc();

    // 삭제되지 않은 특정 공지 유형(NoticeType)의 공지사항을 상단 고정 및 최신순으로 조회
    List<Notice> findByDeletedAtIsNullAndNoticeTypeOrderByIsPinnedDescCreatedAtDesc(NoticeType noticeType);

    // 삭제되지 않은 전체 공지사항 중 제목 또는 내용에 키워드가 포함된 항목 검색
    @Query("""
    SELECT n FROM Notice n
    WHERE n.deletedAt IS NULL
      AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY n.isPinned DESC, n.createdAt DESC
    """)
    List<Notice> searchAllByKeyword(@Param("keyword") String keyword);

    // 삭제되지 않은 특정 공지 유형 내에서 제목 또는 내용 키워드로 검색
    @Query("""
    SELECT n FROM Notice n
    WHERE n.deletedAt IS NULL
      AND n.noticeType = :type
      AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY n.isPinned DESC, n.createdAt DESC
    """)
    List<Notice> searchByTypeAndKeyword(@Param("type") NoticeType type, @Param("keyword") String keyword);

    // 공지 유형(선택)과 키워드(선택) 조건을 한 번에 조합해 공지사항 검색
    @Query("""
    SELECT n FROM Notice n
    WHERE n.deletedAt IS NULL
      AND (:type IS NULL OR n.noticeType = :type)
      AND (
          :keyword IS NULL
          OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
    ORDER BY n.isPinned DESC, n.createdAt DESC
    """)
    List<Notice> searchNotices(@Param("type") NoticeType type, @Param("keyword") String keyword);
}