<!-- 일반 가입과 Google 추가정보 가입을 하나의 검증·약관 동의 흐름으로 처리합니다. -->
<script setup>
import { computed, reactive, ref, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { authService } from "@/services/authService";
import { useAuthSession } from "@/stores/auth";
import SignupCompleteModal from "@/components/SignupCompleteModal.vue";
import { useCart } from "@/stores/cart";

const router = useRouter();
const {
  pendingOAuth,
  setAccessToken,
  setRefreshToken,
  setCurrentUserId,
  setUserRole,
  clearPendingOAuth,
} = useAuthSession();
const form = reactive({
  userId: "",
  username: "",
  email: "",
  tel: "",
  passwordHash: "",
  checkPasswordHash: "",
  verificationCode: "",
});
// OAuth에서 이미 받은 정보는 다시 입력하지 않도록 가입 폼의 초깃값으로 사용합니다.
if (pendingOAuth.provider === "google" && pendingOAuth.ticket) {
  form.email = pendingOAuth.email;
  form.username = pendingOAuth.name;
}
const agreed = reactive({
  terms: false,
  privacy: false,
  efinance: false,
  marketing: false,
});
const state = reactive({
  idChecked: false,
  idCheckMessage: "",
  idCheckStatus: "",
  emailChecked: false,
  emailCheckMessage: "",
  emailCheckStatus: "",
  emailSent: false,
  emailVerified: false,
  loading: false,
});
const notice = ref({ text: "", type: "" });
const activeTerm = ref(null);
const signupComplete = ref(false);
const showPassword = ref(false);
const showPasswordConfirm = ref(false);
const remainingSeconds = ref(0);
let emailExpiresAt = 0;
let emailTimer = null;
let disposed = false;

const emailTimerText = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60);
  const seconds = remainingSeconds.value % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
});

function stopEmailTimer() {
  if (emailTimer !== null) clearInterval(emailTimer);
  emailTimer = null;
}

function startEmailTimer(expiresAt) {
  stopEmailTimer();
  emailExpiresAt = expiresAt;
  function updateTimer() {
    remainingSeconds.value = Math.max(0, Math.ceil((emailExpiresAt - Date.now()) / 1000));
    if (!remainingSeconds.value) stopEmailTimer();
  }
  updateTimer();
  if (remainingSeconds.value) emailTimer = setInterval(updateTimer, 250);
}

