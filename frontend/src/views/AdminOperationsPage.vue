<!-- # 기존 운영 화면을 API 전용·모달 기반 구조로 정리한 관리자 화면이다. -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import AdminSidebar from "@/components/AdminSidebar.vue";
import { adminManagementService } from "@/services/adminManagementService";

const route = useRoute();
const rows = ref([]);
const loading = ref(false);
const error = ref("");
const search = ref("");
const statusFilter = ref("ALL");
const viewMode = ref("card");
const answerOpen = ref(false);
const answerRow = ref(null);
const answerContent = ref("");
const answerSubmitting = ref(false);

const section = computed(
  () =>
    route.params.section || (route.path === "/admin/members" ? "members" : ""),
);
const titleBySection = {
  orders: "주문·배송 관리",
  products: "상품 관리",
  members: "회원 관리",
  notices: "공지사항 관리",
  qna: "QnA 관리",
};
const descriptionBySection = {
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

function withTimeout(task) {
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
    const result = await withTimeout(loaders[section.value]());
    rows.value = Array.isArray(result) ? result : [];
  } catch (cause) {
    error.value = `${cause.message || "데이터를 불러오지 못했습니다."} 백엔드 API 연동이 필요합니다.`;
  } finally {
    loading.value = false;
  }
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
// # 브라우저 prompt 대신 사이트 공통 모달에서 실제 QnA 답변 API를 호출한다.
function openAnswer(row) {
  answerRow.value = row;
  answerContent.value = row.answerContent || row.answer?.content || "";
  answerOpen.value = true;
}
function closeAnswer() {
  if (!answerSubmitting.value) answerOpen.value = false;
}
async function submitAnswer() {
  if (!answerContent.value.trim() || !answerRow.value) return;
  answerSubmitting.value = true;
  try {
    await adminManagementService.createQnaAnswer(
      answerRow.value.id || answerRow.value.qnaId,
      answerContent.value.trim(),
    );
    answerOpen.value = false;
    await load();
  } catch (cause) {
    error.value = cause.message || "답변을 등록하지 못했습니다.";
  } finally {
    answerSubmitting.value = false;
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
        <h1>{{ titleBySection[section] }}</h1>
        <p>{{ descriptionBySection[section] }}</p>
      </header>
      <section class="toolbar">
        <input
          v-model="search"
          placeholder="주문번호·상품명·회원명 검색"
        /><select v-model="statusFilter" aria-label="상태 필터">
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
                <button @click="openAnswer(row)">답변하기</button>
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
          >
        </article>
      </div>
      <p v-else-if="!loading">검색 조건에 맞는 데이터가 없습니다.</p>
    </main>
    <Teleport to="body"
      ><div
        v-if="answerOpen"
        class="common-modal-backdrop"
        @click.self="closeAnswer"
      >
        <form
          class="common-modal admin-qna-answer-modal"
          @submit.prevent="submitAnswer"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="closeAnswer"
          >
            ×</button
          ><small>Q&A ANSWER</small>
          <h2>문의 답변 작성</h2>
          <p>고객에게 전달할 내용을 정확하고 친절하게 작성해주세요.</p>
          <label
            >답변 내용<textarea
              v-model.trim="answerContent"
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
              :disabled="answerSubmitting"
              @click="closeAnswer"
            >
              취소</button
            ><button class="primary" :disabled="answerSubmitting">
              {{ answerSubmitting ? "등록 중..." : "답변 등록" }}
            </button>
          </div>
        </form>
      </div></Teleport
    >
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
.toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 26px 0 12px;
}
.toolbar input,
.toolbar select {
  min-height: 38px;
  border: 1px solid #ddc9b8;
  border-radius: 8px;
  background: #fff;
  padding: 0 10px;
}
.toolbar button {
  border: 1px solid #d9b898;
  background: #fff;
  padding: 8px 12px;
}
.toolbar .selected,
.toolbar .export {
  background: #a66b3f;
  color: #fff;
}
.loading,
.error {
  padding: 10px 0;
}
.loading {
  color: #806653;
}
.error {
  color: #b34d3b;
}
.table-wrap {
  overflow: auto;
  border: 1px solid #e8d8ca;
  border-radius: 14px;
  background: #fff;
}
.table-wrap table {
  width: 100%;
  border-collapse: collapse;
}
.table-wrap th,
.table-wrap td {
  padding: 13px;
  border-bottom: 1px solid #f0e7df;
  text-align: left;
  white-space: nowrap;
}
.table-wrap th {
  background: #fffaf5;
}
.table-wrap select,
.table-wrap td button {
  border: 1px solid #ddc9b8;
  border-radius: 7px;
  background: #fff;
  padding: 7px;
}
.cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 12px;
  border: 0;
  background: transparent;
}
.cards article {
  display: grid;
  gap: 8px;
  border: 1px solid #e8d8ca;
  border-radius: 12px;
  background: #fff;
  padding: 18px;
}
.cards article span {
  color: #806653;
}
.cards article em {
  font-style: normal;
  color: #a66838;
  font-weight: 800;
}
.admin-qna-answer-modal {
  max-width: 680px;
  text-align: left;
}
.admin-qna-answer-modal > small {
  color: #a66838;
  font-weight: 900;
  letter-spacing: 0.12em;
}
.admin-qna-answer-modal label {
  display: grid;
  gap: 9px;
  margin-top: 24px;
  color: #604631;
  font-weight: 800;
}
.admin-qna-answer-modal textarea {
  min-height: 170px;
  resize: vertical;
  border: 1px solid #ddc9b8;
  border-radius: 10px;
  padding: 14px;
  color: #3d281b;
  font: inherit;
  line-height: 1.6;
}
@media (max-width: 640px) {
  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
