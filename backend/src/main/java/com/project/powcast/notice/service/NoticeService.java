package com.project.powcast.notice.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MembersRepository;
import com.project.powcast.notice.dto.NoticeRequest;
import com.project.powcast.notice.dto.NoticeResponse;
import com.project.powcast.notice.entity.Notice;
import com.project.powcast.notice.entity.NoticeSearchType;
import com.project.powcast.notice.entity.NoticeType;
import com.project.powcast.notice.repository.NoticeRepository;
import com.project.powcast.security.AuthorizationValidator;
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
    public Notice findById(Long id){
        return noticeRepository.findById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_NOTICE));
    }
    
    // 목록 조회 및 검색 조회
    public List<NoticeResponse> getListNotice(NoticeSearchType type, String keyword){
        NoticeType noticeType;
        if (type == NoticeSearchType.ALL) {noticeType = null;}
        else {noticeType = NoticeType.valueOf(type.name());}
        return noticeRepository.searchNotices(noticeType, keyword).stream().map(NoticeResponse::from).toList();
    }

    // 단건 조회
    // URL로 삭제처리된 글 접근 막기
    public NoticeResponse detailNotice(Long id){
        return NoticeResponse.from(noticeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_NOTICE)));
    }
    
    // 여기부터는 권한 필요 - USER만 아니면 됨
    // 등록
    @Transactional
    public NoticeResponse createNotice(Long memberId, NoticeRequest request){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        //권한 검사 - 매니저 및 어드민
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

    // 수정
    @Transactional
    public NoticeResponse updateNotice(Long noticeId, Long memberId, NoticeRequest request){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        // 권한 검사 - 매니저 및 어드민
        authorizationValidator.validateManager(member);

        Notice notice = findById(noticeId);

        notice.setMember(member);
        notice.setNoticeType(request.noticeType());
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setPinned(request.isPinned());

        return NoticeResponse.from(notice);
    }

    // 삭제
    @Transactional
    public void deleteNotice(Long noticeId, Long memberId){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        // 권한 검사 - 매니저 및 어드민
        authorizationValidator.validateManager(member);
        Notice notice = findById(noticeId);
        notice.setDeletedAt(LocalDateTime.now());
    }
}
