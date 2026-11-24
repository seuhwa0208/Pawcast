<!-- 회원·비회원 장바구니와 회원 전용 위시리스트를 관리합니다. -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useCart } from "@/stores/cart";
import { useAuthSession } from "@/stores/auth";
import ProductCard from "@/components/ProductCard.vue";
import { wishlistService } from "@/services/wishlistService";
import { productService, toProductCard } from "@/services/productService";

const route = useRoute();
const router = useRouter();

const { items, total, setQuantity, remove } = useCart();
const { accessToken } = useAuthSession();

const BASE_SHIPPING_FEE = 3000;
const FREE_SHIPPING_THRESHOLD = 20000;

const activeTab = ref(
    route.query.tab === "wishlist" ? "wishlist" : "cart"
);

const wishlistItems = ref([]);
const errorMessage = ref("");
const processingKeys = ref([]);

// 위시리스트 조회: 회원만 가능
async function loadWishlist() {
    if (!accessToken.value) {
        wishlistItems.value = [];
        return;
    }

    try {
        const response = await wishlistService.list();

        wishlistItems.value = await Promise.all(
            (Array.isArray(response) ? response : []).map(async (item) => {
                const product = item.product || item;
                const detail = await productService.detail(product.id);

                const variants =
                    detail.productType === "DEVICE"
                        ? detail.deviceVariants
                        : detail.foodVariants;

                const available = (variants || []).filter(
                    (v) => v.status === "ON_SALE" && v.stock > 0,
                );

                // 구매 가능한 옵션 중 가장 저렴한 옵션
                const cheapest = [...available].sort(
                    (a, b) => Number(a.price) - Number(b.price),
                )[0];

                return toProductCard({
                    ...detail,
                    price: cheapest?.price,
                    imagePath:
                        detail.images?.find((image) => image.main)?.imagePath ||
                        detail.images?.[0]?.imagePath,
                    soldOut: !available.length,
                });
            }),
        );
    } catch (error) {
        console.error("위시리스트 조회 실패:", error);
        wishlistItems.value = [];
    }
}

// 장바구니 / 위시리스트 탭 전환
function selectTab(tab) {
    activeTab.value = tab;

    router.replace({
        path: "/cart",
        query: tab === "wishlist"
            ? { tab: "wishlist" }
            : {},
    });

    if (tab === "wishlist") {
        loadWishlist();
    }
}

// 회원·비회원 모두 주문·결제 화면으로 이동합니다.
// 비회원은 다음 화면에서 이메일과 배송지를 직접 입력합니다.
function moveToCheckout() {
    router.push("/checkout");
}

// 수량 변경
async function changeQuantity(item, quantity) {
    if (processingKeys.value.includes(item.key)) {
        return;
    }

    if (!Number.isSafeInteger(quantity) || quantity < 1) {
        return;
    }

    errorMessage.value = "";
    processingKeys.value.push(item.key);

    try {
        await setQuantity(item.key, quantity);
    } catch (error) {
        console.error("장바구니 수량 변경 실패:", error);

        errorMessage.value =
            error.message || "상품 수량 변경에 실패했습니다.";
    } finally {
        processingKeys.value = processingKeys.value.filter(
            (key) => key !== item.key
        );
    }
}

// 장바구니 상품 삭제
async function removeCartItem(item) {
    if (processingKeys.value.includes(item.key)) {
        return;
    }

    errorMessage.value = "";
    processingKeys.value.push(item.key);

    try {
        await remove(item.key);
    } catch (error) {
        console.error("장바구니 상품 삭제 실패:", error);

        errorMessage.value =
            error.message || "상품 삭제에 실패했습니다.";
    } finally {
        processingKeys.value = processingKeys.value.filter(
            (key) => key !== item.key
        );
    }
}

// 배송비 계산
const shippingFee = computed(() => {
    if (!items.value.length) {
        return 0;
    }

    return total.value >= FREE_SHIPPING_THRESHOLD
        ? 0
        : BASE_SHIPPING_FEE;
});

// 최종 결제 금액
const finalAmount = computed(() => {
    return total.value + shippingFee.value;
});

// 로그인 상태 변경 시 위시리스트 초기화
watch(accessToken, (token) => {
    if (!token) {
        wishlistItems.value = [];
        return;
    }

    if (activeTab.value === "wishlist") {
        loadWishlist();
    }
});

// URL 쿼리 변경 시 탭 동기화
watch(
    () => route.query.tab,
    (tab) => {
        activeTab.value =
            tab === "wishlist" ? "wishlist" : "cart";

        if (activeTab.value === "wishlist") {
            loadWishlist();
        }
    }
);

onMounted(() => {
    // 비회원도 장바구니 페이지에 접근할 수 있습니다.
    if (
        accessToken.value &&
        activeTab.value === "wishlist"
    ) {
        loadWishlist();
    }
});
</script>

