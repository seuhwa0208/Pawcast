<!-- 이메일과 주문번호가 모두 일치하는 비회원 주문만 공개 조회합니다. -->
<script setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { orderService } from "@/services/orderService";
import "@/styles/guest-order-lookup.css";

const route = useRoute();
const email = ref(String(route.query.email || ""));
const orderNumber = ref(String(route.query.orderNumber || ""));
const order = ref(null);
const loading = ref(false);
const error = ref("");

const statusLabel = {
  PENDING_PAYMENT: "결제 대기",
  PAID: "결제 완료",
  PREPARING: "배송 준비",
  SHIPPING: "배송 중",
  DELIVERED: "배송 완료",
  CANCELLED: "주문 취소",
};

function formatAmount(value) {
  return Number(value || 0).toLocaleString();
}

function formatDate(value) {
  return String(value || "").slice(0, 10).replaceAll("-", ".");
}

async function searchOrder() {
  error.value = "";
  order.value = null;
  if (!email.value || !orderNumber.value) {
    error.value = "주문 이메일과 주문번호를 모두 입력해주세요.";
    return;
  }
  try {
    loading.value = true;
    order.value = await orderService.getGuestOrder({
      email: email.value.trim(),
      orderNumber: orderNumber.value.trim(),
    });
  } catch (cause) {
    error.value = cause.message || "일치하는 주문을 찾지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="container guest-order-page">
    <header class="guest-order-heading">
      <small>GUEST ORDER LOOKUP</small>
      <h1>비회원 주문 조회</h1>
      <p>결제 시 입력한 이메일과 주문번호를 입력해주세요.</p>
    </header>

    <section class="guest-order-search-card">
      <h2>주문 정보 확인</h2>
      <form @submit.prevent="searchOrder">
        <label>주문 이메일<input v-model.trim="email" type="email" autocomplete="email" placeholder="example@email.com" required /></label>
        <label>주문번호<input v-model.trim="orderNumber" autocomplete="off" placeholder="메일로 받은 주문번호" required /></label>
        <button type="submit" :disabled="loading">{{ loading ? "조회 중..." : "주문 조회" }}</button>
      </form>
      <p class="guest-order-help">주문번호는 결제 완료 안내 이메일에서 확인할 수 있습니다.</p>
      <p v-if="error" class="guest-order-error" role="alert">{{ error }}</p>
    </section>

    <section v-if="order" class="guest-order-result" aria-live="polite">
      <header><div><small>{{ formatDate(order.created_at || order.createdAt) }} 주문</small><h2>주문번호 {{ order.orderNumber }}</h2></div><b>{{ statusLabel[order.status] || order.status }}</b></header>
      <div class="guest-order-items"><article v-for="item in order.items || []" :key="item.orderItemId"><div><strong>{{ item.productName }}</strong><span v-if="item.optionLabel">옵션: {{ item.optionLabel }}</span><small>수량 {{ item.quantity }}개</small></div><b>{{ formatAmount(item.totalPrice) }}원</b></article></div>
      <dl><dt>상품 금액</dt><dd>{{ formatAmount(order.total_amount || order.totalAmount) }}원</dd><dt>배송비</dt><dd>{{ formatAmount(order.shipping_fee || order.shippingFee) }}원</dd><dt>최종 결제 금액</dt><dd>{{ formatAmount(order.final_amount || order.finalAmount) }}원</dd></dl>
      <section class="guest-order-address"><h3>배송지</h3><p>{{ order.recipient_name || order.recipientName }} · {{ order.recipient_phone || order.recipientPhone }}</p><p>[{{ order.delivery_zipcode || order.deliveryZipcode }}] {{ order.delivery_address1 || order.deliveryAddress1 }} {{ order.delivery_address2 || order.deliveryAddress2 }}</p></section>
    </section>
  </main>
</template>
