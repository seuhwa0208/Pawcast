<!-- 주문 현황과 회원 활동을 요약하고 비밀번호 재확인 흐름을 여는 마이페이지 대시보드입니다. -->
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";
import { useCart } from "@/stores/cart";
import { useWishlist } from "@/stores/wishlist";
import { orderService } from "@/services/orderService";
import { cartService } from "@/services/cartService";
import { reviewService } from "@/services/reviewService";
import { verifyMyPassword } from "@/services/memberService";
import MemberSidebar from "@/components/MemberSidebar.vue";

const route = useRoute();
const router = useRouter();
const { accessToken, currentUserId } = useAuthSession();
const { count: cartCount, add: addLocalCart } = useCart();
const wishlistStore = useWishlist();
const effectiveUserId = computed(() => currentUserId.value);
const displayName = computed(() => effectiveUserId.value || "포캐스트 회원");
const verifyOpen = ref(false);
const orders = ref([]);
const ordersLoading = ref(false);
const ordersError = ref("");
const selectedOrder = ref(null);
const orderDetailOpen = ref(false);
const expandedOrderIds = ref([]);
const actionMessage = ref("");
const reviewOpen = ref(false);
const reviewItem = ref(null);
const reviewRating = ref(5);
const reviewContent = ref("");
const reviewSubmitting = ref(false);
const cancelOpen = ref(false);
const cancelOrderTarget = ref(null);
const cancelReason = ref("");
const cancelSubmitting = ref(false);
function showActionMessage(message) {
  actionMessage.value = message;
  window.setTimeout(() => (actionMessage.value = ""), 2400);
}
function toggleOrderItems(orderId) {
  expandedOrderIds.value = expandedOrderIds.value.includes(orderId)
    ? expandedOrderIds.value.filter((id) => id !== orderId)
    : [...expandedOrderIds.value, orderId];
}
const password = ref("");
const verifyError = ref("");
const verifying = ref(false);
const statusLabels = {
  PAYMENT_COMPLETED: "결제 완료",
  PENDING_PAYMENT: "결제 대기",
  PAID: "결제 완료",
  PREPARING: "상품 준비중",
  SHIPPING: "배송중",
  DELIVERED: "배송 완료",
  CONFIRMED: "구매 확정",
  CANCELLED: "주문 취소",
};
const orderCards = computed(() =>
  orders.value.map((order) => {
    const orderItems = order.items || order.orderItems || [];
    const firstItem = orderItems[0] || {};
    return {
      ...order,
      id: order.orderId ?? order.id,
      orderItems,
      firstItem,
      displayStatus: statusLabels[order.status] || order.status,
      displayDate: String(
        order.createdAt || order.created_at || order.orderDate || "",
      )
        .slice(0, 10)
        .replaceAll("-", "."),
      extraCount: Math.max(0, orderItems.length - 1),
    };
  }),
);
const statusCounts = computed(() =>
  orders.value.reduce((counts, order) => {
    counts[order.status] = (counts[order.status] || 0) + 1;
    return counts;
  }, {}),
);

