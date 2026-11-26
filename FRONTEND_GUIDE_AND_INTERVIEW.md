# PAWCAST Vue 프론트엔드 전체 정리 및 실무 예상 질문

> 기준 경로: `D:\pawcast-fin\frontend`  
> Vue 3 Composition API, Pinia, Vue Router, Vite 기반의 현재 코드 상태를 설명한다.

## 1. 프론트엔드 역할

PAWCAST 프론트엔드는 일반 쇼핑몰과 관리자 운영 화면을 한 Vue SPA에서 제공한다.

- 회원가입·로그인·OAuth·비밀번호 재설정
- 상품 카테고리·상세·리뷰·찜
- 장바구니·주문·결제·비회원 주문 조회
- 마이페이지·반려동물·취소/환불
- 공지사항·QnA
- 관리자 매출 AI·주문/배송/환불·상품·회원·공지·QnA
- Spring Boot와 FastAPI를 용도에 따라 직접 호출

## 2. 기술 스택

| 기술 | 용도 |
|---|---|
| Vue 3 | 화면 및 Composition API 반응성 |
| Vue Router 4 | SPA 라우팅과 권한 가드 |
| Pinia 3 | 인증·장바구니·찜 전역 상태 |
| Vite 6 | 개발 서버와 production build |
| ECharts 6 | 관리자 매출 시각화 |
| Fetch API | Spring/FastAPI HTTP 통신 |

### 실제 프론트엔드 실행 환경

현재 로컬 설치 기준으로 Node.js `v24.18.1`, npm `11.16.0`을 사용한다. 프로젝트는 별도의 Python식 가상환경 대신 `node_modules`에 npm 의존성을 설치한다.

#### Node.js, npm, node_modules는 무엇인가

| 용어 | 쉬운 의미 | 이 프로젝트에서 하는 일 |
|---|---|---|
| Node.js | 브라우저 밖에서 JavaScript를 실행하는 런타임 | Vite 개발 서버와 production build 실행 |
| npm | JavaScript 패키지 설치·실행 도구 | Vue, Vite, ECharts 등을 설치하고 `npm run build` 실행 |
| `package.json` | 프로젝트가 필요한 패키지와 명령을 적은 목록 | 개발 서버·빌드 명령과 의존성 범위를 선언 |
| `package-lock.json` | 실제로 선택된 패키지의 정확한 버전 기록 | 팀원과 배포 환경의 설치 결과를 통일 |
| `node_modules` | npm이 내려받은 실제 패키지 폴더 | 프론트엔드에서 Python `.venv`와 비슷한 역할 |

Python AI의 `.venv`가 Python 패키지를 프로젝트별로 격리하듯, 프론트엔드의 `node_modules`는 Node 패키지를 프로젝트별로 둔다. 다만 Node는 보통 별도의 활성화 명령 없이, 프로젝트 폴더에서 `npm run ...`을 실행하면 그 폴더의 `node_modules`를 자동으로 사용한다.

| 패키지 | 실제 설치 버전 | 역할 |
|---|---:|---|
| Vue | 3.5.45 | UI와 반응성 시스템 |
| Vue Router | 4.6.4 | SPA 라우팅·가드 |
| Pinia | 3.0.4 | 인증·장바구니·찜 상태 |
| Vite | 6.4.5 | 개발 서버·번들링 |
| `@vitejs/plugin-vue` | 5.2.4 | `.vue` SFC 변환 |
| ECharts | 6.1.0 | 관리자 매출 차트 |

설치와 버전 재현은 `package.json`과 `package-lock.json`이 담당한다.

```cmd
cd /d D:\pawcast-fin\frontend
npm ci
npm run dev
```

`npm install`은 lock 파일을 갱신할 수 있지만, 팀원·배포 환경에서 이미 확정된 `package-lock.json` 그대로 설치하려면 `npm ci`가 더 적합하다.

## 3. 전체 통신 구조

```text
Vue
├─ springClient → VITE_SPRING_BASE_URL/api
│   └─ 회원, 상품, 장바구니, 주문, 결제, 환불, 공지, QnA, 관리 CRUD
└─ aiClient → VITE_AI_BASE_URL
    └─ 리뷰 분석/랭킹, 추천, 매출·재고 분석, 재학습, 다운로드
```

