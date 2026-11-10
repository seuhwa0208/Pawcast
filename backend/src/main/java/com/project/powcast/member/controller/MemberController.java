package com.project.powcast.member.controller;

import com.project.powcast.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService service;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

}
