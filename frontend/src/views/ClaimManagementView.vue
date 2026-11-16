<!-- 취소·반품·교환 요청을 상태별로 확인하는 회원 화면입니다. -->
<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import MemberSidebar from "@/components/MemberSidebar.vue";
import { useAuthSession } from "@/stores/auth";

const router = useRouter();
const { currentUserId } = useAuthSession();
const userId = computed(() => currentUserId.value);
const displayName = computed(
  () => userId.value || "포캐스트 회원",
);
const filter = ref("ALL");
const selectedClaim = ref(null);
const claims = computed(() => []);
const typeLabels = { CANCEL: "취소", RETURN: "반품", EXCHANGE: "교환" };

function requestSettings() {
  router.push("/mypage?verify=1");
}
</script>

<template>
  <main class="container member-dashboard claim-management-page">
    <header class="dashboard-heading">
      <span>POWCAST ORDER CARE</span>
      <h1>취소·반품·교환</h1>
      <p>주문 이후 접수한 요청과 처리 상태를 확인하세요.</p>
    </header>
    <div class="dashboard-layout">
      <MemberSidebar
        :name="displayName"
        :user-id="userId"
        active="claims"
        @settings="requestSettings"
      />
      <div class="dashboard-content">
        <section class="claim-summary-card">
          <div>
            <small>최근 6개월</small>
            <h2>취소·반품·교환 내역</h2>
            <p>백엔드에서 접수 내역을 제공하면 이 화면에 표시됩니다.</p>
          </div>
          <nav aria-label="요청 유형 필터">
            <button
              v-for="item in [
                ['ALL', '전체'],
                ['CANCEL', '취소'],
                ['RETURN', '반품'],
                ['EXCHANGE', '교환'],
              ]"
              :key="item[0]"
              type="button"
              :class="{ on: filter === item[0] }"
              @click="filter = item[0]"
            >
              {{ item[1] }}
            </button>
          </nav>
        </section>

        <section v-if="claims.length" class="claim-list">
          <article v-for="claim in claims" :key="claim.id">
            <header>
              <div>
                <span :class="['claim-type', claim.type.toLowerCase()]">
                  {{ typeLabels[claim.type] }}
                </span>
                <b>{{ claim.statusLabel }}</b>
              </div>
              <small>{{ claim.requestedAt }} 접수</small>
            </header>
            <div class="claim-product-row">
              <RouterLink :to="`/products/${claim.pet}/${claim.productId}`">
                <img :src="claim.imageUrl" :alt="claim.productName" />
              </RouterLink>
              <div>
                <small>주문번호 {{ claim.orderNumber }}</small>
                <h3>
                  <RouterLink :to="`/products/${claim.pet}/${claim.productId}`">
                    {{ claim.productName }}
                  </RouterLink>
                </h3>
                <p>{{ claim.optionLabel }} · 수량 {{ claim.quantity }}개</p>
                <b>{{ Number(claim.amount).toLocaleString() }}원</b>
              </div>
              <button type="button" @click="selectedClaim = claim">
                처리 상세
              </button>
            </div>
            <footer>
              <div><strong>{{ claim.statusLabel }}</strong><span>{{ claim.description }}</span></div>
              <p>사유: {{ claim.reason }}</p>
            </footer>
          </article>
        </section>
        <p v-else class="dashboard-order-state">해당 유형의 요청 내역이 없습니다.</p>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="selectedClaim"
        class="common-modal-backdrop"
        @click.self="selectedClaim = null"
      >
        <section class="common-modal claim-detail-modal">
          <button class="common-modal-close" type="button" @click="selectedClaim = null">×</button>
          <small>{{ typeLabels[selectedClaim.type] }} REQUEST</small>
          <h2>{{ selectedClaim.statusLabel }}</h2>
          <p>{{ selectedClaim.description }}</p>
          <dl>
            <dt>주문번호</dt><dd>{{ selectedClaim.orderNumber }}</dd>
            <dt>접수일</dt><dd>{{ selectedClaim.requestedAt }}</dd>
            <dt>상품</dt><dd>{{ selectedClaim.productName }}</dd>
            <dt>요청 사유</dt><dd>{{ selectedClaim.reason }}</dd>
            <template v-if="selectedClaim.refundAmount">
              <dt>환불 금액</dt><dd>{{ Number(selectedClaim.refundAmount).toLocaleString() }}원</dd>
            </template>
          </dl>
          <div class="common-modal-actions single">
            <button class="primary" type="button" @click="selectedClaim = null">확인</button>
          </div>
        </section>
      </div>
    </Teleport>
  </main>
</template>
