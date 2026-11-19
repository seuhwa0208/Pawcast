package com.project.pawcast.member.controller;

import com.project.pawcast.auth.dto.ChangePasswordRequest;
import com.project.pawcast.member.dto.MemberRequest;
import com.project.pawcast.member.dto.MemberResponse;
import com.project.pawcast.member.dto.PasswordRequest;
import com.project.pawcast.member.service.MemberService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;


    // 기본 정보 조회
    @GetMapping("/me")
    public MemberResponse getMyInfo(@AuthenticationPrincipal LoginMember member){
        return memberService.findByMyInfo(member.getId());
    }
    
    // 기본 정보 수정 창에 들어가기 전에 비밀번호 확인
    @PostMapping("/me/verify-password")
    public boolean checkPasswordViewMyInfo(@AuthenticationPrincipal LoginMember member, @RequestBody PasswordRequest request) {
        return memberService.checkPasswordViewMyInfo(member.getId(), request);
    }

    // 기본 정보 수정
    @PatchMapping("/me")
    public MemberResponse updateMyInfo(@AuthenticationPrincipal LoginMember member, @RequestBody MemberRequest request){
        return memberService.updateMyInfo(member.getId(), request);
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public void changePassword(@AuthenticationPrincipal LoginMember member, @RequestBody ChangePasswordRequest request){
        memberService.changePassword(member.getId(), request);
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public void deleteAccount(@AuthenticationPrincipal LoginMember member){
        memberService.deletedMyInfo(member.getId());
    }
}
