<!-- URL 쿼리와 사용자의 필터 선택을 조합해 상품 목록을 계산하는 카테고리 화면입니다. -->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import ProductCard from "@/components/ProductCard.vue";
import { productService } from "@/services/productService";

const props = defineProps({ pet: String, device: Boolean });
const route = useRoute();
const router = useRouter();
const products = ref([]);
const loadError = ref("");
const foodCategories = ["사료", "간식", "영양제"];
const deviceCategories = ["급식기", "급수기", "일반 식기", "정수 / 필터"];
const subcategoryMap = {
  사료: ["전체보기", "건식 사료", "습식 / 화식", "기능성 사료", "연령별 사료"],
  간식: [
    "전체보기",
    "덴탈 간식",
    "동결건조 / 트릿",
    "육포 / 저키",
    "츄르 / 파우치",
  ],
  영양제: [
    "전체보기",
    "관절 / 뼈",
    "피부 / 모질",
    "장 / 유산균",
    "눈 / 종합영양",
  ],
  급식기: ["전체보기", "자동 급식기", "수동 급식기", "휴대용 급식기"],
  급수기: ["전체보기", "자동 급수기", "휴대용 급수기"],
  "일반 식기": ["전체보기", "도자기 식기", "스테인리스 식기", "높이 조절 식기"],
  "정수 / 필터": ["전체보기", "정수 필터", "교체용 필터", "세척 용품"],
};
const categories = computed(() =>
  props.device ? deviceCategories : foodCategories,
);
const category = ref(
  categories.value.includes(String(route.query.category))
    ? String(route.query.category)
    : categories.value[0],
);
const subcategory = ref("전체보기");
const sort = ref("popular");
const gridMode = ref("five");
const keyword = ref(String(route.query.keyword || ""));
const inStock = ref(route.query.inStock === "1");
const selectedIngredients = ref(
  String(route.query.ingredients || route.query.ingredient || "")
    .split(",")
    .filter(Boolean),
);
const selectedBrands = ref(
  String(route.query.brands || route.query.brand || "")
    .split(",")
    .filter(Boolean),
);
const sideDetailOpen = ref(true);
const priceFilterOpen = ref(false);
const ratingFilterOpen = ref(false);
const ingredientFilterOpen = ref(false);
const brandFilterOpen = ref(false);
const selectedPriceRanges = ref(
  String(route.query.prices || "")
    .split(",")
    .filter(Boolean),
);
const selectedRatings = ref(
  String(route.query.ratings || "")
    .split(",")
    .filter(Boolean)
    .map(Number),
);
const priceRanges = [
  { key: "under1", label: "1만원 이하", min: 0, max: 10000 },
  { key: "1to3", label: "1~3만원", min: 10000, max: 30000 },
  { key: "3to5", label: "3~5만원", min: 30000, max: 50000 },
  { key: "over5", label: "5만원 이상", min: 50000, max: Infinity },
];
const ratingRanges = [
  { label: "1점대", value: 1, min: 1, max: 2 },
  { label: "2점대", value: 2, min: 2, max: 3 },
  { label: "3점대", value: 3, min: 3, max: 4 },
  { label: "4점대", value: 4, min: 4, max: 4.5 },
  { label: "4.5점 이상", value: 4.5, min: 4.5, max: 5.1 },
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
  { label: "활성탄", keywords: ["활성탄"] },
  { label: "스테인리스", keywords: ["스테인리스"] },
  { label: "도자기", keywords: ["도자기"] },
  { label: "실리콘", keywords: ["실리콘"] },
];
const appliedFilters = ref({
  keyword: keyword.value,
  inStock: inStock.value,
  ingredients: [...selectedIngredients.value],
  brands: [...selectedBrands.value],
  priceRanges: [...selectedPriceRanges.value],
  ratings: [...selectedRatings.value],
});
const subcategories = computed(
  () => subcategoryMap[category.value] || ["전체보기"],
);
const scopedProducts = computed(() =>
  products.value.filter((item) => {
    const inSection = props.device
      ? item.mainCategory === "식기·급수관"
      : item.pet === props.pet;
    return inSection && item.category === category.value;
  }),
);
const brandOptions = computed(() =>
  [
    ...new Set(scopedProducts.value.map((item) => item.brand).filter(Boolean)),
  ].sort((a, b) => a.localeCompare(b, "ko")),
);
const ingredientOptions = computed(() =>
  ingredientDefinitions.filter((option) =>
    scopedProducts.value.some((item) =>
      option.keywords.some((keyword) => item.name.includes(keyword)),
    ),
  ),
);
const selectedIngredientLabel = computed(() =>
  selectedIngredients.value.length
    ? `${selectedIngredients.value.length}개 원료 선택`
    : "원료 전체",
);
const selectedBrandLabel = computed(() =>
  selectedBrands.value.length
    ? `${selectedBrands.value.length}개 브랜드 선택`
    : "브랜드 전체",
);
const activeFilterCount = computed(
  () =>
    appliedFilters.value.priceRanges.length +
    appliedFilters.value.ratings.length +
    appliedFilters.value.ingredients.length +
    appliedFilters.value.brands.length +
    Number(!!appliedFilters.value.keyword) +
    Number(appliedFilters.value.inStock),
);
const selectedPriceLabel = computed(() =>
  selectedPriceRanges.value.length
    ? `${selectedPriceRanges.value.length}개 가격대 선택`
    : "가격대 선택",
);
const selectedRatingLabel = computed(() =>
  selectedRatings.value.length
    ? `${selectedRatings.value.length}개 평점 선택`
    : "평점 선택",
);
watch(
  () => [props.pet, props.device],
  async () => {
    category.value = categories.value[0];
    subcategory.value = "전체보기";
    await loadProducts();
  },
);

