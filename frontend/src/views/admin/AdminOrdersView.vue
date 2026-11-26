<!-- # 사이드바: 주문·배송 관리 - 실제 주문 조회, 건별 배송 상태 변경, 환불 승인 -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { adminManagementService } from "@/services/adminManagementService";
import PaginationBar from "@/components/PaginationBar.vue";

const orders = ref([]);
const refunds = ref([]);
const loading = ref(false);
const error = ref("");
const search = ref("");
const statusFilter = ref("ALL");
const orderPage = ref(1);
const refundPage = ref(1);
const refundSection = ref(null);
const openStatusMenuId = ref(null);
const refundActionId = ref(null);
const ORDER_PAGE_SIZE = 10;
const REFUND_PAGE_SIZE = 10;

const orderStatusOptions = [
  { value: "ALL", label: "전체" },
  { value: "PAID", label: "결제 완료" },
  { value: "SHIPPING", label: "배송 중" },
  { value: "DELIVERED", label: "배송 완료" },
  { value: "CANCELLED", label: "취소" },
];

const orderStatusLabel = Object.fromEntries(
  orderStatusOptions.map((item) => [item.value, item.label]),
);

const refundStatusLabel = {
  PENDING: "승인 대기",
  COMPLETED: "환불 완료",
  REJECTED: "승인 거절",
  FAILED: "처리 실패",
};

const pendingRefundCount = computed(() =>
  refunds.value.filter((refund) => refund.status === "PENDING").length,
);

const refundPageCount = computed(() =>
  Math.max(1, Math.ceil(refunds.value.length / REFUND_PAGE_SIZE)),
);
const pagedRefunds = computed(() => {
  const start = (refundPage.value - 1) * REFUND_PAGE_SIZE;
  return refunds.value.slice(start, start + REFUND_PAGE_SIZE);
});

const statusSummary = computed(() =>
  orderStatusOptions.filter((item) => ["PAID", "SHIPPING", "DELIVERED", "CANCELLED"].includes(item.value)).map((item) => ({
    ...item,
    // 기존 데이터의 PREPARING은 운영 화면에서 별도 단계로 노출하지 않고 배송 중에 포함합니다.
    count: orders.value.filter((order) => item.value === "SHIPPING"
      ? ["PREPARING", "SHIPPING"].includes(order.status)
      : order.status === item.value).length,
  })),
);