두 client 모두 access token을 `Authorization: Bearer`로 전달한다. Spring 요청은 공통 오류 이벤트를 발생시키고 `App.vue`의 전역 오류 UI가 처리한다. AI client는 JSON 또는 Blob 응답을 구분한다.

## 4. 주요 폴더

```text
frontend/
├─ src/
│  ├─ main.js                 # Vue·Pinia·Router 시작점
│  ├─ App.vue                 # 전역 레이아웃과 공통 모달/오류
│  ├─ router.js               # 공개·회원·관리자 라우트와 가드
│  ├─ layouts/AdminLayout.vue # 관리자 공통 레이아웃
│  ├─ views/                  # 페이지 컴포넌트
│  ├─ views/admin/            # 관리자 페이지
│  ├─ components/             # 재사용 UI
│  ├─ services/               # 도메인 API 계층
│  ├─ services/http/          # Spring/FastAPI 공통 client
│  └─ stores/                 # auth, cart, wishlist
├─ shared/                    # 내보내기 등 공유 계약
├─ public/
├─ .env.example
├─ vite.config.js
└─ package.json
```

## 5. 실행과 빌드

```cmd
cd /d D:\pawcast-fin\frontend
npm install
npm run dev
```

개발 주소는 `http://127.0.0.1:5174`다.

```cmd
npm run build
npm run preview
```

현재 `npm run build`는 production build가 성공하는 상태다.

## 6. 환경변수

```env
VITE_SPRING_BASE_URL=http://127.0.0.1:8080
VITE_AI_BASE_URL=http://127.0.0.1:8000
```

추가로 화면에서 Google OAuth client ID, Toss client key를 사용한다. `VITE_` 변수는 브라우저 번들에 노출되므로 비밀 키를 넣으면 안 된다. 공개 client key만 허용되며 서버 secret은 Spring 또는 FastAPI 환경변수에 둔다.

## 7. 라우팅 구조

### 공개/회원 화면

- `/`, `/dog`, `/cat`, `/tableware`: 홈·카테고리
- `/products/:pet/:id`: 상품 상세
- `/cart`, `/checkout`, `/payment/result`: 구매 흐름
- `/login`, `/signup`: 인증
- `/member`: 로그인 회원 홈
- `/mypage`, `/mypage/settings`, `/mypage/claims`, `/mypage/pets`: 회원 기능
- `/notices`, `/qna`: 고객지원

### 관리자 화면

- `/admin/sales`: AI 매출 분석
- `/admin/orders`: 주문·배송·환불
- `/admin/products`, `/admin/products/new`: 상품 관리
- `/admin/members`: ADMIN 전용 회원 관리
- `/admin/notices`, `/admin/qna`: 운영 관리

관리자 화면은 lazy import를 사용한다. 특히 ECharts가 일반 사용자 초기 번들에 포함되지 않게 한다.

## 8. 라우트 권한

- `requiresAuth`: access token 필요
- `requiresManager`: ADMIN 또는 MANAGER
- `requiresAdmin`: ADMIN만 가능
- `requiresPasswordVerification`: 현재 토큰으로 비밀번호 재확인한 세션 필요

클라이언트 가드는 화면 진입을 막아 사용자 경험을 개선하지만 보안 경계는 아니다. Spring과 FastAPI가 각 API에서 역할을 다시 검증해야 한다.

## 9. 인증 상태

Pinia auth store가 다음 값을 관리한다.

- access token: `localStorage`
- refresh token: `sessionStorage`
- role: `sessionStorage`
- current user ID: `localStorage`
- OAuth 가입 중 임시 정보: reactive memory

앱 시작 시 JWT payload의 `exp`를 읽어 만료 토큰을 제거한다. 단, payload 해석은 서명 검증이 아니며 실제 유효성은 서버가 판단한다.

### 보안 개선 권장

- XSS에 대비해 access token을 HttpOnly Secure SameSite cookie로 옮기는 방안 검토
- refresh token 자동 갱신과 동시 401 요청 single-flight 처리
- 갱신 응답의 최신 role을 store에 반영
- CSP와 사용자 입력 escaping 강화

## 10. API 서비스 계층

컴포넌트가 URL을 직접 조합하지 않고 도메인 service를 호출한다.

