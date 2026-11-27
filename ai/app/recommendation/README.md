# 상품 추천 도메인

회원·반려동물·구매·찜·매출·리뷰 분석 결과를 조합해 상품을 추천합니다.

Spring Boot는 `PETS.birthday`와 `PETS.pet_type`, 판매 가능한 후보 상품을
`POST /api/recommendations`로 전달합니다. Python은 생애주기를 계산한 뒤
반려동물 종류·생애주기·알레르기에 맞는 상품을 필터링하고 점수가 높은 상품을
최대 5개 반환합니다.

생애주기 정책:

- 강아지: `PUPPY` 0~5개월, `JUNIOR` 6~17개월, `ADULT` 18~83개월,
  `SENIOR` 84개월 이상
- 고양이: `JUNIOR` 0~11개월, `ADULT` 12~83개월, `SENIOR` 84개월 이상
- `ALL_AGES`는 반려동물 판정값이 아니라 모든 생애주기에 대응하는 상품 코드

향후 파일 배치:

- `schemas.py`: 추천 요청·응답 DTO
- `candidate.py`: 추천 후보 상품 생성
- `ranking.py`: 추천 점수와 순위 계산 및 상위 5개 선정
- `rules.py`: 품절·알레르기·반려동물 조건 필터
- `service.py`: 전체 추천 파이프라인
- `api.py`: FastAPI 추천 엔드포인트
