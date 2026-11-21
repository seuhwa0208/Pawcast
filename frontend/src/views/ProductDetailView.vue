<!-- URL의 상품 식별자로 상세 데이터를 찾고 갤러리·옵션·구매 동작을 제어합니다. -->
<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
  watchEffect,
} from "vue";
import { useRouter } from "vue-router";
import { productService } from "@/services/productService";
import { useCart } from "@/stores/cart";
import { useWishlist } from "@/stores/wishlist";
import { useAuthSession } from "@/stores/auth";
import CartAddedModal from "@/components/CartAddedModal.vue";
import LoginRequiredModal from "@/components/LoginRequiredModal.vue";
import { reviewService } from "@/services/reviewService";

const props = defineProps({ pet: String, id: String });
const router = useRouter();
const product = ref(null);
const detailLoading = ref(true);
const detailError = ref("");
const reviews = ref([]);
const reviewsLoading = ref(false);

function normalizeDetail(detail) {
  const variants =
    detail.productType === "DEVICE"
      ? detail.deviceVariants
      : detail.foodVariants;
  const available = (variants || []).filter(
    (item) => item.status !== "DISCONTINUED",
  );
  const first = available[0] || {};
  return {
    ...detail,
    category: detail.categoryName || "기타",
    image:
      detail.images?.find((item) => item.main)?.imagePath ||
      detail.images?.[0]?.imagePath ||
      "/images/product-placeholder.svg",
    options: available
      .map((item) => item.optionLabel || item.optionKey)
      .filter(Boolean),
    optionPrices: Object.fromEntries(
      available.map((item) => [
        item.optionLabel || item.optionKey,
        Number(item.price || 0),
      ]),
    ),
    // # 선택한 옵션의 실제 variant ID를 주문·장바구니 API에 전달한다.
    optionVariantIds: Object.fromEntries(
      available.map((item) => [item.optionLabel || item.optionKey, item.id]),
    ),
    option: first.optionLabel || first.optionKey || "기본 옵션",
    price: Number(first.price || 0),
    stock: available.some((item) => Number(item.stock || 0) > 0) ? 1 : 0,
    rating: Number(detail.averageRating || 0),
    reviewCount: Number(detail.reviewCount || 0),
  };
}

async function loadProduct() {
  detailLoading.value = true;
  detailError.value = "";
  try {
    reviewsLoading.value = true;
    const detail = await productService.detail(props.id);
    product.value = normalizeDetail(detail);
    try {
      const reviewSummary = await reviewService.getProductReviews(props.id);
      // # 백엔드가 별점 높은 순·동점 작성일 빠른 순으로 정렬해 전달한다.
      reviews.value = Array.isArray(reviewSummary.reviews)
        ? reviewSummary.reviews
        : [];
    } catch (reviewError) {
      // # 리뷰 API 오류가 상품 상세 페이지 전체를 막지 않도록 리뷰 영역만 빈 상태로 둔다.
      reviews.value = [];
      console.warn("상품 리뷰를 불러오지 못했습니다.", reviewError);
    }
  } catch (error) {
    product.value = null;
    detailError.value = error.message || "상품 정보를 불러오지 못했습니다.";
  } finally {
    detailLoading.value = false;
    reviewsLoading.value = false;
  }
}
// # 상품 옵션 드롭다운은 다른 영역을 누르면 닫혀야 키보드·마우스 모두에서 상태가 남지 않는다.
const optionSelector = ref(null);
const closeOptionOnOutside = (event) => {
  if (
    optionOpen.value &&
    optionSelector.value &&
    !optionSelector.value.contains(event.target)
  ) {
    optionOpen.value = false;
  }
};
onMounted(() => {
  loadProduct();
  document.addEventListener("pointerdown", closeOptionOnOutside);
});
onBeforeUnmount(() =>
  document.removeEventListener("pointerdown", closeOptionOnOutside),
);
watch(() => props.id, loadProduct);
const sectionName = computed(() =>
  props.pet === "CAT"
    ? "고양이관"
    : props.pet === "ALL"
      ? "식기·급수관"
      : "강아지관",
);
const petLabel = computed(() =>
  props.pet === "CAT" ? "고양이" : props.pet === "ALL" ? "식기·급수" : "강아지",
);
// 품절 상세 URL로 직접 들어온 경우 해당 상품관 목록으로 즉시 되돌립니다.
watchEffect(() => {
  if (product.value && product.value.stock <= 0)
    router.replace(
      props.pet === "CAT"
        ? "/cat"
        : props.pet === "ALL"
          ? "/tableware"
          : "/dog",
    );
});
const tab = ref("detail");
const showAllReviews = ref(false);
const galleryIndex = ref(0);
const optionOpen = ref(false);
const cartAdded = ref(false);
const loginRequired = ref(false);
const selectedOption = ref("");
const galleryImages = computed(() =>
  !product.value
    ? []
    : Array.from({ length: 4 }, (_, index) => ({
        src: product.value.image,
        alt: `${product.value.name} 상품 이미지 ${index + 1}`,
      })),
);
const productOptions = computed(() =>
  product.value?.options?.length
    ? product.value.options
    : product.value
      ? [product.value.option]
      : [],
);
const currentOption = computed(
  () => selectedOption.value || productOptions.value[0] || "기본 옵션",
);
// 옵션별 가격표가 없는 API 상품도 기본 판매가로 안전하게 표시합니다.
const currentPrice = computed(
  () =>
    product.value?.optionPrices?.[currentOption.value] ||
    product.value?.price ||
    0,
);
const currentVariantId = computed(
  () => product.value?.optionVariantIds?.[currentOption.value] || null,
);
const getOptionPrice = (option) =>
  product.value?.optionPrices?.[option] || product.value?.price || 0;