- `authService`: 로그인·가입·OAuth·토큰 갱신
- `productService`: 상품·카테고리·AI 베스트 리뷰
- `reviewService`: CRUD 후 FastAPI 분석 요청
- `cartService`, `wishlistService`: 쇼핑 상태
- `orderService`, `paymentService`: 주문·결제·환불
- `petService`, `memberService`, `addressService`: 회원 정보
- `adminManagementService`: 주문 상태, 환불, 상품, 회원, QnA
- `adminDashboardService`: FastAPI 매출 대시보드·재학습·파일 다운로드
- `aiAnalyticsService`: 감정 요약·재고 예측
- `aiRecommendationService`: 추천 API

이 계층 덕분에 화면은 loading/error/state에 집중하고 서버 계약 변경을 service에서 흡수할 수 있다.

## 11. 리뷰 처리 흐름

```text
사용자 리뷰 입력
  → reviewService가 Spring에 리뷰 저장
  → 성공 응답 review.id 확보
  → FastAPI /api/reviews/{id}/analyze 호출
  → 분석 실패 시에도 저장된 리뷰 자체는 유지
  → 조회 시 FastAPI ranked API로 AI 정렬 결과 표시
```

Spring 저장과 AI 분석이 하나의 분산 트랜잭션은 아니다. 분석 실패를 사용자 리뷰 등록 실패로 처리할지, 재시도 큐로 보완할지 정책이 필요하다.

## 12. 추천 흐름

Vue는 추천 요청 계약을 만들고 `/api/recommendations`를 호출한다. 현재 FastAPI 추천 계약은 회원·반려동물·선호·후보 상품 정보를 요청 본문으로 받는다.

주의할 점은 브라우저가 보낸 회원 ID와 후보 값은 신뢰할 수 없다는 것이다. 개인화 데이터는 JWT subject와 대조하거나 서버에서 조회하도록 개선하는 것이 안전하다.

## 13. 관리자 매출 AI

`AdminSalesAnalyticsView`는 다음을 FastAPI에서 직접 가져온다.

- 기간별 매출 대시보드
- 매출 예측 재계산
- 리뷰 감정 통계
- 재고 부족 예측
- CSV/XLSX 파일

다운로드는 `aiClient`가 Blob을 반환하고 서비스가 `Content-Disposition` 파일명을 처리한다. 사용 가능한 section만 드롭다운으로 제한해 잘못된 요청을 줄인다.

## 14. 주문·환불 관리자 화면

- 주문 상태는 결제 완료·배송 중·배송 완료 흐름으로 제한한다.
- 상태 변경 후 서버 응답으로 해당 행을 즉시 교체한다.
- 환불 `PENDING`만 승인·거절 버튼을 노출한다.
- 처리 후 주문 목록과 환불 목록을 다시 조회하지만 브라우저 새로고침은 하지 않는다.
- 회원 환불 화면은 완료·거절 이력도 유지하며 공통 `PaginationBar`를 사용한다.

## 15. 전역 오류 처리

`springClient`는 네트워크·HTTP 오류를 표준 Error 객체로 만들고 `powcast-api-error` 이벤트를 발행한다. 401, 403, 404, 500과 회원 차단 오류를 구분해 전역 UI가 처리한다.

개선점:

- Spring과 FastAPI 오류 형식을 하나의 application error schema로 통일
- 사용자 메시지와 개발자 로그 메시지 분리
- 요청 ID 표시
- abort controller로 화면 이탈 시 요청 취소
- 중복 오류 모달 억제

## 16. 상태 관리 기준

전역 store에 적합한 것:

- 여러 화면이 공유하는 인증·장바구니·찜
- 페이지 이동 후에도 유지할 필요가 있는 상태

컴포넌트 local state에 적합한 것:

- 검색어, 현재 페이지, 모달 열림, loading/error
- 특정 화면에서만 쓰는 응답 목록

서버 상태가 복잡해지면 TanStack Query 같은 캐시 계층을 도입해 stale time, retry, request deduplication을 표준화할 수 있다.

## 17. 성능 전략

- 관리자 화면 lazy loading
- ECharts를 관리자 진입 시점에만 다운로드
- computed로 필터·페이지 결과 파생
- 목록 key에 안정적인 DB ID 사용
- production build chunk 분리

추가 개선:

