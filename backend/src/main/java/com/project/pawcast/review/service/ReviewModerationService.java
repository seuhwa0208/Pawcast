package com.project.pawcast.review.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.review.client.ModerationClient;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    private final ModerationClient moderationClient;

    public void validateContent(String content) {
        // 파이썬에서 글의 통과·차단 결과 받아옴
        var result = moderationClient.check(content);

        // 차단된 글이면 예외를 발생시켜 저장 흐름을 중단
        if (!result.allowed()) {
            throw new BusinessException(
                    ErrorCode.PROFANITY_DETECTED
            );
        }
    }
}