package com.project.pawcast.member.admin.controller;

import com.project.pawcast.member.admin.dto.MemberRoleRequest;
import com.project.pawcast.member.admin.dto.MemberRoleResponse;
import com.project.pawcast.member.admin.dto.MemberStatusRequest;
import com.project.pawcast.member.admin.service.AdminService;
import com.project.pawcast.member.dto.MemberResponse;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private AdminService adminService;
    
    // 회원 전체목록
    @GetMapping
    public List<MemberResponse> getMemberList(@AuthenticationPrincipal LoginMember member){
        return adminService.getMemberList(member.getId());
    }

    // 회원 상태 변경
    @PatchMapping("/members/{userId}/status")
    public void changeMemberStatus(@AuthenticationPrincipal LoginMember member, @PathVariable Long userId, @RequestBody MemberStatusRequest request){
        adminService.changeMemberStatus(member.getId(),userId, request);
    }
    // 회원 역할 변경
    @PatchMapping("/members/{userId}/role")
    public void changeMemberRole(@AuthenticationPrincipal LoginMember member, @PathVariable Long userId, @RequestBody MemberRoleRequest request){
        adminService.changeMemberRole(member.getId(),userId, request);
    }
}
