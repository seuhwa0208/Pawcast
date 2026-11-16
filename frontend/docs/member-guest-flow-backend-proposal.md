# POWCAST 회원·비회원 구매 흐름 백엔드·DB 보완 제안서

## 1. 문서 목적

회원·비회원 화면 점검에서 확인된 데이터 연동 공백을 백엔드와 DB 담당자에게 전달하기 위한 문서다.
서버 계약 없이 임시 화면만 만들면 실제 데이터와 달라지므로, 이 문서의 항목은 API와 DB가 준비된 후 프런트에서 연동한다.

## 2. 현재 프런트에서 완료한 항목

- 결제 완료 화면의 회원·비회원 버튼 분기
- 회원은 `주문 내역 보기`, 비회원은 `주문번호 복사` 제공
- 비회원 장바구니가 현재 브라우저에만 저장된다는 안내
- API `401` 발생 시 인증정보 제거, 로그인 화면 이동, 기존 주소 보존
- 로그인 세션 만료 안내 팝업
- `400`, `409`, `413` 및 계정 차단 오류 공통 팝업
- `403`, `404`, `500` 공통 오류 페이지

## 3. 비회원 주문 조회

### 필요한 API

#### 비회원 주문 조회 인증

`POST /api/guest/orders/lookup`

```json
{
  "orderNumber": "ORD-20261115-000001",
  "email": "guest@example.com"
}
```

권장 응답:

```json
{
  "orderAccessToken": "short-lived-token",
  "expiresIn": 600,
  "order": {
    "orderId": 1001,
    "orderNumber": "ORD-20261115-000001",
    "status": "PAID",
    "orderedAt": "2026-11-15T14:30:00+09:00",
    "items": [],
    "shippingAddress": {},
    "payment": {}
  }
}
```

#### 주문 이메일 재발송

`POST /api/guest/orders/{orderNumber}/email/resend`

```json
{
  "email": "guest@example.com"
}
```

### 보안 기준

- 주문번호와 이메일이 모두 일치해야 한다.
- 이메일 존재 여부나 주문 존재 여부를 구분해 노출하지 않는다.
- 반복 조회와 이메일 재발송에 횟수 제한을 적용한다.
- 응답의 주소와 전화번호는 필요한 범위만 마스킹한다.
- 단기 주문 접근 토큰은 해당 주문 한 건에만 사용할 수 있어야 한다.

### 프런트 후속 작업

API 준비 후에만 다음을 추가한다.

- 헤더의 `비회원 주문조회`
- 주문번호·이메일 입력 화면
- 비회원 주문 상세 화면
- 결제 완료 화면의 `비회원 주문 상세 보기`
- 주문 이메일 재발송 버튼

## 4. 회원 장바구니와 비회원 장바구니 병합

현재 프런트 Store는 비회원용 `localStorage` 장바구니다. 회원 장바구니는 서버가 기준이 되어야 한다.

### 필요한 API

- `GET /api/cart`
- `POST /api/cart/items`
- `PATCH /api/cart/items/{cartItemId}`
- `DELETE /api/cart/items/{cartItemId}`
- `POST /api/cart/merge`

병합 요청 예시:

```json
{
  "items": [
    {
      "variantId": 501,
      "quantity": 2
    }
  ]
}
```

### DB 권장 구조

#### `CARTS`

- `cart_id` PK
- `member_id` FK, 회원별 UNIQUE
- `created_at`
- `updated_at`

#### `CART_ITEMS`

- `cart_item_id` PK
- `cart_id` FK
- `variant_id` FK
- `quantity`
- `created_at`
- `updated_at`
- UNIQUE (`cart_id`, `variant_id`)

### 병합 정책

- 로그인 성공 직후 프런트가 비회원 장바구니를 병합 API로 전달한다.
- 같은 옵션은 수량을 합치되 최대 구매 수량을 초과하지 않는다.
- 품절·판매 중지·가격 변경 상품은 병합 결과에 사유를 포함한다.
- 병합 성공 후에만 브라우저의 비회원 장바구니를 삭제한다.

## 5. 회원 위시리스트

현재 공통 브라우저 키를 사용하므로 실제 운영에서는 회원 간 데이터가 분리되지 않는다.

### 필요한 API

- `GET /api/members/me/wishlist`
- `POST /api/members/me/wishlist/items`
- `DELETE /api/members/me/wishlist/items/{productId}`

### DB 권장 구조

#### `WISHLIST_ITEMS`

