package com.project.pawcast.notice.entity;

import com.project.pawcast.common.entity.BaseTime;
import com.project.pawcast.member.entity.Members;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notices")
public class Notice extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id",nullable = false)
    private Members member;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private NoticeType noticeType = NoticeType.GENERAL;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, name = "is_pinned")
    @Builder.Default
    private boolean isPinned = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
