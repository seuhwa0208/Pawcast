<!-- URL의 상품 식별자로 상세 데이터를 찾고 갤러리·옵션·구매 동작을 제어합니다. -->
<script setup>
import { computed, ref, watchEffect } from "vue";
import { useRouter } from "vue-router";
import { products } from "@/data/products";
import { useCart } from "@/stores/cart";
import { useWishlist } from "@/stores/wishlist";
import { useAuthSession } from "@/stores/auth";
import CartAddedModal from "@/components/CartAddedModal.vue";
import LoginRequiredModal from "@/components/LoginRequiredModal.vue";

const props = defineProps({ pet: String, id: String });
const router = useRouter();
const product = computed(
  () => products.find((item) => item.id === Number(props.id)) || products[0],
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
  if (product.value.stock <= 0)
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
const galleryImages = computed(() =>
  Array.from({ length: 4 }, (_, index) => ({
    src: product.value.image,
    alt: `${product.value.name} 상품 이미지 ${index + 1}`,
  })),
);
const productOptions = computed(
  () => product.value.options || [product.value.option],
);
const currentOption = computed(
  () => selectedOption.value || productOptions.value[0],
);
// 옵션별 가격표가 없는 API 상품도 기본 판매가로 안전하게 표시합니다.
const currentPrice = computed(
  () => product.value.optionPrices?.[currentOption.value] || product.value.price,
);
const getOptionPrice = (option) =>
  product.value.optionPrices?.[option] || product.value.price;
const getOptionExtra = (option) =>
  Math.max(0, getOptionPrice(option) - getOptionPrice(productOptions.value[0]));
const { add } = useCart();
const { isFavorite, toggle } = useWishlist();
const { accessToken } = useAuthSession();
const addToCart = () => {
  add(
    {
      ...product.value,
      option: currentOption.value,
      price: currentPrice.value,
    },
    props.pet,
  );
  cartAdded.value = true;
};
const buyNow = () => {
  sessionStorage.setItem(
    "pawcast-buy-now",
    JSON.stringify({
      key: `buy-now-${product.value.id}`,
      id: product.value.id,
      pet: props.pet,
      name: product.value.name,
      image: product.value.image,
      price: currentPrice.value,
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
      highlights: ["간편한 세척", "안정적인 사용", "생활 방수", "반려동물 맞춤"],
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
    storage: "개봉 후 밀봉하여 직사광선을 피해 서늘하고 건조한 곳에 보관해주세요.",
  };
});

const purchaseGuide = [
  {
    title: "배송 안내",
    text: "기본 배송비는 3,000원이며, 상품 금액 20,000원 이상 구매 시 무료배송입니다.",
  },
  {
    title: "교환·반품 안내",
    text: "상품 수령 후 상태를 확인해주세요. 개봉 또는 사용한 상품은 단순 변심 교환·반품이 제한될 수 있습니다.",
  },
  {
    title: "주문 변경 안내",
    text: "결제완료 상태에서는 배송지와 배송 요청사항을 변경할 수 있으며 배송준비 이후에는 고객 문의로 접수해주세요.",
  },
];

const temporaryReviews = computed(() => [
  {
    id: `${product.value.id}-review-1`,
    writer: "bo****",
    rating: 5,
    date: "2026.11.10",
    option: currentOption.value,
    content: `${product.value.name}을 처음 주문했는데 아이가 거부감 없이 잘 먹어요. 포장 상태도 깔끔했습니다.`,
  },
  {
    id: `${product.value.id}-review-2`,
    writer: "mi****",
    rating: 4,
    date: "2026.11.03",
    option: currentOption.value,
    content:
      "며칠 사용해보니 관리하기 편하고 품질도 만족스럽습니다. 다음에도 재구매할 생각이에요.",
  },
  {
    id: `${product.value.id}-review-3`,
    writer: "su****",
    rating: 5,
    date: "2026.10.27",
    option: currentOption.value,
    content:
      "배송이 안전하게 도착했고 상세 설명과 같은 상품이 왔어요. 꾸준히 사용해보려고 합니다.",
  },
]);
</script>

<template>
  <div class="container detail-page">
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
          <dd>3,000원 · 2만원 이상 무료</dd>
        </dl>
        <div class="option-selector">
          <strong>{{ pet === "ALL" ? "옵션 선택" : "중량 선택" }}</strong>
          <div class="option-control">
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
            <div v-if="optionOpen" class="option-menu">
              <button
                v-for="option in productOptions"
                :key="option"
                type="button"
                :class="{ selected: currentOption === option }"
                @click="chooseOption(option)"
              >
                <span>{{ option }}</span>
                <small v-if="getOptionExtra(option)">
                  (+{{ getOptionExtra(option).toLocaleString() }}원)
                </small>
                <span v-if="currentOption === option">✓</span>
              </button>
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
        <dl class="temporary-detail-copy">
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
      <article v-else class="tab-content temporary-review-panel">
        <header>
          <div>
            <small>PRODUCT REVIEWS</small>
            <h2>상품 리뷰</h2>
          </div>
          <strong>★ {{ product.rating }}</strong>
        </header>
        <p class="temporary-data-notice">
          현재 리뷰는 화면 검수용 임시 데이터입니다.
        </p>
        <section
          v-for="review in temporaryReviews"
          :key="review.id"
          class="temporary-review"
        >
          <div>
            <b>{{ "★".repeat(review.rating) }}</b>
            <span>{{ review.writer }} · {{ review.date }}</span>
          </div>
          <small>구매 옵션: {{ review.option }}</small>
          <p>{{ review.content }}</p>
        </section>
      </article>
    </section>
    <CartAddedModal v-model="cartAdded" @cart="router.push('/cart')" />
    <LoginRequiredModal v-model="loginRequired" @login="goToLogin" />
  </div>
</template>