onMounted(async () => {
  if (route.query.verify === "1") verifyOpen.value = true;
  await loadOrders();
});
async function loadOrders() {
  try {
    ordersLoading.value = true;
    ordersError.value = "";
    const result = await orderService.getMyOrders({ period: "6m" });
    const serverOrders = Array.isArray(result)
      ? result
      : result.content || result.items || result.orders || [];
    orders.value = serverOrders;
  } catch (error) {
    orders.value = [];
    ordersError.value = error.message || "주문 내역을 불러오지 못했습니다.";
  } finally {
    ordersLoading.value = false;
  }
}
async function openOrderDetail(orderId) {
  try {
    selectedOrder.value = await orderService.getOrder(orderId);
    orderDetailOpen.value = true;
  } catch (error) {
    showActionMessage(error.message || "주문 상세를 불러오지 못했습니다.");
  }
}
function openCancelOrder(order) {
  cancelOrderTarget.value = order;
  cancelReason.value = "";
  cancelOpen.value = true;
}
async function submitCancelOrder() {
  const orderId =
    cancelOrderTarget.value?.orderId || cancelOrderTarget.value?.id;
  if (!orderId || !cancelReason.value.trim()) return;
  try {
    cancelSubmitting.value = true;
    await orderService.cancelOrder(orderId, cancelReason.value.trim());
    cancelOpen.value = false;
    showActionMessage("주문이 취소되었습니다.");
    await loadOrders();
  } catch (error) {
    showActionMessage(error.message || "주문을 취소하지 못했습니다.");
  } finally {
    cancelSubmitting.value = false;
  }
}
async function repurchaseOrderItem(item) {
  if (!item.variantId) return;
  try {
    await cartService.addItem({ variantId: item.variantId, quantity: 1 });
    showActionMessage("상품을 장바구니에 담았습니다.");
  } catch (error) {
    showActionMessage(error.message || "재구매 상품을 담지 못했습니다.");
  }
}
function openReview(item) {
  reviewItem.value = item;
  reviewRating.value = 5;
  reviewContent.value = "";
  reviewOpen.value = true;
}
async function submitReview() {
  if (!reviewContent.value.trim()) {
    showActionMessage("리뷰 내용을 입력해주세요.");
    return;
  }
  try {
    reviewSubmitting.value = true;
    await reviewService.create({
      orderItemId: reviewItem.value.id,
      rating: reviewRating.value,
      content: reviewContent.value.trim(),
    });
    reviewOpen.value = false;
    showActionMessage("리뷰가 등록되었습니다.");
  } catch (error) {
    showActionMessage(error.message || "리뷰를 등록하지 못했습니다.");
  } finally {
    reviewSubmitting.value = false;
  }
}
function notifyDirectPaymentPending() {
  showActionMessage("바로 결제는 주문·PG 요청 형식 확정 후 연결됩니다.");
}
function productDetailPath(item) {
  return `/products/${item?.pet || "DOG"}/${item?.productId || 1}`;
}
function requestSettings() {
  password.value = "";
  verifyError.value = "";
  verifyOpen.value = true;
}
async function verifyPassword() {
  if (!password.value) {
    verifyError.value = "비밀번호를 입력해주세요.";
    return;
  }
  try {
    verifying.value = true;
    const verified = await verifyMyPassword(password.value);
    if (!verified) {
      verifyError.value = "비밀번호가 일치하지 않습니다.";
      return;
    }
    // 라우터 가드가 같은 로그인 토큰으로 검증 완료 여부를 확인합니다.
    sessionStorage.setItem("powcast-profile-verified", accessToken.value);
    verifyOpen.value = false;
    password.value = "";
    await router.push("/mypage/settings");
  } catch (error) {
    verifyError.value = error.message || "비밀번호가 일치하지 않습니다.";
  } finally {
    verifying.value = false;
  }
}
</script>

