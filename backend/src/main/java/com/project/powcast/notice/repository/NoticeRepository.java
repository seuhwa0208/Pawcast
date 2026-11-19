package com.project.powcast.notice.repository;

import com.project.powcast.notice.entity.Notice;
import com.project.powcast.notice.entity.NoticeType;
import com.project.powcast.pets.entity.Pets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long>{
    // List<Notice> findAllByOrderByIsPinnedDescCreatedAtDesc();
    Optional<Notice> findByIdAndDeletedAtIsNull(Long id);
    @Query("""
    SELECT n FROM Notice n WHERE n.deletedAt IS NULL
      AND (:type IS NULL OR n.noticeType = :type)
      AND (
          :keyword IS NULL
          OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ) ORDER BY n.isPinned DESC, n.createdAt DESC
    """)
    List<Notice> searchNotices(@Param("type") NoticeType type, @Param("keyword") String keyword);
}
