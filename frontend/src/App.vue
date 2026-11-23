<!-- 전역 헤더·현재 라우트 화면·푸터를 조립하는 애플리케이션 최상위 셸입니다. -->
<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppHeader from "@/components/AppHeader.vue";
import AppFooter from "@/components/AppFooter.vue";
import GlobalErrorModal from "@/components/GlobalErrorModal.vue";
import PetRegistrationPrompt from "@/components/PetRegistrationPrompt.vue";
import { useAuthSession } from "@/stores/auth";
import { petService } from "@/services/petService";

const route = useRoute();
const router = useRouter();
const { accessToken, currentUserId, clearAuthSession } = useAuthSession();
const errorOpen = ref(false);
const errorContent = reactive({ title: "", message: "", details: [] });
const usesAdminLayout = computed(() => Boolean(route.meta.adminLayout));
const petPromptOpen = ref(false);
let checkedPetToken = "";

function petPromptStorageKey() {
  return `powcast-pet-prompt-hidden-until:${currentUserId.value}`;
}

function isPetPromptDeferred() {
  const hiddenUntil = Number(localStorage.getItem(petPromptStorageKey()) || 0);
  return Number.isFinite(hiddenUntil) && hiddenUntil > Date.now();
}

const errorTitles = {
  MEMBER_BLOCKED: "임시 차단된 계정입니다",
  MEMBER_BANNED: "이용이 제한된 계정입니다",
  NETWORK_ERROR: "서버에 연결할 수 없어요",
  400: "입력 내용을 확인해주세요",
  409: "이미 처리된 정보입니다",
  413: "파일 용량을 확인해주세요",
};

// 모든 API 오류를 한곳에서 받아 중복 구현 없이 명세에 맞는 팝업 또는 로그인 이동을 수행합니다.
function handleApiError(event) {
  const detail = event.detail || {};
  if (detail.status === 401) {
    const sessionExpired = Boolean(accessToken.value);
    clearAuthSession();
    router.push({ path: "/login", query: { redirect: route.fullPath } });
    if (sessionExpired) {
      errorContent.title = "로그인이 만료되었습니다";
      errorContent.message = "안전한 이용을 위해 다시 로그인해주세요.";
      errorContent.details = [];
      errorOpen.value = true;
    }
    return;
  }
  if ([403, 404, 500].includes(detail.status)) {
    router.push(`/error/${detail.status}`);
    return;
  }

  errorContent.title =
    errorTitles[detail.code] ||
    errorTitles[detail.status] ||
    "요청을 처리하지 못했어요";
  errorContent.message = detail.message || "잠시 후 다시 시도해주세요.";
  errorContent.details = Array.isArray(detail.fieldErrors)
    ? detail.fieldErrors
    : [];
  errorOpen.value = true;
}

onMounted(() => window.addEventListener("powcast-api-error", handleApiError));
onBeforeUnmount(() =>
  window.removeEventListener("powcast-api-error", handleApiError),
);

async function checkPetRegistration(token) {
  // 관리자는 쇼핑·반려동물 추천 대상이 아니므로 PETS API를 호출하지 않습니다.
  const isOperator = ["ADMIN", "MANAGER"].includes(sessionStorage.getItem("powcast-user-role"));
  if (!token || !currentUserId.value || token === checkedPetToken || route.meta.requiresManager || route.meta.requiresAdmin || isOperator) {
    if (isOperator) petPromptOpen.value = false;
    return;
  }
  checkedPetToken = token;
  try {
    const pets = await petService.getPets();
    petPromptOpen.value = Array.isArray(pets) && pets.length === 0 && !isPetPromptDeferred();
  } catch {
    // 공통 API 오류 처리기가 서버·인증 오류를 안내합니다.
  }
}

watch([accessToken, currentUserId], ([token], previous = []) => {
  const previousUserId = previous[1];
  if (currentUserId.value !== previousUserId) checkedPetToken = "";
  checkPetRegistration(token);
}, { immediate: true });

function goToPetRegistration() {
  router.push("/mypage/pets");
}

function deferPetRegistration(days) {
  const safeDays = [7, 30].includes(days) ? days : 7;
  localStorage.setItem(
    petPromptStorageKey(),
    String(Date.now() + safeDays * 24 * 60 * 60 * 1000),
  );
}
</script>
<template>
  <div class="app-shell" :class="{ 'admin-app-shell': usesAdminLayout }">
    <AppHeader v-if="!usesAdminLayout" />
    <main><RouterView /></main>
    <AppFooter v-if="!usesAdminLayout" />
    <GlobalErrorModal
      v-model="errorOpen"
      :title="errorContent.title"
      :message="errorContent.message"
      :details="errorContent.details"
    />
    <PetRegistrationPrompt
      v-model="petPromptOpen"
      @register="goToPetRegistration"
      @defer="deferPetRegistration"
    />
  </div>
</template>