const getOptionExtra = (option) =>
  Math.max(0, getOptionPrice(option) - getOptionPrice(productOptions.value[0]));
const { add } = useCart();
const { isFavorite, toggle } = useWishlist();
const { accessToken } = useAuthSession();
const addToCart = async () => {
  if (!accessToken.value) {
    loginRequired.value = true;
    return;
  }
  try {
    await add(
      {
        ...product.value,
        option: currentOption.value,
        price: currentPrice.value,
        variantId: currentVariantId.value,
      },
      props.pet,
    );
    cartAdded.value = true;
  } catch (error) {
    if (error.message === "LOGIN_REQUIRED") loginRequired.value = true;
  }
};
const buyNow = () => {
  sessionStorage.setItem(
    "powcast-buy-now",
    JSON.stringify({
      key: `buy-now-${product.value.id}`,
      id: product.value.id,
      pet: props.pet,
      name: product.value.name,
      image: product.value.image,
      price: currentPrice.value,
      variantId: currentVariantId.value,
      option: currentOption.value,
      quantity: 1,
    }),
  );
  router.push({ path: "/checkout", query: { mode: "buy-now" } });
};
// 회원은 같은 위시리스트 Store를 토글하고, 비회원은 로그인 안내를 표시합니다.
const toggleWishlist = () => {
  if (!accessToken.value) {
    loginRequired.value = true;
    return;
  }
  toggle(product.value.id);
};
const goToLogin = () => {
  loginRequired.value = false;
  router.push({
    path: "/login",
    query: { redirect: router.currentRoute.value.fullPath },
  });
};
// 나머지 연산으로 양 끝에서 반대편 이미지로 자연스럽게 순환합니다.
const previousImage = () => {
  galleryIndex.value =
    (galleryIndex.value - 1 + galleryImages.value.length) %
    galleryImages.value.length;
};
const nextImage = () => {
  galleryIndex.value = (galleryIndex.value + 1) % galleryImages.value.length;
};
const chooseOption = (option) => {
  selectedOption.value = option;
  optionOpen.value = false;
};

