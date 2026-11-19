package com.project.powcast.notice.controller;

import com.project.powcast.notice.dto.NoticeRequest;
import com.project.powcast.notice.dto.NoticeResponse;
import com.project.powcast.notice.entity.NoticeSearchType;
import com.project.powcast.notice.service.NoticeService;
import com.project.powcast.security.LoginMember;
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
    public NoticeResponse createNotice(@AuthenticationPrincipal LoginMember member, NoticeRequest request){
        return noticeService.createNotice(member.getId(),request);
    }
    
    // 수정
    @PatchMapping("{noticeId}")
    public NoticeResponse updateNotice(@AuthenticationPrincipal LoginMember member, @PathVariable Long noticeId, NoticeRequest request){
        return noticeService.updateNotice(noticeId,member.getId(),request);
    }
    
    // 삭제
    @DeleteMapping("{noticeId}")
    public void deletePets(@AuthenticationPrincipal LoginMember member, @PathVariable Long noticeId){
        noticeService.deleteNotice(noticeId, member.getId());
    }
}
