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

## 전체 파일 안내

### 진입·라우팅

| 파일 | 주요 기능 |
| --- | --- |
| `src/main.js` | Vue·Pinia·라우터를 등록하고 전역/관리자 공통 CSS를 불러옵니다. |
| `src/App.vue` | 전역 오류 안내와 공통 앱 영역을 연결합니다. |
| `src/router.js` | URL과 화면을 연결하고 로그인, 관리자, 매니저, 비밀번호 재확인 권한을 검사합니다. 관리자 화면은 지연 로딩합니다. |

### 사용자 화면: `src/views/`

| 파일 | 주요 기능 |
| --- | --- |
| `HomeView.vue` | 비회원 메인(`/`)과 회원 메인(`/member`)의 배너, 인기 상품, AI 추천 진입을 표시합니다. |
| `CategoryView.vue` | 강아지·고양이·식기·급수 상품 목록의 검색, 필터, 정렬을 처리합니다. |
| `ProductDetailView.vue` | 상품 옵션, 찜, 장바구니, 바로 구매, 베스트/전체 리뷰를 제공합니다. |
| `CartView.vue` / `CheckoutView.vue` / `PaymentResultView.vue` | 장바구니, 주문/결제, 결제 결과를 담당합니다. |
| `LoginView.vue` / `SignupView.vue` / `PasswordResetView.vue` / `OAuthCallbackView.vue` | 일반/Google 인증, 이메일 인증, 비밀번호 초기화, 소셜 콜백을 처리합니다. |
| `MemberDashboardView.vue` | 주문 내역·상세, 리뷰 작성, 마이페이지 진입을 제공합니다. |
| `MyPageView.vue` | 회원 정보, 마케팅 동의, 비밀번호 변경, 배송지 CRUD, 회원 탈퇴를 처리합니다. |
| `PetManagementView.vue` | 강아지/고양이 반려동물 등록·수정·삭제 및 품종 선택 화면입니다. |
| `ClaimManagementView.vue` | 배송 완료 주문의 환불 신청과 환불 상태를 처리합니다. |
| `NoticeView.vue` / `QnaView.vue` | 사용자 공지·이벤트 및 문의의 검색, 목록, 상세, 등록을 제공합니다. |
| `ErrorView.vue` | 403·404·500 공통 오류 화면입니다. |

### 관리자·매니저 화면: `src/views/`

| 파일 | 주요 기능 |
| --- | --- |
| `AdminOverviewView.vue` | **대시보드**입니다. 매출·주문·상품·Q&A·공지 API 요약과 매출/주문, 3개 관 매출 비중 ECharts를 표시합니다. |
| `AdminDashboardView.vue` | **매출 분석**입니다. 일/월/분기/연도 조건, DB 매출 연도, 매출 KPI, ECharts, AI 판매 예측·재학습, CSV/XLSX 내보내기를 제공합니다. |
| `AdminOperationsView.vue` | 주문·배송, 상품, 회원, 공지, Q&A 공용 운영 화면입니다. 주문 상태 변경, 환불 승인, 판매 상/하위 3, 회원 상태/역할 변경, Q&A 답변 모달을 처리합니다. |
| `AdminProductCreateView.vue` | Spring `ProductCreateRequest`에 맞춘 실제 상품 등록 화면입니다. 식품/식기 상세 정보와 기본 옵션·가격·원가·초기 재고를 등록합니다. 업로드 API가 없어 이미지 입력은 포함하지 않습니다. |
| `AdminDemoView.vue` | 이전 시연용 보조 화면이며 현재 운영 메뉴에서는 사용하지 않습니다. |
| `AdminOperationsPage.vue` | 이전 보조 구현입니다. 현재 라우터는 `AdminOperationsView.vue`를 사용합니다. |

### 컴포넌트: `src/components/`

| 파일 | 주요 기능 |
| --- | --- |
| `AppHeader.vue` / `AppFooter.vue` | 사용자 사이트 상단 메뉴와 푸터입니다. |
| `AdminSidebar.vue` / `MemberSidebar.vue` | 관리자·매니저 및 마이페이지 사이드 메뉴입니다. |
| `ProductCard.vue` | 목록·추천·인기 상품에 재사용되는 상품 카드입니다. |
| `UiTag.vue` | 상품·상태 라벨 공통 UI입니다. |
| `AccessDeniedModal.vue` / `GlobalErrorModal.vue` | 권한 및 API 공통 오류 안내 모달입니다. |
| `LoginRequiredModal.vue` / `CartAddedModal.vue` / `SoldOutModal.vue` / `SignupCompleteModal.vue` | 구매·회원 흐름의 공통 안내 모달입니다. |

### API 서비스: `src/services/`

| 파일 | 주요 기능 |
| --- | --- |
| `http/springClient.js` | Spring 기본 URL, JWT Authorization 헤더, JSON·공통 오류 처리를 담당합니다. |
| `http/aiClient.js` | FastAPI AI 요청과 오류 처리를 담당합니다. |
| `authService.js` | 로그인, 회원가입, 이메일 인증, 비밀번호 초기화, Google 가입·계정 연결 API입니다. |
| `productService.js` | 상품 목록/상세/카테고리/브랜드 및 AI 베스트 리뷰 상품 응답을 변환합니다. |
| `cartService.js` / `wishlistService.js` | 회원 장바구니·찜 CRUD입니다. |
| `orderService.js` / `paymentService.js` | 주문, 취소, 환불 요청, 결제 승인 API입니다. |
| `reviewService.js` | 리뷰 CRUD와 AI 감성 분석 API입니다. |
| `qnaService.js` / `noticeService.js` | Q&A와 공지사항 API입니다. |
| `memberService.js` / `petService.js` / `addressService.js` | 회원 정보·비밀번호·반려동물·배송지 API입니다. |
| `aiRecommendationService.js` | 반려동물 기반 AI 상품 추천 API입니다. |
| `adminDashboardService.js` | Spring 경유 매출 집계·연도 조회, AI 재학습, AI 통계 파일 내보내기 API입니다. |
| `adminManagementService.js` | 주문 상태, 환불 승인, 상품 성과, 회원 상태/역할, 관리자 Q&A 답변, 상품 등록 API입니다. |

### 상태·스타일

| 경로/파일 | 주요 기능 |
| --- | --- |
| `src/stores/auth.js` | Access Token, 회원 식별자·역할, Google 가입 대기 정보, 로그인 시각을 저장합니다. |
| `src/stores/cart.js` / `src/stores/wishlist.js` | 장바구니와 찜 동기화 상태를 관리합니다. |
| `src/styles/global.css` | 사이트 기본 색상·폰트·레이아웃입니다. |
| `src/styles/corrections.css` | 사용자 공통 버튼·드롭다운·모달·접근성 보정입니다. |
| `src/styles/admin.css` | 관리자·매니저 공통 레이아웃입니다. |
| `src/styles/admin-overview.css` | 관리자 대시보드 KPI·ECharts 패널 스타일입니다. |
| `src/styles/admin-operations.css` | 운영 표·도구모음·환불·Q&A 답변 모달 스타일입니다. |

## 환경 변수

실제 `.env`는 Git에 올리지 않으며 `frontend/.env.example`을 복사해 사용합니다.

```env
VITE_SPRING_BASE_URL=http://127.0.0.1:8080
VITE_AI_BASE_URL=http://127.0.0.1:8000
VITE_GOOGLE_CLIENT_ID=your-google-client-id
VITE_TOSS_CLIENT_KEY=your-toss-client-key
```
