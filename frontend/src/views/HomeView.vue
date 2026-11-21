<!-- 홈 검색 조건, 추천 상품, 공지 배너와 인기 상품 자동 전환을 관리하는 메인 화면입니다. -->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import ProductCard from "@/components/ProductCard.vue";
import UiTag from "@/components/UiTag.vue";
import { productService } from "@/services/productService";
import { noticeService } from "@/services/noticeService";
import { petService } from "@/services/petService";
import { getMyProfile } from "@/services/memberService";
import { aiRecommendationService } from "@/services/aiRecommendationService";

const props = defineProps({ memberMode: { type: Boolean, default: false } });

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
const homeBrandOptions = ref([]);
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
const topDogProducts = ref([]);
const topCatProducts = ref([]);
const bestReviewProducts = ref([]);
const bannerItems = ref([]);
const recommendationPets = ref([]);
const selectedRecommendationPetId = ref("");
const recommendationPetType = ref("DOG");
const recommendedProducts = ref([]);
const recommendationLoading = ref(false);
const recommendationMessage = ref("");
const popularPet = ref("DOG");
const popularPaused = ref(false);
const visiblePopularProducts = computed(() =>
  popularPet.value === "DOG" ? topDogProducts.value : topCatProducts.value,
);
const bannerIndex = ref(0);
const currentBanner = computed(() => bannerItems.value[bannerIndex.value]);
let bannerTimer;
let popularTimer;
let recommendationTimer;
const guestRecommendationProducts = computed(() =>
  [...topDogProducts.value, ...topCatProducts.value].slice(0, 5),
);

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
      keyword: keyword.value.trim() || undefined,
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
      searched: "1",
    },
  });
}

