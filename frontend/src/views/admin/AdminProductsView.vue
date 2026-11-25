<!-- # 사이드바: 상품 관리 - 실제 상품 목록·판매 성과·AI 리뷰/재고 분석 -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { aiAnalyticsService } from "@/services/aiAnalyticsService";
import { adminManagementService } from "@/services/adminManagementService";
import PaginationBar from "@/components/PaginationBar.vue";
import { isAdminRole, readUserRole } from "@/utils/roles";

const isAdmin = isAdminRole(readUserRole());
const products = ref([]);
const bestProducts = ref([]);
const worstProducts = ref([]);
const inventoryForecast = ref(null);
const sentimentSummary = ref(null);
const sentimentLoading = ref(false);
const sentimentMessage = ref("");
const loading = ref(false);
const error = ref("");
const statusError = ref("");
const statusUpdatingId = ref(null);
const aiNotice = ref("");
const keyword = ref("");
const page = ref(1);
const PRODUCT_PAGE_SIZE = 10;

const visibleProducts = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) return products.value;

  return products.value.filter((product) =>
    [product.name, product.brand, product.categoryName]
      .filter(Boolean)
      .join(" ")
      .toLowerCase()
      .includes(query),
  );
});

const productPageCount = computed(() =>
  Math.max(1, Math.ceil(visibleProducts.value.length / PRODUCT_PAGE_SIZE)),
);

const pagedProducts = computed(() => {
  const start = (page.value - 1) * PRODUCT_PAGE_SIZE;
  return visibleProducts.value.slice(start, start + PRODUCT_PAGE_SIZE);
});

watch(keyword, () => {
  page.value = 1;
});

watch(productPageCount, (count) => {
  if (page.value > count) page.value = count;
});

const productSummary = computed(() => ({
  total: products.value.length,
  soldOut: products.value.filter((product) => product.soldOut).length,
  totalSales: products.value.reduce(
    (sum, product) => sum + Number(product.salesCount || 0),
    0,
  ),
  reviews: products.value.reduce(
    (sum, product) => sum + Number(product.reviewCount || 0),
    0,
  ),
}));

const forecastItems = computed(() => inventoryForecast.value?.items || []);
const sentiment = computed(() => sentimentSummary.value);
const formatCurrency = (value) =>
  `${Number(value || 0).toLocaleString("ko-KR")}원`;
const formatNumber = (value) => Number(value || 0).toLocaleString("ko-KR");
const formatRating = (value) => Number(value || 0).toFixed(1);
const stockoutLabel = (item) =>
  item.daysUntilStockout == null ? "계산 불가" : `${item.daysUntilStockout}일`;

function rankProducts(direction) {
  const factor = direction === "best" ? -1 : 1;
  return [...products.value]
    .sort(
      (left, right) =>
        factor * (Number(left.salesCount || 0) - Number(right.salesCount || 0)) ||
        Number(right.id || 0) - Number(left.id || 0),
    )
    .slice(0, 3);
}

async function changeProductStatus(product, status) {
  if (
    !product.id ||
    status === product.status ||
    statusUpdatingId.value != null
  ) {
    return;
  }

  statusError.value = "";
  statusUpdatingId.value = product.id;

  try {
    // 상품 한 행의 선택은 상품 전체 옵션에 적용되며, 서버가 돌려준 집계 상태를
    // 사용해 화면의 값이 DB의 실제 저장 결과와 정확히 일치하게 합니다.
    const updatedProduct = await adminManagementService.changeProductStatus(
      product.id,
      status,
    );
    Object.assign(product, updatedProduct);
  } catch (cause) {
    statusError.value = cause.message || "상품 상태를 변경하지 못했습니다.";

    // API 실패 시 선택값이 브라우저 DOM에 남지 않도록 목록을 재조회합니다.
    // 재조회마저 실패하면 Vue 상태의 기존 값으로 셀렉트를 되돌립니다.
    try {
      products.value = await adminManagementService.getProducts();
    } catch (refreshCause) {
      statusError.value += " 상품 목록을 새로고침하지 못해 기존 상태를 유지합니다.";
      product.status = product.status || (product.soldOut ? "SOLD_OUT" : "ON_SALE");
    }
  } finally {
    statusUpdatingId.value = null;
  }
}

