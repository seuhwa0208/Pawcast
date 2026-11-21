# PAWCAST Frontend

Vue 3 + Vite 기반의 PAWCAST 반려동물 쇼핑몰 프론트엔드입니다. 개발 서버는 `5174` 포트를 사용합니다.

## 화면 역할

- `HomeView.vue` → 비회원 메인(`/`)과 회원 메인(`/member`): 상품 추천·인기 상품·카테고리 진입
- `CategoryView.vue` → 강아지·고양이·식기 상품 목록, 검색, 필터, 정렬
- `ProductDetailView.vue` → 상품 옵션, 찜·장바구니·바로구매, 베스트 리뷰·전체 리뷰
- `CartView.vue` / `CheckoutView.vue` / `PaymentResultView.vue` → 장바구니, 주문·결제, 결제 결과
- `LoginView.vue` / `SignupView.vue` / `PasswordResetView.vue` / `OAuthCallbackView.vue` → 인증·회원가입·비밀번호 초기화·소셜 로그인
- `MemberDashboardView.vue` / `MyPageView.vue` / `PetManagementView.vue` / `ClaimManagementView.vue` → 주문 내역, 회원 정보, 반려동물, 취소·반품·환불
- `NoticeView.vue` / `QnaView.vue` → 사용자 공지사항 및 문의
- `AdminOverviewView.vue` → 관리자·매니저 통합 현황 대시보드
- `AdminDashboardView.vue` → 매출·AI 분석·재학습·통계 다운로드. ECharts는 이 화면에서만 지연 로딩합니다.
- `AdminOperationsView.vue` → 주문·상품·회원·공지·Q&A 운영 관리. Q&A 답변은 사이트 모달과 실제 API를 사용합니다.

## 스타일 구조

- `src/styles/global.css` → 사이트 전체 기본 스타일
- `src/styles/corrections.css` → 사용자 공통 UI·모달·접근성 보정
- `src/styles/admin.css` → 관리자·매니저 공통 레이아웃
- `src/styles/admin-operations.css` → 관리자 운영 화면의 표·도구모음·Q&A 답변 모달

페이지 Vue의 `<style scoped>`에는 해당 화면에서만 필요한 스타일을 두고, 여러 화면에서 재사용될 스타일은 위 공통 CSS 파일에 추가합니다.
