package com.project.pawcast.qna.controller;

import com.project.pawcast.notice.entity.NoticeSearchType;
import com.project.pawcast.qna.dto.QnaRequest;
import com.project.pawcast.qna.dto.QnaResponse;
import com.project.pawcast.qna.entity.QnaSearchType;
import com.project.pawcast.qna.service.QnaService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/qna")
public class QnaController {

    private QnaService qnaService;

    // QNA-01 공개 문의 목록 조회
    @GetMapping
    public List<QnaResponse> getQnas(@RequestParam(defaultValue = "ALL") QnaSearchType type, @RequestParam(required = false) String keyword) {
        return qnaService.getListQna(type,keyword);
    }

    // QNA-02 공개 문의 상세 조회
    @GetMapping("/{qnaId}")
    public QnaResponse detailQna(@PathVariable Long qnaId) {
        return qnaService.detailQna(qnaId);
    }

    // QNA-03 문의 등록
    @PostMapping
    public QnaResponse createQna(@AuthenticationPrincipal LoginMember member, @RequestBody QnaRequest request) {
        return qnaService.createQna(member.getId(), request);
    }

    // QNA-04 내 문의 목록 조회
    @GetMapping("/me")
    public List<QnaResponse> getMyQnas(@AuthenticationPrincipal LoginMember member) {
        return qnaService.myQnaList(member.getId());
    }

    // QNA-05 내 문의 상세 조회
    @GetMapping("/me/{qnaId}")
    public QnaResponse detailMyQna(@AuthenticationPrincipal LoginMember member, @PathVariable Long qnaId) {
        return qnaService.myQna(qnaId,member.getId());
    }

    // QNA-06 문의 수정
    @PutMapping("/{qnaId}")
    public QnaResponse updateQna(@AuthenticationPrincipal LoginMember member, @PathVariable Long qnaId, @RequestBody QnaRequest request) {
        return qnaService.updateQna(qnaId, member.getId(), request);
    }

    // QNA-07 문의 삭제
    @DeleteMapping("/{qnaId}")
    public void deleteQna(@AuthenticationPrincipal LoginMember member, @PathVariable Long qnaId) {
        qnaService.deleteQna(qnaId, member.getId());
    }

    // QNA-08 관리자 전체 문의 목록 조회
    // 매니저·관리자 전체 문의 목록
    @GetMapping("/manage")
    public List<QnaResponse> getListQnaAdminAndManager(@AuthenticationPrincipal LoginMember member) {
        return qnaService.getListQnaAdminAndManager(member.getId());
    }

    // QNA-09 관리자 문의 상세 조회
    @GetMapping("/manage/{qnaId}")
    public QnaResponse detailQnaAdminAndManager(@AuthenticationPrincipal LoginMember member, @PathVariable Long qnaId) {
        return qnaService.detailQnaAdminAndManager(qnaId,member.getId());
    }


}
