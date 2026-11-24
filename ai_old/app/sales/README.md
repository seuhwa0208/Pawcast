# 매출 분석 도메인

주문·결제·환불 데이터를 기반으로 기간별 매출 집계와 판매 추세를 계산합니다.

향후 파일 배치:

- `schemas.py`: 매출 분석 요청·응답 DTO
- `service.py`: 집계와 예측 비즈니스 로직
- `repository.py`: Spring Boot 또는 분석 저장소 연동
- `api.py`: FastAPI 매출 분석 엔드포인트
- `forecasting/`: 판매량·매출 예측 모델
