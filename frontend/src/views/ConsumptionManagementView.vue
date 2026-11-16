<!-- 소진일 관리 UI는 Store에만 의존해 향후 localStorage를 백엔드 API로 쉽게 교체할 수 있습니다. -->
<script setup>
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import MemberSidebar from "@/components/MemberSidebar.vue";
import { products } from "@/data/products";
import { useAuthSession } from "@/stores/auth";
import { useConsumption } from "@/stores/consumption";

const router = useRouter();
const { currentUserId } = useAuthSession();
const { items, settings, addItem, updateItem, saveSettings } =
  useConsumption();
const userId = computed(() => currentUserId.value);
const displayName = computed(
  () => userId.value || "포캐스트 회원",
);
const selectableProducts = computed(() =>
  products.filter(
    (product) => product.category === "사료" || product.category === "영양제",
  ),
);

const formOpen = ref(false);
const editingId = ref(null);
const toast = ref("");
const form = reactive({
  productId: "",
  opened: "",
  amount: 0,
  daily: 1,
  unit: "g",
});
const settingForm = reactive({ ...settings.value });

const enrichedItems = computed(() =>
  items.value.map((item) => ({
    ...item,
    days: Math.max(
      0,
      Math.ceil(Number(item.amount) / Math.max(1, Number(item.daily))),
    ),
  })),
);

function showToast(message) {
  toast.value = message;
  window.setTimeout(() => (toast.value = ""), 2200);
}
function openAddForm() {
  editingId.value = null;
  Object.assign(form, {
    productId: "",
    opened: "",
    amount: 0,
    daily: 1,
    unit: "g",
  });
  formOpen.value = true;
}
function openEditForm(item) {
  editingId.value = item.id;
  Object.assign(form, {
    productId: item.productId,
    opened: item.opened,
    amount: item.amount,
    daily: item.daily,
    unit: item.unit,
  });
  formOpen.value = true;
}
function submitItem() {
  const product = products.find((item) => item.id === Number(form.productId));
  if (!product || !form.opened || form.amount <= 0 || form.daily <= 0) return;
  const payload = {
    productId: product.id,
    name: product.name,
    image: product.image,
    category: product.category,
    pet: product.pet,
    option: product.options?.[0] || product.option,
    opened: form.opened,
    amount: Number(form.amount),
    daily: Number(form.daily),
    unit: form.unit,
  };
  if (editingId.value) updateItem(editingId.value, payload);
  else addItem(payload);
  formOpen.value = false;
  showToast(
    editingId.value
      ? "급여량 정보가 수정되었습니다."
      : "관리 상품이 추가되었습니다.",
  );
}
function repurchase(item) {
  const product = products.find(
    (productItem) => productItem.id === item.productId,
  );
  if (!product) return;

  // 구매 이력이 연동된 항목은 당시 옵션을 재사용하고, 임시·직접 등록 상품은 최소 옵션을 기본값으로 사용합니다.
  const option = product.options?.includes(item.option)
    ? item.option
    : product.options?.[0] || product.option;
  const price = product.optionPrices?.[option] || product.price;

  sessionStorage.setItem(
    "powcast-buy-now",
    JSON.stringify({
      key: `repurchase-${product.id}-${option}`,
      id: product.id,
      pet: product.pet,
      name: product.name,
      image: product.image,
      price,
      option,
      quantity: 1,
      source: "consumption-repurchase",
    }),
  );
  router.push({ path: "/checkout", query: { mode: "buy-now" } });
}
function submitSettings() {
  saveSettings({ ...settingForm });
  showToast("계산 기준이 저장되었습니다.");
}
function requestSettings() {
  router.push("/mypage?verify=1");
}
</script>

