package com.project.pawcast.notice.controller;

import com.project.pawcast.notice.dto.NoticeRequest;
import com.project.pawcast.notice.dto.NoticeResponse;
import com.project.pawcast.notice.entity.NoticeSearchType;
import com.project.pawcast.notice.service.NoticeService;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    // 목록 조회 및 검색 조회
    @GetMapping
    public List<NoticeResponse> getListNotice(@RequestParam(defaultValue = "ALL") NoticeSearchType type, @RequestParam(required = false) String keyword){
        return noticeService.getListNotice(type, keyword);
    }

    // 상세 정보 조회
    @GetMapping("{noticeId}")
    public NoticeResponse detailNotice(@PathVariable Long noticeId){
        return noticeService.detailNotice(noticeId);
    }

    // 토큰 필요
    // 등록
    @PostMapping
    public NoticeResponse createNotice(@AuthenticationPrincipal LoginMember member, @Valid @RequestBody NoticeRequest request){
        return noticeService.createNotice(member.getId(),request);
    }
    
    // 수정
    @PatchMapping("{noticeId}")
    public NoticeResponse updateNotice(@AuthenticationPrincipal LoginMember member, @PathVariable Long noticeId,@Valid @RequestBody NoticeRequest request){
        return noticeService.updateNotice(noticeId,member.getId(),request);
    }
    
    // 삭제
    @DeleteMapping("{noticeId}")
    public void deletePets(@AuthenticationPrincipal LoginMember member, @PathVariable Long noticeId){
        noticeService.deleteNotice(noticeId, member.getId());
    }
}
