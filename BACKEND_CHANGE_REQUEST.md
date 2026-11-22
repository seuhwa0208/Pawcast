# PAWCAST 백엔드 수정 요청사항

작성일: 2026-11-23  
대상: Spring Boot 백엔드 담당자

## 1. 확정된 리뷰 감정 분석 호출 구조

리뷰 감정 분석은 Spring Boot가 FastAPI를 호출하지 않습니다.

확정된 흐름은 다음과 같습니다.

```text
Vue
→ Spring Boot 리뷰 등록 요청
→ Java 욕설 검사
→ product_reviews 저장
→ Spring Boot가 저장된 리뷰 ID 반환
→ Vue가 FastAPI 감정 분석 API 직접 호출
→ FastAPI가 product_reviews에서 리뷰 조회
→ 감정 분석 실행
→ review_sentiments 저장
→ 분석 결과를 Vue에 반환
```

사용 API:

```http
POST /api/reviews
POST {FASTAPI_URL}/api/reviews/{reviewId}/analyze
```

따라서 Spring Boot의 다음 감정 분석 호출 코드는 운영 흐름에서 사용하지 않습니다.

```text
review/client/SentimentClient.java
review/service/ReviewSentimentService.java
review/service/ReviewSentimentWriter.java
review/event/ReviewAnalysisListener.java
review/event/ReviewAnalysisRequested.java
review/config/ReviewAnalysisConfig.java
```

현재 `SentimentClient`가 호출하는 `/internal/sentiments`는 FastAPI에 존재하지 않습니다. 리뷰 등록 후 이벤트를 발행하는 코드도 현재 존재하지 않으므로 당장 중복 호출은 발생하지 않지만, 이후 해당 이벤트를 연결하면 실패하거나 Vue 호출과 중복 분석될 수 있습니다.

### 백엔드 요청사항

- 위 코드를 미사용 코드로 명확히 표시하거나 제거합니다.
- 리뷰 등록·수정 서비스에서 감정 분석 이벤트를 새로 발행하지 않습니다.
- Java의 `ProfanityFilter` 욕설 검사와 리뷰 DB 저장 기능은 유지합니다.
- `ReviewSentiment` 엔티티와 Repository는 유지합니다.
  - FastAPI가 저장한 감정 분석 결과를 Java가 리뷰 조회 응답에 포함할 때 필요합니다.

---

## 2. Q&A 응답에 등록일 추가

현재 `QnaResponse`에는 Q&A 등록일이 없습니다. 프론트는 목록에서 등록일을 표시해야 하므로 현재 `-`로 표시됩니다.

대상 파일:

```text
backend/src/main/java/com/project/pawcast/qna/dto/QnaResponse.java
```

현재 주요 필드:

```java
Long qnaId,
Long memberId,
String memberName,
Long productId,
String productName,
QnaType type,
String title,
String content,
boolean isPrivate,
QnaStatus status,
String answer
```

필요한 추가 필드:

```java
LocalDateTime createdAt
```

`QnaResponse.from(...)`에서도 다음 값을 넣어야 합니다.

```java
qna.getCreatedAt()
```

예상 응답:

```json
{
  "qnaId": 53,
  "memberId": 10,
  "memberName": "윤지유",
  "type": "GENERAL",
  "title": "가상 배송 문의 53",
  "content": "주문 안내를 확인하고 싶습니다.",
  "isPrivate": false,
  "status": "ANSWERED",
  "answer": "문의 내용을 확인했습니다.",
  "createdAt": "2026-11-20T14:30:00"
}
```

### 검증 기준

- `GET /api/qna` 목록의 모든 항목에 `createdAt`이 포함됩니다.
- `GET /api/qna/{qnaId}` 상세 응답에도 같은 `createdAt`이 포함됩니다.
- `GET /api/qna/me`와 `/api/qna/me/{qnaId}`에도 포함됩니다.
- 관리자용 `/api/qna/manage` 응답에도 포함됩니다.

---

## 3. 주문 응답에 주문 상품 목록 추가

현재 `OrderResponse`에는 주문 기본 정보만 있고 주문 상품 목록이 없습니다.

이로 인해 프론트에서 다음 기능을 정상적으로 구현할 수 없습니다.

- 주문 상품명과 옵션 표시
- 주문 상품 목록 펼쳐보기
- 주문 상품 이미지 표시
- 배송 완료된 주문 상품의 리뷰 작성
- 리뷰 등록 시 필요한 `orderItemId` 확인

대상 파일:

```text
backend/src/main/java/com/project/pawcast/order/dto/OrderResponse.java
backend/src/main/java/com/project/pawcast/order/service/OrderService.java
```

### 필요한 주문 상품 DTO

예시 파일명:

```text
OrderItemResponse.java
```

권장 필드:

```java
public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        Long variantId,
        String productName,
        String optionLabel,
        String imagePath,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {}
```

`OrderResponse`에 다음 필드를 추가해야 합니다.

```java
List<OrderItemResponse> items
```

예상 응답:

```json
{
  "orderId": 100,
  "orderNumber": "DEMO-000000000100",
  "status": "DELIVERED",
  "final_amount": 56000,
  "created_at": "2026-11-20T14:30:00",
  "items": [
    {
      "orderItemId": 301,
      "productId": 25,
      "variantId": 90,
      "productName": "강아지 연어 건식 사료",
      "optionLabel": "2kg",
      "imagePath": "/images/products/25.jpg",
      "quantity": 2,
      "unitPrice": 28000,
      "totalPrice": 56000
    }
  ]
}
```

### 필수 주의사항

- `orderItemId`는 반드시 포함해야 합니다.
  - 리뷰 생성 API가 구매 인증을 위해 `orderItemId`를 사용합니다.
- 상품 대표 이미지가 없다면 `imagePath`는 `null`로 반환해도 됩니다.
- 주문 목록과 주문 상세에서 동일한 `items` 구조를 사용하는 것을 권장합니다.
- JPA Lazy Loading 문제를 피하도록 조회 단계에서 주문 항목·상품 옵션·상품을 함께 조회해야 합니다.
- `Members` 엔티티 전체를 `OrderResponse`로 직접 반환하는 현재 구조는 개인정보 노출 및 순환 참조 가능성이 있으므로 회원 ID나 별도 간단 DTO로 교체하는 것을 권장합니다.

### 검증 기준

- `GET /api/orders`의 각 주문에 `items` 배열이 포함됩니다.
- `GET /api/orders/{orderId}`에도 같은 구조의 `items`가 포함됩니다.
- 배송 완료 주문의 각 상품에 실제 `orderItemId`가 포함됩니다.
- 해당 `orderItemId`로 `POST /api/reviews` 요청이 정상 처리됩니다.
- 다른 회원의 주문 상품으로 리뷰를 작성하면 기존 권한 검증에 의해 거절됩니다.

---

## 4. 리뷰 수정 후 감정 분석 처리 기준

리뷰 본문이 수정되면 Spring Boot는 기존 `review_sentiments`를 삭제하는 현재 동작을 유지합니다.

감정 분석 재실행은 Vue가 리뷰 수정 성공 후 다음 FastAPI API를 다시 호출하는 방식으로 처리합니다.

```http
POST {FASTAPI_URL}/api/reviews/{reviewId}/analyze
```

따라서 Spring Boot에서 리뷰 수정 후 FastAPI를 추가 호출할 필요는 없습니다.

---

## 5. 유지해야 하는 API 계약

프론트가 현재 사용하는 주요 Spring API입니다. 경로와 HTTP 메서드를 유지해야 합니다.

### 리뷰

```http
GET    /api/products/{productId}/reviews
GET    /api/reviews/{reviewId}
POST   /api/reviews
PATCH  /api/reviews/{reviewId}
DELETE /api/reviews/{reviewId}
```

### Q&A

```http
GET    /api/qna
GET    /api/qna/{qnaId}
POST   /api/qna
GET    /api/qna/me
GET    /api/qna/me/{qnaId}
PUT    /api/qna/{qnaId}
DELETE /api/qna/{qnaId}
GET    /api/qna/manage
```

### 공지사항

```http
GET    /api/notices
GET    /api/notices/{noticeId}
POST   /api/notices
PATCH  /api/notices/{noticeId}
DELETE /api/notices/{noticeId}
```

### 주문

```http
POST /api/orders
GET  /api/orders
GET  /api/orders/{orderId}
PATCH /api/orders/{orderId}
POST /api/orders/{orderId}/cancel
GET  /api/orders/admin
POST /api/orders/admin/{orderId}/status
```

---

## 6. 백엔드 완료 체크리스트

- [ ] 감정 분석 호출 주체가 Vue임을 백엔드 코드와 문서에 명시
- [ ] 미사용 `SentimentClient` 및 이벤트 기반 코드 제거 또는 비활성화
- [ ] 리뷰 등록·수정 과정에서 FastAPI 중복 호출이 발생하지 않는지 확인
- [ ] `QnaResponse.createdAt` 추가
- [ ] 전체·내 문의·관리자 Q&A 응답에 `createdAt` 포함 확인
- [ ] `OrderItemResponse` 추가
- [ ] `OrderResponse.items` 추가
- [ ] 주문 목록과 상세 응답에 `orderItemId` 포함 확인
- [ ] 배송 완료 주문의 실제 상품으로 리뷰 등록 테스트
- [ ] 기존 리뷰 조회 시 FastAPI가 저장한 `review_sentiments`가 정상 포함되는지 확인

