<!-- 홈 검색 조건, 추천 상품, 공지 배너와 인기 상품 자동 전환을 관리하는 메인 화면입니다. -->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import ProductCard from "@/components/ProductCard.vue";
import UiTag from "@/components/UiTag.vue";
import { products } from "@/data/products";
import { notices } from "@/data/notices";

defineProps({ memberMode: { type: Boolean, default: false } });

const detailOpen = ref(true);
const router = useRouter();
const pet = ref("DOG");
const deviceSelected = ref(false);
const stock = ref("ALL");
const keyword = ref("");
const selectedIngredients = ref([]);
const selectedBrands = ref([]);
const selectedPriceRanges = ref([]);
const selectedRatings = ref([]);
const openFilterMenu = ref("");
const priceRanges = [
  { key: "under1", label: "1만원 이하" },
  { key: "1to3", label: "1~3만원" },
  { key: "3to5", label: "3~5만원" },
  { key: "over5", label: "5만원 이상" },
];
const ratingRanges = [
  { value: 1, label: "1점대" },
  { value: 2, label: "2점대" },
  { value: 3, label: "3점대" },
  { value: 4, label: "4점대" },
  { value: 4.5, label: "4.5점 이상" },
];
const ingredientDefinitions = [
  { label: "닭고기", keywords: ["닭", "치킨"] },
  { label: "연어", keywords: ["연어"] },
  { label: "참치", keywords: ["참치"] },
  { label: "오리", keywords: ["오리"] },
  { label: "소고기", keywords: ["소고기", "비프"] },
  { label: "양고기", keywords: ["양고기", "램"] },
  { label: "흰살생선", keywords: ["흰살생선", "대구", "피쉬", "복어"] },
  { label: "칠면조", keywords: ["칠면조", "터키"] },
  { label: "돼지고기", keywords: ["돼지고기", "포크"] },
  { label: "고구마", keywords: ["고구마"] },
  { label: "감자", keywords: ["감자", "포테이토"] },
  { label: "유산균", keywords: ["유산균", "프로바이오틱스"] },
  { label: "글루코사민", keywords: ["글루코사민"] },
  { label: "오메가", keywords: ["오메가"] },
];
const homePetProducts = computed(() =>
  products.filter((item) =>
    deviceSelected.value ? item.pet === "ALL" : item.pet === pet.value,
  ),
);
const homeIngredientOptions = computed(() =>
  ingredientDefinitions.filter((option) =>
    homePetProducts.value.some((item) =>
      option.keywords.some((keyword) => item.name.includes(keyword)),
    ),
  ),
);
const homeBrandOptions = computed(() =>
  [
    ...new Set(homePetProducts.value.map((item) => item.brand).filter(Boolean)),
  ].sort((a, b) => a.localeCompare(b, "ko")),
);
const priceSelectionLabel = computed(() =>
  selectedPriceRanges.value.length
    ? `${selectedPriceRanges.value.length}개 가격대 선택`
    : "가격대 전체",
);
const ratingSelectionLabel = computed(() =>
  selectedRatings.value.length
    ? `${selectedRatings.value.length}개 평점 선택`
    : "평점 전체",
);
const ingredientSelectionLabel = computed(() =>
  selectedIngredients.value.length
    ? `${selectedIngredients.value.length}개 원료 선택`
    : "원료 전체",
);
const brandSelectionLabel = computed(() =>
  selectedBrands.value.length
    ? `${selectedBrands.value.length}개 브랜드 선택`
    : "브랜드 전체",
);
const reviewHighlights = [
  {
    text: "기호성이 정말 좋아요. 입맛 까다로운 아이가 먼저 찾아 먹어요.",
    author: "보리 보호자",
  },
  {
    text: "성분을 확인하고 골랐는데 잘 먹고 배송 상태도 깔끔했습니다.",
    author: "콩이 보호자",
  },
  {
    text: "여러 제품을 비교해 봤는데 재구매하고 싶을 만큼 만족스러워요.",
    author: "두부 보호자",
  },
  {
    text: "급여하기 편하고 포장도 꼼꼼해서 안심하고 먹이고 있습니다.",
    author: "호두 보호자",
  },
  {
    text: "꾸준히 먹여 보니 만족도가 높아서 주변 집사에게도 추천했어요.",
    author: "나비 보호자",
  },
];
const topDogProducts = computed(() =>
  products
    .filter((item) => item.pet === "DOG" && item.stock > 0)
    .sort((a, b) => b.salesCount - a.salesCount)
    .slice(0, 5),
);
const topCatProducts = computed(() =>
  products
    .filter((item) => item.pet === "CAT" && item.stock > 0)
    .sort((a, b) => b.salesCount - a.salesCount)
    .slice(0, 5),
);
const popularPet = ref("DOG");
const popularPaused = ref(false);
const visiblePopularProducts = computed(() =>
  popularPet.value === "DOG" ? topDogProducts.value : topCatProducts.value,
);
const bestReviewProducts = computed(() =>
  products
    .filter((item) => item.stock > 0 && item.reviewCount > 0)
    .sort((a, b) => b.rating - a.rating || b.reviewCount - a.reviewCount)
    .slice(0, 5)
    .map((item, index) => ({
      ...item,
      featuredReview: reviewHighlights[index],
    })),
);
const bannerItems = computed(() => notices.filter((item) => item.pinned));
const bannerIndex = ref(0);
const currentBanner = computed(() => bannerItems.value[bannerIndex.value]);
let bannerTimer;
let popularTimer;

