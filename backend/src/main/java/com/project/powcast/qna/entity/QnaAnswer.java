package com.project.powcast.qna.entity;

import com.project.powcast.common.entity.BaseTime;
import com.project.powcast.member.entity.Members;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "qna_answers")
public class QnaAnswer extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "qna_id",nullable = false, unique = true)
    private Qna qna;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Members member;

    @Column(columnDefinition = "TEXT",nullable = false)
    private String content;

}