onUnmounted(() => {
  disposed = true;
  stopEmailTimer();
});
const termDetails = {
  terms: {
    title: "서비스 이용약관 동의",
    required: true,
    items: [
      ["수집 항목", "별도 개인정보 수집 없음 — 서비스 이용 계약 관계 성립"],
      [
        "수집·이용 목적",
        "회원과 회사 간의 권리·의무 및 책임사항 규정, 서비스 이용 조건 및 절차 안내, 계정 관리",
      ],
      ["보유·이용 기간", "회원 탈퇴 시 또는 서비스 이용계약 종료 시까지"],
      ["안내 문구", "[필수] 서비스 이용약관 동의"],
    ],
    laws: [
      "민법 제105조 및 제543조 — 계약 성립 및 해지에 관한 일반 원칙",
      "약관의 규제에 관한 법률 제3조 — 고객이 약관 내용을 알 수 있도록 명시하고 설명할 의무",
    ],
  },
  privacy: {
    title: "개인정보 수집 및 이용 동의",
    required: true,
    items: [
      [
        "수집 항목",
        "[기본 정보] 아이디, 비밀번호, 이름, 이메일, 휴대폰 번호 / [자동 수집] IP 주소, 쿠키, 방문 일시, 서비스 이용 기록",
      ],
      [
        "수집·이용 목적",
        "본인 확인 및 식별, 서비스 제공 및 운영, 고객 문의 응대, 공지사항 전달",
      ],
      [
        "보유·이용 기간",
        "회원 탈퇴 시까지. 단, 관계 법령에 따라 보관이 필요한 경우 해당 법령 기준을 준수합니다.",
      ],
      [
        "동의 거부 안내",
        "동의를 거부할 권리가 있으나, 필수 정보 수집에 동의하지 않으면 회원가입 및 서비스 이용이 제한될 수 있습니다.",
      ],
    ],
    laws: [
      "개인정보 보호법 제15조 제1항 제1호 — 정보주체의 동의를 받은 경우 개인정보 수집·이용",
      "개인정보 보호법 제22조 — 필수 항목과 선택 항목을 구분하여 동의를 받는 방법",
    ],
  },
  efinance: {
    title: "전자금융거래 이용약관 동의",
    required: true,
    items: [
      [
        "수집 항목",
        "[결제·거래 정보] 계좌번호, 카드 정보, 거래 일시, 승인번호, 지급인·수취인 정보, 기기 식별 정보",
      ],
      [
        "수집·이용 목적",
        "전자금융거래의 안전성 확보, 결제 및 송금 처리, 거래 내역 조회 및 확인, 정산 및 오류 처리",
      ],
      ["보유·이용 기간", "5년 — 전자금융거래법 및 전자상거래법 기준"],
      ["안내 문구", "[필수] 전자금융거래 이용약관 동의"],
    ],
    laws: [
      "전자금융거래법 제21조 및 제22조 — 안전성 확보와 전자금융거래기록 생성·보존",
      "전자상거래 등에서의 소비자보호에 관한 법률 제6조 — 대금결제 및 재화 공급 기록 5년 보존",
    ],
  },
  marketing: {
    title: "혜택 및 이벤트 정보 수신 동의",
    required: false,
    items: [
      ["수집 항목", "이메일, 휴대폰 번호, 앱 푸시(Push) 토큰"],
      [
        "수집·이용 목적",
        "신규 상품·서비스 안내, 이벤트 및 프로모션 정보 제공, 맞춤형 혜택 추천, 마케팅 분석",
      ],
      ["보유·이용 기간", "동의 철회 시 또는 회원 탈퇴 시까지"],
      [
        "동의 거부 안내",
        "동의를 거부하시더라도 기본 서비스 이용이 가능합니다.",
      ],
    ],
    laws: [
      "개인정보 보호법 제15조 제1항 제1호 및 제22조 제4항 — 선택 항목 동의 거부를 이유로 서비스 제공을 거부할 수 없음",
      "정보통신망 이용촉진 및 정보보호 등에 관한 법률 제50조 — 영리 목적 광고성 정보 전송 시 명시적인 사전 동의 필요",
    ],
  },
};

const userIdValid = computed(() => /^[a-z][a-z0-9]{5,19}$/.test(form.userId));
const emailValid = computed(() => /^\S+@\S+\.\S+$/.test(form.email));
const telValid = computed(() => /^010-\d{4}-\d{4}$/.test(form.tel));
const passwordCompositionValid = computed(() =>
  /^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]+$/.test(form.passwordHash),
);
const passwordLengthValid = computed(
  () => form.passwordHash.length >= 8 && form.passwordHash.length <= 20,
);
const passwordValid = computed(
  () => passwordCompositionValid.value && passwordLengthValid.value,
);
const passwordMatches = computed(
  () =>
    !!form.checkPasswordHash && form.passwordHash === form.checkPasswordHash,
);
const oauthSignup = computed(
  () => pendingOAuth.provider === "google" && !!pendingOAuth.ticket,
);
const allAgreed = computed({
  get: () =>
    agreed.terms && agreed.privacy && agreed.efinance && agreed.marketing,
  set: (value) =>
    Object.keys(agreed).forEach((key) => {
      agreed[key] = value;
    }),
});
const canSubmit = computed(() => {
  const required =
    form.username.trim() &&
    telValid.value &&
    userIdValid.value &&
    state.idChecked &&
    passwordValid.value &&
    passwordMatches.value &&
    agreed.terms &&
    agreed.privacy &&
    agreed.efinance;

  return Boolean(required && (oauthSignup.value || (
    emailValid.value && state.emailChecked && state.emailVerified
  )));
});

function showNotice(text, type = "error") {
  notice.value = { text, type };
}

function resetIdCheck() {
  state.idChecked = false;
  state.idCheckMessage = "";
  state.idCheckStatus = "";
}

