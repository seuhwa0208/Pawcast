package com.project.powcast.qna.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import com.project.powcast.notice.dto.NoticeRequest;
import com.project.powcast.notice.dto.NoticeResponse;
import com.project.powcast.notice.entity.Notice;
import com.project.powcast.qna.dto.QnaRequest;
import com.project.powcast.qna.dto.QnaResponse;
import com.project.powcast.qna.entity.Qna;
import com.project.powcast.qna.entity.QnaSearchType;
import com.project.powcast.qna.entity.QnaType;
import com.project.powcast.qna.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final MemberRepository memberRepository;

    // 단순 조회
    public Qna findById(Long id){
        return qnaRepository.findById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_QNA));
    }
    
    //목록 조회
    // 비공개 여부는 프론트에서
    // 검색 가능
    public List<QnaResponse> getListQna(QnaSearchType type, String keyword){
        QnaType qnaType;
        if (type == QnaSearchType.ALL) {qnaType = null;}
        else {qnaType = QnaType.valueOf(type.name());}
        return qnaRepository.searchQnas(qnaType, keyword).stream().map(QnaResponse::from).toList();
    }
    
    // 조회
    // 다만 삭제 처리한 글(deleted_at)은 URL로 접근 못하게
    public QnaResponse detailQna(Long id){
        return QnaResponse.from(qnaRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_QNA)));
    }
    
    // 등록
    // 회원만 , 비회원은 안됨
    @Transactional
    public QnaResponse createQna(Long memberId, QnaRequest request){
        Members member =  memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        Qna qna = Qna.builder()
                .member(member)
                .product(request.)
                .title(request.title())
                .content(request.content())
                .isPrivate(request.isPrivate())
                .build();

        return QnaResponse.from(qnaRepository.save(qna));
    }
    
    // 수정
    @Transactional
    public QnaResponse updateQna(Long noticeId, Long memberId, QnaRequest request){
        Members member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        // 작성한 사람과 수정하는 사람이 일치하는지 검사


        Notice notice = findById(noticeId);

        notice.setMember(member);
        notice.setNoticeType(request.noticeType());
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setPinned(request.isPinned());

        return NoticeResponse.from(notice);
    }
    
    // 삭제

    //답변하기
}