async function analyzeRecentSentiments() {
  if (sentimentLoading.value) {
    return;
  }

  sentimentLoading.value = true;
  sentimentMessage.value = "";
  aiNotice.value = "";

  try {
    let analyzedCount = 0;
    let remainingCount = 0;

    // 서버가 100건씩 처리하므로 리뷰가 많아도 브라우저 요청 하나가 과도하게
    // 오래 걸리지 않습니다. 아직 누락·레거시 라벨이 남아 있으면 다음 배치를 요청합니다.
    do {
      const result = await aiAnalyticsService.analyzeRecentReviewSentiments(28, 100);
      analyzedCount += Number(result?.analyzedCount || 0);
      remainingCount = Number(result?.remainingCount || 0);
    } while (remainingCount > 0);

    // 쓰기 응답의 단순 건수 대신 DB에서 다시 계산한 요약을 사용해
    // 비율과 상품별 부정 현황이 실제 저장 결과와 함께 바뀌도록 합니다.
    sentimentSummary.value = await aiAnalyticsService.getReviewSentimentSummary(5, 28);
    sentimentMessage.value = analyzedCount > 0
      ? `최근 28일 리뷰 ${formatNumber(analyzedCount)}건 분석 완료`
      : "최근 28일 AI 감성 분석 결과가 최신 상태입니다.";
  } catch (cause) {
    sentimentMessage.value = "";
    aiNotice.value = cause.message || "리뷰 감정 분석을 완료하지 못했습니다.";
  } finally {
    sentimentLoading.value = false;
  }
}

