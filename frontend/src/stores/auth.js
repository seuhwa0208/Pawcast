// 인증 상태는 Pinia가 소유하고, 기존 컴포넌트에는 ref 형태로 노출합니다.
import { reactive, ref } from "vue";
import { defineStore, storeToRefs } from "pinia";

const useAuthStore = defineStore("auth", () => {
  const accessToken = ref(localStorage.getItem("powcast-access-token") || "");
  const currentUserId = ref(
    localStorage.getItem("powcast-current-user-id") || "",
  );
  const pendingOAuth = reactive({
    ticket: "",
    email: "",
    name: "",
    provider: "",
  });

  function setAccessToken(token) {
    const isNewLogin = Boolean(token) && token !== accessToken.value;
    accessToken.value = token || "";
    if (accessToken.value) {
      localStorage.setItem("powcast-access-token", accessToken.value);
      if (isNewLogin)
        sessionStorage.setItem("powcast-login-at", new Date().toISOString());
      return;
    }
    localStorage.removeItem("powcast-access-token");
    localStorage.removeItem("powcast-current-user-id");
    currentUserId.value = "";
  }

  function setUserRole(role) {
    if (role) sessionStorage.setItem("powcast-user-role", role);
    else sessionStorage.removeItem("powcast-user-role");
  }

  function setCurrentUserId(userId) {
    currentUserId.value = userId || "";
    if (currentUserId.value)
      localStorage.setItem("powcast-current-user-id", currentUserId.value);
    else localStorage.removeItem("powcast-current-user-id");
  }

  const setPendingOAuth = (data) => Object.assign(pendingOAuth, data);
  const clearPendingOAuth = () =>
    Object.assign(pendingOAuth, {
      ticket: "",
      email: "",
      name: "",
      provider: "",
    });

  return {
    accessToken,
    currentUserId,
    pendingOAuth,
    setAccessToken,
    setCurrentUserId,
    setUserRole,
    setPendingOAuth,
    clearPendingOAuth,
  };
});

export function useAuthSession() {
  const store = useAuthStore();
  const { accessToken, currentUserId } = storeToRefs(store);
  return {
    accessToken,
    currentUserId,
    pendingOAuth: store.pendingOAuth,
    setAccessToken: store.setAccessToken,
    setCurrentUserId: store.setCurrentUserId,
    setUserRole: store.setUserRole,
    setPendingOAuth: store.setPendingOAuth,
    clearPendingOAuth: store.clearPendingOAuth,
  };
}