// 같은 함수로 여러 다중 선택 배열을 토글해 필터별 중복 코드를 줄입니다.
function toggleSelection(list, value) {
  const index = list.indexOf(value);
  if (index === -1) list.push(value);
  else list.splice(index, 1);
}
function toggleHomeFilterMenu(type) {
  openFilterMenu.value = openFilterMenu.value === type ? "" : type;
}
// 현재 필터 선택 영역이 아닌 곳을 누르면 열려 있는 드롭다운을 닫습니다.
function closeFilterMenuOnOutside(event) {
  if (!event.target.closest?.(".home-filter-panel .side-multi-select"))
    openFilterMenu.value = "";
}
function selectPet(value) {
  const selectionChanged = deviceSelected.value || pet.value !== value;
  deviceSelected.value = false;
  if (!selectionChanged) return;
  pet.value = value;
  selectedIngredients.value = [];
  selectedBrands.value = [];
  openFilterMenu.value = "";
}
function toggleDevice() {
  deviceSelected.value = !deviceSelected.value;
  selectedIngredients.value = [];
  selectedBrands.value = [];
  openFilterMenu.value = "";
}
function resetSearch() {
  keyword.value = "";
  stock.value = "ALL";
  openFilterMenu.value = "";
  selectedPriceRanges.value = [];
  selectedRatings.value = [];
  selectedIngredients.value = [];
  selectedBrands.value = [];
}
// 선택 조건을 URL 쿼리로 직렬화해 카테고리 화면이 같은 검색 상태를 복원하게 합니다.
function performSearch() {
  router.push({
    path: deviceSelected.value
      ? "/tableware"
      : pet.value === "CAT"
        ? "/cat"
        : "/dog",
    query: {
      keyword: keyword.value || undefined,
      ingredients: selectedIngredients.value.length
        ? selectedIngredients.value.join(",")
        : undefined,
      brands: selectedBrands.value.length
        ? selectedBrands.value.join(",")
        : undefined,
      inStock: stock.value === "AVAILABLE" ? "1" : undefined,
      prices: selectedPriceRanges.value.length
        ? selectedPriceRanges.value.join(",")
        : undefined,
      ratings: selectedRatings.value.length
        ? selectedRatings.value.join(",")
        : undefined,
    },
  });
}

