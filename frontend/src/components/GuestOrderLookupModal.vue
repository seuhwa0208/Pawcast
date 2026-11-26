<!-- 비회원이 현재 페이지를 떠나지 않고 주문번호와 이메일로 주문을 확인하는 모달입니다. -->
<script setup>
import { ref, watch } from "vue";
import { orderService } from "@/services/orderService";

const props = defineProps({ open: Boolean });
const emit = defineEmits(["close"]);
const email = ref("");
const orderNumber = ref("");
const order = ref(null);
const loading = ref(false);
const error = ref("");
const statusLabel = { PENDING_PAYMENT: "결제 대기", PAID: "결제 완료", PREPARING: "배송 중", SHIPPING: "배송 중", DELIVERED: "배송 완료", CANCELLED: "주문 취소" };

watch(() => props.open, (opened) => { if (opened) { error.value = ""; order.value = null; } });
function close() { if (!loading.value) emit("close"); }
function amount(value) { return `${Number(value || 0).toLocaleString("ko-KR")}원`; }
function date(value) { return String(value || "").slice(0, 10).replaceAll("-", "."); }
async function searchOrder() {
  error.value = ""; order.value = null;
  if (!email.value.trim() || !orderNumber.value.trim()) { error.value = "주문 이메일과 주문번호를 모두 입력해주세요."; return; }
  try { loading.value = true; order.value = await orderService.getGuestOrder({ email: email.value.trim(), orderNumber: orderNumber.value.trim() }); }
  catch (cause) { error.value = cause.message || "일치하는 주문을 찾지 못했습니다."; }
  finally { loading.value = false; }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="guest-modal-backdrop" role="presentation" @click.self="close">
      <section class="guest-modal" role="dialog" aria-modal="true" aria-labelledby="guest-order-title">
        <button class="close" type="button" aria-label="비회원 주문 조회 닫기" :disabled="loading" @click="close">×</button>
        <small>GUEST ORDER LOOKUP</small><h2 id="guest-order-title">비회원 주문 조회</h2><p>결제 시 입력한 이메일과 안내 메일의 주문번호를 입력해주세요.</p>
        <form @submit.prevent="searchOrder"><label>주문 이메일<input v-model.trim="email" type="email" autocomplete="email" placeholder="example@email.com" required /></label><label>주문번호<input v-model.trim="orderNumber" autocomplete="off" placeholder="메일로 받은 주문번호" required /></label><button class="lookup" type="submit" :disabled="loading">{{ loading ? "조회 중..." : "주문 조회" }}</button></form>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <section v-if="order" class="result" aria-live="polite"><header><div><small>{{ date(order.created_at || order.createdAt) }} 주문</small><b>주문번호 {{ order.orderNumber }}</b></div><em>{{ statusLabel[order.status] || order.status }}</em></header><article v-for="item in order.items || []" :key="item.orderItemId"><div><b>{{ item.productName }}</b><span v-if="item.optionLabel">옵션: {{ item.optionLabel }}</span><small>수량 {{ item.quantity }}개</small></div><strong>{{ amount(item.totalPrice) }}</strong></article><dl><dt>최종 결제 금액</dt><dd>{{ amount(order.final_amount || order.finalAmount) }}</dd><dt>배송지</dt><dd>{{ order.recipient_name || order.recipientName }} · {{ order.recipient_phone || order.recipientPhone }}</dd></dl></section>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.guest-modal-backdrop{position:fixed;z-index:1000;inset:0;display:grid;place-items:center;background:rgba(43,30,22,.5);padding:20px}.guest-modal{position:relative;width:min(100%,560px);max-height:calc(100dvh - 40px);overflow-y:auto;border-radius:20px;background:#fffdfb;padding:30px;color:#3d281b;box-shadow:0 24px 60px rgba(0,0,0,.24)}.guest-modal>.close{position:absolute;top:14px;right:16px;border:0;background:transparent;color:#876e5a;font-size:28px;cursor:pointer}.guest-modal>small,.result header small{color:#a66838;font-weight:900;letter-spacing:.1em}.guest-modal h2{margin:7px 0;font-size:26px}.guest-modal>p{margin:0;color:#806b5c}.guest-modal form{display:grid;gap:12px;margin-top:22px}.guest-modal label{display:grid;gap:6px;color:#674630;font-size:14px;font-weight:800}.guest-modal input{border:1px solid #dcc7b6;border-radius:9px;padding:12px;font:inherit}.lookup{border:0;border-radius:9px;background:#a66b3f;padding:13px;color:#fff;font:inherit;font-weight:900;cursor:pointer}.lookup:disabled,.close:disabled{cursor:wait;opacity:.55}.error{margin-top:14px!important;color:#b94e41!important;font-size:14px}.result{display:grid;gap:11px;margin-top:22px;border-top:1px solid #eadfd5;padding-top:18px}.result header,.result article{display:flex;align-items:center;justify-content:space-between;gap:12px}.result header div,.result article div{display:grid;gap:3px}.result header b,.result article b{font-size:15px}.result article span,.result article small{color:#876f5e;font-size:13px}.result header em{border-radius:99px;background:#e4f0e1;padding:6px 9px;color:#527a4c;font-size:12px;font-style:normal;font-weight:800}.result article{border-top:1px solid #f1e8e1;padding-top:11px}.result strong{white-space:nowrap;color:#9d5f2c}.result dl{display:grid;grid-template-columns:120px 1fr;gap:9px;margin:4px 0 0;border-radius:10px;background:#fff7ef;padding:13px;font-size:13px}.result dt{color:#836957}.result dd{margin:0;font-weight:700}@media(max-width:540px){.guest-modal{padding:26px 20px}.result header,.result article{align-items:flex-start;flex-direction:column}.result dl{grid-template-columns:1fr}}
</style>
