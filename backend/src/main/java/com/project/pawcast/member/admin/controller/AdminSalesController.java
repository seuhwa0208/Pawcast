package com.project.pawcast.member.admin.controller;

import com.project.pawcast.member.admin.dto.SalesRequest;
import com.project.pawcast.member.admin.dto.SalesResponse;
import com.project.pawcast.member.admin.service.AdminSalesService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sales")
public class AdminSalesController {

    private final AdminSalesService adminSalesService;

    //매출 통계
    @GetMapping
    public List<SalesResponse> salesStatics(@AuthenticationPrincipal LoginMember member, SalesRequest request){
        return adminSalesService.getSalesStatistics(member.getId(), request);
    }


}