function moveBanner(step) {
  if (!bannerItems.value.length) return;
  bannerIndex.value =
    (bannerIndex.value + step + bannerItems.value.length) % bannerItems.value.length;
  restartBannerTimer();
}
// 재시작 전에 기존 타이머를 지워 중복 interval이 쌓이는 것을 방지합니다.
function restartBannerTimer() {
  window.clearInterval(bannerTimer);
  if (!bannerItems.value.length) return;
  bannerTimer = window.setInterval(() => {
    bannerIndex.value = (bannerIndex.value + 1) % bannerItems.value.length;
  }, 10000);
}
function selectPopularPet(value) {
  popularPet.value = value;
  restartPopularTimer();
}
function restartPopularTimer() {
  window.clearInterval(popularTimer);
  popularTimer = window.setInterval(() => {
    if (!popularPaused.value)
      popularPet.value = popularPet.value === "DOG" ? "CAT" : "DOG";
  }, 10000);
}
onMounted(() => {
  restartBannerTimer();
  restartPopularTimer();
  document.addEventListener("pointerdown", closeFilterMenuOnOutside);
});
// 화면을 떠난 뒤에도 자동 전환이 실행되는 메모리 누수를 막습니다.
onBeforeUnmount(() => {
  window.clearInterval(bannerTimer);
  window.clearInterval(popularTimer);
  document.removeEventListener("pointerdown", closeFilterMenuOnOutside);
});
</script>

