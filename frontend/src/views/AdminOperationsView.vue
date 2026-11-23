<!-- # 운영 관리 데이터는 실제 API만 사용하며, 연결 실패 시 임시 행 대신 연동 필요 안내를 표시합니다. -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { adminManagementService } from "@/services/adminManagementService";
import AdminSidebar from "@/components/AdminSidebar.vue";
const route = useRoute();
const section = computed(
    () =>
      route.params.section ||
      (route.path === "/admin/members" ? "members" : ""),
  ),
  rows = ref([]),
  refundRequests = ref([]),
  refundError = ref(""),
  productHighlights = ref({ best: [], worst: [] }),
  loading = ref(false),
  error = ref(""),
  search = ref(""),
  statusFilter = ref("ALL"),
  viewMode = ref("card");
// # 브라우저 prompt 대신 사이트 공통 모달에서 QnA 답변을 작성한다.
const qnaAnswerOpen = ref(false),
  qnaAnswerRow = ref(null),
  qnaAnswerContent = ref(""),
  qnaAnswerSubmitting = ref(false);
const titles = {
  orders: "주문·배송 관리",
  products: "상품 관리",
  members: "회원 관리",
  notices: "공지사항 관리",
  qna: "QnA 관리",
};
const detail = {
  orders: "주문 현황과 취소·반품 신청을 확인하고 배송 상태를 처리합니다.",
  products: "상품별 판매·재고·리뷰 지표를 확인합니다.",
  members: "활성화·차단·비활성화 회원을 관리합니다.",
  notices: "공지 등록, 수정, 삭제와 현재 목록을 관리합니다.",
  qna: "전체·답변 대기 문의를 분류하여 답변합니다.",
};
const loaders = {
  orders: adminManagementService.getOrders,
  products: adminManagementService.getProducts,
  members: adminManagementService.getMembers,
  notices: adminManagementService.getNotices,
  qna: adminManagementService.getManagedQnas,
};
function timeout(task) {
  return Promise.race([
    task,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error("API 응답 시간 초과")), 5000),
    ),
  ]);
}
async function load() {
  loading.value = true;
  error.value = "";
  rows.value = [];
  try {
    const result = await timeout(loaders[section.value]());
    rows.value = Array.isArray(result) ? result : [];
    if (section.value === "orders") {
      try {
        const refunds = await timeout(adminManagementService.getRefunds());
        refundRequests.value = Array.isArray(refunds) ? refunds : [];
      } catch (cause) {
        refundRequests.value = [];
        refundError.value =
          cause.message || "환불 신청서를 불러오지 못했습니다.";
      }
    }
    if (section.value === "products") {
      const [best, worst] = await Promise.allSettled([
        timeout(adminManagementService.getBestProducts()),
        timeout(adminManagementService.getWorstProducts()),
      ]);
      productHighlights.value = {
        best: best.status === "fulfilled" && Array.isArray(best.value) ? best.value : [],
        worst: worst.status === "fulfilled" && Array.isArray(worst.value) ? worst.value : [],
      };
    }
  } catch (cause) {
    error.value = `${cause.message || "데이터를 불러오지 못했습니다."} 백엔드 API 연동이 필요합니다.`;
  } finally {
    loading.value = false;
  }
}
async function approveRefund(row) {
  try {
    await adminManagementService.approveRefund(row.refundId || row.id);
    await load();
  } catch (cause) {
    error.value = cause.message || "환불을 승인하지 못했습니다.";
  }
}
const filteredRows = computed(() =>
  rows.value.filter((row) => {
    const matches = Object.values(row)
      .join(" ")
      .toLowerCase()
      .includes(search.value.toLowerCase());
    const status = row.status || row.orderStatus || row.answerStatus || "ALL";
    return (
      matches && (statusFilter.value === "ALL" || status === statusFilter.value)
    );
  }),
);
const columns = computed(() =>
  filteredRows.value.length ? Object.keys(filteredRows.value[0]) : [],
);
function exportCsv() {
  const data = [
    columns.value,
    ...filteredRows.value.map((row) =>
      columns.value.map((key) => row[key] ?? ""),
    ),
  ];
  const url = URL.createObjectURL(
    new Blob([`\uFEFF${data.map((row) => row.join(",")).join("\n")}`], {
      type: "text/csv;charset=utf-8",
    }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = `pawcast-${section.value}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}
async function changeStatus(row, event) {
  try {
    await adminManagementService.changeOrderStatus(
      row.orderId || row.id,
      event.target.value,
    );
    await load();
  } catch (cause) {
    error.value = cause.message || "상태를 변경하지 못했습니다.";
  }
}
async function changeMemberRole(row, event) {
  try {
    await adminManagementService.changeMemberRole(row.id, event.target.value);
    await load();
  } catch (cause) {
    error.value = cause.message || "역할을 변경하지 못했습니다.";
  }
}
async function changeMemberStatus(row, event) {
  try {
    await adminManagementService.changeMemberStatus(
      row.memberId || row.id,
      event.target.value,
    );
    await load();
  } catch (cause) {
    error.value = cause.message || "회원 상태를 변경하지 못했습니다.";
  }
}
function openQnaAnswer(row) {
  qnaAnswerRow.value = row;
  qnaAnswerContent.value = row.answerContent || row.answer?.content || "";
  qnaAnswerOpen.value = true;
}
function closeQnaAnswer() {
  if (!qnaAnswerSubmitting.value) qnaAnswerOpen.value = false;
}
async function submitQnaAnswer() {
  if (!qnaAnswerContent.value.trim() || !qnaAnswerRow.value) return;
  qnaAnswerSubmitting.value = true;
  try {
    await adminManagementService.createQnaAnswer(
      qnaAnswerRow.value.id || qnaAnswerRow.value.qnaId,
      qnaAnswerContent.value.trim(),
    );
    qnaAnswerOpen.value = false;
    await load();
  } catch (cause) {
    error.value = cause.message || "답변을 등록하지 못했습니다.";
  } finally {
    qnaAnswerSubmitting.value = false;
  }
}
watch(section, load);
onMounted(load);
</script>
<template>
  <div class="admin-operations">
    <AdminSidebar />
    <main>
      <header>
        <small>ADMIN MANAGEMENT</small>
        <h1>{{ titles[section] }}</h1>
        <p>{{ detail[section] }}</p>
      </header>
      <section class="toolbar">
        <input
          v-model="search"
          placeholder="주문번호·상품명·회원명 검색"
        /><select v-model="statusFilter">
          <option value="ALL">전체 상태</option>
          <option value="PREPARING">상품 준비</option>
          <option value="SHIPPING">배송 중</option>
          <option value="DELIVERED">배송 완료</option>
          <option value="PENDING">답변 대기</option></select
        ><button
          v-if="section === 'members'"
          :class="{ selected: viewMode === 'card' }"
          @click="viewMode = 'card'"
        >
          카드형</button
        ><button
          v-if="section === 'members'"
          :class="{ selected: viewMode === 'list' }"
          @click="viewMode = 'list'"
        >
          리스트형</button
        ><button class="export" @click="exportCsv">엑셀용 CSV 다운로드</button>
      </section>
      <p v-if="loading" class="loading">
        실제 운영 데이터를 불러오는 중입니다.
      </p>
      <p v-if="error" class="error">{{ error }}</p>
      <section v-if="section === 'products'" class="product-sales-highlights">
        <article>
          <header><small>BEST 3</small><h2>판매 상위 상품</h2></header>
          <ol v-if="productHighlights.best.length">
            <li v-for="product in productHighlights.best" :key="product.id"><b>{{ product.name }}</b><span>판매 {{ Number(product.salesCount || 0).toLocaleString() }}건</span></li>
          </ol>
          <p v-else>판매 성과 API에서 상품 데이터를 기다리고 있습니다.</p>
        </article>
        <article>
          <header><small>WORST 3</small><h2>판매 하위 상품</h2></header>
          <ol v-if="productHighlights.worst.length">
            <li v-for="product in productHighlights.worst" :key="product.id"><b>{{ product.name }}</b><span>판매 {{ Number(product.salesCount || 0).toLocaleString() }}건</span></li>
          </ol>
          <p v-else>판매 성과 API에서 상품 데이터를 기다리고 있습니다.</p>
        </article>
      </section>
      <div
        v-if="filteredRows.length"
        :class="[
          'table-wrap',
          { cards: section === 'members' && viewMode === 'card' },
        ]"
      >
        <table v-if="!(section === 'members' && viewMode === 'card')">
          <thead>
            <tr>
              <th v-for="key in columns" :key="key">{{ key }}</th>
              <th v-if="section === 'orders'">배송 상태 처리</th>
              <th v-if="section === 'qna'">답변 처리</th>
              <th v-if="section === 'members'">역할 변경</th>
              <th v-if="section === 'members'">상태 변경</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, index) in filteredRows"
              :key="row.id || row.orderId || index"
            >
              <td v-for="key in columns" :key="key">
                {{ typeof row[key] === "object" ? "-" : row[key] }}
              </td>
              <td v-if="section === 'orders'">
                <select
                  :value="row.status || row.orderStatus"
                  @change="changeStatus(row, $event)"
                >
                  <option value="PAID">결제 완료</option>
                  <option value="PREPARING">상품 준비</option>
                  <option value="SHIPPING">배송 중</option>
                  <option value="DELIVERED">배송 완료</option>
                </select>
              </td>
              <td v-if="section === 'qna'">
                <button @click="openQnaAnswer(row)">답변하기</button>
              </td>
              <td v-if="section === 'members'">
                <select
                  :value="row.role"
                  @change="changeMemberRole(row, $event)"
                >
                  <option value="USER">사용자</option>
                  <option value="MANAGER">매니저</option>
                </select>
              </td>
              <td v-if="section === 'members'">
                <select
                  :value="row.status"
                  @change="changeMemberStatus(row, $event)"
                >
                  <option value="ACTIVE">활성화</option>
                  <option value="INACTIVE">비활성화</option>
                  <option value="KICKED">차단</option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
        <article
          v-else
          v-for="(row, index) in filteredRows"
          :key="row.memberId || index"
        >
          <b>{{ row.name || row.memberName }}</b
          ><span>{{ row.email }}</span
          ><em>{{ row.status }}</em
          ><label
            >역할
            <select :value="row.role" @change="changeMemberRole(row, $event)">
              <option value="USER">사용자</option>
              <option value="MANAGER">매니저</option>
            </select></label
          ><label
            >상태
            <select
              :value="row.status"
              @change="changeMemberStatus(row, $event)"
            >
              <option value="ACTIVE">활성화</option>
              <option value="INACTIVE">비활성화</option>
              <option value="KICKED">차단</option>
            </select></label
          >
          >
        </article>
      </div>
      <p v-else>검색 조건에 맞는 데이터가 없습니다.</p>
      <section v-if="section === 'orders'" class="admin-refund-requests">
        <header>
          <div>
            <small>REFUND REQUESTS</small>
            <h2>환불 신청서</h2>
            <p>고객이 작성한 환불 사유를 확인한 뒤 건별로 승인합니다.</p>
          </div>
        </header>
        <p v-if="refundError" class="error">{{ refundError }}</p>
        <p v-else-if="!refundRequests.length">
          대기 중인 환불 신청서가 없습니다.
        </p>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>신청 번호</th>
                <th>환불 사유</th>
                <th>환불 금액</th>
                <th>상태</th>
                <th>신청일</th>
                <th>처리</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="refund in refundRequests" :key="refund.refundId">
                <td>{{ refund.refundId }}</td>
                <td class="refund-reason">{{ refund.reason }}</td>
                <td>
                  {{ Number(refund.refundAmount || 0).toLocaleString() }}원
                </td>
                <td>{{ refund.status }}</td>
                <td>{{ String(refund.createdAt || "").slice(0, 10) }}</td>
                <td>
                  <button
                    v-if="refund.status === 'PENDING'"
                    @click="approveRefund(refund)"
                  >
                    환불 승인</button
                  ><span v-else>처리 완료</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
    <Teleport to="body">
      <div
        v-if="qnaAnswerOpen"
        class="common-modal-backdrop"
        @click.self="closeQnaAnswer"
      >
        <form
          class="common-modal admin-qna-answer-modal"
          @submit.prevent="submitQnaAnswer"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="closeQnaAnswer"
          >
            ×
          </button>
          <small>Q&A ANSWER</small>
          <h2>문의 답변 작성</h2>
          <p>고객에게 전달할 내용을 정확하고 친절하게 작성해주세요.</p>
          <label>
            답변 내용
            <textarea
              v-model.trim="qnaAnswerContent"
              rows="7"
              maxlength="2000"
              required
              placeholder="답변을 입력해주세요."
            />
          </label>
          <div class="common-modal-actions">
            <button
              type="button"
              class="secondary"
              :disabled="qnaAnswerSubmitting"
              @click="closeQnaAnswer"
            >
              취소
            </button>
            <button class="primary" :disabled="qnaAnswerSubmitting">
              {{ qnaAnswerSubmitting ? "등록 중..." : "답변 등록" }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>
  </div>
</template>
<style scoped>
.admin-operations {
  background: #f7f5f2;
  color: #3d281b;
}
.admin-operations main {
  min-width: 0;
  overflow: auto;
}
.admin-operations header small {
  color: #a66838;
  font-weight: 900;
  letter-spacing: 0.12em;
}
.admin-operations h1 {
  margin: 8px 0;
}
.admin-operations header p {
  color: #917866;
}
.product-sales-highlights { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; margin:22px 0; }
.product-sales-highlights article { border:1px solid #e8d8ca; border-radius:14px; background:#fffdfb; padding:18px; }
.product-sales-highlights h2 { margin:5px 0 12px; font-size:18px; }.product-sales-highlights small { color:#a66838; font-weight:900; letter-spacing:.08em; }
.product-sales-highlights ol { display:grid; gap:8px; margin:0; padding:0; list-style:none; }.product-sales-highlights li { display:flex; justify-content:space-between; gap:12px; border-top:1px solid #f0e4d9; padding-top:9px; }.product-sales-highlights li span,.product-sales-highlights article>p { color:#927562; font-size:13px; }
@media (max-width:760px){.product-sales-highlights{grid-template-columns:1fr;}}
</style>
