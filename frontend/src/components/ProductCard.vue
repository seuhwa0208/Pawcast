<!-- 상품 요약, 품절 분기, 찜 권한 확인, 장바구니 추가를 묶은 재사용 카드입니다. -->
<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useCart } from "@/stores/cart";
import { useWishlist } from "@/stores/wishlist";
import { useAuthSession } from "@/stores/auth";
import LoginRequiredModal from "@/components/LoginRequiredModal.vue";
import SoldOutModal from "@/components/SoldOutModal.vue";
import UiTag from "@/components/UiTag.vue";
import CartAddedModal from "@/components/CartAddedModal.vue";

const props = defineProps({ product: Object, pet: String });
const { add } = useCart();
const { isFavorite, toggle } = useWishlist();
const { accessToken } = useAuthSession();
const router = useRouter();
const loginRequired = ref(false);
const soldOutNotice = ref(false);
const cartAdded = ref(false);
const discountedPrice = () => {
  const rate = Number(props.product.discountRate || 0);
  return rate > 0
    ? Math.round(Number(props.product.price) * (1 - rate / 100))
    : Number(props.product.price);
};
// 찜은 회원 전용 기능이므로 토큰 유무에 따라 실제 토글과 로그인 안내를 분기합니다.
const handleWishlist = (id) =>
  accessToken.value ? toggle(id) : (loginRequired.value = true);
const handleCart = () => {
  add(props.product, props.pet);
  cartAdded.value = true;
};
const goToLogin = () => {
  loginRequired.value = false;
  router.push("/login");
};
</script>

<template>
  <article class="product-card" :class="{ 'sold-out-card': !product.stock }">
    <RouterLink v-if="product.stock" :to="`/products/${pet}/${product.id}`">
      <div class="product-image">
        <img :src="product.image" :alt="product.name" />
        <div v-if="!product.stock" class="soldout">품절 상품</div>
      </div>
      <UiTag
        :label="product.subcategory || product.category"
        :variant="pet === 'CAT' ? 'cat' : 'dog'"
      />
      <h3>{{ product.name }}</h3>
      <p>★ {{ product.rating }} (리뷰 {{ product.reviewCount || 76 }}건)</p>
    </RouterLink>
    <div
      v-else
      class="disabled-product"
      role="button"
      tabindex="0"
      aria-label="품절 상품 안내 보기"
      @click="soldOutNotice = true"
      @keydown.enter="soldOutNotice = true"
    >
      <div class="product-image">
        <img :src="product.image" :alt="product.name" />
        <div class="soldout">
          <strong>품절 상품</strong><small>현재 구매할 수 없습니다</small>
        </div>
      </div>
      <UiTag
        :label="product.subcategory || product.category"
        :variant="pet === 'CAT' ? 'cat' : 'dog'"
      />
      <h3>{{ product.name }}</h3>
      <p>★ {{ product.rating }} (리뷰 {{ product.reviewCount || 76 }}건)</p>
    </div>
    <div class="product-card-footer">
      <div class="product-price">
        <span v-if="product.discountRate" class="discount-rate"
          >{{ product.discountRate }}%</span
        >
        <strong>{{ discountedPrice().toLocaleString() }}원</strong>
        <del v-if="product.discountRate"
          >{{ product.price.toLocaleString() }}원</del
        >
      </div>
      <div class="card-actions">
        <button
          type="button"
          class="wishlist-button"
          :class="{ active: accessToken && isFavorite(product.id) }"
          :aria-label="isFavorite(product.id) ? '찜 해제' : '찜하기'"
          @click="handleWishlist(product.id)"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M20.8 4.7a5.5 5.5 0 0 0-7.8 0L12 5.8l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.4 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z"
            />
          </svg>
        </button>
        <button
          type="button"
          class="card-cart-button"
          :disabled="!product.stock"
          aria-label="장바구니 담기"
          @click="handleCart"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M3 4h2l2.1 9.2a2 2 0 0 0 2 1.6h7.8a2 2 0 0 0 2-1.6L20.3 7H6"
            />
            <circle cx="9.5" cy="19" r="1.4" />
            <circle cx="17" cy="19" r="1.4" />
          </svg>
        </button>
      </div>
    </div>
    <LoginRequiredModal v-model="loginRequired" @login="goToLogin" />
    <SoldOutModal v-model="soldOutNotice" :product-name="product.name" />
    <CartAddedModal v-model="cartAdded" @cart="router.push('/cart')" />
  </article>
</template>