async function loadProducts() {
  try {
    loadError.value = "";
    products.value = await productService.list(
      { productType: props.device ? "DEVICE" : props.pet },
      props.device ? "ALL" : props.pet,
    );
  } catch (error) {
    products.value = [];
    loadError.value = error.message || "상품 목록을 불러오지 못했습니다.";
  }
}

function selectCategory(value) {
  category.value = value;
  subcategory.value = "전체보기";
  selectedIngredients.value = [];
  selectedBrands.value = [];
  router.replace({ query: { category: value } });
}
function selectSubcategory(value) {
  subcategory.value = value;
  router.replace({
    query: {
      category: category.value,
      subcategory: value === "전체보기" ? undefined : value,
    },
  });
}
function toggleSelection(list, value) {
  const index = list.indexOf(value);
  if (index === -1) list.push(value);
  else list.splice(index, 1);
}
function toggleFilterMenu(type) {
  const next = { price: false, rating: false, ingredient: false, brand: false };
  next[type] = !{
    price: priceFilterOpen.value,
    rating: ratingFilterOpen.value,
    ingredient: ingredientFilterOpen.value,
    brand: brandFilterOpen.value,
  }[type];
  priceFilterOpen.value = next.price;
  ratingFilterOpen.value = next.rating;
  ingredientFilterOpen.value = next.ingredient;
  brandFilterOpen.value = next.brand;
}
function closeFilterMenus() {
  priceFilterOpen.value = false;
  ratingFilterOpen.value = false;
  ingredientFilterOpen.value = false;
  brandFilterOpen.value = false;
}
// 사이드바의 현재 선택 상자가 아닌 곳을 누르면 열린 드롭다운을 닫습니다.
function closeFilterMenuOnOutside(event) {
  if (!event.target.closest?.(".category-sidebar .side-multi-select"))
    closeFilterMenus();
}
onMounted(async () => {
  document.addEventListener("pointerdown", closeFilterMenuOnOutside);
  await loadProducts();
});
onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", closeFilterMenuOnOutside);
});
function clearFilters() {
  keyword.value = "";
  inStock.value = false;
  selectedIngredients.value = [];
  selectedBrands.value = [];
  selectedPriceRanges.value = [];
  selectedRatings.value = [];
  applyFilters();
}
function applyFilters() {
  appliedFilters.value = {
    keyword: keyword.value.trim(),
    inStock: inStock.value,
    ingredients: [...selectedIngredients.value],
    brands: [...selectedBrands.value],
    priceRanges: [...selectedPriceRanges.value],
    ratings: [...selectedRatings.value],
  };
}