- 이미지 lazy loading과 responsive source
- 상품 목록 서버 페이지네이션
- 검색 debounce
- route 단위 prefetch 정책
- Web Vitals 측정
- 큰 chart library manualChunks 검토

## 18. 접근성·UX 체크리스트

- 아이콘 버튼 `aria-label`
- 모달 focus trap과 닫힌 뒤 focus 복원
- 키보드로 custom select 조작
- loading/error/empty 상태 각각 제공
- 색상만으로 상태를 전달하지 않기
- form label과 오류 메시지 연결
- `prefers-reduced-motion` 대응
- 결제/환불 중 중복 클릭 방지

## 19. 테스트 전략

현재 build 검증 외에 다음 계층이 필요하다.

1. Vitest: store, formatter, service adapter 단위 테스트
2. Vue Test Utils: form·modal·pagination 컴포넌트 테스트
3. MSW: Spring/FastAPI API mock 통합 테스트
4. Playwright: 로그인→장바구니→주문, 리뷰→분석, 관리자 환불 승인 E2E
5. 접근성: axe 자동 검사
6. 계약 테스트: OpenAPI에서 생성한 타입과 실제 응답 확인

## 20. 배포 체크리스트

- production `.env`의 Spring/AI HTTPS URL 확인
- SPA history fallback 설정
- static asset cache hash 활용
- `index.html`은 짧은 cache, hash asset은 immutable
- API CORS origin에 실제 프론트 주소 등록
- source map 공개 정책 결정
- CSP, HSTS, frame-ancestors 적용
- Sentry 등 오류 수집 시 개인정보 마스킹
- build 후 핵심 E2E smoke test

## 21. 현재 코드의 강점

- Spring과 AI client의 책임 분리
- 도메인별 service 모듈화
- 관리자 lazy loading
- Pinia 기반 인증 상태 중앙화
- 라우트 meta를 이용한 권한 표현
- loading/error/empty 상태 구현
- 재사용 가능한 PaginationBar와 공통 모달
- AI 다운로드의 Blob 처리
- 주문·환불 처리 후 새로고침 없는 갱신

## 22. 우선 개선할 부분

1. refresh token 자동 갱신과 최신 role 반영
2. TypeScript 전환 또는 최소한 OpenAPI 기반 타입 생성
3. AI 공개 API의 사용자 식별·소유권 검증
4. Spring/FastAPI 오류 계약 통일
5. 컴포넌트 및 E2E 테스트 도입
6. 관리자 custom select 키보드 접근성 강화
7. 목록 서버 페이지네이션
8. 토큰 저장 방식과 CSP 보강
9. 중복/레거시 관리자 View 정리
10. API request cancellation과 race condition 방지

## 23. 현직자 예상 질문과 답변

### Vue 구조

1. **Composition API를 선택한 이유는 무엇인가요?**  
   관련 상태와 로직을 기능 단위로 모으기 쉽고 composable로 재사용하기 좋기 때문이다.

2. **computed와 watch의 차이는 무엇인가요?**  
   computed는 기존 상태에서 값을 파생하고 캐시하며, watch는 상태 변화에 따른 부수효과를 실행한다.

3. **왜 필터 결과를 computed로 만들었나요?**  
   원본 배열을 변경하지 않고 검색어·상태 변경 시 필요한 경우에만 재계산하기 위해서다.

4. **v-for의 key는 왜 중요하나요?**  
   Vue가 DOM 노드의 동일성을 판단해 올바르게 재사용하도록 한다. index보다 DB ID가 안정적이다.

5. **props를 직접 수정하면 안 되는 이유는요?**  
   단방향 데이터 흐름이 깨진다. 자식은 emit으로 부모에게 변경을 요청해야 한다.

6. **화면과 API 코드를 왜 분리했나요?**  
   URL·직렬화·오류 계약 변경을 service에서 흡수하고 컴포넌트 테스트를 쉽게 하기 위해서다.

7. **composable과 Pinia는 언제 구분하나요?**  
   여러 컴포넌트가 공유하고 앱 생명주기 동안 유지할 상태는 Pinia, 재사용 로직이지만 인스턴스별 상태면 composable이 적합하다.

### 라우팅·권한

8. **라우트 meta는 무엇에 사용했나요?**  
   인증, MANAGER, ADMIN, 비밀번호 재확인 요구사항을 선언적으로 표현했다.

