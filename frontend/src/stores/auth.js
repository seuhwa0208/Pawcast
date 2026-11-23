
 // 인증 상태는 Pinia가 소유하고, 기존 컴포넌트에는 ref 형태로 노출합니다.
import { reactive, ref } from "vue";
import { defineStore, storeToRefs } from "pinia";

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
    localStorage.removeItem("powcast-access-token");
    sessionStorage.removeItem("powcast-refresh-token");
    sessionStorage.removeItem("powcast-user-role");
}

const useAuthStore = defineStore("auth", () => {
    const accessToken = ref(
        hasExpiredJwt(savedAccessToken) ? "" : savedAccessToken
    );

    const refreshToken = ref(
        sessionStorage.getItem("powcast-refresh-token") || ""
    );

    const currentUserId = ref(
        localStorage.getItem("powcast-current-user-id") || ""
    );

    const pendingOAuth = reactive({
        ticket: "",
        email: "",
        name: "",
        provider: "",
    });

    function setAccessToken(token) {
        const isNewLogin =
            Boolean(token) && token !== accessToken.value;

        accessToken.value = token || "";

        if (accessToken.value) {
            localStorage.setItem(
                "powcast-access-token",
                accessToken.value
            );

            if (isNewLogin) {
                sessionStorage.setItem(
                    "powcast-login-at",
                    new Date().toISOString()
                );
            }

            return;
        }

        localStorage.removeItem("powcast-access-token");
        localStorage.removeItem("powcast-current-user-id");

        currentUserId.value = "";
    }

    // Refresh Token 저장 및 삭제
    function setRefreshToken(token) {
        refreshToken.value = token || "";

        if (refreshToken.value) {
            sessionStorage.setItem(
                "powcast-refresh-token",
                refreshToken.value
            );

            return;
        }

        sessionStorage.removeItem("powcast-refresh-token");
    }

    function setUserRole(role) {
        if (role) {
            sessionStorage.setItem("powcast-user-role", role);
        } else {
            sessionStorage.removeItem("powcast-user-role");
        }
    }

    function setCurrentUserId(userId) {
        currentUserId.value = userId || "";

        if (currentUserId.value) {
            localStorage.setItem(
                "powcast-current-user-id",
                currentUserId.value
            );
        } else {
            localStorage.removeItem("powcast-current-user-id");
        }
    }

    // 로그아웃 시 인증 정보 초기화
    function clearAuthSession() {
        setRefreshToken("");
        setAccessToken("");
        setUserRole("");

        sessionStorage.removeItem("powcast-profile-verified");
        sessionStorage.removeItem("powcast-login-at");

        clearPendingOAuth();
    }

    const setPendingOAuth = (data) =>
        Object.assign(pendingOAuth, data);

    const clearPendingOAuth = () =>
        Object.assign(pendingOAuth, {
            ticket: "",
            email: "",
            name: "",
            provider: "",
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
        setPendingOAuth,
        clearPendingOAuth,
        clearAuthSession,
    };
});

export function useAuthSession() {
    const store = useAuthStore();

    const {
        accessToken,
        refreshToken,
        currentUserId,
    } = storeToRefs(store);

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
