<!-- # 사이드바: 주문·배송 관리 - 실제 주문 조회, 건별 배송 상태 변경, 환불 승인 -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { adminManagementService } from "@/services/adminManagementService";

const orders = ref([]);
const refunds = ref([]);
const loading = ref(false);
const error = ref("");
const refundError = ref("");
const search = ref("");
const statusFilter = ref("ALL");
const orderPage = ref(1);
const refundPage = ref(1);
const ORDER_PAGE_SIZE = 10;
const REFUND_PAGE_SIZE = 5;

const orderStatusOptions = [
  { value: "ALL", label: "전체" },
  { value: "PAID", label: "결제 완료" },
  { value: "PREPARING", label: "상품 준비" },
  { value: "SHIPPING", label: "배송 중" },
  { value: "DELIVERED", label: "배송 완료" },
  { value: "CANCELLED", label: "취소" },
];

const orderStatusLabel = Object.fromEntries(
  orderStatusOptions.map((item) => [item.value, item.label]),
);

const statusSummary = computed(() =>
  orderStatusOptions.filter((item) => ["PAID", "PREPARING", "SHIPPING", "DELIVERED", "CANCELLED"].includes(item.value)).map((item) => ({
    ...item,
    count: orders.value.filter((order) => order.status === item.value).length,
  })),
);

const pendingRefunds = computed(() =>
  refunds.value.filter((refund) => refund.status === "PENDING"),
);