9. **프론트 라우트 가드가 보안인가요?**  
   아니다. 화면 접근 UX일 뿐 API 서버에서 동일한 권한을 검증해야 한다.

10. **MANAGER와 ADMIN을 어떻게 구분하나요?**  
    공통 관리자 부모는 둘 다 허용하고 회원 관리 자식 라우트에는 `requiresAdmin`을 추가한다.

11. **권한이 변경된 사용자는 언제 반영되나요?**  
    로그인 또는 토큰 갱신 응답에서 DB의 최신 role을 받고 store/sessionStorage를 갱신해야 한다.

12. **404 처리는 어떻게 하나요?**  
    catch-all route를 `/error/404`로 리다이렉트한다. 서버도 SPA history fallback을 지원해야 한다.

### 인증·보안

13. **localStorage 토큰의 위험은 무엇인가요?**  
    XSS가 발생하면 JavaScript가 토큰을 읽을 수 있다. HttpOnly cookie와 CSP를 검토해야 한다.

14. **JWT payload에서 exp를 읽는 것이 검증인가요?**  
    아니다. 클라이언트 편의상 만료 여부를 빠르게 판단할 뿐 서명과 권한은 서버가 검증한다.

15. **refresh 요청이 동시에 여러 개 발생하면요?**  
    refresh promise를 하나만 유지하는 single-flight 패턴으로 한 번만 갱신하고 대기 요청을 재시도해야 한다.

16. **VITE 환경변수에 secret을 넣으면 안 되는 이유는요?**  
    build 시 브라우저 JavaScript에 문자열로 포함되어 누구나 볼 수 있기 때문이다.

17. **CORS와 CSRF 차이는 무엇인가요?**  
    CORS는 다른 Origin의 응답 읽기를 제한하고, CSRF는 사용자 인증 정보를 이용한 위조 요청 문제다. cookie 인증이면 CSRF 방어가 특히 필요하다.

18. **XSS는 어떻게 줄이나요?**  
    사용자 HTML 직접 삽입을 피하고 Vue escaping을 유지하며 CSP, 입력 검증, 의존성 점검을 적용한다.

### API 통신

19. **Spring client와 AI client를 나눈 이유는요?**  
    base URL, 오류 형식, JSON/Blob 처리, 책임 도메인이 다르기 때문이다.

20. **왜 fetch wrapper를 만들었나요?**  
    JWT 헤더, JSON parsing, 네트워크 오류, 공통 메시지를 모든 서비스에서 반복하지 않기 위해서다.

21. **204 응답은 어떻게 처리하나요?**  
    JSON parsing을 시도하지 않고 `null`을 반환한다.

22. **파일 다운로드는 어떻게 처리하나요?**  
    content type이 JSON이 아니면 Blob으로 받고 object URL과 download attribute로 저장한다.

23. **요청 race condition은 어떻게 막나요?**  
    최신 요청 ID 비교 또는 AbortController로 이전 요청을 취소할 수 있다.

24. **API 응답 타입이 바뀌면 어떻게 감지하나요?**  
    현재는 runtime 의존성이 크다. TypeScript와 OpenAPI 생성 타입, contract test를 도입하는 것이 좋다.

25. **FastAPI가 꺼져 있으면 일반 쇼핑몰도 멈추나요?**  
    일반 Spring 기능은 유지된다. AI 영역은 독립적인 오류·빈 상태를 보여주고 리뷰 분석 재시도를 지원해야 한다.

### 상태·렌더링

26. **서버 상태를 Pinia에 모두 넣지 않은 이유는요?**  
    화면 한 곳에서만 쓰는 데이터까지 전역화하면 lifecycle과 무효화가 복잡해지기 때문이다.

27. **새로고침 없이 주문 상태를 갱신한 방법은요?**  
    변경 API의 최신 응답으로 배열의 해당 행을 교체하거나 관련 목록을 다시 fetch한다.

28. **낙관적 업데이트와 보수적 업데이트 차이는요?**  
    낙관적 업데이트는 서버 응답 전 UI를 바꾸고 실패 시 rollback한다. 주문·환불은 정합성이 중요해 성공 응답 후 반영하는 보수적 방식이 안전하다.

