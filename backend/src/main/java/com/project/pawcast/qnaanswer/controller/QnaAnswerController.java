package com.project.pawcast.qnaanswer.controller;

import com.project.pawcast.qnaanswer.dto.QnaAnswerRequest;
import com.project.pawcast.qnaanswer.dto.QnaAnswerResponse;
import com.project.pawcast.qnaanswer.service.QnaAnswerService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries/{qnaId}")
public class QnaAnswerController {
    private final QnaAnswerService qnaAnswerService;

    // 답변 하기
    @PostMapping
    public QnaAnswerResponse createAnswer(@AuthenticationPrincipal LoginMember member,
                                          @PathVariable Long qnaId, @RequestBody QnaAnswerRequest request) {
        return qnaAnswerService.answerQna(qnaId, member.getId(), request);
    }

    //답변 수정
    @PutMapping
    public QnaAnswerResponse updateAnswer(@AuthenticationPrincipal LoginMember member,
                                          @PathVariable Long qnaId, @RequestBody QnaAnswerRequest request) {
        return qnaAnswerService.updateAnswer(qnaId, member.getId(), member.getRole(), request);
    }

    //답변 삭제
    @DeleteMapping
    public void deleteAnswer(@AuthenticationPrincipal LoginMember member, @PathVariable Long qnaId) {
        qnaAnswerService.deleteAnswer(qnaId, member.getId());
    }
}