async function loadProducts() {
  loading.value = true;
  error.value = "";
  statusError.value = "";
  aiNotice.value = "";

  try {
    const [
      productsResult,
      bestResult,
      worstResult,
      forecastResult,
      sentimentResult,
    ] = await Promise.allSettled([
      adminManagementService.getProducts(),
      adminManagementService.getBestProducts(),
      adminManagementService.getWorstProducts(),
      aiAnalyticsService.getInventoryForecast(8),
      isAdmin
        ? aiAnalyticsService.getReviewSentimentSummary(5, 28)
        : Promise.resolve(null),
    ]);

    if (productsResult.status === "rejected") {
      throw productsResult.reason;
    }

    products.value = Array.isArray(productsResult.value)
      ? productsResult.value
      : [];
    page.value = 1;
    bestProducts.value =
      bestResult.status === "fulfilled" && bestResult.value?.length
        ? bestResult.value
        : rankProducts("best");
    worstProducts.value =
      worstResult.status === "fulfilled" && worstResult.value?.length
        ? worstResult.value
        : rankProducts("worst");
    inventoryForecast.value =
      forecastResult.status === "fulfilled" ? forecastResult.value : null;
    sentimentSummary.value =
      isAdmin && sentimentResult.status === "fulfilled"
        ? sentimentResult.value
        : null;

    if (
      forecastResult.status === "rejected" ||
      (isAdmin && sentimentResult.status === "rejected")
    ) {
      aiNotice.value = "AI 분석 결과는 현재 확인 중입니다.";
    }
  } catch (cause) {
    products.value = [];
    error.value = cause.message || "상품 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

onMounted(loadProducts);
</script>

<template>
  <main class="admin-products">
    <header class="products-heading">
      <small>PRODUCT MANAGEMENT</small>
      <h1>상품 관리</h1>
      <p>상품 목록과 판매 성과, AI 분석 결과를 실제 조회 데이터로 확인합니다.</p>
    </header>

    <!-- # 상품 관리: Spring 상품 목록의 실제 집계 -->
    <section class="product-summary-grid" aria-label="상품 현황">
      <article>
        <small>등록 상품</small>
        <strong>{{ formatNumber(productSummary.total) }}<em>개</em></strong>
        <p>관리자 상품 목록 기준</p>
      </article>
      <article class="warning">
        <small>품절 상품</small>
        <strong>{{ formatNumber(productSummary.soldOut) }}<em>개</em></strong>
        <p>현재 품절 여부 기준</p>
      </article>
      <article>
        <small>누적 판매 수량</small>
        <strong>{{ formatNumber(productSummary.totalSales) }}<em>개</em></strong>
        <p>상품별 판매 수량 합계</p>
      </article>
      <article>
        <small>등록 리뷰</small>
        <strong>{{ formatNumber(productSummary.reviews) }}<em>건</em></strong>
        <p>상품별 리뷰 수 합계</p>
      </article>
    </section>

    <p v-if="loading" class="products-message">
      실제 상품 정보를 불러오는 중입니다.
    </p>
    <p v-else-if="error" class="products-message error">
      {{ error }}
    </p>

    <template v-else>
      <!-- # 상품 관리: AI 재고 소진 예상 및 권장 입고 수량 -->
      <section class="product-grid">
        <article class="product-panel inventory-panel">
          <header class="panel-heading">
            <div>
              <small>AI INVENTORY FORECAST</small>
              <h2>재고 · 판매량 현황</h2>
              <p v-if="inventoryForecast">
                최근 {{ inventoryForecast.lookbackDays }}일 판매량과 현재 옵션 재고 기준입니다.
              </p>
              <p v-else>재고 소진 예측 결과를 확인 중입니다.</p>
            </div>
          </header>

          <div v-if="forecastItems.length" class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>상품 / 옵션</th>
                  <th>재고</th>
                  <th>기간 판매</th>
                  <th>일평균</th>
                  <th>소진 예상</th>
                  <th>권장 입고</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in forecastItems" :key="item.variantId">
                  <td>
                    <b>{{ item.productName }}</b>
                    <small>{{ item.optionLabel }}</small>
                  </td>
                  <td>{{ formatNumber(item.stock) }}</td>
                  <td>{{ formatNumber(item.soldQuantity) }}</td>
                  <td>{{ item.averageDailySales }}</td>
                  <td>
                    <span class="stockout-pill">{{ stockoutLabel(item) }}</span>
                  </td>
                  <td>{{ formatNumber(item.recommendedInboundQuantity) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty-panel">AI 재고 소진 예측 데이터가 없습니다.</p>
        </article>

        <article v-if="isAdmin" class="product-panel compact-panel">
          <header class="panel-heading">
            <div>
              <small>AI REVIEW SENTIMENT</small>
              <h2>리뷰 감성 비율</h2>
              <p>최근 28일 · AI 분석 완료 리뷰 기준</p>
            </div>
            <button
              type="button"
              class="sentiment-analyze-button"
              :disabled="sentimentLoading"
              @click="analyzeRecentSentiments"
            >
              {{ sentimentLoading ? "감정 분석 중..." : "감정 분석하기" }}
            </button>
          </header>

          <p v-if="sentimentMessage" class="sentiment-message" role="status">
            {{ sentimentMessage }}
          </p>

          <template v-if="sentiment?.analyzedCount">
            <div class="sentiment-bar">
              <span
                class="positive"
                :style="{ width: `${sentiment.positiveRate}%` }"
              />
              <span
                class="negative"
                :style="{ width: `${sentiment.negativeRate}%` }"
              />
            </div>
            <div class="sentiment-legend">
              <span>긍정 {{ sentiment.positiveRate }}%</span>
              <span>부정 {{ sentiment.negativeRate }}%</span>
            </div>
            <p class="analysis-count">
              분석 리뷰 {{ formatNumber(sentiment.analyzedCount) }}건
            </p>
            <h3>부정 리뷰 많은 상품</h3>
            <ol class="negative-products">
              <li v-for="item in sentiment.negativeProducts" :key="item.productId">
                <span>{{ item.productName }}</span>
                <b>{{ item.negativeRate }}%</b>
              </li>
            </ol>
          </template>
          <p v-else class="empty-panel">
            최근 28일에 분석 완료된 AI 감성 데이터가 없습니다. 감정 분석하기를 눌러 분석하세요.
          </p>
          <p v-if="aiNotice" class="sentiment-error" role="alert">
            {{ aiNotice }}
          </p>
        </article>
      </section>

      <!-- # 상품 관리: Spring 판매량 기준 베스트·워스트 상품 -->
      <section class="product-panel performance-panel">
        <header class="panel-heading">
          <div>
            <small>SALES PERFORMANCE</small>
            <h2>상품 성과 · 베스트 3 / 워스트 3</h2>
          </div>
        </header>

        <div class="performance-cards">
          <article
            v-for="(product, index) in bestProducts.slice(0, 3)"
            :key="`best-${product.id}`"
            class="performance-card best"
          >
            <small>BEST {{ index + 1 }}</small>
            <b>{{ product.name }}</b>
            <span>{{ product.brand || "-" }}</span>
            <p>
              판매 {{ formatNumber(product.salesCount) }}개 · 리뷰
              {{ formatNumber(product.reviewCount) }}건
            </p>
          </article>

          <article
            v-for="(product, index) in worstProducts.slice(0, 3)"
            :key="`worst-${product.id}`"
            class="performance-card worst"
          >
            <small>WORST {{ index + 1 }}</small>
            <b>{{ product.name }}</b>
            <span>{{ product.brand || "-" }}</span>
            <p>
              판매 {{ formatNumber(product.salesCount) }}개 · 리뷰
              {{ formatNumber(product.reviewCount) }}건
            </p>
          </article>
        </div>

        <p v-if="!bestProducts.length && !worstProducts.length" class="empty-panel">
          판매 성과 데이터가 없습니다.
        </p>
      </section>

      <!-- # 상품 관리: 실제 상품 목록과 상품 등록 이동 -->
      <section class="product-panel list-panel">
        <header class="panel-heading list-heading">
          <div>
            <small>PRODUCT LIST</small>
            <h2>상품 목록</h2>
          </div>
          <div class="list-actions">
            <input
              v-model="keyword"
              type="search"
              placeholder="상품명 · 브랜드 · 분류 검색"
            />
            <RouterLink to="/admin/products/new">상품 등록</RouterLink>
          </div>
        </header>

        <p v-if="statusError" class="products-message error" role="alert">
          {{ statusError }}
        </p>

        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>상품명</th>
                <th>브랜드</th>
                <th>분류</th>
                <th>가격</th>
                <th>재고</th>
                <th>판매</th>
                <th>찜</th>
                <th>리뷰</th>
                <th>평점</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="product in pagedProducts"
                :key="`${product.id}-${product.variantId}`"
              >
                <td><b>{{ product.name }}</b></td>
                <td>{{ product.brand || "-" }}</td>
                <td>
                  <span class="category-pill">
                    {{ product.categoryName || product.productType || "-" }}
                  </span>
                </td>
                <td>{{ formatCurrency(product.price) }}</td>
                <td>{{ product.stock == null ? "-" : formatNumber(product.stock) }}</td>
                <td>{{ formatNumber(product.salesCount) }}</td>
                <td>{{ formatNumber(product.wishCount) }}</td>
                <td>{{ formatNumber(product.reviewCount) }}</td>
                <td>{{ formatRating(product.averageRating) }}</td>
                <td>
                  <span class="product-status-select">
                    <select
                      :value="product.status || (product.soldOut ? 'SOLD_OUT' : 'ON_SALE')"
                      :disabled="statusUpdatingId != null"
                      @change="changeProductStatus(product, $event.target.value)"
                    >
                      <option value="ON_SALE">판매 중</option>
                      <option value="SOLD_OUT">품절</option>
                      <option value="DISCONTINUED">판매 종료</option>
                    </select>
                  </span>
                </td>
              </tr>
              <tr v-if="!pagedProducts.length">
                <td colspan="10" class="empty-row">
                  검색 조건에 맞는 상품이 없습니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <PaginationBar
          v-model:page="page"
          :total-pages="productPageCount"
          label="상품 목록 페이지"
        />
      </section>
    </template>
  </main>
</template>

<style scoped>
.admin-products {
  min-height: 100dvh;
  box-sizing: border-box;
  padding: 34px;
  background: #f7f5f1;
  color: #3d281b;
}

.products-heading small,
.panel-heading small {
  color: #a66838;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.products-heading h1 {
  margin: 8px 0;
  font-size: clamp(29px, 3vw, 38px);
}

.products-heading p,
.panel-heading p {
  margin: 0;
  color: #917866;
}

.product-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 28px 0 18px;
}

.product-summary-grid article,
.product-panel {
  border: 1px solid #eadfd5;
  border-radius: 18px;
  background: #fffdfb;
}

.product-summary-grid article {
  min-height: 116px;
  padding: 17px;
}

.product-summary-grid article.warning {
  border-color: #edcbb9;
  background: #fff8f3;
}

.product-summary-grid small {
  display: block;
  color: #97765e;
  font-weight: 800;
}

.product-summary-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 27px;
}

.product-summary-grid em {
  margin-left: 3px;
  color: #927460;
  font-size: 14px;
  font-style: normal;
}

.product-summary-grid p {
  margin: 6px 0 0;
  color: #a18570;
  font-size: 12px;
}

.product-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(290px, 1fr);
  gap: 18px;
}

.product-panel {
  padding: 22px;
}

.panel-heading {
  display: flex;
  justify-content: space-between;
  gap: 15px;
}

.panel-heading h2 {
  margin: 6px 0;
  font-size: 22px;
}

.table-scroll {
  overflow-x: auto;
  margin-top: 16px;
}

.table-scroll table {
  width: 100%;
  min-width: 680px;
  border-collapse: collapse;
}

.table-scroll th,
.table-scroll td {
  border-bottom: 1px solid #f1e8e1;
  padding: 12px 10px;
  text-align: left;
  vertical-align: middle;
}

.table-scroll th {
  background: #fff9f4;
  color: #94755f;
  font-size: 12px;
}

.table-scroll td small {
  display: block;
  margin-top: 2px;
  color: #a18169;
  font-size: 12px;
}

.stockout-pill,
.category-pill,
.product-status {
  display: inline-flex;
  border-radius: 999px;
  padding: 4px 8px;
  background: #f7ead8;
  color: #98612e;
  font-size: 12px;
  font-weight: 800;
}

.stockout-pill {
  background: #fee9cf;
}

.product-status {
  background: #e5f0df;
  color: #5b8253;
}

.product-status.product-sold-out {
  background: #fae1de;
  color: #b15349;
}

.product-status-select {
  position: relative;
  display: inline-flex;
  min-width: 98px;
}

.product-status-select select {
  width: 100%;
  height: 36px;
  appearance: none;
  border: 1px solid #d9b99e;
  border-radius: 9px;
  background: #fffaf5;
  padding: 0 34px 0 12px;
  color: #5a3b29;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.product-status-select::after {
  content: "";
  position: absolute;
  top: 50%;
  right: 13px;
  width: 0;
  height: 0;
  border-right: 5px solid transparent;
  border-left: 5px solid transparent;
  border-top: 6px solid #805737;
  pointer-events: none;
  transform: translateY(-25%);
}

.product-status-select:focus-within::after {
  border-top: 0;
  border-bottom: 6px solid #805737;
  transform: translateY(-50%);
}

.product-status-select select:hover,
.product-status-select select:focus {
  border-color: #a66a42;
  outline: none;
  box-shadow: 0 0 0 3px rgb(166 106 66 / 12%);
}

.empty-panel {
  margin: 28px 0 8px;
  color: #947a66;
  text-align: center;
}

.compact-panel {
  min-height: 300px;
}

.sentiment-analyze-button {
  flex: 0 0 auto;
  min-height: 36px;
  border: 1px solid #bd8965;
  border-radius: 9px;
  padding: 0 12px;
  background: #fff8f2;
  color: #70482e;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.sentiment-analyze-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.sentiment-message,
.sentiment-error {
  margin: 12px 0 0;
  font-size: 13px;
}

.sentiment-message {
  color: #587348;
}

.sentiment-error {
  color: #b54f3d;
}

.sentiment-bar {
  display: flex;
  overflow: hidden;
  height: 14px;
  margin: 28px 0 10px;
  border-radius: 999px;
  background: #eee5d9;
}

.sentiment-bar .positive {
  background: #81945b;
}

.sentiment-bar .negative {
  background: #cd786c;
}

.sentiment-legend {
  display: flex;
  justify-content: space-between;
  color: #806b5b;
  font-size: 13px;
}

.analysis-count {
  margin: 13px 0;
  color: #947a66;
  font-size: 13px;
}

.compact-panel h3 {
  margin: 22px 0 9px;
  font-size: 15px;
}

.negative-products {
  margin: 0;
  padding: 0;
  list-style: none;
}

.negative-products li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px solid #f1e8e1;
}

.negative-products b {
  color: #b95d51;
}

@media (max-width: 560px) {
  .compact-panel .panel-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}

.performance-panel {
  margin-top: 18px;
}

.performance-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.performance-card {
  border: 1px solid #eee1d2;
  border-radius: 12px;
  padding: 14px;
}

.performance-card small {
  display: inline-flex;
  border-radius: 999px;
  padding: 3px 7px;
  font-size: 11px;
  font-weight: 900;
}

.performance-card.best {
  background: #fbfcf7;
}

.performance-card.best small {
  background: #e3efd9;
  color: #62804d;
}

.performance-card.worst {
  background: #fff9f7;
}

.performance-card.worst small {
  background: #f9e0dc;
  color: #bb564b;
}

.performance-card b,
.performance-card span {
  display: block;
  margin-top: 8px;
}

.performance-card span,
.performance-card p {
  color: #917866;
  font-size: 13px;
}

.performance-card p {
  margin: 8px 0 0;
}

.list-panel {
  margin-top: 18px;
}

.list-heading {
  align-items: flex-end;
}

.list-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.list-actions input {
  min-width: 230px;
  border: 1px solid #ddc9b8;
  border-radius: 9px;
  padding: 9px 12px;
  background: #fff;
  font: inherit;
}

.list-actions a,
.list-actions button {
  border: 1px solid #ac6a3d;
  border-radius: 9px;
  padding: 9px 12px;
  background: #ac6a3d;
  color: #fff;
  font: inherit;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
}

.list-actions button {
  background: #fff;
  color: #68452e;
  border-color: #dfc8b4;
}

.empty-row {
  padding: 30px !important;
  color: #947a66;
  text-align: center !important;
}

.products-message {
  margin: 18px 0;
  color: #866e5e;
}

.products-message.error {
  color: #b54f3d;
}

.products-api-note {
  margin: 14px 2px 0;
  color: #927765;
  font-size: 13px;
}

@media (max-width: 1120px) {
  .product-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .performance-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .product-grid {
    grid-template-columns: 1fr;
  }

  .list-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .list-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .admin-products {
    padding: 24px 16px;
  }

  .product-summary-grid,
  .performance-cards {
    grid-template-columns: 1fr;
  }

  .list-actions {
    width: 100%;
  }

  .list-actions input {
    width: 100%;
    min-width: 0;
  }
}
</style>