const list = computed(() => {
  let result = props.device
    ? products.value.filter((item) => item.mainCategory === "식기·급수관")
    : products.value.filter((item) => item.pet === props.pet);
  result = result.filter((item) => item.category === category.value);
  if (subcategory.value !== "전체보기")
    result = result.filter((item) => item.subcategory === subcategory.value);
  const filters = appliedFilters.value;
  if (filters.keyword)
    result = result.filter((item) => item.name.includes(filters.keyword));
  if (filters.inStock) result = result.filter((item) => item.stock > 0);
  if (filters.ingredients.length) {
    result = result.filter((item) =>
      filters.ingredients.some((label) => {
        const definition = ingredientDefinitions.find(
          (option) => option.label === label,
        );
        return definition?.keywords.some((keyword) =>
          item.name.includes(keyword),
        );
      }),
    );
  }
  if (filters.brands.length)
    result = result.filter((item) => filters.brands.includes(item.brand));
  if (filters.priceRanges.length)
    result = result.filter((item) =>
      filters.priceRanges.some((key) => {
        const range = priceRanges.find((option) => option.key === key);
        return item.price >= range.min && item.price < range.max;
      }),
    );
  if (filters.ratings.length)
    result = result.filter((item) =>
      filters.ratings.some((value) => {
        const range = ratingRanges.find((option) => option.value === value);
        return item.rating >= range.min && item.rating < range.max;
      }),
    );
  if (sort.value === "popular")
    result.sort((a, b) => b.popularity - a.popularity);
  if (sort.value === "new")
    result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  if (sort.value === "low") result.sort((a, b) => a.price - b.price);
  if (sort.value === "high") result.sort((a, b) => b.price - a.price);
  if (sort.value === "discount")
    result.sort((a, b) => b.discountRate - a.discountRate);
  if (sort.value === "sales")
    result.sort((a, b) => b.salesCount - a.salesCount);
  if (sort.value === "reviews")
    result.sort((a, b) => b.reviewCount - a.reviewCount);
  if (sort.value === "rating") result.sort((a, b) => b.rating - a.rating);
  return result;
});
</script>

