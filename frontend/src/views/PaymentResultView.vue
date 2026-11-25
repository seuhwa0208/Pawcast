<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useCart } from "@/stores/cart";
import { paymentService } from "@/services/paymentService";

const route = useRoute();
const router = useRouter();
const { clear } = useCart();

const state = ref("loading");
const message = ref("결제 결과를 확인하고 있습니다.");
const orderNumber = ref("");
const orderEmail = ref("");
const memberOrder = ref(false);
const copyState = ref("");
const retryMode = ref("cart");
const isGuestOrder = computed(() => !memberOrder.value);

async function copyOrderNumber() {
  if (!orderNumber.value) return;
  try {
    await navigator.clipboard.writeText(orderNumber.value);
    copyState.value = "주문번호가 복사되었습니다.";
  } catch {
    copyState.value = `주문번호: ${orderNumber.value}`;
  }
}

function retryPayment() {
  if (retryMode.value === "buy-now" && sessionStorage.getItem("powcast-buy-now")) {
    router.push({ path: "/checkout", query: { mode: "buy-now" } });
    return;
  }

  const hasSelected = Boolean(sessionStorage.getItem("powcast-selected-checkout"));
  router.push(
    hasSelected
      ? { path: "/checkout", query: { mode: "selected" } }
      : "/checkout"
  );
}

onMounted(async () => {
  const savedContext = JSON.parse(
    sessionStorage.getItem("powcast-payment-context") || "{}",
  );
  retryMode.value = savedContext.mode || "cart";

  if (route.query.status !== "success") {
    state.value = "fail";
    message.value =
      route.query.message || "결제가 취소되었거나 인증에 실패했습니다.";
    return;
  }

  try {
    orderNumber.value = savedContext.orderNumber || route.query.orderId || "";
    orderEmail.value = savedContext.orderEmail || "";
    memberOrder.value = Boolean(savedContext.isMember);

    if (!savedContext.orderId) {
      throw new Error("저장된 주문 정보를 찾지 못했습니다.");
    }

    await paymentService.confirmPayment(
      savedContext.orderId,
      {
        paymentKey: route.query.paymentKey,
        orderNumber: route.query.orderId,
        amount: Number(route.query.amount),
      },
      savedContext.orderAccessToken,
    );

    if (savedContext.mode !== "buy-now") {
      await clear();
    }
    sessionStorage.removeItem("powcast-payment-context");
    sessionStorage.removeItem("powcast-buy-now");
    sessionStorage.removeItem("powcast-selected-checkout");

    state.value = "success";
    message.value = "결제가 완료되었습니다.";
  } catch (error) {
    // 결제 승인 실패 시 장바구니와 선택 목록을 비우지 않아 상품이 그대로 남도록 둡니다.
    state.value = "fail";
    message.value = error.message || "결제 승인에 실패했습니다.";
  }
});
</script>

<template>
  <main class="container payment-result-page">
    <section class="payment-result-card">
      <div :class="['payment-result-icon', state]">
        {{ state === "loading" ? "…" : state === "success" ? "✓" : "!" }}
      </div>
      <small>POWCAST PAYMENT</small>
      <h1>
        {{
          state === "loading"
            ? "결제 확인 중"
            : state === "success"
              ? "주문이 완료되었습니다"
              : "결제를 완료하지 못했습니다"
        }}
      </h1>
      <p>{{ message }}</p>
      <dl
        v-if="state === 'success' && (orderNumber || orderEmail)"
        class="payment-result-order-info"
      >
        <dt>주문번호</dt>
        <dd>{{ orderNumber }}</dd>
        <dt v-if="orderEmail">주문 이메일</dt>
        <dd v-if="orderEmail">{{ orderEmail }}</dd>
      </dl>
      <div
        v-if="state === 'success' && isGuestOrder"
        class="payment-result-actions payment-result-guest-actions"
      >
        <button
          type="button"
          class="secondary"
          @click="copyOrderNumber"
        >
          주문번호 복사
        </button>
        <button type="button" class="secondary" @click="router.push('/')">
          홈으로 이동
        </button>
        <button
          type="button"
          class="payment-result-lookup"
          @click="router.push({ path: '/guest-orders', query: { email: orderEmail, orderNumber } })"
        >
          비회원 주문 조회
        </button>
      </div>
      <div
        v-else-if="state !== 'loading'"
        class="payment-result-actions"
      >
        <button
          v-if="state === 'success' && memberOrder"
          type="button"
          @click="router.push('/mypage#orders')"
        >
          주문 내역 보기
        </button>
        <button v-else type="button" @click="retryPayment">
          결제 다시 시도
        </button>
        <button type="button" class="secondary" @click="router.push('/')">
          홈으로 이동
        </button>
      </div>
      <p v-if="copyState" class="payment-copy-message" role="status">
        {{ copyState }}
      </p>
    </section>
  </main>
</template>