<template>
  <main class="container member-dashboard">
    <header class="dashboard-heading">
      <span>POWCAST MY PAGE</span>
      <h1>마이페이지</h1>
      <p>주문과 관심 상품을 한곳에서 편리하게 관리하세요.</p>
    </header>
    <div class="dashboard-layout">
      <MemberSidebar
        :name="displayName"
        :user-id="effectiveUserId"
        active="orders"
        @settings="requestSettings"
      />
      <div class="dashboard-content">
        <section class="shopping-summary">
          <h2>나의 쇼핑 현황</h2>
          <div>
            <RouterLink to="/mypage#orders"
              ><b>{{ orders.length }}</b
              ><span>주문·배송</span></RouterLink
            ><RouterLink :to="{ path: '/cart', query: { tab: 'wishlist' } }"
              ><b>{{ wishlistStore.itemIds.length }}</b
              ><span>위시리스트</span></RouterLink
            ><RouterLink to="/cart"
              ><b>{{ cartCount }}</b
              ><span>장바구니</span></RouterLink
            >
          </div>
        </section>
        <section id="orders" class="order-section">
          <header>
            <div>
              <h2>주문·배송 조회</h2>
              <p>최근 6개월 동안의 주문 내역입니다.</p>
            </div>
            <span>최근 6개월</span>
          </header>
          <div class="delivery-steps">
            <article
              v-for="(step, index) in [
                { key: 'PAYMENT_COMPLETED', label: '결제 완료' },
                { key: 'PREPARING', label: '상품 준비중' },
                { key: 'SHIPPING', label: '배송중' },
                { key: 'DELIVERED', label: '배송 완료' },
                { key: 'CONFIRMED', label: '구매 확정' },
              ]"
              :key="step.key"
            >
              <b :class="{ active: statusCounts[step.key] }">{{ index + 1 }}</b
              ><span>{{ step.label }}</span
              ><small>{{ statusCounts[step.key] || 0 }}건</small>
            </article>
          </div>
          <p v-if="ordersLoading" class="dashboard-order-state">
            주문 내역을 불러오는 중입니다.
          </p>
          <div v-else-if="orderCards.length" class="order-list">
            <article
              v-for="order in orderCards"
              :key="order.orderId || order.id"
            >
              <header>
                <b>{{ order.displayDate }} 주문</b
                ><button
                  type="button"
                  @click="openOrderDetail(order.orderId || order.id)"
                >
                  주문 상세</button
                ><button
                  v-if="
                    ['PENDING_PAYMENT', 'PAID', 'PAYMENT_COMPLETED'].includes(
                      order.status,
                    )
                  "
                  type="button"
                  @click="openCancelOrder(order)"
                >
                  주문 취소
                </button>
              </header>
              <div class="order-product-list">
                <article
                  v-for="item in expandedOrderIds.includes(order.id)
                    ? order.orderItems
                    : order.orderItems.slice(0, 1)"
                  :key="item.id"
                  class="order-product-row"
                >
                  <RouterLink
                    class="order-product-image-link"
                    :to="productDetailPath(item)"
                    :aria-label="`${item.productName || '주문 상품'} 상세보기`"
                  >
                    <img
                      :src="item.imageUrl || item.image"
                      :alt="item.productName || '주문 상품'"
                    />
                  </RouterLink>
                  <div class="order-info">
                    <em>{{ order.displayStatus }}</em>
                    <h3>
                      <RouterLink :to="productDetailPath(item)">
                        {{ item.productName || "주문 상품" }}
                      </RouterLink>
                    </h3>
                    <p>
                      {{ item.optionLabel || "기본 옵션" }} · 수량
                      {{ item.quantity || 1 }}개
                    </p>
                    <b
                      >{{
                        Number(
                          item.unitPrice || item.price || 0,
                        ).toLocaleString()
                      }}원</b
                    >
                  </div>
                  <div class="order-item-actions">
                    <button type="button" @click="repurchaseOrderItem(item)">
                      장바구니 담기
                    </button>
                    <button type="button" @click="notifyDirectPaymentPending">
                      바로 결제하기
                    </button>
                    <button
                      type="button"
                      :disabled="
                        !['DELIVERED', 'CONFIRMED'].includes(order.status)
                      "
                      @click="openReview(item)"
                    >
                      리뷰 작성
                    </button>
                  </div>
                </article>
              </div>
              <button
                v-if="order.orderItems.length > 1"
                type="button"
                class="order-expand-button"
                :class="{ expanded: expandedOrderIds.includes(order.id) }"
                @click="toggleOrderItems(order.id)"
              >
                총 {{ order.orderItems.length }}건 주문
                {{ expandedOrderIds.includes(order.id) ? "접기" : "펼쳐보기" }}
              </button>
            </article>
          </div>
          <p
            v-else
            class="dashboard-order-state"
            :class="{ error: ordersError }"
          >
            {{ ordersError || "최근 6개월 주문 내역이 없습니다." }}
          </p>
        </section>
      </div>
    </div>
    <Teleport to="body">
      <div
        v-if="cancelOpen"
        class="common-modal-backdrop"
        @click.self="!cancelSubmitting && (cancelOpen = false)"
      >
        <form
          class="common-modal claim-request-modal"
          @submit.prevent="submitCancelOrder"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            :disabled="cancelSubmitting"
            @click="cancelOpen = false"
          >
            ×
          </button>
          <small>ORDER CANCELLATION</small>
          <h2>주문 취소를 요청할까요?</h2>
          <p>취소 사유를 확인한 뒤 결제 취소와 환불 절차가 진행됩니다.</p>
          <label
            >취소 사유<textarea
              v-model.trim="cancelReason"
              rows="5"
              maxlength="500"
              required
              placeholder="취소 사유를 입력해주세요."
            />
          </label>
          <div class="common-modal-actions">
            <button
              type="button"
              class="secondary"
              :disabled="cancelSubmitting"
              @click="cancelOpen = false"
            >
              취소
            </button>
            <button
              class="primary"
              :disabled="cancelSubmitting || !cancelReason.trim()"
            >
              {{ cancelSubmitting ? "요청 중..." : "취소 요청" }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>
    <Teleport to="body"
      ><div
        v-if="verifyOpen"
        class="common-modal-backdrop"
        @click.self="verifyOpen = false"
      >
        <form
          class="common-modal password-verify-modal"
          @submit.prevent="verifyPassword"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="verifyOpen = false"
          >
            ×
          </button>
          <div class="modal-status-icon lock">
            <svg viewBox="0 0 24 24">
              <rect x="5" y="10" width="14" height="11" rx="2" />
              <path d="M8.5 10V7.5a3.5 3.5 0 0 1 7 0V10" />
            </svg>
          </div>
          <h2>비밀번호를 확인해주세요</h2>
          <p>회원정보 보호를 위해 현재 비밀번호를 다시 입력해주세요.</p>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="비밀번호 입력"
            autofocus
          /><small v-if="verifyError">{{ verifyError }}</small
          ><button
            type="button"
            class="password-reset-request-link"
            @click="
              verifyOpen = false;
              router.push({
                path: '/password-reset',
                query: { email: member?.email || '', userId: effectiveUserId },
              });
            "
          >
            비밀번호를 잊으셨나요? 초기화 요청하기
          </button>
          <div class="common-modal-actions">
            <button type="button" class="secondary" @click="verifyOpen = false">
              취소</button
            ><button class="primary" :disabled="verifying">
              {{ verifying ? "확인 중..." : "확인" }}
            </button>
          </div>
        </form>
      </div></Teleport
    >
    <Teleport to="body">
      <div
        v-if="orderDetailOpen"
        class="common-modal-backdrop"
        @click.self="orderDetailOpen = false"
      >
        <section class="common-modal order-detail-modal order-detail-sheet">
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="orderDetailOpen = false"
          >
            ×
          </button>
          <small>ORDER DETAIL</small>
          <h2>주문 상세정보</h2>
          <div class="order-detail-number">
            <span>{{
              String(selectedOrder?.createdAt || "")
                .slice(0, 16)
                .replace("T", " ")
            }}</span>
            <b>주문번호 {{ selectedOrder?.orderNumber }}</b>
          </div>
          <section class="order-detail-block">
            <header>
              <h3>주문상품</h3>
              <span>{{
                statusLabels[selectedOrder?.status] || selectedOrder?.status
              }}</span>
            </header>
            <article
              v-for="item in selectedOrder?.orderItems ||
              selectedOrder?.items ||
              []"
              :key="item.id"
              class="order-detail-product"
            >
              <RouterLink
                class="order-detail-product-link"
                :to="productDetailPath(item)"
                @click="orderDetailOpen = false"
              >
                <img
                  :src="item.imageUrl || item.image"
                  :alt="item.productName"
                />
              </RouterLink>
              <div>
                <h4>
                  <RouterLink
                    :to="productDetailPath(item)"
                    @click="orderDetailOpen = false"
                    >{{ item.productName }}</RouterLink
                  >
                </h4>
                <p>
                  {{ item.optionLabel || "기본 옵션" }} · 수량
                  {{ item.quantity || 1 }}개
                </p>
                <b>{{ Number(item.unitPrice || 0).toLocaleString() }}원</b>
              </div>
              <div class="order-detail-product-actions">
                <button type="button" @click="repurchaseOrderItem(item)">
                  장바구니 담기
                </button>
                <button type="button" @click="notifyDirectPaymentPending">
                  바로 결제하기
                </button>
                <button
                  type="button"
                  :disabled="
                    !['DELIVERED', 'CONFIRMED'].includes(selectedOrder?.status)
                  "
                  @click="openReview(item)"
                >
                  리뷰 작성
                </button>
              </div>
            </article>
          </section>
          <section class="order-detail-block compact">
            <header><h3>배송지</h3></header>
            <dl>
              <dt>받는 분</dt>
              <dd>{{ selectedOrder?.recipientName }}</dd>
              <dt>연락처</dt>
              <dd>
                {{ selectedOrder?.recipientPhone || "주문 정보 확인 필요" }}
              </dd>
              <dt>주소</dt>
              <dd>
                [{{ selectedOrder?.deliveryZipcode || "-" }}]
                {{ selectedOrder?.deliveryAddress1 }}
                {{ selectedOrder?.deliveryAddress2 }}
              </dd>
              <dt>배송 요청</dt>
              <dd>{{ selectedOrder?.deliveryMessage || "없음" }}</dd>
            </dl>
          </section>
          <section class="order-detail-block compact">
            <header><h3>결제정보</h3></header>
            <dl>
              <dt>상품 금액</dt>
              <dd>
                {{ Number(selectedOrder?.totalAmount || 0).toLocaleString() }}원
              </dd>
              <dt>할인 금액</dt>
              <dd>
                -{{
                  Number(selectedOrder?.discountAmount || 0).toLocaleString()
                }}원
              </dd>
              <dt>배송비</dt>
              <dd>
                {{ Number(selectedOrder?.shippingFee || 0).toLocaleString() }}원
              </dd>
              <dt>총 결제 금액</dt>
              <dd>
                <strong
                  >{{
                    Number(selectedOrder?.finalAmount || 0).toLocaleString()
                  }}원</strong
                >
              </dd>
              <dt>결제 수단</dt>
              <dd>
                {{
                  selectedOrder?.paymentMethod ||
                  selectedOrder?.payments?.[0]?.paymentMethod ||
                  "결제 정보 확인 필요"
                }}
              </dd>
            </dl>
          </section>
        </section>
      </div>
    </Teleport>
    <Teleport to="body">
      <div
        v-if="reviewOpen"
        class="common-modal-backdrop"
        @click.self="reviewOpen = false"
      >
        <form
          class="common-modal review-write-modal"
          @submit.prevent="submitReview"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="reviewOpen = false"
          >
            ×
          </button>
          <small>PURCHASE REVIEW</small>
          <h2>리뷰 작성</h2>
          <p>{{ reviewItem?.productName }}</p>
          <fieldset class="review-rating-field">
            <legend>별점</legend>
            <!-- # 별을 직접 선택해 드롭다운보다 빠르고 직관적으로 평점을 입력한다. -->
            <div
              class="review-rating-stars"
              role="radiogroup"
              aria-label="별점 선택"
            >
              <button
                v-for="score in [1, 2, 3, 4, 5]"
                :key="score"
                type="button"
                :class="{ selected: score <= reviewRating }"
                :aria-label="`${score}점`"
                :aria-checked="score === reviewRating"
                role="radio"
                @click="reviewRating = score"
              >
                ★
              </button>
              <strong>{{ reviewRating }}점</strong>
            </div>
          </fieldset>
          <label
            >리뷰 내용
            <textarea
              v-model="reviewContent"
              rows="5"
              placeholder="상품에 대한 리뷰를 남겨쥬세요."
            ></textarea>
          </label>
          <div class="common-modal-actions">
            <button type="button" class="secondary" @click="reviewOpen = false">
              취소
            </button>
            <button class="primary" :disabled="reviewSubmitting">
              {{ reviewSubmitting ? "등록 중..." : "등록" }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>
    <Transition name="toast">
      <div v-if="actionMessage" class="site-toast" role="status">
        {{ actionMessage }}
      </div>
    </Transition>
  </main>
</template>
