package com.project.pawcast.member.admin.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.admin.dto.SalesRequest;
import com.project.pawcast.member.admin.dto.SalesResponse;
import com.project.pawcast.member.admin.entity.SalesPeriod;
import com.project.pawcast.member.admin.repository.AdminSalesRepository;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.security.AuthorizationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSalesService {

    private final MembersRepository membersRepository;
    private final AdminSalesRepository adminSalesRepository;
    private final AuthorizationValidator validator;

    // 매출 통계
    public List<SalesResponse> getSalesStatistics(Long memberId, SalesRequest request) {
        //권한 검사
        Members member = membersRepository.findById(memberId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateAdmin(member);

        LocalDateTime[] dateRange = getDateRange(request);

        List<Object[]> results = adminSalesRepository.findSalesStatistics(dateRange[0], dateRange[1]);

        // findSalesStatistics 쿼리는 집계값 4개(매출·주문 수·평균 주문금액·취소금액)만 반환합니다.
        // 기존에는 이를 5개 열(기간 포함)으로 읽어 첫 번째 BigDecimal을 String으로 캐스팅하면서
        // 관리자 로그인 직후 대시보드가 500으로 실패했습니다. 기간 라벨은 요청 값으로 구성합니다.
        String periodLabel = buildPeriodLabel(request, dateRange[0]);
        return results.stream().map(row -> new SalesResponse(
                periodLabel,
                decimal(row[0]),
                number(row[1]),
                decimal(row[2]),
                decimal(row[3])
        )).toList();
    }

    // 일별, 월별, 분기별, 연도별 계산
    private LocalDateTime[] getDateRange(SalesRequest request) {

        LocalDate startDate;
        LocalDate endDate;

        switch (request.period()) {
            case DAILY -> {
                startDate = LocalDate.now();
                endDate = startDate.plusDays(1);
            }

            case MONTHLY -> {
                if (request.year()==null || request.month()== null) { throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);}
                startDate = LocalDate.of(request.year(), request.month(), 1);
                endDate = startDate.plusMonths(1);
            }

            case QUARTERLY -> {
                if (request.quarter()==null || request.year()== null) { throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);}
                int startMonth = (request.quarter() - 1) * 3 + 1;
                // 1분기 -> 1월 1일 ~ 4월 1일까지(미포함)=3월31일까지
                startDate = LocalDate.of(request.year(), startMonth, 1);
                endDate = startDate.plusMonths(3);
            }

            case YEARLY -> {
                if (request.startYear()==null || request.endYear()== null) { throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);}
                startDate = LocalDate.of(request.startYear(), 1, 1);
                endDate = LocalDate.of(request.endYear() + 1, 1, 1);
            }
            default -> throw new BusinessException(ErrorCode.INVALID_SALES_PERIOD);
        }
        return new LocalDateTime[]{startDate.atStartOfDay(), endDate.atStartOfDay()
        };
    }

    private String buildPeriodLabel(SalesRequest request, LocalDateTime startDate) {
        return switch (request.period()) {
            case DAILY -> startDate.toLocalDate().toString();
            case MONTHLY -> String.format("%d-%02d", request.year(), request.month());
            case QUARTERLY -> String.format("%d Q%d", request.year(), request.quarter());
            case YEARLY -> String.format("%d", request.startYear());
        };
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private Long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