<template>
  <div class="container category-page">
    <p class="breadcrumb">
      홈 ›
      {{ device ? "식기·급수관" : pet === "CAT" ? "고양이관" : "강아지관" }}
    </p>
    <header>
      <small>POWCAST CATEGORY</small>
      <h1>
        {{
          device
            ? "식기·급수 용품"
            : `${pet === "CAT" ? "고양이" : "강아지"} 용품`
        }}
      </h1>
      <p>
        {{
          device
            ? "편안한 급식과 깨끗한 음수를 위한 생활용품을 모았어요."
            : `${pet === "CAT" ? "고양이" : "강아지"}의 건강하고 즐거운 하루를 위한 상품을 모았어요.`
        }}
      </p>
    </header>

    <div class="catalog">
      <div class="catalog-side-column">
        <section class="category-selection-summary">
          <small>현재 선택</small
          ><strong>{{
            device ? "식기·급수관" : pet === "CAT" ? "고양이관" : "강아지관"
          }}</strong
          ><span>{{ category }} · {{ subcategory }}</span
          ><em>{{
            activeFilterCount
              ? `${activeFilterCount}개 검색 조건 적용`
              : "전체 상품 기준"
          }}</em>
        </section>
        <aside class="category-sidebar">
          <h2>카테고리</h2>
          <button
            v-for="item in categories"
            :key="item"
            :class="{ on: category === item }"
            @click="selectCategory(item)"
          >
            {{ item }}
          </button>
          <h3>검색하기</h3>
          <label class="side-keyword"
            ><span class="side-search-icon" aria-hidden="true" /><input
              v-model="keyword"
              placeholder="검색어를 입력하세요."
          /></label>
          <button
            class="side-detail"
            :class="{ open: sideDetailOpen }"
            type="button"
            :aria-expanded="sideDetailOpen"
            aria-controls="category-detail-filters"
            @click="sideDetailOpen = !sideDetailOpen"
          >
            <span class="filter-icon" aria-hidden="true"><i /><i /><i /></span
            ><span>상세 조건</span
            ><i class="filter-chevron" aria-hidden="true" />
          </button>
          <div
            v-show="sideDetailOpen"
            id="category-detail-filters"
            class="sidebar-filters"
          >
            <fieldset class="side-multi-filter">
              <legend>가격</legend>
              <div class="side-multi-select">
                <button
                  type="button"
                  class="side-multi-trigger"
                  :class="{ open: priceFilterOpen }"
                  :aria-expanded="priceFilterOpen"
                  @click="toggleFilterMenu('price')"
                >
                  <span>{{ selectedPriceLabel }}</span
                  ><i aria-hidden="true" />
                </button>
                <div v-if="priceFilterOpen" class="side-multi-menu">
                  <label v-for="item in priceRanges" :key="item.key"
                    ><input
                      type="checkbox"
                      :checked="selectedPriceRanges.includes(item.key)"
                      @change="toggleSelection(selectedPriceRanges, item.key)"
                    /><i aria-hidden="true" /><span>{{
                      item.label
                    }}</span></label
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
                  :class="{ open: ingredientFilterOpen }"
                  :aria-expanded="ingredientFilterOpen"
                  @click="toggleFilterMenu('ingredient')"
                >
                  <span>{{ selectedIngredientLabel }}</span
                  ><i aria-hidden="true" />
                </button>
                <div v-if="ingredientFilterOpen" class="side-multi-menu">
                  <label v-for="item in ingredientOptions" :key="item.label"
                    ><input
                      type="checkbox"
                      :checked="selectedIngredients.includes(item.label)"
                      @change="toggleSelection(selectedIngredients, item.label)"
                    /><i aria-hidden="true" /><span>{{
                      item.label
                    }}</span></label
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
                  :class="{ open: brandFilterOpen }"
                  :aria-expanded="brandFilterOpen"
                  @click="toggleFilterMenu('brand')"
                >
                  <span>{{ selectedBrandLabel }}</span
                  ><i aria-hidden="true" />
                </button>
                <div
                  v-if="brandFilterOpen"
                  class="side-multi-menu side-brand-menu"
                >
                  <label v-for="item in brandOptions" :key="item"
                    ><input
                      type="checkbox"
                      :checked="selectedBrands.includes(item)"
                      @change="toggleSelection(selectedBrands, item)"
                    /><i aria-hidden="true" /><span>{{ item }}</span></label
                  >
                </div>
              </div>
            </fieldset>
            <fieldset>
              <legend>재고 상태</legend>
              <div class="side-stock">
                <button
                  type="button"
                  :class="{ on: !inStock }"
                  @click="inStock = false"
                >
                  전체</button
                ><button
                  type="button"
                  :class="{ on: inStock }"
                  @click="inStock = true"
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
                  :class="{ open: ratingFilterOpen }"
                  :aria-expanded="ratingFilterOpen"
                  @click="toggleFilterMenu('rating')"
                >
                  <span>{{ selectedRatingLabel }}</span
                  ><i aria-hidden="true" />
                </button>
                <div v-if="ratingFilterOpen" class="side-multi-menu">
                  <label v-for="item in ratingRanges" :key="item.value"
                    ><input
                      type="checkbox"
                      :checked="selectedRatings.includes(item.value)"
                      @change="toggleSelection(selectedRatings, item.value)"
                    /><i aria-hidden="true" /><span
                      >★ {{ item.label }}</span
                    ></label
                  >
                </div>
              </div>
            </fieldset>
          </div>
          <div class="sidebar-actions">
            <button type="button" @click="clearFilters">초기화</button
            ><button type="button" class="apply" @click="applyFilters">
              검색
            </button>
          </div>
        </aside>
      </div>

      <section class="catalog-main">
        <div class="chips">
          <button
            v-for="item in subcategories"
            :key="item"
            :class="{ on: subcategory === item }"
            @click="selectSubcategory(item)"
          >
            {{ item }}
          </button>
        </div>
        <div class="sort-menu">
          <button
            v-for="item in [
              { k: 'popular', v: '인기도순' },
              { k: 'new', v: '최신 등록순' },
              { k: 'low', v: '낮은 가격순' },
              { k: 'high', v: '높은 가격순' },
              { k: 'discount', v: '할인율순' },
              { k: 'sales', v: '판매 많은순' },
              { k: 'reviews', v: '리뷰 많은순' },
              { k: 'rating', v: '평점 높은순' },
            ]"
            :key="item.k"
            :class="{ on: sort === item.k }"
            @click="sort = item.k"
          >
            {{ item.v }}
          </button>
        </div>
        <div class="catalog-meta">
          <b
            >총 <strong>{{ list.length }}</strong
            >개 상품</b
          >
          <div class="view-controls">
            <div class="grid-buttons" aria-label="상품 보기 방식">
              <button
                type="button"
                :class="{ on: gridMode === 'five' }"
                aria-label="5열로 보기"
                @click="gridMode = 'five'"
              >
                <span v-for="n in 10" :key="n" /></button
              ><button
                type="button"
                :class="{ on: gridMode === 'four' }"
                aria-label="4열로 보기"
                @click="gridMode = 'four'"
              >
                <span v-for="n in 8" :key="n" /></button
              ><button
                type="button"
                :class="{ on: gridMode === 'three' }"
                aria-label="3열로 보기"
                @click="gridMode = 'three'"
              >
                <span v-for="n in 6" :key="n" />
              </button>
            </div>
          </div>
        </div>
        <div
          v-if="list.length"
          class="catalog-grid"
          :class="`grid-${gridMode}`"
        >
          <ProductCard
            v-for="item in list"
            :key="item.id"
            :product="item"
            :pet="item.pet"
          />
        </div>
        <div v-else class="catalog-empty">
          <span aria-hidden="true">···</span>
          <h2>
            {{
              loadError
                ? "상품 목록을 불러오지 못했습니다"
                : "상품 준비 중입니다"
            }}
          </h2>
          <p>
            {{ loadError || "선택하신 분류의 상품을 준비하고 있어요." }}<br />
            {{
              loadError ? "잠시 후 다시 시도해주세요." : "조금만 기다려 주세요."
            }}
          </p>
        </div>
      </section>
    </div>
  </div>
</template>
