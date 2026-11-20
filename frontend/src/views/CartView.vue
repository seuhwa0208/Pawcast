<!-- 장바구니 수량·삭제·합계를 전역 cart 스토어와 연결해 보여주는 화면입니다. -->
<script setup>
import { computed, ref } from "vue";
import { useCart } from "@/stores/cart";
import { useWishlist } from "@/stores/wishlist";
import { useAuthSession } from "@/stores/auth";
import { products } from "@/data/products";
import ProductCard from "@/components/ProductCard.vue";
import { useRoute, useRouter } from "vue-router";

const { items, total, setQuantity, remove } = useCart();
const { isFavorite } = useWishlist();
const { accessToken } = useAuthSession();
const route = useRoute();
const router = useRouter();
const BASE_SHIPPING_FEE = 3000;
const FREE_SHIPPING_THRESHOLD = 20000;
const activeTab = ref(route.query.tab === "wishlist" ? "wishlist" : "cart");
const wishlistItems = computed(() =>
  products.filter((item) => isFavorite(item.id)),
);
function selectTab(tab) {
  activeTab.value = tab;
  router.replace({
    path: "/cart",
    query: tab === "wishlist" ? { tab: "wishlist" } : {},
  });
}
function moveToCheckout() {
  router.push("/checkout");
}
const shippingFee = computed(() =>
  total.value >= FREE_SHIPPING_THRESHOLD ? 0 : BASE_SHIPPING_FEE,
);
const finalAmount = computed(() => total.value + shippingFee.value);
</script>

<template>
  <main class="container cart-page">
    <header class="cart-heading">
      <small>POWCAST SHOPPING LIST</small>
      <h1>{{ activeTab === "cart" ? "장바구니" : "위시리스트" }}</h1>
      <p>
        {{
          activeTab === "cart"
            ? "담아두신 상품과 수량을 확인해 주세요."
            : "관심 있는 상품을 한곳에서 확인해 보세요."
        }}
      </p>
    </header>
    <nav class="shopping-list-tabs" aria-label="장바구니와 위시리스트">
      <button
        type="button"
        :class="{ on: activeTab === 'cart' }"
        @click="selectTab('cart')"
      >
        장바구니 <b>{{ items.length }}</b></button
      ><button
        type="button"
        :class="{ on: activeTab === 'wishlist' }"
        @click="selectTab('wishlist')"
      >
        위시리스트 <b>{{ wishlistItems.length }}</b>
      </button>
    </nav>
    <p
      v-if="activeTab === 'cart' && !accessToken"
      class="guest-cart-storage-notice"
    >
      비회원 장바구니는 현재 브라우저에 저장됩니다. 브라우저 데이터 삭제 또는
      다른 기기 이용 시 담은 상품이 유지되지 않을 수 있습니다.
    </p>
    <section v-if="activeTab === 'cart' && items.length" class="cart-layout">
      <div class="cart-list">
        <article v-for="item in items" :key="item.key" class="cart-item">
          <RouterLink
            :to="`/products/${item.pet || 'DOG'}/${item.id || 1}`"
            class="cart-thumb"
            ><img v-if="item.image" :src="item.image" :alt="item.name" /><span
              v-else
              >POWCAST</span
            ></RouterLink
          >
          <div class="cart-info">
            <small>{{
              item.pet === "CAT" ? "고양이 상품" : "강아지 상품"
            }}</small>
            <h2>{{ item.name }}</h2>
            <p v-if="item.option"><b>옵션:</b> {{ item.option }}</p>
          </div>
          <div class="cart-quantity" aria-label="수량 선택">
            <button
              type="button"
              aria-label="수량 줄이기"
              @click="setQuantity(item.key, item.quantity - 1)"
            >
              −</button
            ><b>{{ item.quantity }}</b
            ><button
              type="button"
              aria-label="수량 늘리기"
              @click="setQuantity(item.key, item.quantity + 1)"
            >
              ＋
            </button>
          </div>
          <strong>{{ (item.price * item.quantity).toLocaleString() }}원</strong>
          <button
            type="button"
            class="cart-remove"
            aria-label="상품 삭제"
            @click="remove(item.key)"
          >
            ×
          </button>
        </article>
      </div>
      <aside class="cart-summary">
        <h2>결제 예정 금액</h2>
        <dl>
          <dt>상품 금액</dt>
          <dd>{{ total.toLocaleString() }}원</dd>
          <dt>배송비</dt>
          <dd>{{ shippingFee ? `${shippingFee.toLocaleString()}원` : "무료" }}</dd>
          <dt>총 결제 금액</dt>
          <dd>{{ finalAmount.toLocaleString() }}원</dd>
        </dl>
        <p class="cart-shipping-notice">20,000원 이상 구매 시 배송비 무료</p>
        <button type="button" @click="moveToCheckout">주문하기</button>
      </aside>
    </section>
    <section v-else-if="activeTab === 'cart'" class="cart-empty">
      <h2>장바구니가 비어 있습니다.</h2>
      <p>아이에게 필요한 상품을 담아보세요.</p>
      <RouterLink to="/">쇼핑 계속하기</RouterLink>
    </section>
    <section v-else-if="!accessToken" class="cart-empty">
      <h2>로그인이 필요합니다.</h2>
      <p>위시리스트는 회원 전용 기능입니다.</p>
      <RouterLink
        :to="{ path: '/login', query: { redirect: '/cart?tab=wishlist' } }"
        >로그인하러 가기</RouterLink
      >
    </section>
    <section v-else-if="wishlistItems.length" class="wishlist-panel">
      <div class="product-grid">
        <ProductCard
          v-for="item in wishlistItems"
          :key="item.id"
          :product="item"
          :pet="item.pet"
        />
      </div>
    </section>
    <section v-else class="cart-empty">
      <h2>위시리스트가 비어 있습니다.</h2>
      <p>관심 있는 상품의 하트를 눌러 저장해 보세요.</p>
      <RouterLink to="/">상품 둘러보기</RouterLink>
    </section>
  </main>
</template>