const filteredOrders = computed(() => {
  const keyword = search.value.trim().toLowerCase();

  return orders.value.filter((order) => {
    // 결제 완료 전 임시 주문은 운영 처리 대상이 아니므로 목록에서 제외한다.
    if (order.status === "PENDING_PAYMENT") return false;
    const matchesStatus =
      statusFilter.value === "ALL" || order.status === statusFilter.value;
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
const refundPageCount = computed(() => Math.max(1, Math.ceil(pendingRefunds.value.length / REFUND_PAGE_SIZE)));
const pagedRefunds = computed(() => {
  const start = (refundPage.value - 1) * REFUND_PAGE_SIZE;
  return pendingRefunds.value.slice(start, start + REFUND_PAGE_SIZE);
});

watch([search, statusFilter], () => { orderPage.value = 1; });
watch(orderPageCount, (count) => { if (orderPage.value > count) orderPage.value = count; });
watch(refundPageCount, (count) => { if (refundPage.value > count) refundPage.value = count; });

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

async function loadOrders() {
  loading.value = true;
  error.value = "";
  refundError.value = "";

  try {
    const [orderResult, refundResult] = await Promise.allSettled([
      adminManagementService.getOrders(),
      adminManagementService.getRefunds(),
    ]);

    if (orderResult.status === "rejected") throw orderResult.reason;

    orders.value = Array.isArray(orderResult.value) ? orderResult.value : [];
    orderPage.value = 1;
    refundPage.value = 1;
    if (refundResult.status === "fulfilled") {
      refunds.value = Array.isArray(refundResult.value) ? refundResult.value : [];
    } else {
      refunds.value = [];
      refundError.value =
        refundResult.reason?.message || "환불 신청 정보를 확인 중입니다.";
    }
  } catch (cause) {
    orders.value = [];
    error.value = cause.message || "주문 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

async function changeStatus(order, event) {
  const nextStatus = event.target.value;
  if (nextStatus === order.status) return;

  try {
    await adminManagementService.changeOrderStatus(order.orderId, nextStatus);
    await loadOrders();
  } catch (cause) {
    error.value = cause.message || "배송 상태를 변경하지 못했습니다.";
    event.target.value = order.status;
  }
}

async function approveRefund(refund) {
  try {
    await adminManagementService.approveRefund(refund.refundId);
    await loadOrders();
  } catch (cause) {
    refundError.value = cause.message || "환불을 승인하지 못했습니다.";
  }
}

function canChangeOrderStatus(status) {
  return ["PAID", "PREPARING", "SHIPPING"].includes(status);
}

function nextStatusOptions(status) {
  const transitions = {
    PAID: ["PAID", "PREPARING"],
    PREPARING: ["PREPARING", "SHIPPING"],
    SHIPPING: ["SHIPPING", "DELIVERED"],
  };
  return transitions[status] || [];
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
      <article class="refund-summary">
        <small>환불 승인 대기</small>
        <strong>{{ pendingRefunds.length.toLocaleString() }}<em>건</em></strong>
      </article>
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
          placeholder="주문번호·수령인·이메일 검색"
        />
        <button type="button" @click="loadOrders">새로고침</button>
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
              <th>결제금액</th>
              <th>주문 상태</th>
              <th>주문 상태 변경</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in pagedOrders" :key="order.orderId">
              <td>{{ order.orderNumber || order.orderId }}</td>
              <td>{{ formatDateTime(order.created_at) }}</td>
              <td>
                <b>{{ order.recipient_name || "-" }}</b>
                <small>{{ order.recipient_phone || order.recipient_email || "" }}</small>
              </td>
              <td>{{ formatCurrency(order.final_amount) }}</td>
              <td><span class="order-status" :class="order.status?.toLowerCase()">{{ orderStatusLabel[order.status] || order.status }}</span></td>
              <td>
                <select
                  v-if="canChangeOrderStatus(order.status)"
                  :value="order.status"
                  @change="changeStatus(order, $event)"
                >
                  <option v-for="status in nextStatusOptions(order.status)" :key="status" :value="status">
                    {{ orderStatusLabel[status] }}
                  </option>
                </select>
                <small v-else class="status-lock">처리 완료</small>
              </td>
            </tr>
            <tr v-if="!filteredOrders.length">
              <td colspan="6" class="empty-row">검색 조건에 맞는 주문이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
      <nav v-if="orderPageCount > 1" class="orders-pagination" aria-label="주문 목록 페이지">
        <button type="button" :disabled="orderPage === 1" @click="orderPage -= 1">이전</button>
        <button v-for="page in orderPageCount" :key="page" type="button" :class="{ active: orderPage === page }" @click="orderPage = page">{{ page }}</button>
        <button type="button" :disabled="orderPage === orderPageCount" @click="orderPage += 1">다음</button>
      </nav>
    </section>

    <!-- # 환불 신청 승인 대기 기능: 사용자 환불 사유를 그대로 조회하고 관리자 승인만 제공 -->
    <section class="refund-workspace">
      <header>
        <div>
          <small>REFUND REQUESTS</small>
          <h2>환불 신청 · 승인 대기</h2>
          <p>환불 사유와 금액은 고객 요청 API 응답을 그대로 표시합니다.</p>
        </div>
        <span>{{ pendingRefunds.length }}건 대기</span>
      </header>

      <p v-if="refundError" class="orders-message error">{{ refundError }}</p>
      <div v-else class="orders-table-wrap refund-table-wrap">
        <table>
          <thead>
            <tr>
              <th>신청 번호</th>
              <th>환불 사유</th>
              <th>환불 금액</th>
              <th>신청일</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="refund in pagedRefunds" :key="refund.refundId">
              <td>{{ refund.refundId }}</td>
              <td class="refund-reason">{{ refund.reason || "-" }}</td>
              <td>{{ formatCurrency(refund.refundAmount) }}</td>
              <td>{{ formatDateTime(refund.createdAt) }}</td>
              <td><button type="button" class="refund-approve" @click="approveRefund(refund)">승인</button></td>
            </tr>
            <tr v-if="!pendingRefunds.length">
              <td colspan="5" class="empty-row">승인 대기 중인 환불 신청이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
      <nav v-if="refundPageCount > 1" class="orders-pagination" aria-label="환불 목록 페이지">
        <button type="button" :disabled="refundPage === 1" @click="refundPage -= 1">이전</button>
        <button v-for="page in refundPageCount" :key="page" type="button" :class="{ active: refundPage === page }" @click="refundPage = page">{{ page }}</button>
        <button type="button" :disabled="refundPage === refundPageCount" @click="refundPage += 1">다음</button>
      </nav>
    </section>

    <p class="orders-api-note">
      배송사·송장번호·배송 추적 이력 및 DB 기준 CSV/Excel 내려받기 API는 현재 확인 중이라 표시하지 않았습니다.
    </p>
  </main>
</template>

<style scoped>
.admin-orders { min-height: 100dvh; box-sizing: border-box; padding: 34px; background: #f7f5f1; color: #3d281b; }
.orders-heading small, .refund-workspace header small { color: #a66838; font-weight: 900; letter-spacing: .12em; }
.orders-heading h1 { margin: 8px 0; font-size: clamp(29px, 3vw, 38px); }
.orders-heading p, .refund-workspace header p { margin: 0; color: #917866; }
.orders-summary-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin: 28px 0 18px; }
.orders-summary-grid article { min-height: 100px; border: 1px solid #ead8c7; border-radius: 15px; background: #fffdfb; padding: 17px; }
.orders-summary-grid small { display: block; color: #97765e; font-weight: 800; }
.orders-summary-grid strong { display: block; margin-top: 10px; font-size: 27px; }
.orders-summary-grid em { margin-left: 3px; color: #927460; font-size: 14px; font-style: normal; }
.orders-summary-grid .refund-summary { border-color: #edcbb9; background: #fff8f3; }
.orders-summary-grid .refund-summary strong { color: #bb5d36; }
.orders-workspace, .refund-workspace { border: 1px solid #eadfd5; border-radius: 18px; background: #fffdfb; padding: 22px; }
.workspace-header, .refund-workspace > header { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.orders-filter-tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.orders-filter-tabs button, .orders-search-row button, .refund-approve { border: 1px solid #dfc8b4; border-radius: 9px; background: #fff; padding: 9px 13px; color: #68452e; font: inherit; font-weight: 800; cursor: pointer; }
.orders-filter-tabs button.active, .refund-approve { border-color: #ac6a3d; background: #ac6a3d; color: #fff; }
.orders-search-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; margin: 15px 0; }
.orders-search-row input { min-width: 0; border: 1px solid #ddc9b8; border-radius: 9px; background: #fff; padding: 11px 13px; font: inherit; }
.orders-table-wrap { overflow-x: auto; border: 1px solid #eee2d8; border-radius: 12px; }
.orders-table-wrap table { width: 100%; min-width: 760px; border-collapse: collapse; }
.orders-table-wrap th, .orders-table-wrap td { border-bottom: 1px solid #f1e8e1; padding: 13px; text-align: left; vertical-align: middle; }
.orders-table-wrap th { background: #fff9f4; color: #94755f; font-size: 13px; }
.orders-table-wrap td small { display: block; margin-top: 3px; color: #967966; font-size: 12px; }
.status-lock { color: #967966; font-size: 12px; }
.orders-table-wrap select { border: 1px solid #ddc9b8; border-radius: 8px; background: #fff; padding: 8px; color: #4b3425; font: inherit; }
.order-status { display: inline-flex; border-radius: 999px; background: #eee7dd; padding: 5px 8px; color: #755945; font-size: 12px; font-weight: 800; }
.order-status.preparing { background: #f9ead4; color: #a05f25; }.order-status.shipping { background: #e2eef5; color: #47728b; }.order-status.delivered { background: #e2f0df; color: #568150; }.order-status.cancelled { background: #fae2df; color: #b14f45; }
.orders-message { margin: 14px 0; color: #866e5e; }.orders-message.error { color: #b54f3d; }.empty-row { padding: 30px !important; color: #947a66; text-align: center !important; }
.orders-pagination{display:flex;justify-content:center;gap:6px;margin-top:16px}.orders-pagination button{min-width:34px;border:1px solid #dfc8b4;border-radius:8px;background:#fff;padding:7px 9px;color:#68452e;font:inherit;font-size:13px;font-weight:800;cursor:pointer}.orders-pagination button.active{border-color:#ac6a3d;background:#ac6a3d;color:#fff}.orders-pagination button:disabled{cursor:not-allowed;opacity:.45}
.refund-workspace { margin-top: 18px; }.refund-workspace > header h2 { margin: 7px 0; font-size: 22px; }.refund-workspace > header > span { border-radius: 999px; background: #fff0df; padding: 7px 10px; color: #a66432; font-size: 13px; font-weight: 900; }.refund-table-wrap { margin-top: 17px; }.refund-reason { min-width: 260px; white-space: pre-wrap; }.orders-api-note { margin: 14px 2px 0; color: #927765; font-size: 13px; }
@media (max-width: 1120px) { .orders-summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 680px) { .admin-orders { padding: 24px 16px; }.orders-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.workspace-header, .refund-workspace > header { align-items: flex-start; flex-direction: column; }.orders-search-row { grid-template-columns: 1fr; } }
</style>
