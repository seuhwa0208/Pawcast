<!-- 주문 현황과 회원 활동을 요약하고 비밀번호 재확인 흐름을 여는 마이페이지 대시보드입니다. -->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";
import { useCart } from "@/stores/cart";
import { useWishlist } from "@/stores/wishlist";
import { orderService } from "@/services/orderService";
import { cartService } from "@/services/cartService";
import { reviewService } from "@/services/reviewService";
import { getMyProfile, verifyMyPassword } from "@/services/memberService";
import { paymentService } from "@/services/paymentService";
import MemberSidebar from "@/components/MemberSidebar.vue";
import PaginationBar from "@/components/PaginationBar.vue";
import ReviewEditModal from "@/components/ReviewEditModal.vue";

const route = useRoute();
const router = useRouter();
const { accessToken, currentUserId } = useAuthSession();
const { count: cartCount, add: addLocalCart } = useCart();
const wishlistStore = useWishlist();
const effectiveUserId = computed(() => currentUserId.value);
const profileName = ref("");
const isGeneratedSocialId = (value) =>
  /^(google|kakao|naver)_[0-9]{8,}$/i.test(String(value || ""));
const displayName = computed(() => {
  const name = String(profileName.value || "").trim();
  if (name && !isGeneratedSocialId(name)) return name;
  const userId = String(effectiveUserId.value || "").trim();
  return userId && !isGeneratedSocialId(userId) ? userId : "포캐스트 회원";
});
const verifyOpen = ref(false);
const orders = ref([]);
const ordersLoading = ref(false);
const ordersError = ref("");
const selectedPeriodMonths = ref(12);
const appliedPeriodMonths = ref(12);
const orderPeriodOpen = ref(false);
const orderPage = ref(1);
const orderPageSize = 4;
const selectedOrder = ref(null);
const orderDetailOpen = ref(false);
const expandedOrderIds = ref([]);
const actionMessage = ref("");
const reviewOpen = ref(false);
const reviewItem = ref(null);
const reviewRating = ref(5);
const reviewContent = ref("");
const reviewSubmitting = ref(false);
const reviewImageInput = ref(null);
const reviewImages = ref([]);

