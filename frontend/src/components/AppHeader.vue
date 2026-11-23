
<!-- 로그인 상태, 장바구니 수량과 반응형 메뉴를 한곳에서 제어하는 전역 헤더입니다. -->
<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useCart } from "@/stores/cart";
import { useAuthSession } from "@/stores/auth";
import { authService } from "@/services/authService";

const open = ref(false);
const isLoggingOut = ref(false);

const { count } = useCart();

const router = useRouter();
const route = useRoute();

const {
    accessToken,
    refreshToken,
    clearAuthSession,
} = useAuthSession();

const homeTarget = computed(() => {
    return accessToken.value ? "/member" : "/";
});

// 로그아웃
async function logout() {
    if (isLoggingOut.value) {
        return;
    }

    isLoggingOut.value = true;

    try {
        if (!refreshToken.value) {
            throw new Error(
                "Refresh Token이 존재하지 않습니다."
            );
        }

        // 백엔드에 Refresh Token 무효화 요청
        await authService.logout(refreshToken.value);

        // 프론트엔드 인증 정보 삭제
        clearAuthSession();

        // 메인 페이지 이동
        await router.push("/");
    } catch (error) {
        console.error("로그아웃 처리 실패:", error);

        alert(
            "로그아웃 처리에 실패했습니다. 다시 시도해 주세요."
        );
    } finally {
        isLoggingOut.value = false;
    }
}

// 페이지 이동 시 모바일 메뉴 닫기
watch(
    () => route.fullPath,
    () => {
        open.value = false;
    }
);
</script>

<template>
    <section class="top-hero">
        <div class="container hero-inner">
            <div>
                <span>PET FOOD &amp; CARE</span>

                <h1>
                    우리 아이를 위한
                    <em>균형 잡힌 영양</em>
                </h1>

                <p>
                    강아지 · 고양이 식품관 |
                    사료 · 간식 · 영양제를 한곳에서
                </p>
            </div>

            <div class="hero-pets"></div>
        </div>
    </section>

    <header class="site-header nav-wrap">
        <div class="container nav-inner">
            <RouterLink
                :to="homeTarget"
                class="logo"
            >
                <img
                    src="/assets/pawcast-logo-large-tagline.png"
                    alt="POWCAST · 포캐스트 반려동물 용품"
                />
            </RouterLink>

            <button
                class="mobile-menu"
                type="button"
                @click="open = !open"
            >
                ☰
            </button>

            <nav :class="{ open }">
                <RouterLink :to="homeTarget">
                    홈
                </RouterLink>

                <RouterLink to="/dog">
                    강아지관
                </RouterLink>

                <RouterLink to="/cat">
                    고양이관
                </RouterLink>

                <RouterLink to="/tableware">
                    식기·급수관
                </RouterLink>

                <RouterLink
                    :to="{ name: 'qna-list' }"
                    :class="{
                        'router-link-active':
                            route.path.startsWith('/qna')
                    }"
                >
                    Q&amp;A
                </RouterLink>

                <RouterLink
                    :to="{ name: 'notice-list' }"
                    :class="{
                        'router-link-active':
                            route.path.startsWith('/notices')
                    }"
                >
                    공지/이벤트
                </RouterLink>
            </nav>

            <div class="account">
                <template v-if="accessToken">
                    <RouterLink
                        class="member-home-link"
                        to="/mypage"
                    >
                        마이페이지
                    </RouterLink>

                    <button
                        class="header-logout"
                        type="button"
                        :disabled="isLoggingOut"
                        @click="logout"
                    >
                        {{
                            isLoggingOut
                                ? "로그아웃 중..."
                                : "로그아웃"
                        }}
                    </button>
                </template>

                <template v-else>
                    <RouterLink to="/login">
                        로그인
                    </RouterLink>

                    <RouterLink
                        class="signup"
                        to="/signup"
                    >
                        회원가입
                    </RouterLink>
                </template>

                <RouterLink
                    to="/cart"
                    class="cart-button"
                    :aria-label="`장바구니, 상품 ${count}개`"
                >
                    <svg
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                    >
                        <path
                            d="M3 4h2l2.1 9.2a2 2 0 0 0 2 1.6h7.8a2 2 0 0 0 2-1.6L20.3 7H6"
                        />

                        <circle
                            cx="9.5"
                            cy="19"
                            r="1.4"
                        />

                        <circle
                            cx="17"
                            cy="19"
                            r="1.4"
                        />
                    </svg>

                    <span
                        v-if="count"
                        class="cart-count"
                    >
                        {{ count > 99 ? "99+" : count }}
                    </span>
                </RouterLink>
            </div>
        </div>
    </header>
</template>
