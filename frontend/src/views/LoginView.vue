
<!-- 회원 로그인과 Google OAuth 진입, 로그인 후 원래 경로 복귀를 담당합니다. -->
<script setup>
import {
    computed,
    onBeforeUnmount,
    onMounted,
    reactive,
    ref,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import { authService } from "@/services/authService";
import { useAuthSession } from "@/stores/auth";
import { useCart } from "@/stores/cart";

const route = useRoute();
const router = useRouter();

const form = reactive({
    userId: "",
    password: "",
});

const loading = ref(false);
const errorMessage = ref("");
const showPassword = ref(false);

const registered = computed(() => {
    return route.query.registered === "1";
});

const {
    setAccessToken,
    setRefreshToken,
    setCurrentUserId,
    setUserRole,
    setPendingOAuth,
} = useAuthSession();

const googleButton = ref(null);
const googleLinkOpen = ref(false);
const googleLinkPassword = ref("");
const googleLinkToken = ref("");
const googleLinkEmail = ref("");
const googleLinkError = ref("");

let googleInitTimer;

// 로그인 성공 시 인증 정보 저장
function saveLoginSession(result, userId) {
    setUserRole(result.role || result.memberRole || "MEMBER");

    setRefreshToken(result.refreshToken || "");
    setAccessToken(result.accessToken);

    setCurrentUserId(
        userId ||
        result.userId ||
        result.name ||
        String(result.memberId || "")
    );
}

// Google 로그인
async function handleGoogleCredential(response) {
    errorMessage.value = "";

    try {
        loading.value = true;

        const result = await authService.googleLogin(
            response.credential
        );

        if (result.nextStep === "SIGNUP") {
            setPendingOAuth({
                ticket: response.credential,
                email: result.email || "",
                name: result.name || "",
                provider: "google",
            });

            await router.push("/signup?oauth=google");
            return;
        }

        if (result.nextStep === "LINK") {
            googleLinkToken.value = response.credential;
            googleLinkEmail.value = result.email || "";
            googleLinkPassword.value = "";
            googleLinkError.value = "";
            googleLinkOpen.value = true;
            return;
        }

        saveLoginSession(
            result,
            result.userId ||
                result.name ||
                String(result.memberId || "google-member")
        );

        await mergeCartAfterLogin();

        await router.push(
            typeof route.query.redirect === "string"
                ? route.query.redirect
                : "/member"
        );
    } catch (error) {
        errorMessage.value =
            error.message || "Google 로그인에 실패했습니다.";
    } finally {
        loading.value = false;
    }
}

// Google 계정 연결 후 로그인
async function submitGoogleLink() {
    if (!googleLinkPassword.value) {
        googleLinkError.value =
            "기존 계정의 비밀번호를 입력해주세요.";
        return;
    }

    try {
        loading.value = true;
        googleLinkError.value = "";

        const result = await authService.linkGoogleAccount(
            googleLinkToken.value,
            googleLinkPassword.value
        );

        saveLoginSession(
            result,
            result.userId || result.name || ""
        );

        await mergeCartAfterLogin();

        googleLinkOpen.value = false;

        await router.push("/member");
    } catch (error) {
        googleLinkError.value =
            error.message || "계정 연결에 실패했습니다.";
    } finally {
        loading.value = false;
    }
}

// Google 로그인 버튼 생성
function renderGoogleButton() {
    if (!window.google?.accounts?.id || !googleButton.value) {
        return false;
    }

    window.google.accounts.id.initialize({
        client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
        callback: handleGoogleCredential,
    });

    window.google.accounts.id.renderButton(
        googleButton.value,
        {
            type: "standard",
            theme: "outline",
            size: "large",
            text: "continue_with",
            shape: "rectangular",
            width: Math.min(
                450,
                googleButton.value.clientWidth || 450
            ),
        }
    );

    return true;
}

onMounted(() => {
    if (renderGoogleButton()) {
        return;
    }

    googleInitTimer = window.setInterval(() => {
        if (renderGoogleButton()) {
            window.clearInterval(googleInitTimer);
        }
    }, 200);
});

onBeforeUnmount(() => {
    window.clearInterval(googleInitTimer);
});

// 일반 로그인
async function submit() {
    errorMessage.value = "";

    if (!form.userId.trim() || !form.password) {
        errorMessage.value =
            "아이디와 비밀번호를 모두 입력해주세요.";
        return;
    }

    try {
        loading.value = true;

        const result = await authService.login({
            userId: form.userId.trim(),
            passwordHash: form.password,
        });

        if (!result.accessToken || !result.refreshToken) {
            throw new Error(
                "로그인 토큰을 정상적으로 발급받지 못했습니다."
            );
        }

        saveLoginSession(result, form.userId.trim());

        await mergeCartAfterLogin();

        await router.push(
            typeof route.query.redirect === "string"
                ? route.query.redirect
                : result.role === "ADMIN"
                    ? "/admin/dashboard"
                    : "/member"
        );
    } catch (error) {
        // 차단·강퇴 계정은 전역 오류 팝업이 안내합니다.
        if (
            !["MEMBER_BLOCKED", "MEMBER_BANNED"].includes(
                error.code
            )
        ) {
            errorMessage.value =
                error.message || "로그인에 실패했습니다.";
        }
    } finally {
        loading.value = false;
    }
}

// 장바구니 기능
const { mergeGuestCart } = useCart();

async function mergeCartAfterLogin() {
    try {
        await mergeGuestCart();
    } catch (error) {
        console.error("비회원 장바구니 병합 실패:", error);
        errorMessage.value =
            "로그인은 완료됐지만 장바구니 병합에 실패했습니다. 비회원 상품은 브라우저에 보관되어 있습니다.";
    }
}
</script>

<template>
    <main class="login-page">
        <section class="login-card">
            <header class="signup-heading">
                <span>WELCOME BACK</span>

                <h1>로그인</h1>

                <p>
                    포캐스트에서 우리 아이를 위한 상품을 만나보세요.
                </p>
            </header>

            <form
                class="login-form"
                @submit.prevent="submit"
            >
                <p
                    v-if="registered"
                    class="login-success"
                >
                    회원가입이 완료되었습니다. 로그인해주세요.
                </p>

                <label class="form-field">
                    <span>아이디</span>

                    <input
                        v-model="form.userId"
                        autocomplete="username"
                        placeholder="아이디를 입력해주세요"
                    />
                </label>

                <label class="form-field">
                    <span>비밀번호</span>

                    <div class="password-input">
                        <input
                            v-model="form.password"
                            :type="showPassword ? 'text' : 'password'"
                            autocomplete="current-password"
                            placeholder="비밀번호를 입력해주세요"
                        />

                        <button
                            type="button"
                            class="password-visibility"
                            :aria-label="
                                showPassword
                                    ? '비밀번호 숨기기'
                                    : '비밀번호 표시'
                            "
                            :aria-pressed="showPassword"
                            @click="showPassword = !showPassword"
                        >
                            <svg
                                viewBox="0 0 24 24"
                                aria-hidden="true"
                            >
                                <path
                                    d="M2.5 12s3.2-5 9.5-5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z"
                                />

                                <circle
                                    cx="12"
                                    cy="12"
                                    r="2.7"
                                />

                                <path
                                    v-if="!showPassword"
                                    d="m4 4 16 16"
                                />
                            </svg>
                        </button>
                    </div>
                </label>

                <nav
                    class="login-tools"
                    aria-label="계정 찾기"
                >
                    <RouterLink to="/password-reset">
                        비밀번호 초기화하기
                    </RouterLink>
                </nav>

                <p
                    v-if="errorMessage"
                    class="form-message login-error"
                >
                    {{ errorMessage }}
                </p>

                <button
                    class="login-submit"
                    type="submit"
                    :disabled="loading"
                >
                    {{ loading ? "로그인 중..." : "로그인" }}
                </button>

                <div class="auth-divider">
                    <span>또는 간편 로그인</span>
                </div>

                <div
                    ref="googleButton"
                    class="google-identity-button"
                    aria-label="Google로 계속하기"
                ></div>

                <div class="signup-guide">
                    <div>
                        <b>아직 포캐스트 회원이 아니신가요?</b>

                        <span>
                            회원가입하고 맞춤 상품을 추천받아보세요.
                        </span>
                    </div>

                    <RouterLink to="/signup">
                        회원가입
                    </RouterLink>
                </div>
            </form>
        </section>

        <Teleport to="body">
            <div
                v-if="googleLinkOpen"
                class="common-modal-backdrop"
                @click.self="
                    !loading && (googleLinkOpen = false)
                "
            >
                <form
                    class="common-modal google-link-modal"
                    @submit.prevent="submitGoogleLink"
                >
                    <button
                        type="button"
                        class="common-modal-close"
                        aria-label="닫기"
                        :disabled="loading"
                        @click="googleLinkOpen = false"
                    >
                        ×
                    </button>

                    <small>GOOGLE ACCOUNT LINK</small>

                    <h2>기존 계정과 연결해주세요</h2>

                    <p>
                        <b>{{ googleLinkEmail }}</b>
                        이메일로 가입된 일반 계정이 있습니다.
                        기존 계정의 비밀번호를 입력하면
                        Google 로그인과 안전하게 연결됩니다.
                    </p>

                    <label>
                        기존 계정 비밀번호

                        <input
                            v-model="googleLinkPassword"
                            type="password"
                            autocomplete="current-password"
                            placeholder="현재 비밀번호를 입력해주세요"
                        />
                    </label>

                    <small
                        v-if="googleLinkError"
                        class="form-message error"
                    >
                        {{ googleLinkError }}
                    </small>

                    <div class="common-modal-actions">
                        <button
                            type="button"
                            class="secondary"
                            :disabled="loading"
                            @click="googleLinkOpen = false"
                        >
                            취소
                        </button>

                        <button
                            class="primary"
                            type="submit"
                            :disabled="loading"
                        >
                            {{
                                loading
                                    ? "연결 중..."
                                    : "계정 연결 후 로그인"
                            }}
                        </button>
                    </div>
                </form>
            </div>
        </Teleport>
    </main>
</template>