// 리뷰 수정 모달 상태 관리
const reviewEditOpen = ref(false);
const editingReview = ref(null);
const editingProductName = ref("");
const reviewEditLoading = ref(false);

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
  PREPARING: "배송 중",
  SHIPPING: "배송중",
  DELIVERED: "배송 완료",
  CONFIRMED: "구매 확정",
  CANCELLED: "주문 취소",
};
const orderPeriodOptions = [
  { value: 3, label: "최근 3개월" },
  { value: 6, label: "최근 6개월" },
  { value: 12, label: "최근 1년" },
  { value: 24, label: "최근 2년" },
  { value: 0, label: "전체 기간" },
];
const appliedPeriodLabel = computed(
  () => orderPeriodOptions.find((item) => item.value === appliedPeriodMonths.value)?.label || "전체 기간",
);
const fallbackOrderImage = "/assets/products/generated/dry-food.png";
const TOSS_CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY || "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
function orderDateValue(order) {
  const raw = order.createdAt || order.created_at || order.orderDate;
  const date = raw ? new Date(raw) : null;
  return date && !Number.isNaN(date.valueOf()) ? date : null;
}
function orderImageSrc(item) {
  const value = item?.image || item?.imageUrl;
  if (!value || /^[A-Za-z]:\\/.test(value)) return fallbackOrderImage;
  if (/^https?:\/\//.test(value) || value.startsWith("/")) return value;
  return `/${value.replace(/^\.\//, "")}`;
}
function replaceBrokenOrderImage(event) {
  const image = event.currentTarget;
  if (image.dataset.fallbackApplied) return;
  image.dataset.fallbackApplied = "true";
  image.src = fallbackOrderImage;
}
const filteredOrders = computed(() => {
  const months = Number(appliedPeriodMonths.value);
  if (!months) return orders.value;
  const boundary = new Date();
  boundary.setMonth(boundary.getMonth() - months);
  boundary.setHours(0, 0, 0, 0);
  return orders.value.filter((order) => {
    const createdAt = orderDateValue(order);
    return createdAt && createdAt >= boundary;
  });
});
const totalOrderPages = computed(() =>
  Math.max(1, Math.ceil(filteredOrders.value.length / orderPageSize)),
);
const paginatedOrders = computed(() => {
  const start = (orderPage.value - 1) * orderPageSize;
  return filteredOrders.value.slice(start, start + orderPageSize);
});
function changeOrderPeriod() {
  appliedPeriodMonths.value = selectedPeriodMonths.value;
  orderPage.value = 1;
}
const orderCards = computed(() =>
  paginatedOrders.value.map((order) => {
    const orderItems = order.items || order.orderItems || [];
    const firstItem = orderItems[0] || {};
    return {
      ...order,
      id: order.orderId ?? order.id,
      orderItems: orderItems.map((item) => ({
        ...item,
        id: item.id ?? item.orderItemId,
        variantId: item.variantId ?? item.productVariantId,
        image: item.image || item.imageUrl || "/assets/products/generated/dry-food.png",

        // 백엔드에서 반환한 주문 항목별 리뷰 ID
        reviewId: item.reviewId ?? null,
      })),
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
  filteredOrders.value.reduce((counts, order) => {
    counts[order.status] = (counts[order.status] || 0) + 1;
    return counts;
  }, {}),
);

onMounted(async () => {
  if (route.query.verify === "1") verifyOpen.value = true;
  await Promise.all([loadOrders(), loadProfileName()]);
});
async function loadProfileName() {
  try {
    const profile = await getMyProfile();
    profileName.value = profile?.name || profile?.username || profile?.nickname || "";
  } catch {
    profileName.value = "";
  }
}
async function loadOrders() {
  try {
    ordersLoading.value = true;
    ordersError.value = "";
    const result = await orderService.getMyOrders();
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
// 리뷰 작성 가능 여부 확인
function canWriteReview(status) {
    return status === "DELIVERED";
}

// 리뷰 작성 또는 수정 버튼 처리
async function handleReviewAction(item, status) {
    if (reviewEditLoading.value || reviewSubmitting.value) {
        return;
    }

    if (!accessToken.value) {
        showActionMessage("로그인이 필요한 서비스입니다.");
        return;
    }

    // 이미 작성한 리뷰가 존재하면 수정 모달 열기
    if (item.reviewId != null) {
        await openReviewEdit(item);
        return;
    }

    // 배송 완료된 주문만 리뷰 작성 가능
    if (!canWriteReview(status)) {
        showActionMessage("배송 완료 후 리뷰를 작성할 수 있습니다.");
        return;
    }

    openReview(item);
}

// 신규 리뷰 작성 모달 열기
function openReview(item) {
    reviewItem.value = {
        ...item,
        id: item.id ?? item.orderItemId,
    };

    reviewRating.value = 5;
    reviewContent.value = "";

    clearReviewImages();

    reviewOpen.value = true;
}

// 기존 리뷰 조회 및 수정 모달 열기
async function openReviewEdit(item) {
    if (reviewEditLoading.value) {
        return;
    }

    try {
        reviewEditLoading.value = true;

        const review = await reviewService.getReview(
            item.reviewId
        );

        // 본인이 작성한 리뷰인지 확인
        if (
            !accessToken.value ||
            !currentUserId.value ||
            review?.writerId !== currentUserId.value
        ) {
            showActionMessage("본인이 작성한 리뷰만 수정할 수 있습니다.");
            return;
        }

        editingReview.value = review;
        editingProductName.value = item.productName || "주문 상품";

        reviewEditOpen.value = true;
    } catch (error) {
        showActionMessage(
            error.message || "리뷰 정보를 불러오지 못했습니다."
        );
    } finally {
        reviewEditLoading.value = false;
    }
}

// 리뷰 수정 모달 닫기
function closeReviewEdit() {
    reviewEditOpen.value = false;
    editingReview.value = null;
    editingProductName.value = "";
}

// 리뷰 변경 후 주문 목록 및 주문 상세 정보 갱신
async function refreshReviewOrders() {
    await loadOrders();

    if (orderDetailOpen.value && selectedOrder.value) {
        const orderId =
            selectedOrder.value.orderId ??
            selectedOrder.value.id;

        if (orderId != null) {
            try {
                const result = await orderService.getOrder(orderId);

                if (
                    orderDetailOpen.value &&
                    (selectedOrder.value?.orderId ??
                        selectedOrder.value?.id) === orderId
                ) {
                    selectedOrder.value = result;
                }
            } catch {
                showActionMessage(
                    "주문 상세 정보를 갱신하지 못했습니다."
                );
            }
        }
    }
}

// 리뷰 수정 완료 처리
async function handleReviewUpdated() {
    closeReviewEdit();

    await refreshReviewOrders();

    showActionMessage("리뷰가 수정되었습니다.");
}
function clearReviewImages() {
  reviewImages.value.forEach((item) => URL.revokeObjectURL(item.previewUrl));
  reviewImages.value = [];
  if (reviewImageInput.value) reviewImageInput.value.value = "";
}
function closeReview() {
  if (reviewSubmitting.value) return;
  reviewOpen.value = false;
  clearReviewImages();
}
function onReviewImagesChange(event) {
  for (const file of Array.from(event.target.files || [])) {
    const extension = file.name.split(".").pop()?.toLowerCase();
    if (!["jpg", "jpeg", "png", "webp"].includes(extension) || file.size === 0 || file.size > 5 * 1024 * 1024) {
      showActionMessage("리뷰 이미지는 JPG, JPEG, PNG, WebP 형식이며 장당 5MB 이하여야 합니다.");
      continue;
    }
    if (reviewImages.value.length >= 5) {
      showActionMessage("리뷰 이미지는 최대 5장까지 등록할 수 있습니다.");
      break;
    }
    reviewImages.value.push({ file, previewUrl: URL.createObjectURL(file) });
  }
  if (reviewImageInput.value) reviewImageInput.value.value = "";
}
function removeReviewImage(index) {
  const [item] = reviewImages.value.splice(index, 1);
  if (item) URL.revokeObjectURL(item.previewUrl);
}
async function submitReview() {
    if (reviewSubmitting.value) {
        return;
    }

    if (!reviewContent.value.trim()) {
        showActionMessage("리뷰 내용을 입력해주세요.");
        return;
    }

    const orderItemId =
        reviewItem.value?.id ??
        reviewItem.value?.orderItemId;

    if (orderItemId == null) {
        showActionMessage("주문 상품 정보를 확인할 수 없습니다.");
        return;
    }

    try {
        reviewSubmitting.value = true;

        await reviewService.create({
            orderItemId,
            rating: reviewRating.value,
            content: reviewContent.value.trim(),
            images: reviewImages.value.map((item) => item.file),
        });

        // 제출 상태 해제 후 리뷰 작성 모달 닫기
        reviewSubmitting.value = false;

        closeReview();

        // 리뷰 등록 후 주문 목록 및 상세 정보 갱신
        await refreshReviewOrders();

        showActionMessage("리뷰가 등록되었습니다.");
    } catch (error) {
        showActionMessage(
            error.message || "리뷰를 등록하지 못했습니다."
        );
    } finally {
        reviewSubmitting.value = false;
    }
}
onBeforeUnmount(clearReviewImages);
function loadTossSdk() {
  if (window.TossPayments) return Promise.resolve(window.TossPayments);
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://js.tosspayments.com/v2/standard";
    script.onload = () => resolve(window.TossPayments);
    script.onerror = () => reject(new Error("토스 결제 SDK를 불러오지 못했습니다."));
    document.head.appendChild(script);
  });
}
function tossCustomerKey() {
  const userId = String(effectiveUserId.value || "member").replace(/[^A-Za-z0-9\-._=@]/g, "") || "member";
  const storageKey = `powcast-toss-customer-key:${userId}`;
  const saved = localStorage.getItem(storageKey);
  if (saved) return saved;
  const key = `powcast_${userId}_${crypto.randomUUID()}`.slice(0, 50);
  localStorage.setItem(storageKey, key);
  return key;
}
async function resumePendingPayment(order) {
  if (order.status !== "PENDING_PAYMENT") {
    showActionMessage("결제 대기 주문만 결제를 다시 진행할 수 있습니다.");
    return;
  }
  const orderId = order.orderId || order.id;
  const orderNumber = order.orderNumber;
  const amount = Number(order.final_amount ?? order.finalAmount ?? 0);
  if (!orderId || !orderNumber || amount <= 0) {
    showActionMessage("결제에 필요한 주문 정보를 찾지 못했습니다.");
    return;
  }
  try {
    sessionStorage.setItem("powcast-payment-context", JSON.stringify({
      orderId,
      orderNumber,
      orderEmail: order.recipient_email || order.recipientEmail || "",
      isMember: true,
      mode: "resume-order",
    }));
    const TossPayments = await loadTossSdk();
    const widgets = TossPayments(TOSS_CLIENT_KEY).widgets({ customerKey: tossCustomerKey() });
    await widgets.setAmount({ currency: "KRW", value: amount });
    const paymentWindow = await widgets.renderPaymentWindow({ orderName: order.orderName || "POWCAST 주문" });
    paymentWindow.on("paymentRequest", () => widgets.requestPayment({
      orderId: String(orderNumber),
      orderName: order.orderName || "POWCAST 주문",
      customerName: order.recipient_name || order.recipientName || "고객",
      customerEmail: order.recipient_email || order.recipientEmail || undefined,
      successUrl: `${window.location.origin}/payment/result?status=success`,
      failUrl: `${window.location.origin}/payment/result?status=fail`,
    }));
  } catch (error) {
    showActionMessage(error.message || "결제를 시작하지 못했습니다.");
  }
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
              <p>{{ appliedPeriodLabel }} 주문 내역입니다.</p>
            </div>
            <div class="order-period-select">
              <span>조회 기간</span>
              <span class="order-period-control" :class="{ open: orderPeriodOpen }">
                <select v-model.number="selectedPeriodMonths" @focus="orderPeriodOpen = true" @blur="orderPeriodOpen = false">
                  <option v-for="period in orderPeriodOptions" :key="period.value" :value="period.value">
                    {{ period.label }}
                  </option>
                </select>
              </span>
              <button type="button" @click="changeOrderPeriod">적용</button>
            </div>
          </header>
          <div class="delivery-steps">
            <article
              v-for="(step, index) in [
                { key: 'PAID', label: '결제 완료' },
                { key: 'SHIPPING', label: '배송중' },
                { key: 'DELIVERED', label: '배송 완료' },
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
                ><div class="order-card-actions"><button
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
                </button></div>
              </header>
              <div class="order-product-list">
                <article
                  v-for="item in expandedOrderIds.includes(order.id)
                    ? order.orderItems
                    : order.orderItems.slice(0, 1)"
                  :key="item.id"
                  class="order-product-row"
                  @click="openOrderDetail(order.orderId || order.id)"
                >
                  <button
                    type="button"
                    class="order-product-image-link"
                    :aria-label="`${item.productName || '주문 상품'} 주문 상세 보기`"
                    @click.stop="openOrderDetail(order.orderId || order.id)"
                  >
                    <img
                      :src="orderImageSrc(item)"
                      :alt="item.productName || '주문 상품'"
                      @error="replaceBrokenOrderImage"
                    />
                  </button>
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
                    <button type="button" @click.stop="repurchaseOrderItem(item)">
                      장바구니 담기
                    </button>
                    <button v-if="order.status === 'PENDING_PAYMENT'" type="button" @click.stop="resumePendingPayment(order)">
                      바로 결제하기
                    </button>
                    <button
                      type="button"
                      :disabled="
                          reviewEditLoading ||
                          reviewSubmitting ||
                          (
                              item.reviewId == null &&
                              !canWriteReview(order.status)
                          )
                      "
                      :title="
                          item.reviewId != null
                              ? '리뷰 수정'
                              : canWriteReview(order.status)
                                  ? '리뷰 작성'
                                  : '배송 완료 후 리뷰를 작성할 수 있습니다.'
                      "
                      @click.stop="handleReviewAction(item, order.status)">
                        {{ item.reviewId != null ? "리뷰 수정" : "리뷰 작성" }}
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
          <PaginationBar
            v-if="filteredOrders.length"
            v-model:page="orderPage"
            :total-pages="totalOrderPages"
            label="주문 내역 페이지"
          />
          <p
            v-else
            class="dashboard-order-state"
            :class="{ error: ordersError }"
          >
            {{ ordersError || "선택한 기간의 주문 내역이 없습니다." }}
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
                :key="item.id ?? item.orderItemId"
                class="order-detail-product"
            >
              <RouterLink
                class="order-detail-product-link"
                :to="productDetailPath(item)"
                @click="orderDetailOpen = false"
              >
                <img
                  :src="orderImageSrc(item)"
                  :alt="item.productName"
                  @error="replaceBrokenOrderImage"
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
                <button v-if="selectedOrder?.status === 'PENDING_PAYMENT'" type="button" @click="resumePendingPayment(selectedOrder)">
                  바로 결제하기
                </button>
                <button
                  type="button"
                  :disabled="
                    !['DELIVERED', 'CONFIRMED'].includes(selectedOrder?.status)
                  "
                  :title="['DELIVERED', 'CONFIRMED'].includes(selectedOrder?.status) ? '리뷰 작성' : '배송 완료 후 리뷰를 작성할 수 있습니다.'"
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
        @click.self="closeReview"
      >
        <form
          class="common-modal review-write-modal"
          @submit.prevent="submitReview"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="closeReview"
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
          <section class="review-image-upload">
            <div class="review-image-upload-head">
              <div><b>사진 첨부</b><small>선택 · 최대 5장 / 장당 5MB</small></div>
              <label class="review-image-upload-button" for="review-images">사진 선택</label>
              <input id="review-images" ref="reviewImageInput" type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" multiple @change="onReviewImagesChange" />
            </div>
            <ul v-if="reviewImages.length" class="review-image-preview-list">
              <li v-for="(image,index) in reviewImages" :key="image.previewUrl"><img :src="image.previewUrl" :alt="image.file.name" /><button type="button" :aria-label="`${image.file.name} 삭제`" @click="removeReviewImage(index)">×</button></li>
            </ul>
          </section>
          <div class="common-modal-actions">
            <button type="button" class="secondary" @click="closeReview">
              취소
            </button>
            <button class="primary" :disabled="reviewSubmitting">
              {{ reviewSubmitting ? "등록 중..." : "등록" }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>
    
    <!-- 기존 리뷰 수정 모달 재사용 -->
    <ReviewEditModal
        v-if="reviewEditOpen && editingReview"
        :key="editingReview.id"
        :review="editingReview"
        :product-name="editingProductName"
        @close="closeReviewEdit"
        @updated="handleReviewUpdated"
    />

    <Transition name="toast">
        <div
            v-if="actionMessage"
            class="site-toast"
            role="status"
        >
            {{ actionMessage }}
        </div>
    </Transition>
</main>
</template>