<template>
  <main class="container member-dashboard consumption-page">
    <header class="dashboard-heading">
      <span>POWCAST SMART CARE</span>
      <h1>소진일 관리</h1>
      <p>구매 상품의 잔량과 하루 급여량으로 예상 소진일을 관리하세요.</p>
    </header>
    <div class="dashboard-layout">
      <MemberSidebar
        :name="displayName"
        :user-id="userId"
        active="consumption"
        @settings="requestSettings"
      />
      <div class="dashboard-content consumption-content">
        <section class="consumption-summary">
          <div>
            <small>가장 빠른 소진 예정</small>
            <h2>약 {{ enrichedItems[0]?.days || 0 }}일 후 재구매가 필요해요</h2>
            <p>등록한 잔량과 하루 급여량을 기준으로 계산한 예상 일정입니다.</p>
          </div>
          <button type="button" @click="router.push('/dog')">
            상품 보러가기
          </button>
        </section>

        <section class="managed-products">
          <header>
            <h2>관리 중인 상품</h2>
            <button type="button" @click="openAddForm">+ 상품 직접 추가</button>
          </header>
          <article v-for="item in enrichedItems" :key="item.id">
            <RouterLink
              class="managed-product-image-link"
              :to="`/products/${item.pet}/${item.productId}`"
              :aria-label="`${item.name} 상품 상세보기`"
            >
              <img :src="item.image" :alt="item.name" />
            </RouterLink>
            <div class="managed-info">
              <span>{{ item.category }}</span>
              <h3>
                <RouterLink :to="`/products/${item.pet}/${item.productId}`">
                  {{ item.name }}
                </RouterLink>
              </h3>
              <p>
                개봉일 {{ item.opened }} · 현재 잔량 {{ item.amount
                }}{{ item.unit }}
              </p>
              <div class="remaining">
                <strong :class="{ safe: item.days > 14 }"
                  >{{ item.days }}일 남음</strong
                ><b>하루 {{ item.daily }}{{ item.unit }} 기준</b>
              </div>
              <div class="consumption-progress">
                <i
                  :class="{ safe: item.days > 14 }"
                  :style="{ width: `${Math.min(100, item.days * 4)}%` }"
                />
              </div>
            </div>
            <div class="managed-actions">
              <button type="button" @click="repurchase(item)">재구매</button>
              <button type="button" @click="openEditForm(item)">
                급여량 수정
              </button>
            </div>
          </article>
        </section>

        <section class="consumption-setting-card">
          <h2>소진일 계산 기준</h2>
          <div>
            <label
              >반려동물<input
                v-model.number="settingForm.petWeight"
                type="number"
                min="0"
                step=".1"
              /><span>kg</span></label
            >
            <label
              >하루 급여량<input
                v-model.number="settingForm.dailyAmount"
                type="number"
                min="1"
              /><span>g</span></label
            >
            <label
              >하루 급여 횟수<input
                v-model.number="settingForm.dailyCount"
                type="number"
                min="1"
              /><span>회</span></label
            >
          </div>
          <p>
            백엔드 연동 후 회원별 설정과 관리 상품은 서버 DB를 기준으로
            저장됩니다.
          </p>
          <button type="button" @click="submitSettings">계산 기준 저장</button>
        </section>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="formOpen"
        class="common-modal-backdrop"
        @click.self="formOpen = false"
      >
        <form
          class="common-modal consumption-form-modal"
          @submit.prevent="submitItem"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="formOpen = false"
          >
            ×
          </button>
          <h2>{{ editingId ? "급여량 수정" : "관리 상품 추가" }}</h2>
          <label
            >상품<select v-model="form.productId" required>
              <option value="" disabled>상품을 선택해주세요</option>
              <option
                v-for="product in selectableProducts"
                :key="product.id"
                :value="product.id"
              >
                {{ product.name }}
              </option>
            </select></label
          >
          <label
            >개봉일<input v-model="form.opened" type="date" required
          /></label>
          <div>
            <label
              >현재 잔량<input
                v-model.number="form.amount"
                type="number"
                min="1"
                required /></label
            ><label
              >하루 급여량<input
                v-model.number="form.daily"
                type="number"
                min="1"
                required
            /></label>
          </div>
          <label
            >단위<select v-model="form.unit">
              <option value="g">g</option>
              <option value="ml">ml</option>
              <option value="정">정</option>
              <option value="개">개</option>
            </select></label
          >
          <div class="common-modal-actions">
            <button type="button" class="secondary" @click="formOpen = false">
              취소</button
            ><button class="primary">
              {{ editingId ? "수정하기" : "추가하기" }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>
    <Transition name="toast"
      ><div v-if="toast" class="site-toast" role="status">
        ✓ {{ toast }}
      </div></Transition
    >
  </main>
</template>
