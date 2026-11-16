package com.project.powcast.qnaanswer.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import com.project.powcast.qna.dto.QnaRequest;
import com.project.powcast.qna.entity.Qna;
import com.project.powcast.qna.repository.QnaRepository;
import com.project.powcast.qnaanswer.dto.QnaAnswerRequest;
import com.project.powcast.qnaanswer.dto.QnaAnswerResponse;
import com.project.powcast.qnaanswer.entity.QnaAnswer;
import com.project.powcast.qnaanswer.repository.QnaAnswerRepository;
import com.project.powcast.security.AuthorizationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaAnswerService {

    private final QnaAnswerRepository qnaAnswerRepository;
    private final QnaRepository qnaRepository;
    private final MemberRepository memberRepository;
    private final AuthorizationValidator validator;

    // 단순 Id 조회
    public QnaAnswer findById(Long qnaId){

    }

    //답변하기
    //답변은 관리자와 매니저만 가능
    @Transactional
    public QnaAnswerResponse answerQna(Long qnaId, Long memberId, QnaAnswerRequest request) {
        // 권한 검사
        Members member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);
        // 문의 조회
        Qna qna = qnaRepository.findByIdAndDeletedAtIsNull(qnaId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_QNA));
        // 답변 작성자 조회

        QnaAnswer answer = QnaAnswer.builder()
                .qna(qna)
                .member(member)
                .content(request.content())
                .build();
        return QnaAnswerResponse.from(qnaAnswerRepository.save(answer));
    }

    //답변 수정
    @Transactional
    public QnaAnswerResponse updateAnswer(Long qnaId, Long memberId, String role, QnaAnswerRequest request){
        //권한 검사
        Members member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);

        Qna qna = qnaRepository.findByIdAndDeletedAtIsNull(qnaId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_QNA));
        QnaAnswer answer = qnaAnswerRepository.findByQna(qna).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_QNA_ANSWER));
        answer.setContent(request.content());
        return QnaAnswerResponse.from(qnaAnswerRepository.save(answer));
    }

    //답변 삭제
    @Transactional
    public void deleteAnswer(Long qnaId, Long memberId) {
        // 권한 검사
        Members member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);

        Qna qna = qnaRepository.findByIdAndDeletedAtIsNull(qnaId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_QNA));
        QnaAnswer answer = qnaAnswerRepository.findByQna(qna).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_QNA_ANSWER));
        qnaAnswerRepository.delete(answer);
    }
}
