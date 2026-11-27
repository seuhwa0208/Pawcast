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
import PaginationBar from "@/components/PaginationBar.vue";
import ReviewEditModal from "@/components/ReviewEditModal.vue";
import { reviewService } from "@/services/reviewService";

const props = defineProps({ pet: String, id: String });
const router = useRouter();
const product = ref(null);
const detailLoading = ref(true);
const detailError = ref("");
const reviews = ref([]);
useAuthSession();
const reviewsLoading = ref(false);
const reviewPage = ref(1);
const REVIEW_PAGE_SIZE = 5;

// 리뷰 수정 모달 관련 상태
const reviewEditOpen = ref(false);
const editingReview = ref(null);
const reviewEditLoading = ref(false);
const reviewEditError = ref("");
const reviewEditSuccess = ref("");

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

function resolveImageUrl(rawPath) {
  if (!rawPath) return "/images/product-placeholder.svg";
  const path = String(rawPath).trim();
  if (!path) return "/images/product-placeholder.svg";
  if (
    /^https?:\/\//i.test(path) ||
    path.startsWith("data:") ||
    path.startsWith("blob:")
  ) {
    return path;
  }
  const normalizedBase = API_BASE_URL.replace(/\/+$/, "");
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${normalizedBase}${normalizedPath}`;
}

function handleImageError(event) {
  const img = event?.target;
  if (!img || img.dataset.fallbackApplied === "true") return;
  img.dataset.fallbackApplied = "true";
  img.src = "/images/product-placeholder.svg";
}

function normalizeDetail(detail) {
  const variants =
    detail.productType === "DEVICE"
      ? detail.deviceVariants
      : detail.foodVariants;
  const available = (variants || []).filter(
    (item) => item.status !== "DISCONTINUED",
  );
  const first = available[0] || {};

  // main 우선 정렬 후 중복 제거한 실제 이미지 목록 구성
  const sortedImages = [...(detail.images || [])].sort(
    (a, b) => Number(Boolean(b?.main)) - Number(Boolean(a?.main)),
  );
  const resolvedImages = sortedImages
    .map((item) =>
      resolveImageUrl(item?.imagePath || item?.url || item?.imageUrl),
    )
    .filter(Boolean);
  const uniqueImages = [...new Set(resolvedImages)];

  const fallbackImage = resolveImageUrl(
    detail.imageUrl || detail.thumbnailUrl || "/images/product-placeholder.svg",
  );
  const mainImage = uniqueImages[0] || fallbackImage;

  return {
    ...detail,
    category: detail.categoryName || "기타",
    image: mainImage,
    images: uniqueImages.length ? uniqueImages : [mainImage],
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
      // 공개 리뷰는 Spring이 정한 별점·작성일 기준 정렬 결과를 그대로 사용한다.
      const reviewResult = await reviewService.getProductReviews(props.id);
      reviews.value = Array.isArray(reviewResult)
        ? reviewResult
        : reviewResult?.reviews || [];
      reviewPage.value = 1;
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

watch(
  () => props.id,
  () => {
    closeReviewEdit();
    reviewEditError.value = "";
    reviewEditSuccess.value = "";
    galleryIndex.value = 0;
    selectedOption.value = "";
    loadProduct();
  },
);

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
const galleryIndex = ref(0);
const optionOpen = ref(false);
const cartAdded = ref(false);
const loginRequired = ref(false);
const selectedOption = ref("");

const galleryImages = computed(() => {
  if (!product.value) return [];
  const list = product.value.images?.length
    ? product.value.images
    : [product.value.image];
  return list.map((src, index) => ({
    src,
    alt: `${product.value.name} 상품 이미지 ${index + 1}`,
  }));
});

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
const bestReview = computed(() => reviews.value[0] || null);
const regularReviews = computed(() => reviews.value.slice(1));
const reviewPageCount = computed(() =>
  Math.max(1, Math.ceil(regularReviews.value.length / REVIEW_PAGE_SIZE)),
);
const pagedReviews = computed(() => {
  const start = (reviewPage.value - 1) * REVIEW_PAGE_SIZE;
  return regularReviews.value.slice(start, start + REVIEW_PAGE_SIZE);
});

function maskReviewWriter(userId) {
  const value = String(userId || "").trim();
  if (!value) return "고객님";
  const visibleLength =
    value.length <= 3 ? 1 : Math.min(3, Math.ceil(value.length / 2));
  return `${value.slice(0, visibleLength)}${"*".repeat(Math.min(4, Math.max(2, value.length - visibleLength)))} 고객님`;
}

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
const { accessToken, currentUserId } = useAuthSession();

// 로그인 회원 본인의 리뷰인지 확인합니다.
function isMyReview(review) {
  return Boolean(
    accessToken.value &&
      currentUserId.value &&
      review?.writerId === currentUserId.value,
  );
}

// 리뷰 수정 모달 열기
async function openReviewEdit(review) {
  if (!accessToken.value) {
    loginRequired.value = true;
    return;
  }

  if (!isMyReview(review)) {
    return;
  }

  if (reviewEditLoading.value) {
    return;
  }

  reviewEditError.value = "";
  reviewEditSuccess.value = "";
  reviewEditLoading.value = true;

  const requestedProductId = props.id;

  try {
    // 수정 전 서버에서 최신 리뷰 정보를 조회합니다.
    const latestReview = await reviewService.getReview(review.id);

    // 조회 도중 다른 상품으로 이동한 경우 모달을 열지 않습니다.
    if (requestedProductId !== props.id) {
      return;
    }

    // 최신 리뷰 정보에서도 작성자 본인인지 확인합니다.
    if (!isMyReview(latestReview)) {
      reviewEditError.value = "본인이 작성한 리뷰만 수정할 수 있습니다.";
      return;
    }

    editingReview.value = latestReview;
    reviewEditOpen.value = true;
  } catch (error) {
    reviewEditError.value =
      error.message || "리뷰 정보를 불러오지 못했습니다.";
  } finally {
    reviewEditLoading.value = false;
  }
}

// 리뷰 수정 모달 닫기
function closeReviewEdit() {
  reviewEditOpen.value = false;
  editingReview.value = null;
}

// 리뷰 수정 완료 후 리뷰 목록 및 상품 평점 갱신
async function handleReviewUpdated(updatedReview) {
  reviewEditError.value = "";
  reviewEditSuccess.value = "리뷰가 정상적으로 수정되었습니다.";

  const requestedProductId = props.id;

  // 수정된 리뷰를 화면에 우선 반영합니다.
  reviews.value = reviews.value.map((review) =>
    review.id === updatedReview.id ? { ...review, ...updatedReview } : review,
  );

  reviewsLoading.value = true;

  try {
    // 서버에서 최신 리뷰 목록 및 평균 별점을 조회합니다.
    const reviewResult =
      await reviewService.getProductReviews(requestedProductId);

    if (requestedProductId !== props.id) {
      return;
    }

    reviews.value = Array.isArray(reviewResult)
      ? reviewResult
      : reviewResult?.reviews || [];

    // 상품 상세 및 리뷰 영역의 평균 별점을 함께 갱신합니다.
    if (product.value) {
      product.value = {
        ...product.value,
        rating: Number(reviewResult?.averageRating ?? product.value.rating),
        reviewCount: Number(reviewResult?.reviewCount ?? reviews.value.length),
      };
    }

    // 현재 리뷰 페이지가 전체 페이지 수를 초과하지 않도록 처리합니다.
    reviewPage.value = Math.min(reviewPage.value, reviewPageCount.value);
  } catch (error) {
    reviewEditError.value =
      "리뷰는 수정되었지만 최신 목록을 불러오지 못했습니다. 새로고침 후 확인해주세요.";
    console.warn("리뷰 수정 후 목록 갱신 실패", error);
  } finally {
    reviewsLoading.value = false;
  }
}

const addToCart = async () => {
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
            @error="handleImageError"
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
            <img
              :src="image.src"
              :alt="image.alt"
              @error="handleImageError"
            />
          </button>
        </div>
      </div>
      <div class="buy-panel">
        <span>{{ petLabel }} {{ product.category }}</span>
        <h2>{{ product.name }}</h2>
        <p>신선한 원료를 천천히 조리해 만든 건강한 상품</p>
        <b
          >★ {{ product.rating }} 리뷰
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
              <span class="option-trigger-name">{{ currentOption }}</span>
              <small class="option-trigger-price">
                {{ getOptionPrice(currentOption).toLocaleString() }}원
                <template v-if="getOptionExtra(currentOption)">
                  <b>(+{{ getOptionExtra(currentOption).toLocaleString() }}원)</b>
                </template>
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
                @click="chooseOption(option)"
              >
                <input
                  type="radio"
                  name="product-option"
                  :checked="currentOption === option"
                  :value="option"
                  @change="selectedOption = option"
                />
                <i class="option-choice" aria-hidden="true" />
                <span class="option-menu-name">{{ option }}</span>
                <small class="option-menu-price">
                  {{ getOptionPrice(option).toLocaleString() }}원
                  <template v-if="getOptionExtra(option)">
                    (+{{ getOptionExtra(option).toLocaleString() }}원)
                  </template>
                </small>
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

        <!-- 리뷰 수정 성공 메시지 -->
        <p v-if="reviewEditSuccess" class="review-update-success" role="status">
          {{ reviewEditSuccess }}
        </p>

        <!-- 리뷰 수정 오류 메시지 -->
        <p v-if="reviewEditError" class="review-update-error" role="alert">
          {{ reviewEditError }}
        </p>

        <!-- 리뷰 조회 중 안내 -->
        <p v-if="reviewsLoading" class="empty-data-notice" role="status">
          상품 리뷰를 불러오는 중입니다.
        </p>

        <!-- Spring 공개 리뷰 정렬의 첫 항목을 베스트 리뷰로 노출하고, 나머지는 페이지로 나눠 표시한다. -->
        <section v-if="reviews.length" class="review-list">
          <!-- BEST 리뷰 -->
          <article v-if="bestReview && reviewPage === 1" class="best-review">
            <small>BEST REVIEW</small>
            <div class="review-meta">
              <b>★ {{ bestReview.rating }}</b>
              <span>{{ maskReviewWriter(bestReview.writerId) }}</span>
              <span v-if="isMyReview(bestReview)" class="my-review-label">내 리뷰</span>
              <time>{{ String(bestReview.createdAt || "").slice(0, 10) }}</time>
              <button
                v-if="isMyReview(bestReview)"
                type="button"
                class="review-edit-button"
                :disabled="reviewEditLoading || reviewsLoading"
                @click="openReviewEdit(bestReview)"
              >
                수정
              </button>
            </div>
            <p>{{ bestReview.content }}</p>
          </article>

          <!-- 일반 리뷰 -->
          <ol v-if="regularReviews.length" class="review-items">
            <li v-for="review in pagedReviews" :key="review.id">
              <div class="review-meta">
                <b>★ {{ review.rating }}</b>
                <span>{{ maskReviewWriter(review.writerId) }}</span>
                <span v-if="isMyReview(review)" class="my-review-label">내 리뷰</span>
                <time>{{ String(review.createdAt || "").slice(0, 10) }}</time>
                <button
                  v-if="isMyReview(review)"
                  type="button"
                  class="review-edit-button"
                  :disabled="reviewEditLoading || reviewsLoading"
                  @click="openReviewEdit(review)"
                >
                  수정
                </button>
              </div>
              <p>{{ review.content }}</p>
            </li>
          </ol>

          <!-- 리뷰 페이지네이션 -->
          <PaginationBar
            v-model:page="reviewPage"
            :total-pages="reviewPageCount"
            label="상품 리뷰 페이지 이동"
          />
        </section>

        <!-- 등록된 리뷰가 없는 경우 -->
        <p v-else-if="!reviewsLoading" class="empty-data-notice">
          등록된 상품 리뷰가 없습니다.
        </p>
      </article>
    </section>
    <CartAddedModal v-model="cartAdded" @cart="router.push('/cart')" />
    <LoginRequiredModal v-model="loginRequired" @login="goToLogin" />

    <!-- 회원 본인 리뷰 수정 모달 -->
    <ReviewEditModal
      v-if="reviewEditOpen && editingReview"
      :key="editingReview.id"
      :review="editingReview"
      :product-name="product.name"
      @close="closeReviewEdit"
      @updated="handleReviewUpdated"
    />
  </div>
  <main v-else class="container detail-page detail-page-state">
    {{ detailLoading ? "상품 정보를 불러오는 중입니다." : detailError }}
  </main>
</template>

<style scoped>
/* 회원 본인 리뷰 표시 */
.my-review-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  background: #f3e6d7;
  padding: 4px 8px;
  color: #95643d;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

/* 리뷰 수정 버튼 */
.review-edit-button {
  min-width: 58px;
  min-height: 30px;
  border: 1px solid #ddc9b7;
  border-radius: 7px;
  background: #fff;
  padding: 5px 12px;
  color: #805737;
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
}

.review-edit-button:hover:not(:disabled) {
  border-color: #95643d;
  background: #95643d;
  color: #fff;
}

.review-edit-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 리뷰 수정 성공 메시지 */
.review-update-success {
  margin: 16px 0;
  border: 1px solid #cfe3ce;
  border-radius: 9px;
  background: #f2f8f0;
  padding: 12px 16px;
  color: #4f8051;
  font-size: 13px;
  font-weight: 700;
}

/* 리뷰 수정 오류 메시지 */
.review-update-error {
  margin: 16px 0;
  border: 1px solid #ebc9c2;
  border-radius: 9px;
  background: #fff5f3;
  padding: 12px 16px;
  color: #c65b49;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 600px) {
  .review-meta {
    flex-wrap: wrap;
  }

  .review-edit-button {
    min-height: 34px;
  }
}
</style>