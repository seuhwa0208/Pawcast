<!-- 토스페이먼츠 리다이렉트 결과를 백엔드에서 최종 승인하고 성공·실패 상태를 안내합니다. -->
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
const isGuestOrder = computed(() => !memberOrder.value);

// 조회 API가 준비되기 전에도 비회원이 이메일과 함께 보관할 수 있도록 주문번호 복사를 제공합니다.
async function copyOrderNumber() {
  if (!orderNumber.value) return;
  try {
    await navigator.clipboard.writeText(orderNumber.value);
    copyState.value = "주문번호가 복사되었습니다.";
  } catch {
    copyState.value = `주문번호: ${orderNumber.value}`;
  }
}

// PG 성공 리다이렉트도 서버 승인에 성공해야만 최종 결제완료로 표시합니다.
onMounted(async () => {
  if (route.query.status !== "success") {
    state.value = "fail";
    message.value =
      route.query.message || "결제가 취소되었거나 인증에 실패했습니다.";
    return;
  }
  try {
    const context = JSON.parse(
      sessionStorage.getItem("pawcast-payment-context") || "{}",
    );
    orderNumber.value = context.orderNumber || route.query.orderId || "";
    orderEmail.value = context.orderEmail || "";
    memberOrder.value = Boolean(context.isMember);
    if (!context.orderId || !context.paymentId) {
      throw new Error("저장된 결제 준비 정보를 찾지 못했습니다.");
    }
    await paymentService.confirmPayment(
      context.orderId,
      context.paymentId,
      {
        paymentKey: route.query.paymentKey,
        orderId: route.query.orderId,
        amount: Number(route.query.amount),
      },
      context.orderAccessToken,
    );
    sessionStorage.removeItem("pawcast-payment-context");
    if (context.mode !== "buy-now") clear();
    sessionStorage.removeItem("pawcast-buy-now");
    state.value = "success";
    message.value = "결제가 완료되었습니다.";
  } catch (error) {
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
      <small>PAWCAST PAYMENT</small>
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
      <div v-if="state !== 'loading'" class="payment-result-actions">
        <button
          v-if="state === 'success' && memberOrder"
          type="button"
          @click="router.push('/mypage#orders')"
        >
          주문 내역 보기
        </button>
        <button
          v-else-if="state === 'success' && isGuestOrder"
          type="button"
          @click="copyOrderNumber"
        >
          주문번호 복사
        </button>
        <button v-else type="button" @click="router.push('/checkout')">
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