// 백엔드 상세 콘텐츠가 준비되기 전 카테고리별로 자연스러운 임시 설명을 구성합니다.
const productDetailContent = computed(() => {
  const category = product.value.category;
  if (["급식기", "급수기", "정수 / 필터", "일반 식기"].includes(category)) {
    return {
      title: `${product.value.name}, 매일 편리하게 사용하는 반려생활 용품`,
      description:
        "반려동물이 편안하게 사용할 수 있도록 안정성과 관리 편의성을 고려한 상품입니다.",
      highlights: [
        "간편한 세척",
        "안정적인 사용",
        "생활 방수",
        "반려동물 맞춤",
      ],
      usage:
        "평평하고 안정적인 장소에 설치한 뒤 사용 전 구성품을 깨끗이 세척해주세요.",
      storage: "직사광선과 습기가 많은 장소를 피해 보관해주세요.",
    };
  }
  if (category === "영양제") {
    return {
      title: `${product.value.name}, 필요한 영양을 매일 간편하게`,
      description:
        "균형 잡힌 영양 보충을 돕도록 기호성과 급여 편의성을 고려한 반려동물용 영양제입니다.",
      highlights: ["영양 균형", "간편 급여", "기호성 고려", "일일 건강관리"],
      usage:
        "반려동물의 체중과 건강 상태를 확인하고 제품 표기 급여량에 맞춰 급여해주세요.",
      storage: "개봉 후 뚜껑을 닫아 서늘하고 건조한 곳에 보관해주세요.",
    };
  }
  return {
    title: `${product.value.name}, 원료와 기호성을 함께 생각한 레시피`,
    description:
      "반려동물이 맛있게 먹을 수 있도록 원료의 풍미와 균형 잡힌 영양 구성을 고려했습니다.",
    highlights: ["엄선한 원료", "균형 영양", "기호성 고려", "신선한 제조"],
    usage:
      "기존에 먹던 식품과 조금씩 섞어 적응 기간을 두고, 체중과 활동량에 따라 급여량을 조절해주세요.",
    storage:
      "개봉 후 밀봉하여 직사광선을 피해 서늘하고 건조한 곳에 보관해주세요.",
  };
});

const purchaseGuide = [
  {
    title: "배송 안내",
    text: "결제 완료 후 상품 준비가 시작되며, 상품 합계 20,000원 미만은 배송비 3,000원이 부과됩니다. 20,000원 이상은 무료 배송입니다.",
  },
  {
    title: "반품·환불 안내",
    text: "상품 수령 후 상태를 확인해주세요. 개봉 또는 사용한 상품은 단순 변심 반품·환불이 제한될 수 있습니다.",
  },
  {
    title: "주문 변경 안내",
    text: "결제완료 상태에서는 배송지와 배송 요청사항을 변경할 수 있으며 배송준비 이후에는 고객 문의로 접수해주세요.",
  },
];
</script>

