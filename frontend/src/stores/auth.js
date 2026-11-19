// 인증 상태는 Pinia가 소유하고, 기존 컴포넌트에는 ref 형태로 노출합니다.
import { reactive, ref } from "vue";
import { defineStore, storeToRefs } from "pinia";

const useAuthStore = defineStore("auth", () => {
  const accessToken = ref(localStorage.getItem("pawcast-access-token") || "");
  const currentUserId = ref(
    localStorage.getItem("pawcast-current-user-id") || "",
  );
  const pendingOAuth = reactive({
    ticket: "",
    email: "",
    name: "",
    provider: "",
  });

  function setAccessToken(token) {
    accessToken.value = token || "";
    if (accessToken.value) {
      localStorage.setItem("pawcast-access-token", accessToken.value);
      return;
    }
    localStorage.removeItem("pawcast-access-token");
    localStorage.removeItem("pawcast-current-user-id");
    currentUserId.value = "";
  }

  function setCurrentUserId(userId) {
    currentUserId.value = userId || "";
    if (currentUserId.value)
      localStorage.setItem("pawcast-current-user-id", currentUserId.value);
    else localStorage.removeItem("pawcast-current-user-id");
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
    setPendingOAuth: store.setPendingOAuth,
    clearPendingOAuth: store.clearPendingOAuth,
  };
}
