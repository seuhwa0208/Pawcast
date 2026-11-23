
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

const props = defineProps({
    product: Object,
    pet: String,
});
const { add } = useCart();
const { isFavorite, toggle } = useWishlist();
const { accessToken } = useAuthSession();
const router = useRouter();

const loginRequired = ref(false);
const soldOutNotice = ref(false);
const cartAdded = ref(false);
const cartError = ref("");
const isAdding = ref(false);

// 이미지 로딩 실패 시 대체 이미지를 표시합니다.
function handleImageError(event) {
    const fallback =
        props.product.imageFallback ||
        "/assets/products/generated/dry-food.png";

    if (event.currentTarget.src.endsWith(fallback)) {
        return;
    }

    event.currentTarget.src = fallback;
}

// 찜하기는 회원 전용입니다.
async function handleWishlist(id) {
    if (!accessToken.value) {
        loginRequired.value = true;
        return;
    }

    try {
        await toggle(id);
    } catch (error) {
        console.error("찜하기 처리 실패:", error);
        cartError.value =
            error.message || "찜하기 처리에 실패했습니다.";
    }
}

// 회원과 비회원 모두 장바구니에 담을 수 있습니다.
async function handleCart() {
    if (isAdding.value || !props.product.stock) {
        return;
    }

    cartError.value = "";

    const variantId = Number(props.product.variantId);

    if (!Number.isSafeInteger(variantId) || variantId < 1) {
        router.push(
            `/products/${props.pet}/${props.product.id}`
        );
        return;
    }

    isAdding.value = true;

    try {
        await add(props.product, props.pet);
        cartAdded.value = true;
    } catch (error) {
        console.error("장바구니 담기 실패:", error);

        if (error.message === "LOGIN_REQUIRED") {
            loginRequired.value = true;
            return;
        }

        cartError.value =
            error.message || "장바구니에 상품을 담지 못했습니다.";
    } finally {
        isAdding.value = false;
    }
}

function goToLogin() {
    loginRequired.value = false;
    router.push("/login");
}
</script>

<template>
    <article
        class="product-card"
        :class="{ 'sold-out-card': !product.stock }"
    >
        <RouterLink
            v-if="product.stock"
            :to="`/products/${pet}/${product.id}`"
        >
            <div class="product-image">
                <img
                    :src="product.image"
                    :alt="product.name"
                    @error="handleImageError"
                />
            </div>
            <UiTag
                :label="product.subcategory || product.category"
                :variant="pet === 'CAT' ? 'cat' : 'dog'"
            />
            <h3>{{ product.name }}</h3>
            <p>
                ★ {{ product.rating }}
                (리뷰 {{ product.reviewCount || 0 }}건)
            </p>
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
                <img
                    :src="product.image"
                    :alt="product.name"
                    @error="handleImageError"
                />
                <div class="soldout">
                    <strong>품절 상품</strong>
                    <small>현재 구매할 수 없습니다</small>
                </div>
            </div>
            <UiTag
                :label="product.subcategory || product.category"
                :variant="pet === 'CAT' ? 'cat' : 'dog'"
            />
            <h3>{{ product.name }}</h3>
            <p>
                ★ {{ product.rating }}
                (리뷰 {{ product.reviewCount || 0 }}건)
            </p>
        </div>

        <div class="product-card-footer">
            <div class="product-card-price">
                <del v-if="product.discountRate">
                    {{ product.originalPrice.toLocaleString() }}원
                </del>
                <strong>
                    <span
                        v-if="product.discountRate"
                        class="discount-rate"
                    >
                        {{ product.discountRate }}%
                    </span>
                    {{ product.price.toLocaleString() }}원
                </strong>
            </div>

            <div class="card-actions">
                <button
                    type="button"
                    class="wishlist-button"
                    :class="{
                        active: accessToken && isFavorite(product.id)
                    }"
                    :aria-label="
                        isFavorite(product.id) ? '찜 해제' : '찜하기'
                    "
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
                    :disabled="!product.stock || isAdding"
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

        <p
            v-if="cartError"
            class="form-message error"
            role="alert"
        >
            {{ cartError }}
        </p>

        <LoginRequiredModal
            v-model="loginRequired"
            @login="goToLogin"
        />
        <SoldOutModal
            v-model="soldOutNotice"
            :product-name="product.name"
        />
        <CartAddedModal
            v-model="cartAdded"
            @cart="router.push('/cart')"
        />
    </article>
</template>
