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
public interface NoticeRepository extends JpaRepository<Notice, Long>{
    Optional<Notice> findByIdAndDeletedAtIsNull(Long id);

    List<Notice> findByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc();

    List<Notice> findByDeletedAtIsNullAndNoticeTypeOrderByIsPinnedDescCreatedAtDesc(NoticeType noticeType);

    @Query("""
    SELECT n FROM Notice n
    WHERE n.deletedAt IS NULL
      AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY n.isPinned DESC, n.createdAt DESC
    """)
    List<Notice> searchAllByKeyword(@Param("keyword") String keyword);

    @Query("""
    SELECT n FROM Notice n
    WHERE n.deletedAt IS NULL
      AND n.noticeType = :type
      AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY n.isPinned DESC, n.createdAt DESC
    """)
    List<Notice> searchByTypeAndKeyword(@Param("type") NoticeType type, @Param("keyword") String keyword);
}