<template>
  <div class="container home">
    <section
      v-if="currentBanner"
      id="notice"
      class="notice notice-carousel"
      aria-label="공지 및 이벤트 배너"
    >
      <button
        class="notice-arrow prev"
        type="button"
        aria-label="이전 소식"
        @click="moveBanner(-1)"
      >
        ‹
      </button>
      <Transition name="notice-slide" mode="out-in">
        <RouterLink
          :key="currentBanner.id"
          class="notice-content"
          :to="`/notices?id=${currentBanner.id}`"
        >
          <b
            >{{
              currentBanner.type === "이벤트"
                ? "POWCAST EVENT"
                : "POWCAST NOTICE"
            }}
            · {{ currentBanner.date }}</b
          >
          <h2>{{ currentBanner.title }}</h2>
          <p>{{ currentBanner.content }}</p>
        </RouterLink>
      </Transition>
      <button
        class="notice-arrow next"
        type="button"
        aria-label="다음 소식"
        @click="moveBanner(1)"
      >
        ›
      </button>
      <div class="notice-dots" aria-label="배너 위치">
        <button
          v-for="(item, index) in bannerItems"
          :key="item.id"
          type="button"
          :class="{ on: index === bannerIndex }"
          :aria-label="`${index + 1}번째 소식`"
          @click="
            bannerIndex = index;
            restartBannerTimer();
          "
        />
      </div>
    </section>

    <section class="category-cards">
      <RouterLink to="/dog" class="dog-card"
        ><UiTag label="DOG FOOD &amp; CARE" variant="dog" />
        <h2>강아지 식품관</h2>
        <p>건식·습식 사료부터 수제간식, 유기농 영양제까지</p></RouterLink
      >
      <RouterLink to="/cat" class="cat-card"
        ><UiTag label="CAT FOOD &amp; CARE" variant="cat" />
        <h2>고양이 식품관</h2>
        <p>기호성 높은 간식, 프리미엄 습식캔, 맞춤 사료</p></RouterLink
      >
    </section>

    <section class="search-box home-search">
      <h2>내가 찾는 상품 검색하기!</h2>
      <form @submit.prevent="performSearch">
        <div class="search-row">
          <label class="search-keyword"
            ><span class="search-icon" aria-hidden="true" /><input
              v-model.trim="keyword"
              placeholder="검색하세요. 예) 연어 사료, 고양이 간식, 츄르"
          /></label>
          <button
            type="button"
            class="detail-button"
            :class="{ open: detailOpen }"
            :aria-expanded="detailOpen"
            aria-controls="home-detail-filters"
            @click="detailOpen = !detailOpen"
          >
            <span class="filter-icon" aria-hidden="true"><i /><i /><i /></span
            ><span>상세 조건</span
            ><i class="filter-chevron" aria-hidden="true" />
          </button>
          <button type="submit" class="search-submit">검색하기</button>
        </div>
        <Transition name="filter-drop"
          ><div
            v-if="detailOpen"
            id="home-detail-filters"
            class="home-filter-panel"
          >
            <fieldset class="pet-filter">
              <legend>상품관</legend>
              <div>
                <button
                  type="button"
                  :class="{ on: pet === 'DOG' && !deviceSelected }"
                  @click="selectPet('DOG')"
                >
                  🐶 강아지</button
                ><button
                  type="button"
                  :class="{ on: pet === 'CAT' && !deviceSelected }"
                  @click="selectPet('CAT')"
                >
                  🐱 고양이</button
                ><button
                  type="button"
                  :class="{ on: deviceSelected }"
                  :aria-pressed="deviceSelected"
                  @click="toggleDevice"
                >
                  식기·급수
                </button>
              </div>
              <small>식기·급수는 필요할 때만 선택할 수 있어요.</small>
            </fieldset>
            <fieldset class="side-multi-filter">
              <legend>가격</legend>
              <div class="side-multi-select">
                <button
                  type="button"
                  class="side-multi-trigger"
                  :class="{ open: openFilterMenu === 'price' }"
                  @click="toggleHomeFilterMenu('price')"
                >
                  <span>{{ priceSelectionLabel }}</span
                  ><i />
                </button>
                <div v-if="openFilterMenu === 'price'" class="side-multi-menu">
                  <label v-for="item in priceRanges" :key="item.key"
                    ><input
                      type="checkbox"
                      :checked="selectedPriceRanges.includes(item.key)"
                      @change="toggleSelection(selectedPriceRanges, item.key)"
                    /><i /><span>{{ item.label }}</span></label
                  >
                </div>
              </div>
            </fieldset>
            <fieldset class="side-multi-filter">
              <legend>원료</legend>
              <div class="side-multi-select">
                <button
                  type="button"
                  class="side-multi-trigger"
                  :class="{ open: openFilterMenu === 'ingredient' }"
                  @click="toggleHomeFilterMenu('ingredient')"
                >
                  <span>{{ ingredientSelectionLabel }}</span
                  ><i />
                </button>
                <div
                  v-if="openFilterMenu === 'ingredient'"
                  class="side-multi-menu"
                >
                  <label v-for="item in homeIngredientOptions" :key="item.label"
                    ><input
                      type="checkbox"
                      :checked="selectedIngredients.includes(item.label)"
                      @change="toggleSelection(selectedIngredients, item.label)"
                    /><i /><span>{{ item.label }}</span></label
                  >
                </div>
              </div>
            </fieldset>
            <fieldset class="side-multi-filter">
              <legend>브랜드</legend>
              <div class="side-multi-select">
                <button
                  type="button"
                  class="side-multi-trigger"
                  :class="{ open: openFilterMenu === 'brand' }"
                  @click="toggleHomeFilterMenu('brand')"
                >
                  <span>{{ brandSelectionLabel }}</span
                  ><i />
                </button>
                <div v-if="openFilterMenu === 'brand'" class="side-multi-menu">
                  <label v-for="item in homeBrandOptions" :key="item"
                    ><input
                      type="checkbox"
                      :checked="selectedBrands.includes(item)"
                      @change="toggleSelection(selectedBrands, item)"
                    /><i /><span>{{ item }}</span></label
                  >
                </div>
              </div>
            </fieldset>
            <fieldset class="stock-filter">
              <legend>재고 상태</legend>
              <div>
                <button
                  type="button"
                  :class="{ on: stock === 'ALL' }"
                  @click="stock = 'ALL'"
                >
                  전체</button
                ><button
                  type="button"
                  :class="{ on: stock === 'AVAILABLE' }"
                  @click="stock = 'AVAILABLE'"
                >
                  재고 있음
                </button>
              </div>
            </fieldset>
            <fieldset class="side-multi-filter">
              <legend>평점</legend>
              <div class="side-multi-select">
                <button
                  type="button"
                  class="side-multi-trigger"
                  :class="{ open: openFilterMenu === 'rating' }"
                  @click="toggleHomeFilterMenu('rating')"
                >
                  <span>{{ ratingSelectionLabel }}</span
                  ><i />
                </button>
                <div v-if="openFilterMenu === 'rating'" class="side-multi-menu">
                  <label v-for="item in ratingRanges" :key="item.value"
                    ><input
                      type="checkbox"
                      :checked="selectedRatings.includes(item.value)"
                      @change="toggleSelection(selectedRatings, item.value)"
                    /><i /><span>★ {{ item.label }}</span></label
                  >
                </div>
              </div>
            </fieldset>
            <button type="button" class="filter-reset" @click="resetSearch">
              ↻ 초기화
            </button>
          </div></Transition
        >
      </form>
    </section>

    <section
      v-if="memberMode"
      class="product-section recommendation member-recommendation"
    >
      <div class="recommendation-heading">
        <div>
          <h2>나와 비슷한 집사의 선택!</h2>
          <p>회원님의 관심사와 비슷한 집사들이 선택한 상품이에요.</p>
        </div>
        <span>MEMBER PICK</span>
      </div>
      <div class="product-grid">
        <ProductCard
          v-for="p in products.slice(0, 5)"
          :key="p.id"
          :product="p"
          :pet="p.pet"
        />
      </div>
    </section>

    <section
      class="product-section popular-products"
      @mouseenter="popularPaused = true"
      @mouseleave="popularPaused = false"
      @focusin="popularPaused = true"
      @focusout="popularPaused = false"
    >
      <div class="popular-heading">
        <div>
          <h2>인기 상품 TOP 5</h2>
          <p>판매 중인 상품 가운데 가장 사랑받는 상품을 만나보세요.</p>
        </div>
        <div
          class="popular-tabs"
          role="tablist"
          aria-label="반려동물별 인기 상품"
        >
          <button
            type="button"
            role="tab"
            :aria-selected="popularPet === 'DOG'"
            :class="{ on: popularPet === 'DOG' }"
            @click="selectPopularPet('DOG')"
          >
            강아지
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="popularPet === 'CAT'"
            :class="{ on: popularPet === 'CAT' }"
            @click="selectPopularPet('CAT')"
          >
            고양이
          </button>
        </div>
      </div>
      <Transition name="popular-slide" mode="out-in">
        <div
          :key="popularPet"
          class="product-grid"
          role="tabpanel"
          :aria-label="`${popularPet === 'DOG' ? '강아지' : '고양이'} 인기 상품`"
        >
          <ProductCard
            v-for="p in visiblePopularProducts"
            :key="p.id"
            :product="p"
            :pet="p.pet"
          />
        </div>
      </Transition>
    </section>
    <section class="product-section review-section">
      <h2>BEST 5 리뷰 상품</h2>
      <div class="product-grid">
        <article
          v-for="p in bestReviewProducts"
          :key="p.id"
          class="review-product"
        >
          <ProductCard :product="p" :pet="p.pet" />
          <div class="review-highlight">
            <b>BEST REVIEW</b>
            <p>“{{ p.featuredReview.text }}”</p>
            <small>{{ p.featuredReview.author }}</small>
          </div>
        </article>
      </div>
    </section>
    <section v-if="!memberMode" class="product-section recommendation">
      <h2>나와 비슷한 집사의 선택!</h2>
      <div class="product-grid blurred">
        <ProductCard
          v-for="p in products.slice(0, 5)"
          :key="p.id"
          :product="p"
          :pet="p.pet"
        />
      </div>
      <div class="member-lock">
        🔒<small>MEMBERS ONLY</small>
        <h3>추천 기능은 회원에게만 제공하고 있는 기능입니다.</h3>
        <p>로그인하시면 맞춤 추천을 받아보실 수 있어요.</p>
        <div>
          <RouterLink to="/login">로그인</RouterLink
          ><RouterLink to="/signup">회원가입</RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>
