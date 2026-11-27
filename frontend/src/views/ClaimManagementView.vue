<!-- 주문 API와 환불 API를 사용해 고객이 취소·반품 요청 현황을 직접 관리하는 화면입니다. -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import MemberSidebar from "@/components/MemberSidebar.vue";
import { useAuthSession } from "@/stores/auth";
import { orderService } from "@/services/orderService";
import PaginationBar from "@/components/PaginationBar.vue";

const router = useRouter();
const { currentUserId } = useAuthSession();
const displayName = computed(() => currentUserId.value || "포캐스트 회원");
const orders = ref([]);
const refunds = ref({});
const loading = ref(false);
const error = ref("");
const refundOpen = ref(false);
const refundOrder = ref(null);
const refundReason = ref("");
const refundSubmitting = ref(false);
const message = ref("");
const page = ref(1);
const CLAIM_PAGE_SIZE = 5;
const claimOrders = computed(() =>
  orders.value.filter((order) =>
    refunds.value[order.orderId]
    || ["DELIVERED", "CONFIRMED"].includes(order.status),
  ),
);
const claimPageCount = computed(() =>
  Math.max(1, Math.ceil(claimOrders.value.length / CLAIM_PAGE_SIZE)),
);
const pagedClaimOrders = computed(() => {
  const start = (page.value - 1) * CLAIM_PAGE_SIZE;
  return claimOrders.value.slice(start, start + CLAIM_PAGE_SIZE);
});
watch(claimPageCount, (count) => {
  if (page.value > count) page.value = count;
});
function displayDate(value) {
  return String(value || "-")
    .slice(0, 10)
    .replaceAll("-", ".");
}
function formatWon(value) {
  return `${Number(value || 0).toLocaleString()}원`;
}
function refundStatus(refund) {
  if (!refund) return "환불 신청 가능";
  const labels = {
    PENDING: "승인 대기",
    COMPLETED: "환불 완료",
    REJECTED: "환불 거절",
    FAILED: "처리 실패",
  };
  return labels[refund.status] || refund.status;
}
async function loadClaims() {
  loading.value = true;
  error.value = "";
  try {
    const response = await orderService.getMyOrders();
    orders.value = Array.isArray(response)
      ? response
      : response.content || response.items || [];
    const results = await Promise.all(
      orders.value.map(async (order) => {
        try {
          return [order.orderId, await orderService.getRefund(order.orderId)];
        } catch {
          return [order.orderId, null];
        }
      }),
    );
    refunds.value = Object.fromEntries(results);
    page.value = 1;
  } catch (cause) {
    orders.value = [];
    error.value = cause.message || "취소·환불 내역을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
function openRefundRequest(order) {
  refundOrder.value = order;
  refundReason.value = "";
  refundOpen.value = true;
}
async function submitRefundRequest() {
  if (!refundOrder.value || !refundReason.value.trim()) return;
  try {
    refundSubmitting.value = true;
    await orderService.requestRefund(
      refundOrder.value.orderId,
      refundReason.value.trim(),
    );
    refundOpen.value = false;
    message.value =
      "환불 요청이 접수되었습니다. 관리자가 사유를 확인한 뒤 처리합니다.";
    await loadClaims();
  } catch (cause) {
    error.value = cause.message || "환불 요청을 접수하지 못했습니다.";
  } finally {
    refundSubmitting.value = false;
  }
}
onMounted(loadClaims);
</script>

<template>
  <main class="container member-dashboard claim-management-page">
    <header class="dashboard-heading">
      <span>POWCAST ORDER CARE</span>
      <h1>취소·반품(환불)</h1>
      <p>주문 취소와 환불 처리 정보를 확인하세요.</p>
    </header>
    <div class="dashboard-layout">
      <MemberSidebar
        :name="displayName"
        :user-id="currentUserId"
        active="claims"
        @settings="router.push('/mypage?verify=1')"
      />
      <section class="dashboard-content claim-content">
        <header class="claim-content-heading">
          <div>
            <h2>반품·환불 요청</h2>
            <p>
              배송 완료 또는 구매 확정 주문만 반품 환불을 신청할 수 있습니다.
            </p>
          </div>
        </header>
        <p v-if="loading" class="dashboard-order-state">
          내역을 불러오는 중입니다.
        </p>
        <p v-else-if="error" class="dashboard-order-state error">{{ error }}</p>
        <p v-else-if="message" class="claim-message">{{ message }}</p>
        <template v-else-if="claimOrders.length">
          <div class="claim-order-list">
          <article v-for="order in pagedClaimOrders" :key="order.orderId">
            <div>
              <b>{{ displayDate(order.created_at || order.createdAt) }} 주문</b>
              <span>{{
                order.orderNumber || `주문번호 ${order.orderId}`
              }}</span>
              <small>{{
                formatWon(order.final_amount || order.finalAmount)
              }}</small>
            </div>
            <div class="claim-order-action">
              <em :class="{ completed: refunds[order.orderId]?.status === 'COMPLETED' }">{{
                refundStatus(refunds[order.orderId])
              }}</em>
              <button
                v-if="!refunds[order.orderId]"
                type="button"
                @click="openRefundRequest(order)"
              >
                환불 요청
              </button>
            </div>
          </article>
          </div>
          <PaginationBar
            v-model:page="page"
            :total-pages="claimPageCount"
            label="취소·반품·교환 내역 페이지"
          />
        </template>
        <p v-else class="dashboard-order-state">
          취소·반품(환불) 내역이 없습니다.
        </p>
      </section>
    </div>
    <Teleport to="body">
      <div
        v-if="refundOpen"
        class="common-modal-backdrop"
        @click.self="!refundSubmitting && (refundOpen = false)"
      >
        <form
          class="common-modal claim-request-modal"
          @submit.prevent="submitRefundRequest"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            :disabled="refundSubmitting"
            @click="refundOpen = false"
          >
            ×
          </button>
          <small>RETURN &amp; REFUND</small>
          <h2>반품·환불 요청</h2>
          <p>관리자가 사유와 주문 상태를 확인한 뒤 환불을 확정합니다.</p>
          <label
            >환불 사유<textarea
              v-model.trim="refundReason"
              rows="11"
              maxlength="500"
              required
              placeholder="환불 사유를 입력해주세요."
            />
          </label>
          <div class="common-modal-actions">
            <button
              type="button"
              class="secondary"
              :disabled="refundSubmitting"
              @click="refundOpen = false"
            >
              취소
            </button>
            <button
              class="primary"
              :disabled="refundSubmitting || !refundReason.trim()"
            >
              {{ refundSubmitting ? "요청 중..." : "환불 요청" }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>
  </main>
</template>