<template>
    <main class="container cart-page">
        <header class="cart-heading">
            <small>POWCAST SHOPPING LIST</small>

            <h1>
                {{
                    activeTab === "cart"
                        ? "장바구니"
                        : "위시리스트"
                }}
            </h1>

            <p>
                {{
                    activeTab === "cart"
                        ? "담아두신 상품과 수량을 확인해 주세요."
                        : "관심 있는 상품을 한곳에서 확인해 보세요."
                }}
            </p>
        </header>

        <nav
            class="shopping-list-tabs"
            aria-label="장바구니와 위시리스트"
        >
            <button
                type="button"
                :class="{ on: activeTab === 'cart' }"
                @click="selectTab('cart')"
            >
                장바구니
                <b>{{ items.length }}</b>
            </button>

            <button
                type="button"
                :class="{ on: activeTab === 'wishlist' }"
                @click="selectTab('wishlist')"
            >
                위시리스트
                <b>{{ wishlistItems.length }}</b>
            </button>
        </nav>

        <p
            v-if="errorMessage"
            class="form-message error"
            role="alert"
        >
            {{ errorMessage }}
        </p>

        <!-- 장바구니 상품 목록 -->
        <section
            v-if="activeTab === 'cart' && items.length"
            class="cart-layout"
        >
            <div class="cart-list">
                <article
                    v-for="item in items"
                    :key="item.key"
                    class="cart-item"
                >
                    <RouterLink
                        :to="`/products/${item.pet || 'DOG'}/${item.id || 1}`"
                        class="cart-thumb"
                    >
                        <img
                            v-if="item.image"
                            :src="item.image"
                            :alt="item.name"
                        />

                        <span v-else>
                            POWCAST
                        </span>
                    </RouterLink>

                    <div class="cart-info">
                        <small>
                            {{
                                item.pet === "CAT"
                                    ? "고양이 상품"
                                    : "강아지 상품"
                            }}
                        </small>

                        <div class="cart-title-row">
                            <h2>{{ item.name }}</h2>
                            <span v-if="item.option" class="cart-option">
                                옵션: {{ item.option }}
                            </span>
                        </div>
                    </div>

                    <div
                        class="cart-quantity"
                        aria-label="수량 선택"
                    >
                        <button
                            type="button"
                            aria-label="수량 줄이기"
                            :disabled="
                                item.quantity <= 1 ||
                                processingKeys.includes(item.key)
                            "
                            @click="
                                changeQuantity(
                                    item,
                                    item.quantity - 1
                                )
                            "
                        >
                            −
                        </button>

                        <b>{{ item.quantity }}</b>

                        <button
                            type="button"
                            aria-label="수량 늘리기"
                            :disabled="
                                processingKeys.includes(item.key) ||
                                (
                                    item.stock != null &&
                                    item.quantity >= item.stock
                                )
                            "
                            @click="
                                changeQuantity(
                                    item,
                                    item.quantity + 1
                                )
                            "
                        >
                            ＋
                        </button>
                    </div>

                    <div class="cart-price">
                        <small v-if="item.quantity > 1">
                            {{ item.price.toLocaleString() }}원 × {{ item.quantity }}
                        </small>
                        <strong>
                            {{
                                (
                                    item.price * item.quantity
                                ).toLocaleString()
                            }}원
                        </strong>
                    </div>

                    <button
                        type="button"
                        class="cart-remove"
                        aria-label="상품 삭제"
                        :disabled="
                            processingKeys.includes(item.key)
                        "
                        @click="removeCartItem(item)"
                    >
                        ×
                    </button>
                </article>
            </div>

            <!-- 결제 금액 -->
            <aside class="cart-summary">
                <h2>결제 예정 금액</h2>

                <dl>
                    <dt>상품 금액</dt>

                    <dd>
                        {{ total.toLocaleString() }}원
                    </dd>

                    <dt>배송비</dt>

                    <dd>
                        {{
                            shippingFee
                                ? `${shippingFee.toLocaleString()}원`
                                : "무료"
                        }}
                    </dd>

                    <dt>총 결제 금액</dt>

                    <dd>
                        {{ finalAmount.toLocaleString() }}원
                    </dd>
                </dl>

                <p class="cart-shipping-notice">
                    * 20,000원 이상 구매 시 배송비 무료
                </p>

                <button
                    type="button"
                    @click="moveToCheckout"
                >
                    주문하기
                </button>
            </aside>
        </section>

        <!-- 장바구니가 비어 있는 경우 -->
        <section
            v-else-if="activeTab === 'cart'"
            class="cart-empty"
        >
            <h2>장바구니가 비어 있습니다.</h2>

            <p>
                아이에게 필요한 상품을 담아보세요.
            </p>

            <RouterLink to="/">
                쇼핑 계속하기
            </RouterLink>
        </section>

        <!-- 비회원 위시리스트 -->
        <section
            v-else-if="!accessToken"
            class="cart-empty"
        >
            <h2>로그인이 필요합니다.</h2>

            <p>
                위시리스트는 회원 전용 기능입니다.
            </p>

            <RouterLink
                :to="{
                    path: '/login',
                    query: {
                        redirect: '/cart?tab=wishlist'
                    }
                }"
            >
                로그인하러 가기
            </RouterLink>
        </section>

        <!-- 회원 위시리스트 -->
        <section
            v-else-if="wishlistItems.length"
            class="wishlist-panel"
        >
            <div class="product-grid">
                <ProductCard
                    v-for="item in wishlistItems"
                    :key="item.id"
                    :product="item"
                    :pet="item.pet"
                />
            </div>
        </section>

        <!-- 위시리스트가 비어 있는 경우 -->
        <section
            v-else
            class="cart-empty"
        >
            <h2>위시리스트가 비어 있습니다.</h2>

            <p>
                관심 있는 상품의 하트를 눌러 저장해 보세요.
            </p>

            <RouterLink to="/">
                상품 둘러보기
            </RouterLink>
        </section>
    </main>
</template>
