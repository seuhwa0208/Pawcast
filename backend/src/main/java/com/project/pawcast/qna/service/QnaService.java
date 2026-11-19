package com.project.pawcast.qna.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.product.entity.Product;
import com.project.pawcast.product.repository.ProductRepository;
import com.project.pawcast.qna.dto.QnaRequest;
import com.project.pawcast.qna.dto.QnaResponse;
import com.project.pawcast.qna.entity.Qna;
import com.project.pawcast.qna.entity.QnaSearchType;
import com.project.pawcast.qna.entity.QnaType;
import com.project.pawcast.qna.repository.QnaRepository;
import com.project.pawcast.security.AuthorizationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final MembersRepository membersRepository;
    private final ProductRepository productRepository;
    private final AuthorizationValidator validator;


    // 단순 조회
    public Qna findById(Long id){
        return qnaRepository.findById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_QNA));
    }
    
    //목록 조회
    // 비공개 여부는 프론트에서 / 전체목록을 보여주고 isPrivate가 Ture인 글만 "비공개된 글입니다." 같은 방식의 처리
    // 검색 가능
    public List<QnaResponse> getListQna(QnaSearchType type, String keyword){
        QnaType qnaType;
        if (type == QnaSearchType.ALL) {qnaType = null;}
        else {qnaType = QnaType.valueOf(type.name());}
        return qnaRepository.searchQnas(qnaType, keyword).stream().map(QnaResponse::from).toList();
    }
    
    // 단건 조회
    // 다만 삭제 처리한 글(deleted_at)은 URL로 접근 못하게
    // 이게 목록에서 비공개글을 클릭했을때 검사하는 기능도 있어야함
    public QnaResponse detailQna(Long id){
        Qna qna =  qnaRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_QNA));
        if (qna.isPrivate()) {throw new BusinessException(ErrorCode.FORBIDDEN);}

        return QnaResponse.from(qna);
    }
    
    // 로그인한 회원의 문의 목록
    public List<QnaResponse> myQnaList(Long memberId){
        return qnaRepository.findAllByMember_IdOrderByCreatedAtDesc(memberId).stream().map(QnaResponse::from).toList();
    }

    // 본인 문의 상세 조회
    public QnaResponse myQna(Long qnaId, Long memberId) {
        Qna qna = qnaRepository.findByIdAndMember_Id(qnaId,memberId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_QNA));
        return QnaResponse.from(qna);
    }
    
    // 등록
    // 회원만 , 비회원은 안됨
    @Transactional
    public QnaResponse createQna(Long memberId, QnaRequest request){
        //회원 조회
        Members member =  membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        // 상품 조회 및 질문 타입 변경
        QnaType type = QnaType.GENERAL;
        Product product =null;
        if(request.productId() != null){ product = productRepository.findById(request.productId())
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PRODUCT));
                type = QnaType.PRODUCT;}

        Qna qna = Qna.builder()
                .member(member)
                .product(product)
                .title(request.title())
                .content(request.content())
                .type(type)
                .isPrivate(request.isPrivate())
                .build();

        return QnaResponse.from(qnaRepository.save(qna));
    }
    
    // 수정
    // 마찬가지로 회원만 가능
    @Transactional
    public QnaResponse updateQna(Long qnaId, Long memberId, QnaRequest request){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        // 작성한 사람과 수정하는 사람이 일치하는지 검사
        Qna qna = findById(qnaId);
        if (!qna.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);}
        QnaType type = QnaType.GENERAL;
        Product product = null;
        if (request.productId() != null) {
            product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT));
            type = QnaType.PRODUCT;}

        qna.setTitle(request.title());
        qna.setContent(request.content());
        qna.setProduct(product);
        qna.setType(type);
        qna.setPrivate(request.isPrivate());

        return QnaResponse.from(qna);
    }
    
    // 삭제
    // 삭제는 회원과 관리자만??
    // 삭제는 소프트로
    @Transactional
    public void deleteQna(Long qnaId, Long memberId){
        Qna qna = findById(qnaId);
        // 똑같은 사람인지 검사
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        if (!"ADMIN".equals(member.getRole()) && !qna.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);}
        qna.setDeletedAt(LocalDateTime.now());
    }

    // 매니저*관리자 전체 문의 목록
    // 소프트삭제 된것 제외
    public List<QnaResponse> getListQnaAdminAndManager(Long memberId){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);
        return qnaRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream().map(QnaResponse::from).toList();
    }

    // 매니저 관리자 상세 조회
    public QnaResponse detailQnaAdminAndManager(Long qnaId, Long memberId){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);
        return QnaResponse.from(findById(qnaId));
    }
}