const filteredOrders = computed(() => {
  const keyword = search.value.trim().toLowerCase();

  return orders.value.filter((order) => {
    // 결제 완료 전 임시 주문은 운영 처리 대상이 아니므로 목록에서 제외한다.
    if (order.status === "PENDING_PAYMENT") return false;
    const matchesStatus = statusFilter.value === "ALL"
      || (statusFilter.value === "SHIPPING" && ["PREPARING", "SHIPPING"].includes(order.status))
      || order.status === statusFilter.value;
    const searchable = [
      order.orderNumber,
      order.recipient_name,
      order.recipient_email,
      order.recipient_phone,
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();

    return matchesStatus && (!keyword || searchable.includes(keyword));
  });
});

const orderPageCount = computed(() => Math.max(1, Math.ceil(filteredOrders.value.length / ORDER_PAGE_SIZE)));
const pagedOrders = computed(() => {
  const start = (orderPage.value - 1) * ORDER_PAGE_SIZE;
  return filteredOrders.value.slice(start, start + ORDER_PAGE_SIZE);
});
watch([search, statusFilter], () => { orderPage.value = 1; });
watch(orderPageCount, (count) => {
  if (orderPage.value > count) {
    orderPage.value = count;
  }
});
watch(refundPageCount, (count) => {
  // 마지막 페이지에서 환불 항목이 줄어들어도 존재하지 않는 페이지에 남지 않게 합니다.
  if (refundPage.value > count) {
    refundPage.value = count;
  }
});

function scrollToRefundSection() {
  refundSection.value?.scrollIntoView({
    behavior: "smooth",
    block: "start",
  });
}

function formatCurrency(value) {
  return `${Number(value || 0).toLocaleString("ko-KR")}원`;
}

function formatDateTime(value) {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace("T", " ");

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
}

async function loadOrders({ resetPage = true } = {}) {
  loading.value = true;
  error.value = "";

  try {
    const [orderResult, refundResult] = await Promise.all([
      adminManagementService.getOrders(),
      adminManagementService.getRefunds(),
    ]);
    orders.value = Array.isArray(orderResult) ? orderResult : [];
    refunds.value = Array.isArray(refundResult) ? refundResult : [];
    if (resetPage) {
      orderPage.value = 1;
      refundPage.value = 1;
    }
  } catch (cause) {
    orders.value = [];
    error.value = cause.message || "주문 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

async function processRefund(refund, action) {
  try {
    refundActionId.value = refund.refundId;
    error.value = "";
    if (action === "approve") {
      await adminManagementService.approveRefund(refund.refundId);
    } else {
      await adminManagementService.rejectRefund(refund.refundId);
    }
    await loadOrders({ resetPage: false });
  } catch (cause) {
    error.value = cause.message || "환불 처리를 완료하지 못했습니다.";
  } finally {
    refundActionId.value = null;
  }
}

async function changeStatus(order, event) {
  const nextStatus = event;
  if (nextStatus === order.status) return;

  try {
    const updated = await adminManagementService.changeOrderStatus(order.orderId, nextStatus);
    const index = orders.value.findIndex((item) => item.orderId === order.orderId);
    if (index >= 0) {
      orders.value.splice(index, 1, updated || { ...order, status: nextStatus });
    }
    openStatusMenuId.value = null;
  } catch (cause) {
    error.value = cause.message || "배송 상태를 변경하지 못했습니다.";
  }
}

function canChangeOrderStatus(status) {
  return ["PAID", "PREPARING", "SHIPPING"].includes(status);
}

function nextStatusOptions(status) {
  const transitions = {
    // 실제 배송 단계 진행과 운영상 배송 완료 처리를 모두 허용합니다.
    PAID: ["SHIPPING", "DELIVERED"],
    PREPARING: ["SHIPPING", "DELIVERED"],
    // 배송 중 주문도 백엔드 상태 전이 규칙과 동일하게 배송 완료로 마칠 수 있습니다.
    SHIPPING: ["DELIVERED"],
  };
  return transitions[status] || [];
}

function displayedStatus(status) {
  return status === "PREPARING" ? "SHIPPING" : status;
}

onMounted(loadOrders);
</script>

<template>
  <main class="admin-orders">
    <header class="orders-heading">
      <small>ORDER & DELIVERY</small>
      <h1>주문·배송 관리</h1>
      <p>주문 상태를 확인하고 배송 상태 및 환불 신청을 건별로 처리합니다.</p>
    </header>

    <!-- # 주문·배송 관리: 실제 주문 상태 집계 -->
    <section class="orders-summary-grid" aria-label="주문 상태 현황">
      <article v-for="item in statusSummary" :key="item.value">
        <small>{{ item.label }}</small>
        <strong>{{ item.count.toLocaleString() }}<em>건</em></strong>
      </article>
      <button
        class="refund-summary-card"
        type="button"
        :aria-label="'환불 신청 ' + refunds.length + '건, 승인 대기 ' + pendingRefundCount + '건. 환불 목록으로 이동'"
        @click="scrollToRefundSection"
      >
        <span class="refund-summary-label">환불 신청</span>
        <strong>{{ refunds.length.toLocaleString() }}<em>건</em></strong>
        <span class="refund-summary-pending">승인 대기 {{ pendingRefundCount.toLocaleString() }}건</span>
      </button>
    </section>

    <section class="orders-workspace">
      <header class="workspace-header">
        <div class="orders-filter-tabs" aria-label="주문 상태 필터">
          <button
            v-for="item in orderStatusOptions"
            :key="item.value"
            type="button"
            :class="{ active: statusFilter === item.value }"
            @click="statusFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </header>

      <div class="orders-search-row">
        <input
          v-model="search"
          type="search"
          placeholder="예) ORD-2026, 홍길동, user@example.com"
        />
      </div>

      <p v-if="loading" class="orders-message">실제 주문 정보를 불러오는 중입니다.</p>
      <p v-else-if="error" class="orders-message error">{{ error }}</p>

      <div v-else class="orders-table-wrap">
        <table>
          <thead>
            <tr>
              <th>주문번호</th>
              <th>주문일</th>
              <th>수령인</th>
              <th>연락처</th>
              <th>결제금액</th>
              <th>주문 상태</th>
              <th>주문 상태 변경</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in pagedOrders" :key="order.orderId">
              <td>{{ order.orderNumber || order.orderId }}</td>
              <td>{{ formatDateTime(order.created_at) }}</td>
              <td><b>{{ order.recipient_name || "-" }}</b></td>
              <td>{{ order.recipient_phone || "-" }}</td>
              <td>{{ formatCurrency(order.final_amount) }}</td>
              <td><span class="order-status" :class="displayedStatus(order.status)?.toLowerCase()">{{ orderStatusLabel[displayedStatus(order.status)] || displayedStatus(order.status) }}</span></td>
              <td>
                <div
                  v-if="canChangeOrderStatus(order.status)"
                  class="status-select"
                  :class="{ open: openStatusMenuId === order.orderId }"
                >
                  <button
                    type="button"
                    class="status-select-trigger"
                    :aria-expanded="openStatusMenuId === order.orderId"
                    @click="openStatusMenuId = openStatusMenuId === order.orderId ? null : order.orderId"
                  >
                    <span>{{ orderStatusLabel[displayedStatus(order.status)] }}</span><i aria-hidden="true"></i>
                  </button>
                  <div v-if="openStatusMenuId === order.orderId" class="status-select-menu">
                    <button
                      v-for="status in nextStatusOptions(order.status)"
                      :key="status"
                      type="button"
                      :class="{ selected: status === order.status }"
                      @click="changeStatus(order, status)"
                    >{{ orderStatusLabel[status] }}</button>
                  </div>
                </div>
                <small v-else class="status-lock">처리 완료</small>
              </td>
            </tr>
            <tr v-if="!filteredOrders.length">
              <td colspan="7" class="empty-row">검색 조건에 맞는 주문이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar v-model:page="orderPage" :total-pages="orderPageCount" label="주문 목록 페이지" />
    </section>

    <section
      ref="refundSection"
      class="refund-workspace"
      aria-label="환불 신청 승인 대기"
      tabindex="-1"
    >
      <header>
        <div>
          <small>REFUND REQUESTS</small>
          <h2>환불 신청 · 승인 대기</h2>
          <p>고객이 요청한 환불 사유와 금액을 확인한 뒤 승인하거나 거절합니다.</p>
        </div>
        <strong>{{ pendingRefundCount.toLocaleString() }}건 대기</strong>
      </header>
      <div class="refund-table-wrap" v-if="refunds.length">
        <table>
          <thead><tr><th>주문번호</th><th>요청일</th><th>사유</th><th>환불 금액</th><th>상태</th><th>처리</th></tr></thead>
          <tbody><tr v-for="refund in pagedRefunds" :key="refund.refundId">
            <td>{{ refund.orderNumber || refund.orderId }}</td>
            <td>{{ formatDateTime(refund.createdAt) }}</td>
            <td>{{ refund.reason || '사유 미입력' }}</td>
            <td>{{ formatCurrency(refund.refundAmount) }}</td>
            <td><span class="refund-status" :class="refund.status?.toLowerCase()">{{ refundStatusLabel[refund.status] || refund.status }}</span></td>
            <td><div v-if="refund.status === 'PENDING'" class="refund-actions"><button type="button" :disabled="refundActionId === refund.refundId" @click="processRefund(refund, 'approve')">승인</button><button type="button" class="reject" :disabled="refundActionId === refund.refundId" @click="processRefund(refund, 'reject')">거절</button></div><small v-else class="status-lock">처리 완료</small></td>
          </tr></tbody>
        </table>
      </div>
      <p v-else class="orders-message">환불 신청 내역이 없습니다.</p>
      <PaginationBar
        v-if="refunds.length"
        v-model:page="refundPage"
        :total-pages="refundPageCount"
        label="환불 신청 페이지"
      />
    </section>
  </main>
</template>

<style scoped>
.admin-orders { min-height: 100dvh; box-sizing: border-box; padding: 34px; background: #f7f5f1; color: #3d281b; }
.orders-heading small, .refund-workspace header small { color: #a66838; font-weight: 900; letter-spacing: .12em; }
.orders-heading h1 { margin: 8px 0; font-size: clamp(29px, 3vw, 38px); }
.orders-heading p, .refund-workspace header p { margin: 0; color: #917866; }
.orders-summary-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; margin: 28px 0 18px; }
.orders-summary-grid article,
.refund-summary-card {
  min-height: 100px;
  border: 1px solid #ead8c7;
  border-radius: 15px;
  background: #fffdfb;
  padding: 17px;
}
.refund-summary-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  color: #3d281b;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}
.refund-summary-card:hover,
.refund-summary-card:focus-visible {
  border-color: #ac6a3d;
  box-shadow: 0 5px 16px rgb(84 49 25 / 12%);
  outline: none;
  transform: translateY(-1px);
}
.refund-summary-label { color: #97765e; font-size: 13px; font-weight: 800; }
.refund-summary-card strong { margin-top: 8px; font-size: 27px; }
.refund-summary-card em { margin-left: 3px; color: #927460; font-size: 14px; font-style: normal; }
.refund-summary-pending { margin-top: 4px; color: #aa6b25; font-size: 12px; font-weight: 800; }
.orders-summary-grid small { display: block; color: #97765e; font-weight: 800; }
.orders-summary-grid strong { display: block; margin-top: 10px; font-size: 27px; }
.orders-summary-grid em { margin-left: 3px; color: #927460; font-size: 14px; font-style: normal; }
.orders-workspace, .refund-workspace { border: 1px solid #eadfd5; border-radius: 18px; background: #fffdfb; padding: 22px; }
.workspace-header, .refund-workspace > header { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.orders-filter-tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.orders-filter-tabs button { border: 1px solid #dfc8b4; border-radius: 9px; background: #fff; padding: 9px 13px; color: #68452e; font: inherit; font-weight: 800; cursor: pointer; }
.orders-filter-tabs button.active { border-color: #ac6a3d; background: #ac6a3d; color: #fff; }
.orders-search-row { display: block; margin: 15px 0; }
.orders-search-row input { min-width: 0; border: 1px solid #ddc9b8; border-radius: 9px; background: #fff; padding: 11px 13px; font: inherit; }
.orders-table-wrap { overflow-x: auto; border: 1px solid #eee2d8; border-radius: 12px; }
.orders-table-wrap table { width: 100%; min-width: 900px; border-collapse: collapse; }
.orders-table-wrap th, .orders-table-wrap td { border-bottom: 1px solid #f1e8e1; padding: 15px 12px; text-align: center; vertical-align: middle; }
.orders-table-wrap th { background: #fff9f4; color: #94755f; font-size: 13px; }
.status-lock { color: #967966; font-size: 12px; }
.status-select { position: relative; display: inline-block; min-width: 138px; }
.status-select-trigger { position: relative; display: inline-flex; align-items: center; justify-content: center; width: 100%; min-height: 42px; gap: 18px; border: 1px solid #d9c0ad; border-radius: 10px; background: #fffdfb; padding: 9px 34px; color: #4b3425; font: inherit; font-weight: 800; text-align: center; cursor: pointer; }
.status-select-trigger span { width: 100%; text-align: center; }
.status-select-trigger i { position: absolute; right: 14px; width: 0; height: 0; border-right: 5px solid transparent; border-left: 5px solid transparent; border-top: 7px solid #805737; transition: transform .16s ease; }
.status-select.open .status-select-trigger i { transform: rotate(180deg); }
.status-select-menu { position: absolute; z-index: 5; top: calc(100% + 5px); left: 0; right: 0; overflow: hidden; border: 1px solid #d9c0ad; border-radius: 10px; background: #fffdfb; box-shadow: 0 9px 18px rgba(75, 52, 37, .12); }
.status-select-menu button { display: block; width: 100%; border: 0; background: transparent; padding: 10px 14px; color: #4b3425; font: inherit; text-align: center; cursor: pointer; }
.status-select-menu button:hover, .status-select-menu button.selected { background: #f8ecdf; color: #805737; font-weight: 800; }
.order-status { display: inline-flex; border-radius: 999px; background: #eee7dd; padding: 5px 8px; color: #755945; font-size: 12px; font-weight: 800; }
.order-status.preparing { background: #f9ead4; color: #a05f25; }.order-status.shipping { background: #e2eef5; color: #47728b; }.order-status.delivered { background: #e2f0df; color: #568150; }.order-status.cancelled { background: #fae2df; color: #b14f45; }
.orders-message { margin: 14px 0; color: #866e5e; }.orders-message.error { color: #b54f3d; }.empty-row { padding: 30px !important; color: #947a66; text-align: center !important; }
.orders-api-note { margin: 14px 2px 0; color: #927765; font-size: 13px; }
.refund-workspace{margin-top:18px}.refund-workspace h2{margin:7px 0;font-size:22px}.refund-workspace>header>strong{border-radius:999px;background:#fff0d9;padding:7px 10px;color:#aa6b25;font-size:13px}.refund-list{display:grid;gap:8px;margin-top:18px}.refund-list article{display:grid;grid-template-columns:minmax(0,1fr) 160px 100px;align-items:center;gap:14px;border-top:1px solid #f0e4da;padding:13px 2px}.refund-list div{display:grid;gap:4px}.refund-list span,.refund-list small{color:#8f7562;font-size:13px}.refund-list button{border:0;border-radius:8px;background:#ac6a3d;padding:9px;color:#fff;font:inherit;font-weight:800;cursor:pointer}.refund-list em{color:#66835e;font-size:13px;font-style:normal;font-weight:800}
.refund-table-wrap{margin-top:18px;overflow-x:auto;border:1px solid #eee2d8;border-radius:12px}.refund-table-wrap table{width:100%;min-width:820px;border-collapse:collapse}.refund-table-wrap th,.refund-table-wrap td{border-bottom:1px solid #f1e8e1;padding:14px 12px;text-align:center}.refund-table-wrap th{background:#fff9f4;color:#94755f;font-size:13px}.refund-status{display:inline-flex;border-radius:999px;padding:5px 9px;background:#eee7dd;color:#755945;font-size:12px;font-weight:800}.refund-status.pending{background:#fff0d9;color:#9a611e}.refund-status.completed{background:#e2f0df;color:#568150}.refund-status.rejected,.refund-status.failed{background:#fae2df;color:#b14f45}.refund-actions{display:flex;justify-content:center;gap:6px}.refund-actions button{border:0;border-radius:8px;background:#ac6a3d;padding:8px 12px;color:#fff;font:inherit;font-weight:800;cursor:pointer}.refund-actions button.reject{background:#7f746c}.refund-actions button:disabled{cursor:wait;opacity:.55}
@media (max-width: 1120px) { .orders-summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 680px) { .admin-orders { padding: 24px 16px; }.orders-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.workspace-header, .refund-workspace > header { align-items: flex-start; flex-direction: column; }.orders-search-row { grid-template-columns: 1fr; } }
</style>