29. **페이지네이션은 어디서 해야 하나요?**  
    데이터가 작으면 클라이언트도 가능하지만 운영 데이터는 서버 페이지네이션이 네트워크·메모리·검색 정합성에 유리하다.

30. **목록 갱신 시 현재 페이지가 사라지면요?**  
    pageCount를 watch해 마지막 유효 페이지로 보정한다.

### 성능

31. **lazy loading의 효과는 무엇인가요?**  
    초기 번들에서 관리자와 ECharts 코드를 분리해 일반 사용자 첫 로딩 비용을 낮춘다.

32. **검색 입력마다 필터가 무거우면요?**  
    debounce, 서버 검색, 인덱싱된 API를 사용한다.

33. **ECharts 번들이 큰데 어떻게 개선하나요?**  
    필요한 chart와 component만 tree-shaking import하고 route chunk를 분리하며 렌더링 데이터 수를 제한한다.

34. **이미지 성능은 어떻게 개선하나요?**  
    WebP/AVIF, sizes/srcset, lazy loading, 고정 width/height, CDN cache를 사용한다.

35. **무엇을 측정해야 최적화할 수 있나요?**  
    LCP, INP, CLS, route chunk 크기, API latency, rendering time을 실제 환경에서 측정한다.

### UX·접근성

36. **custom dropdown의 주의점은요?**  
    Arrow key, Enter, Escape, focus 이동, ARIA role을 직접 구현해야 한다. 가능하면 native select가 더 안전하다.

37. **모달 접근성은 어떻게 보장하나요?**  
    focus trap, Escape 닫기, aria-modal, 제목 연결, 닫힌 후 trigger focus 복원을 구현한다.

38. **loading, empty, error를 왜 나누나요?**  
    사용자가 기다려야 하는지, 데이터가 없는지, 재시도해야 하는지 행동이 다르기 때문이다.

39. **환불 버튼 중복 클릭은 어떻게 막나요?**  
    처리 중 ID를 저장해 해당 버튼을 disabled하고 서버에서도 상태 전이와 lock으로 중복 처리를 막는다.

40. **오류 메시지를 그대로 보여줘도 되나요?**  
    내부 SQL·stack trace는 노출하면 안 된다. 사용자 메시지와 로깅용 상세 정보를 분리한다.

### 테스트·품질

41. **무엇을 단위 테스트하겠나요?**  
    formatter, computed filter, store action, service request mapping, 권한 판단을 우선한다.

42. **E2E 핵심 시나리오는요?**  
    회원 로그인→장바구니→결제, 리뷰 등록→분석, 관리자 배송 완료, 환불 승인·거절, MANAGER 접근 제한이다.

43. **API는 어떻게 mock하나요?**  
    MSW로 실제 fetch 레벨에서 Spring과 FastAPI 응답을 가로채면 서비스와 컴포넌트를 함께 검증할 수 있다.

44. **스냅샷 테스트만으로 충분한가요?**  
    아니다. 사용자 행동과 접근 가능한 이름, 상태 변화를 assertion하는 테스트가 더 가치 있다.

45. **build 성공이 기능 정상까지 의미하나요?**  
    문법·번들 가능성만 보장한다. 실제 서버 계약, 권한, DB 상태는 통합/E2E 테스트가 필요하다.

### 프로젝트 회고

46. **이 구조의 가장 좋은 점은 무엇인가요?**  
    쇼핑몰 트랜잭션과 AI 분석의 호출 계층이 분리되어 기능 책임과 장애 범위를 설명하기 쉽다.

47. **가장 큰 기술 부채는 무엇인가요?**  
    자동 token refresh, API 타입 안전성, 공개 AI API의 사용자 검증, 중복 레거시 화면이다.

48. **TypeScript로 바꾼다면 어디부터 하나요?**  
    HTTP 응답 DTO와 service부터 타입화한 뒤 store, 공통 component, view 순으로 점진 전환한다.

49. **서버 컴포넌트나 SSR이 필요한가요?**  
    관리자 중심이면 SPA로 충분하지만 공개 상품 SEO가 중요하면 Nuxt SSR/SSG를 검토할 수 있다.

50. **입사 후 이 프로젝트를 맡으면 첫 주에 무엇을 하나요?**  
    핵심 사용자 흐름 E2E를 만들고 오류·성능 지표를 수집한 뒤 인증 갱신과 AI API 권한 문제부터 해결한다.

