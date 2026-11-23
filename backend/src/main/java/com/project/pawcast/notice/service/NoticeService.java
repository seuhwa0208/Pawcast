package com.project.pawcast.notice.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.notice.dto.NoticeRequest;
import com.project.pawcast.notice.dto.NoticeResponse;
import com.project.pawcast.notice.entity.Notice;
import com.project.pawcast.notice.entity.NoticeSearchType;
import com.project.pawcast.notice.entity.NoticeType;
import com.project.pawcast.notice.repository.NoticeRepository;
import com.project.pawcast.security.AuthorizationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MembersRepository membersRepository;
    private final AuthorizationValidator authorizationValidator;

    // 단순 id 조회 - 범용
    public Notice findById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_NOTICE));
    }

    // 목록 조회 및 검색 조회 (유형 및 공백/키워드 조건 분기 처리)
    public List<NoticeResponse> getListNotice(NoticeSearchType type, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String trimmedKeyword = hasKeyword ? keyword.strip() : null;
        List<Notice> notices;

        // type이 null이거나 ALL인 경우 전체 공지 기준으로 조회
        if (type == null || type == NoticeSearchType.ALL) {
            notices = hasKeyword
                    ? noticeRepository.searchAllByKeyword(trimmedKeyword)
                    : noticeRepository.findByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc();
        } else {
            // NoticeSearchType과 동일한 이름의 NoticeType enum으로 매핑
            NoticeType noticeType = NoticeType.valueOf(type.name());
            notices = hasKeyword
                    ? noticeRepository.searchByTypeAndKeyword(noticeType, trimmedKeyword)
                    : noticeRepository.findByDeletedAtIsNullAndNoticeTypeOrderByIsPinnedDescCreatedAtDesc(noticeType);
        }

        return notices.stream().map(NoticeResponse::from).toList();
    }

    // 단건 조회 (소프트 딜리트된 게시글 접근 차단)
    public NoticeResponse detailNotice(Long id) {
        return NoticeResponse.from(
                noticeRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_NOTICE))
        );
    }

    // 공지사항 등록 (매니저/어드민 권한 필요)
    @Transactional
    public NoticeResponse createNotice(Long memberId, NoticeRequest request) {
        Members member = membersRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(member);

        Notice notice = Notice.builder()
                .member(member)
                .noticeType(request.noticeType())
                .title(request.title())
                .content(request.content())
                .isPinned(request.isPinned())
                .build();

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    // 공지사항 수정 (매니저/어드민 권한 필요)
    @Transactional
    public NoticeResponse updateNotice(Long noticeId, Long memberId, NoticeRequest request) {
        Members member = membersRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(member);

        Notice notice = findById(noticeId);

        notice.setMember(member);
        notice.setNoticeType(request.noticeType());
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setPinned(request.isPinned());

        return NoticeResponse.from(notice);
    }

    // 공지사항 소프트 딜리트 (매니저/어드민 권한 필요)
    @Transactional
    public void deleteNotice(Long noticeId, Long memberId) {
        Members member = membersRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(member);

        Notice notice = findById(noticeId);
        notice.setDeletedAt(LocalDateTime.now());
    }
}