function resetEmailVerification() {
  stopEmailTimer();
  emailExpiresAt = 0;
  remainingSeconds.value = 0;
  notice.value = { text: "", type: "" };
  state.emailChecked = false;
  state.emailCheckMessage = "";
  state.emailCheckStatus = "";
  state.emailSent = false;
  state.emailVerified = false;
  form.verificationCode = "";
}

async function checkEmail() {
  if (!emailValid.value)
    return showNotice("올바른 이메일 주소를 입력해주세요.");
  try {
    state.loading = true;
    const result = await authService.checkEmailAvailability(form.email);
    state.emailChecked = result.available === true;
    state.emailCheckStatus = state.emailChecked ? "valid" : "invalid";
    state.emailCheckMessage = state.emailChecked
      ? "사용 가능한 이메일입니다."
      : "이미 가입된 이메일입니다.";
  } catch (error) {
    state.emailChecked = false;
    state.emailCheckStatus = "invalid";
    state.emailCheckMessage =
      error.message || "이메일 중복 확인에 실패했습니다.";
  } finally {
    state.loading = false;
  }
}

function formatTel(event) {
  const numbers = event.target.value.replace(/\D/g, "").slice(0, 11);
  if (numbers.length <= 3) form.tel = numbers;
  else if (numbers.length <= 7)
    form.tel = `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
  else
    form.tel = `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7)}`;
}

async function checkId() {
  if (!userIdValid.value) {
    state.idChecked = false;
    state.idCheckMessage =
      "아이디는 영문 소문자로 시작하는 6~20자의 영문 소문자·숫자 조합이어야 합니다.";
    state.idCheckStatus = "invalid";
    return;
  }
  try {
    state.loading = true;
    const result = await authService.checkAvailability({ userId: form.userId });
    if (result.available === false) {
      state.idChecked = false;
      state.idCheckMessage = "이미 가입된 아이디가 있습니다.";
      state.idCheckStatus = "invalid";
      return;
    }
    state.idChecked = true;
    state.idCheckMessage = "사용 가능한 아이디입니다.";
    state.idCheckStatus = "valid";
  } catch (error) {
    state.idChecked = false;
    if (/중복|이미|가입|사용 중/.test(error.message)) {
      state.idCheckMessage = "이미 가입된 아이디가 있습니다.";
      state.idCheckStatus = "invalid";
    } else showNotice(error.message);
  } finally {
    state.loading = false;
  }
}

async function sendEmailCode() {
  if (oauthSignup.value || state.loading || state.emailVerified) return;
  if (!emailValid.value) return showNotice("올바른 이메일 주소를 입력해주세요.");
  if (!state.emailChecked) return showNotice("이메일 중복 확인을 먼저 해주세요.");

  const requestedEmail = form.email;
  const requestedAt = Date.now();
  try {
    state.loading = true;
    await authService.sendEmailCode(requestedEmail);
    if (disposed || form.email !== requestedEmail) return;
    state.emailSent = true;
    state.emailVerified = false;
    form.verificationCode = "";
    startEmailTimer(requestedAt + 300000);
    showNotice("인증번호를 발송했습니다. 5분 이내에 입력해주세요.", "success");
  } catch (error) {
    showNotice(error.message || "인증번호 발송에 실패했습니다.");
  } finally {
    state.loading = false;
  }
}

async function confirmEmailCode() {
  if (oauthSignup.value || state.loading || state.emailVerified) return;
  if (!state.emailSent) return showNotice("인증번호를 먼저 발송해주세요.");
  if (Date.now() >= emailExpiresAt) {
    return showNotice("인증시간이 만료되었습니다. 인증번호를 재발송해주세요.");
  }
  if (!/^\d{6}$/.test(form.verificationCode)) {
    return showNotice("6자리 인증번호를 입력해주세요.");
  }

  const requestedEmail = form.email;
  try {
    state.loading = true;
    const result = await authService.confirmEmailCode(requestedEmail, form.verificationCode);
    if (disposed || form.email !== requestedEmail) return;
    if (result !== true) throw new Error("인증번호가 일치하지 않습니다.");
    state.emailVerified = true;
    stopEmailTimer();
    showNotice("이메일 인증이 완료되었습니다.", "success");
  } catch (error) {
    showNotice(error.message || "이메일 인증에 실패했습니다.");
  } finally {
    state.loading = false;
  }
}

async function submit() {
  if (state.loading) return;
  if (!canSubmit.value)
    return showNotice("필수 입력, 중복 확인, 이메일 인증 및 약관 동의를 확인해주세요.");
  const payload = {
    userId: form.userId,
    name: form.username,
    email: form.email,
    tel: form.tel.replace(/\D/g, ""),
    passwordHash: form.passwordHash,
    checkPasswordHash: form.checkPasswordHash,
    isAgreedTerms: agreed.terms,
    isAgreedPrivacy: agreed.privacy,
    isAgreedEfinance: agreed.efinance,
    isAgreedMarketing: agreed.marketing,
  };
  try {
    state.loading = true;
    if (oauthSignup.value) {
      const result = await authService.completeGoogleSignup({
        idToken: pendingOAuth.ticket,
        userId: form.userId,
        name: form.username,
        tel: payload.tel,
        passwordHash: form.passwordHash,
        checkPasswordHash: form.checkPasswordHash,
        agreedTerms: agreed.terms,
        agreedPrivacy: agreed.privacy,
        agreedEfinance: agreed.efinance,
        agreedMarketing: agreed.marketing,
      });
      setUserRole(result.role || "USER");
      setAccessToken(result.accessToken);
      setRefreshToken(result.refreshToken || "");
      setCurrentUserId(result.userId || result.name || "");
      clearPendingOAuth();
      await router.replace("/member");
    } else {
      await authService.signup(payload);
      signupComplete.value = true;
    }
  } catch (error) {
    showNotice(error.message);
  } finally {
    state.loading = false;
  }
}
// 장바구니 기능
const { mergeGuestCart } = useCart();

async function mergeCartAfterLogin() {
  try {
    await mergeGuestCart();
  } catch (error) {
    console.error("비회원 장바구니 병합 실패:", error);
    showNotice("로그인은 완료됐지만 장바구니 병합에 실패했습니다.");
  }
}
</script>

<template>
  <main class="signup-page">
    <section class="signup-card">
      <header class="signup-heading">
        <span>WELCOME TO POWCAST</span>
        <h1>회원가입</h1>
        <p>포캐스트와 함께 우리 아이에게 꼭 맞는 상품을 찾아보세요.</p>
      </header>
      <form class="signup-form" @submit.prevent="submit">
        <div v-if="oauthSignup" class="oauth-signup-notice">
          <span>Google</span>
          <div>
            <b>{{ form.email }}</b>
            <p>
              Google 계정이 확인되었습니다. 필수 정보와 약관 동의를
              완료해주세요.
            </p>
          </div>
        </div>
        <fieldset>
          <legend>계정 정보</legend>
          <!-- Google OAuth 추가정보 가입에서도 userId가 필수이므로 아이디 입력과 중복 확인을 항상 노출합니다. -->
          <label class="form-field"
            ><span>아이디 <b>*</b></span>
            <div class="input-action">
              <input
                v-model.trim="form.userId"
                :disabled="state.loading"
                autocomplete="username"
                placeholder="영문 소문자로 시작하는 6~20자"
                @input="resetIdCheck"
              /><button
                type="button"
                :disabled="state.loading"
                @click="checkId"
              >
                중복 확인
              </button>
            </div>
            <small v-if="state.idCheckMessage" :class="state.idCheckStatus">{{
              state.idCheckMessage
            }}</small
            ><small v-else-if="!userIdValid" :class="{ invalid: form.userId }"
              >영문 소문자로 시작하고 소문자와 숫자만 사용할 수 있어요.</small
            ></label
          >
          <label v-if="oauthSignup" class="form-field">
            <span>이메일 <b>*</b></span>
            <input v-model="form.email" type="email" readonly aria-readonly="true" />
            <small>Google 계정으로 확인된 이메일입니다. 변경할 수 없습니다.</small>
          </label>

          <section v-else class="email-verification">
            <section class="form-field">
              <label for="signup-email">이메일 <b>*</b></label>
              <div class="input-action">
                <input
                  id="signup-email"
                  v-model.trim="form.email"
                  type="email"
                  autocomplete="email"
                  placeholder="example@powcast.com"
                  :disabled="state.loading"
                  @input="resetEmailVerification"
                />
                <button
                  type="button"
                  :disabled="state.loading || !emailValid || state.emailChecked"
                  @click="checkEmail"
                >
                  {{ state.emailChecked ? "확인 완료" : "중복 확인" }}
                </button>
              </div>
              <small v-if="state.emailCheckMessage" :class="state.emailCheckStatus">
                {{ state.emailCheckMessage }}
              </small>
              <small v-else>가입에 사용할 이메일 주소를 입력해주세요.</small>
            </section>

            <button
              v-if="state.emailChecked && !state.emailVerified"
              class="email-send-button"
              type="button"
              :disabled="state.loading"
              @click="sendEmailCode"
            >
              {{ state.emailSent ? "인증번호 재발송" : "인증번호 발송" }}
            </button>

            <section v-if="state.emailSent && !state.emailVerified" class="form-field">
              <label for="signup-email-code">이메일 인증번호 <b>*</b></label>
              <div class="input-action email-code-action">
                <input
                  id="signup-email-code"
                  v-model.trim="form.verificationCode"
                  type="text"
                  inputmode="numeric"
                  autocomplete="one-time-code"
                  maxlength="6"
                  placeholder="인증번호 6자리"
                  :disabled="state.loading || remainingSeconds === 0"
                />
                <span class="email-timer" :class="{ expired: remainingSeconds === 0 }">
                  {{ emailTimerText }}
                </span>
                <button
                  type="button"
                  :disabled="state.loading || remainingSeconds === 0 || !/^\d{6}$/.test(form.verificationCode)"
                  @click="confirmEmailCode"
                >
                  인증 확인
                </button>
              </div>
              <small v-if="remainingSeconds === 0" class="invalid">
                인증시간이 만료되었습니다. 인증번호를 재발송해주세요.
              </small>
              <small v-else>이메일로 발송된 인증번호를 5분 이내에 입력해주세요.</small>
            </section>

            <p v-if="state.emailVerified" class="email-verified-message" role="status">
              이메일 인증이 완료되었습니다.
            </p>
          </section>
          <div class="form-grid">
            <label class="form-field"
              ><span>비밀번호 <b>*</b></span>
              <div class="password-input">
                <input
                  v-model="form.passwordHash"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  placeholder="영문·숫자·특수문자 포함 8~20자"
                /><button
                  type="button"
                  class="password-visibility"
                  :aria-label="
                    showPassword ? '비밀번호 숨기기' : '비밀번호 표시'
                  "
                  :aria-pressed="showPassword"
                  @click="showPassword = !showPassword"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z"
                    />
                    <circle cx="12" cy="12" r="2.7" />
                    <path v-if="!showPassword" d="m4 4 16 16" />
                  </svg>
                </button>
              </div>
              <small
                v-if="form.passwordHash && !passwordCompositionValid"
                class="invalid"
                >영문, 숫자, 허용 특수문자(!@#$%^&*)를 각각 1개 이상 포함해주세요.</small
              ><small
                v-else-if="form.passwordHash && !passwordLengthValid"
                class="invalid"
                >비밀번호는 8~20자로 입력해주세요.</small
              ></label
            ><label class="form-field"
              ><span>비밀번호 확인 <b>*</b></span>
              <div class="password-input">
                <input
                  v-model="form.checkPasswordHash"
                  :type="showPasswordConfirm ? 'text' : 'password'"
                  autocomplete="new-password"
                  placeholder="비밀번호를 다시 입력해주세요"
                /><button
                  type="button"
                  class="password-visibility"
                  :aria-label="
                    showPasswordConfirm ? '비밀번호 숨기기' : '비밀번호 표시'
                  "
                  :aria-pressed="showPasswordConfirm"
                  @click="showPasswordConfirm = !showPasswordConfirm"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z"
                    />
                    <circle cx="12" cy="12" r="2.7" />
                    <path v-if="!showPasswordConfirm" d="m4 4 16 16" />
                  </svg>
                </button>
              </div>
              <small
                v-if="form.checkPasswordHash"
                :class="{ valid: passwordMatches, invalid: !passwordMatches }"
                >{{
                  passwordMatches
                    ? "비밀번호가 일치합니다."
                    : "비밀번호가 일치하지 않습니다."
                }}</small
              ></label
            >
          </div>
        </fieldset>

        <fieldset>
          <legend>기본 정보</legend>
          <div class="form-grid">
            <label class="form-field"
              ><span>이름 <b>*</b></span
              ><input
                v-model.trim="form.username"
                autocomplete="name"
                placeholder="이름을 입력해주세요" /></label
            ><label class="form-field"
              ><span>휴대전화 <b>*</b></span
              ><input
                :value="form.tel"
                inputmode="numeric"
                maxlength="13"
                autocomplete="tel"
                placeholder="010-1234-5678"
                @input="formatTel"
            /></label>
          </div>
        </fieldset>

        <section class="terms">
          <h2>약관 동의</h2>
          <label class="agree-all"
            ><input v-model="allAgreed" type="checkbox" /><span
              >전체 동의</span
            ></label
          >
          <div class="term-list">
            <label
              ><input v-model="agreed.terms" type="checkbox" /><span
                ><b>필수</b> 이용약관 동의</span
              ><button
                type="button"
                @click.stop="activeTerm = termDetails.terms"
              >
                보기
              </button></label
            ><label
              ><input v-model="agreed.privacy" type="checkbox" /><span
                ><b>필수</b> 개인정보 수집 및 이용 동의</span
              ><button
                type="button"
                @click.stop="activeTerm = termDetails.privacy"
              >
                보기
              </button></label
            ><label
              ><input v-model="agreed.efinance" type="checkbox" /><span
                ><b>필수</b> 전자금융거래 이용약관 동의</span
              ><button
                type="button"
                @click.stop="activeTerm = termDetails.efinance"
              >
                보기
              </button></label
            ><label
              ><input v-model="agreed.marketing" type="checkbox" /><span
                ><em>선택</em> 혜택 및 이벤트 정보 수신 동의</span
              ><button
                type="button"
                @click.stop="activeTerm = termDetails.marketing"
              >
                보기
              </button></label
            >
          </div>
        </section>

        <p v-if="notice.text" class="form-message" :class="notice.type">
          {{ notice.text }}
        </p>
        <button
          class="signup-submit"
          type="submit"
          :disabled="state.loading || !canSubmit"
        >
          {{ state.loading ? "처리 중..." : "회원가입" }}
        </button>
        <p class="login-guide">
          이미 회원이신가요? <RouterLink to="/login">로그인</RouterLink>
        </p>
      </form>
    </section>
    <Teleport to="body"
      ><div
        v-if="activeTerm"
        class="terms-modal-backdrop"
        role="presentation"
        @click.self="activeTerm = null"
      >
        <section
          class="terms-modal"
          role="dialog"
          aria-modal="true"
          :aria-label="activeTerm.title"
        >
          <header>
            <div>
              <span :class="{ optional: !activeTerm.required }">{{
                activeTerm.required ? "필수" : "선택"
              }}</span>
              <h2>{{ activeTerm.title }}</h2>
            </div>
            <button type="button" aria-label="닫기" @click="activeTerm = null">
              ×
            </button>
          </header>
          <div class="terms-modal-body">
            <dl>
              <template v-for="item in activeTerm.items" :key="item[0]"
                ><dt>{{ item[0] }}</dt>
                <dd>{{ item[1] }}</dd></template
              >
            </dl>
            <section>
              <h3>관련 법률 근거</h3>
              <ul>
                <li v-for="law in activeTerm.laws" :key="law">{{ law }}</li>
              </ul>
            </section>
          </div>
          <footer>
            <button type="button" @click="activeTerm = null">확인</button>
          </footer>
        </section>
      </div></Teleport
    >
    <SignupCompleteModal
      :model-value="signupComplete"
      @login="router.push({ path: '/login', query: { registered: '1' } })"
      @home="router.push('/')"
    />
  </main>
</template>
<style scoped src="./signup-email-verification.css"></style>
