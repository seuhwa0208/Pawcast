// Pinia가 인증 상태를 소유하고, 기존 컴포넌트에는 반응형 세션 값을 노출합니다.
import { reactive, ref } from "vue";
import { defineStore, storeToRefs } from "pinia";
import { normalizeRole } from "@/utils/roles";

function hasExpiredJwt(token) {
  if (!token) return true;
  try {
    const payload = token.split(".")[1];
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(normalized));
    return !exp || exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

const savedAccessToken = localStorage.getItem("powcast-access-token") || "";
if (savedAccessToken && hasExpiredJwt(savedAccessToken)) {
  // Access Token 만료는 정상 갱신 상황일 수 있으므로 Refresh Token은 보존합니다.
  localStorage.removeItem("powcast-access-token");
}

const useAuthStore = defineStore("auth", () => {
  const accessToken = ref(
    hasExpiredJwt(savedAccessToken) ? "" : savedAccessToken,
  );
  const refreshToken = ref(sessionStorage.getItem("powcast-refresh-token") || "");
  const currentUserId = ref(localStorage.getItem("powcast-current-user-id") || "");
  const pendingOAuth = reactive({ ticket: "", email: "", name: "", provider: "" });

  function setAccessToken(token) {
    const nextToken = token || "";
    const isNewLogin = Boolean(nextToken) && nextToken !== accessToken.value;
    accessToken.value = nextToken;
    if (accessToken.value) {
      localStorage.setItem("powcast-access-token", accessToken.value);
      if (isNewLogin) {
        sessionStorage.setItem("powcast-login-at", new Date().toISOString());
      }
      return;
    }
    localStorage.removeItem("powcast-access-token");
    localStorage.removeItem("powcast-current-user-id");
    currentUserId.value = "";
  }

  function setRefreshToken(token) {
    refreshToken.value = token || "";
    if (refreshToken.value) sessionStorage.setItem("powcast-refresh-token", refreshToken.value);
    else sessionStorage.removeItem("powcast-refresh-token");
  }

  function setUserRole(role) {
    const normalizedRole = normalizeRole(role);
    if (normalizedRole) {
      sessionStorage.setItem("powcast-user-role", normalizedRole);
    } else {
      sessionStorage.removeItem("powcast-user-role");
    }
  }

  function setCurrentUserId(userId) {
    currentUserId.value = userId || "";
    if (currentUserId.value) localStorage.setItem("powcast-current-user-id", currentUserId.value);
    else localStorage.removeItem("powcast-current-user-id");
  }

  function clearPendingOAuth() {
    Object.assign(pendingOAuth, { ticket: "", email: "", name: "", provider: "" });
  }

  function clearAuthSession() {
    setRefreshToken("");
    setAccessToken("");
    setUserRole("");
    sessionStorage.removeItem("powcast-profile-verified");
    sessionStorage.removeItem("powcast-login-at");
    clearPendingOAuth();
  }

  // HTTP 모듈이 갱신한 토큰을 반응형 상태에도 반영해 화면과 요청이 같은 세션을 보게 합니다.
  window.addEventListener("powcast-token-refreshed", (event) => {
    setAccessToken(event.detail?.accessToken);
    setRefreshToken(event.detail?.refreshToken);
    setUserRole(event.detail?.role);
  });

  return {
    accessToken,
    refreshToken,
    currentUserId,
    pendingOAuth,
    setAccessToken,
    setRefreshToken,
    setCurrentUserId,
    setUserRole,
    setPendingOAuth: (data) => Object.assign(pendingOAuth, data),
    clearPendingOAuth,
    clearAuthSession,
  };
});

export function useAuthSession() {
  const store = useAuthStore();
  const { accessToken, refreshToken, currentUserId } = storeToRefs(store);
  return {
    accessToken,
    refreshToken,
    currentUserId,
    pendingOAuth: store.pendingOAuth,
    setAccessToken: store.setAccessToken,
    setRefreshToken: store.setRefreshToken,
    setCurrentUserId: store.setCurrentUserId,
    setUserRole: store.setUserRole,
    setPendingOAuth: store.setPendingOAuth,
    clearPendingOAuth: store.clearPendingOAuth,
    clearAuthSession: store.clearAuthSession,
  };
}