<template>
  <div v-if="product" class="container detail-page">
    <p class="breadcrumb">홈 › {{ sectionName }} › {{ product.name }}</p>
    <section class="summary">
      <div class="gallery">
        <div class="gallery-main">
          <img
            :src="galleryImages[galleryIndex].src"
            :alt="galleryImages[galleryIndex].alt"
          /><button
            type="button"
            class="gallery-arrow previous"
            aria-label="이전 상품 이미지"
            @click="previousImage"
          >
            ‹</button
          ><button
            type="button"
            class="gallery-arrow next"
            aria-label="다음 상품 이미지"
            @click="nextImage"
          >
            ›</button
          ><span class="gallery-count"
            >{{ galleryIndex + 1 }} / {{ galleryImages.length }}</span
          >
        </div>
        <div class="gallery-thumbnails">
          <button
            v-for="(image, index) in galleryImages"
            :key="index"
            type="button"
            :class="{ active: galleryIndex === index }"
            :aria-label="`${index + 1}번 상품 이미지 보기`"
            @click="galleryIndex = index"
          >
            <img :src="image.src" :alt="image.alt" />
          </button>
        </div>
      </div>
      <div class="buy-panel">
        <span>{{ petLabel }} {{ product.category }}</span>
        <h2>{{ product.name }}</h2>
        <p>신선한 원료를 천천히 조리해 만든 건강한 상품</p>
        <b
          >★ {{ product.rating }}　리뷰
          {{ product.reviewCount.toLocaleString() }}개</b
        >
        <dl>
          <dt>판매가</dt>
          <dd>{{ currentPrice.toLocaleString() }}원</dd>
          <dt>배송</dt>
          <dd>3,000원</dd>
        </dl>
        <div class="option-selector">
          <strong>옵션 선택 (필수) <em>*</em></strong>
          <div ref="optionSelector" class="option-control">
            <button
              type="button"
              class="option-trigger"
              :class="{ open: optionOpen }"
              :aria-expanded="optionOpen"
              @click="optionOpen = !optionOpen"
            >
              <span>{{ currentOption }}</span>
              <small v-if="getOptionExtra(currentOption)">
                (+{{ getOptionExtra(currentOption).toLocaleString() }}원)
              </small>
              <i aria-hidden="true" />
            </button>
            <div
              v-if="optionOpen"
              class="option-menu"
              role="radiogroup"
              aria-label="상품 옵션"
            >
              <label
                v-for="option in productOptions"
                :key="option"
                :class="{ selected: currentOption === option }"
              >
                <input
                  type="radio"
                  name="product-option"
                  :checked="currentOption === option"
                  :value="option"
                  @change="selectedOption = option"
                />
                <i aria-hidden="true" />
                <span>{{ option }}</span>
              </label>
            </div>
          </div>
        </div>
        <div class="buy-actions">
          <button
            type="button"
            class="detail-wishlist-button"
            :class="{ active: accessToken && isFavorite(product.id) }"
            :aria-pressed="Boolean(accessToken && isFavorite(product.id))"
            :aria-label="isFavorite(product.id) ? '찜 해제' : '찜하기'"
            @click="toggleWishlist"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M20.8 4.7a5.5 5.5 0 0 0-7.8 0L12 5.8l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.4 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z"
              />
            </svg>
            {{ isFavorite(product.id) ? "찜 해제" : "찜하기" }}
          </button>
          <button @click="addToCart">장바구니</button
          ><button @click="buyNow">바로 구매</button>
        </div>
      </div>
    </section>
    <section class="tabs">
      <nav>
        <button
          v-for="item in [
            { k: 'detail', v: '상품 상세' },
            { k: 'guide', v: '구매 안내' },
            { k: 'review', v: `상품 리뷰 ${product.reviewCount}` },
          ]"
          :key="item.k"
          :class="{ on: tab === item.k }"
          @click="tab = item.k"
        >
          {{ item.v }}
        </button>
      </nav>
      <article v-if="tab === 'detail'" class="feature">
        <small>PRODUCT DETAIL</small>
        <h2>{{ productDetailContent.title }}</h2>
        <p>{{ productDetailContent.description }}</p>
        <div class="detail-highlights">
          <span
            v-for="highlight in productDetailContent.highlights"
            :key="highlight"
          >
            {{ highlight }}
          </span>
        </div>
        <dl class="product-detail-copy">
          <dt>권장 사용·급여 방법</dt>
          <dd>{{ productDetailContent.usage }}</dd>
          <dt>보관 방법</dt>
          <dd>{{ productDetailContent.storage }}</dd>
          <dt>선택 옵션</dt>
          <dd class="available-options">
            <span
              v-for="option in productOptions"
              :key="option"
              :class="{ selected: option === currentOption }"
            >
              <b>{{ option }}</b>
              <small>{{ getOptionPrice(option).toLocaleString() }}원</small>
            </span>
          </dd>
        </dl>
      </article>
      <article v-else-if="tab === 'guide'" class="tab-content purchase-guide">
        <small>PURCHASE GUIDE</small>
        <h2>구매 안내</h2>
        <div>
          <section v-for="guide in purchaseGuide" :key="guide.title">
            <h3>{{ guide.title }}</h3>
            <p>{{ guide.text }}</p>
          </section>
        </div>
      </article>
      <article v-else class="tab-content product-review-panel">
        <header>
          <div>
            <small>PRODUCT REVIEWS</small>
            <h2>상품 리뷰</h2>
          </div>
          <strong>★ {{ product.rating }}</strong>
        </header>
        <!-- # 첫 번째 항목은 최고 별점·가장 이른 작성일 기준의 베스트 리뷰입니다. -->
        <section v-if="reviews.length" class="review-list">
          <article class="best-review">
            <small>BEST REVIEW</small
            ><b>★ {{ reviews[0].rating }} · {{ reviews[0].writerName }}</b>
            <p>{{ reviews[0].content }}</p>
          </article>
          <button
            v-if="reviews.length > 1"
            type="button"
            class="review-all-button"
            @click="showAllReviews = !showAllReviews"
          >
            {{ showAllReviews ? "접기" : `전체보기 (${reviews.length})` }}
          </button>
          <ol v-if="showAllReviews" class="review-items">
            <li v-for="review in reviews.slice(1)" :key="review.id">
              <b>★ {{ review.rating }} · {{ review.writerName }}</b>
              <p>{{ review.content }}</p>
            </li>
          </ol>
        </section>
        <p v-else-if="!reviewsLoading" class="empty-data-notice">
          등록된 상품 리뷰가 없습니다.
        </p>
      </article>
    </section>
    <CartAddedModal v-model="cartAdded" @cart="router.push('/cart')" />
    <LoginRequiredModal v-model="loginRequired" @login="goToLogin" />
  </div>
  <main v-else class="container detail-page detail-page-state">
    {{ detailLoading ? "상품 정보를 불러오는 중입니다." : detailError }}
  </main>
</template>