- `wishlist_item_id` PK
- `member_id` FK
- `product_id` FK
- `created_at`
- UNIQUE (`member_id`, `product_id`)

API 준비 후 프런트의 `powcast-wishlist` 저장소를 제거하고 서버 응답을 기준으로 표시한다.

## 6. 결제 결과 새로고침과 중복 승인 방지

현재 결제 승인 컨텍스트가 `sessionStorage`에만 있어 결과 화면을 새로고침하면 복구할 수 없다.

### 필요한 API

- `GET /api/orders/{orderId}/payment-status`
- 기존 결제 승인 API는 멱등성을 보장해야 한다.

권장 응답 필드:

- `orderId`
- `orderNumber`
- `orderStatus`
- `paymentStatus`
- `approvedAt`
- `amount`
- `method`
- `guestOrderAccessToken` 또는 비회원 조회 검증 수단

### 처리 기준

- 같은 `paymentKey` 또는 주문에 승인 요청이 반복돼도 이중 결제하지 않는다.
- 이미 승인된 주문은 성공 상태를 그대로 반환한다.
- 프런트는 저장 컨텍스트가 없을 때 URL의 주문 식별자로 상태 조회 API를 호출한다.
- 금액은 프런트 값이 아니라 DB 주문 금액과 PG 승인 금액을 서버에서 비교한다.

## 7. 로그인 갱신

현재 프런트는 `401`에서 로그인 화면으로 이동할 수 있지만 자동 토큰 갱신은 서버 지원이 필요하다.

### 필요한 API

`POST /api/auth/refresh`

권장 방식:

- Refresh Token은 `HttpOnly`, `Secure`, `SameSite` 쿠키로 관리한다.
- Access Token만 짧은 유효기간으로 재발급한다.
- 갱신 실패 시 `401`을 반환하고 프런트가 로그인 화면으로 이동한다.
- 로그아웃·탈퇴·강제 차단 시 Refresh Token을 서버에서 폐기한다.
- 동시에 여러 요청이 `401`을 받아도 갱신 요청은 한 번만 실행되도록 한다.

## 8. 주문 이후 기능의 실데이터 전환

### 취소·반품·교환

- 주문 상품 단위 클레임 생성·조회·취소 API
- 상태 이력, 사유, 환불 금액, 관리자 처리 결과 저장
- 상태별 허용 가능 동작을 백엔드가 검증

### 리뷰

- 구매확정된 주문 상품만 작성 가능
- 주문 상품당 리뷰 중복 방지
- 작성·수정·삭제·목록 API와 리뷰 이미지 저장 정책 필요

### 재구매

주문 상품에 아래 스냅샷을 보관해야 한다.

- `product_id`
- `variant_id`
- 구매 당시 옵션명
- 구매 당시 단가
- 수량

재구매 시 현재 판매 중인 동일 `variant_id`를 조회하고 현재 가격·재고를 다시 검증한다.

### 반려동물

- 회원별 반려동물 CRUD API
- 추천·급여량 계산에서 사용할 체중, 생년월일, 중성화 여부, 활동량, 알레르기 정보 정책
- 회원정보 화면과 반려동물 관리 화면은 동일 API를 사용

### 소진일 관리

상세 테이블과 API 계약은 기존 문서를 따른다.

- `docs/consumption-management-backend-spec.md`
- 구매한 `orderItemId`와 `variantId`를 관리 항목에 연결
- 구매 옵션의 최초 용량과 사용자가 수정한 잔량을 구분해 저장
- 최종 예상 소진일은 서버가 계산

## 9. 권장 구현 순서

1. 비회원 주문 조회·이메일 재발송
2. 회원 장바구니와 로그인 병합
3. 회원 위시리스트
4. 결제 상태 재조회와 승인 멱등성
5. Refresh Token 갱신
6. 주문 상품 옵션 스냅샷과 재구매
7. 클레임·리뷰·반려동물·소진일 실데이터 전환

## 10. 완료 기준

- 같은 브라우저에서 서로 다른 회원의 장바구니와 위시리스트가 섞이지 않는다.
- 비회원이 주문번호와 이메일로 본인 주문만 조회할 수 있다.
- 결제 결과 화면을 새로고침해도 승인 상태가 복구된다.
- Access Token 만료 시 갱신하거나 로그인 화면으로 안전하게 이동한다.
- 주문 이후 화면에서 임시 데이터와 공용 `localStorage`를 사용하지 않는다.
- 가격·재고·권한·주문 상태는 항상 백엔드가 최종 검증한다.
