package com.project.powcast.member.admin.controller;

import com.project.powcast.member.admin.dto.MemberRoleRequest;
import com.project.powcast.member.admin.dto.MemberRoleResponse;
import com.project.powcast.member.admin.dto.MemberStatusRequest;
import com.project.powcast.member.admin.service.AdminService;
import com.project.powcast.member.dto.MemberResponse;
import com.project.powcast.member.entity.Members;
import com.project.powcast.security.LoginMember;
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
        adminService.changeMemberStatus(member.getId(),userId, request.status());
    }
    // 회원 역할 변경
    @PatchMapping("/members/{userId}/role")
    public void changeMemberRole(@AuthenticationPrincipal LoginMember member, @PathVariable Long userId, @RequestBody MemberRoleRequest request){
        adminService.changeMemberRole(member.getId(),userId,request.role());
    }
}