function moveBanner(step) {
  if (!bannerItems.value.length) return;
  bannerIndex.value =
    (bannerIndex.value + step + bannerItems.value.length) %
    bannerItems.value.length;
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
// # 추천 탭은 사용자가 스크롤해도 15초마다 강아지·고양이 추천을 자동 전환한다.
function selectRecommendationPetType(type) {
  recommendationPetType.value = type;
  restartRecommendationTimer();
}
function restartRecommendationTimer() {
  window.clearInterval(recommendationTimer);
  recommendationTimer = window.setInterval(() => {
    recommendationPetType.value =
      recommendationPetType.value === "DOG" ? "CAT" : "DOG";
  }, 15000);
}
onMounted(() => {
  loadHomeData();
  restartBannerTimer();
  restartPopularTimer();
  restartRecommendationTimer();
  document.addEventListener("pointerdown", closeFilterMenuOnOutside);
});

async function loadHomeData() {
  const saleFilters = { sortType: "SALES_DESC", inStockOnly: true };
  const reviewFilters = { sortType: "RATING_DESC", inStockOnly: true };
  try {
    const [dog, cat, reviewProducts, notices] = await Promise.all([
      productService.list({ ...saleFilters, productType: "DOG" }, "DOG"),
      productService.list({ ...saleFilters, productType: "CAT" }, "CAT"),
      productService.bestReviews({ ...reviewFilters, limit: 5 }),
      noticeService.list(),
    ]);
    topDogProducts.value = dog.slice(0, 5);
    topCatProducts.value = cat.slice(0, 5);
    bestReviewProducts.value = reviewProducts.slice(0, 5);
    bannerItems.value = notices.filter((item) => item.pinned);
    bannerIndex.value = 0;
    restartBannerTimer();
    homeBrandOptions.value = [
      ...new Set([...dog, ...cat].map((item) => item.brand).filter(Boolean)),
    ].sort((a, b) => a.localeCompare(b, "ko"));
    if (props.memberMode) await loadRecommendationPets(dog, cat);
  } catch {
    // 공통 API 오류 처리기가 화면 수준의 오류를 안내합니다.
  }
}

async function loadRecommendationPets(dogProducts, catProducts) {
  try {
    const pets = await petService.getPets();
    recommendationPets.value = (Array.isArray(pets) ? pets : []).filter(
      (pet) => pet.id && pet.birthday,
    );
    const firstPet = recommendationPets.value[0];
    recommendationPetType.value = firstPet?.petType === "CAT" ? "CAT" : "DOG";
    selectedRecommendationPetId.value = firstPet?.id || "";
    if (!selectedRecommendationPetId.value)
      recommendationMessage.value =
        "생일을 포함한 반려동물 정보를 등록하면 AI 맞춤 추천을 받을 수 있습니다.";
  } catch {
    recommendationMessage.value = "반려동물 정보를 불러오지 못했습니다.";
  }
}

async function loadRecommendations(
  dogProducts = topDogProducts.value,
  catProducts = topCatProducts.value,
) {
  const pet = recommendationPets.value.find(
    (item) => String(item.id) === String(selectedRecommendationPetId.value),
  );
  if (!pet) return;
  recommendationLoading.value = true;
  recommendationMessage.value = "";
  try {
    const member = await getMyProfile();
    const candidates = [...dogProducts, ...catProducts].map((product) => ({
      productId: product.id,
      variantId: product.variantId,
      name: product.name,
      brand: product.brand || null,
      categoryId: product.categoryId,
      categoryName: product.category,
      imagePath: product.image || null,
      salePrice: product.price,
      averageRating: product.rating,
      reviewCount: product.reviewCount,
      salesCount: product.salesCount,
      soldOut: product.stock <= 0,
      petType: product.productType,
      mainIngredient: product.mainIngredient || null,
      ...(product.lifeStage ? { lifeStage: product.lifeStage } : {}),
    }));
    const result = await aiRecommendationService.recommend({
      memberId: member.id,
      pet: {
        petId: pet.id,
        petType: pet.petType,
        birthday: pet.birthday,
        breedName: pet.breedName || null,
        weightKg: pet.weightKg || null,
        allergies: pet.allergies
          ? pet.allergies
              .split(",")
              .map((item) => item.trim())
              .filter(Boolean)
          : [],
      },
      products: candidates,
      limit: 5,
    });
    recommendedProducts.value = (result.items || []).map((item) => ({
      id: item.productId,
      variantId: item.variantId,
      name: item.name,
      brand: item.brand,
      category: item.categoryName || "기타",
      image: item.imagePath || "/images/product-placeholder.svg",
      price: Number(item.salePrice || 0),
      rating: Number(item.averageRating || 0),
      reviewCount: Number(item.reviewCount || 0),
      stock: 1,
      pet: pet.petType,
      recommendationReasons: item.recommendationReasons || [],
    }));
    if (!recommendedProducts.value.length)
      recommendationMessage.value = "조건에 맞는 추천 상품이 없습니다.";
  } catch (error) {
    recommendationMessage.value =
      error.message || "AI 추천을 불러오지 못했습니다.";
  } finally {
    recommendationLoading.value = false;
  }
}

watch(
  [recommendationPetType, recommendationPets],
  () => {
    const selectedPet = recommendationPets.value.find(
      (item) => item.petType === recommendationPetType.value,
    );
    selectedRecommendationPetId.value = selectedPet?.id || "";
    if (selectedPet) loadRecommendations();
    else {
      recommendedProducts.value = [];
      recommendationMessage.value = `${recommendationPetType.value === "DOG" ? "강아지" : "고양이"} 반려동물을 등록하면 AI 맞춤 추천을 받을 수 있습니다.`;
    }
  },
  { immediate: true },
);
// 화면을 떠난 뒤에도 자동 전환이 실행되는 메모리 누수를 막습니다.
onBeforeUnmount(() => {
  window.clearInterval(bannerTimer);
  window.clearInterval(popularTimer);
  window.clearInterval(recommendationTimer);
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
                ? "PAWCAST EVENT"
                : "PAWCAST NOTICE"
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
                  강아지</button
                ><button
                  type="button"
                  :class="{ on: pet === 'CAT' && !deviceSelected }"
                  @click="selectPet('CAT')"
                >
                  고양이</button
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
          <h2>AI 맞춤 추천</h2>
          <p>등록한 반려동물 정보와 실제 판매 상품을 바탕으로 추천합니다.</p>
        </div>
        <div
          class="recommendation-pet-banner"
          role="tablist"
          aria-label="추천 반려동물 종류"
        >
          <button
            type="button"
            role="tab"
            :aria-selected="recommendationPetType === 'DOG'"
            :class="{ on: recommendationPetType === 'DOG' }"
            @click="selectRecommendationPetType('DOG')"
          >
            강아지 추천
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="recommendationPetType === 'CAT'"
            :class="{ on: recommendationPetType === 'CAT' }"
            @click="selectRecommendationPetType('CAT')"
          >
            고양이 추천
          </button>
        </div>
      </div>
      <p v-if="recommendationLoading" class="dashboard-order-state">
        AI 추천을 불러오는 중입니다.
      </p>
      <p v-else-if="recommendationMessage" class="dashboard-order-state">
        {{ recommendationMessage }}
      </p>
      <div v-else class="product-grid">
        <ProductCard
          v-for="product in recommendedProducts"
          :key="product.id"
          :product="product"
          :pet="product.pet"
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
        </article>
      </div>
    </section>
    <section
      v-if="!memberMode"
      class="product-section recommendation guest-recommendation"
      aria-labelledby="guest-ai-title"
    >
      <div class="recommendation-heading">
        <div>
          <h2 id="guest-ai-title">나와 비슷한 집사의 선택!</h2>
          <p>로그인 후 우리 아이의 정보에 맞는 AI 추천을 만나보세요.</p>
        </div>
      </div>
      <div class="guest-recommendation-preview" aria-hidden="true">
        <div class="product-grid">
          <ProductCard
            v-for="product in guestRecommendationProducts"
            :key="product.id"
            :product="product"
            :pet="product.pet"
          />
        </div>
        <div class="guest-ai-lock">
          <span aria-hidden="true">🔒</span><small>MEMBERS ONLY</small>
          <strong>추천 기능은 회원에게만 제공하고 있는 기능입니다.</strong>
          <p>로그인하시면 AI 맞춤 추천을 받아보실 수 있어요.</p>
          <div>
            <button
              type="button"
              class="guest-login"
              @click="router.push('/login')"
            >
              로그인</button
            ><button
              type="button"
              class="guest-signup"
              @click="router.push('/signup')"
            >
              회원가입
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
