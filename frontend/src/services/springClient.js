// Spring API 요청, 인증 갱신, 제한된 재시도와 공통 오류 알림을 한곳에서 관리합니다.
const BASE_URL = (import.meta.env.VITE_SPRING_BASE_URL || "").replace(/\/$/, "");
const ACCESS_TOKEN_KEY = "powcast-access-token";
const REFRESH_TOKEN_KEY = "powcast-refresh-token";

let refreshInFlight = null;

// 전역 오류 안내(팝업 또는 에러 페이지 이동) 이벤트를 발행합니다.
function notifyApiError(detail) {
    window.dispatchEvent(new CustomEvent("powcast-api-error", { detail }));
}

// 백엔드 오류 응답을 표준 Error 객체로 변환합니다.
function readError(response, data) {
    const error = new Error(
        data.message || data.detail || "요청을 처리하지 못했습니다."
    );
    error.status = response.status;
    error.code = data.code || "UNKNOWN_ERROR";
    error.fieldErrors = Array.isArray(data.fieldErrors) ? data.fieldErrors : [];
    return error;
}

/**
 * 만료된 Access Token을 하나의 공유 요청으로 갱신합니다.
 * 여러 API가 동시에 401을 받아도 Refresh Token 회전 요청은 한 번만 실행하고,
 * 결과 토큰을 저장소와 Pinia 세션에 함께 반영해 다음 요청이 새 값을 사용하게 합니다.
 */
export async function refreshAccessToken() {
    if (refreshInFlight) {
        return refreshInFlight;
    }

    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) {
        return {
            accessToken: null,
            status: 401,
            code: "REFRESH_TOKEN_MISSING",
            message: "저장된 Refresh Token이 없습니다.",
        };
    }

    refreshInFlight = (async () => {
        const query = new URLSearchParams({ refreshTokenValue: refreshToken });
        let response;

        try {
            response = await fetch(`${BASE_URL}/api/auth/token/refresh?${query}`, {
                method: "POST",
                credentials: "include",
                headers: { Accept: "application/json" },
            });
        } catch (cause) {
            return {
                accessToken: null,
                status: 0,
                code: "REFRESH_NETWORK_ERROR",
                message: "토큰 갱신 서버에 연결할 수 없습니다.",
                cause,
            };
        }

        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            return {
                accessToken: null,
                status: response.status,
                code: data.code || "REFRESH_FAILED",
                message: data.message || "토큰을 갱신하지 못했습니다.",
            };
        }

        if (!data.accessToken || !data.refreshToken) {
            return {
                accessToken: null,
                status: 500,
                code: "REFRESH_INVALID_RESPONSE",
                message: "토큰 갱신 응답에 새 토큰이 없습니다.",
            };
        }

        localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
        sessionStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);

        if (data.role) {
            sessionStorage.setItem("powcast-user-role", data.role);
        }

        window.dispatchEvent(
            new CustomEvent("powcast-token-refreshed", { detail: data })
        );

        return {
            accessToken: data.accessToken,
            status: 200,
            code: "REFRESHED",
            message: "",
        };
    })().finally(() => {
        refreshInFlight = null;
    });

    return refreshInFlight;
}

// 공통 HTTP 요청을 수행하고 401 토큰 재발급 및 전역 오류 처리를 담당합니다.
async function sendSpringRequest(path, options, retryAfterRefresh) {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    const isFormData = options.body instanceof FormData;
    const headers = new Headers(options.headers || {});

    if (options.body && !isFormData && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    // 로그인 및 인증 엔드포인트 요청 시에는 기존 잔여 만료 토큰을 실어 보내지 않습니다.
    if (token && !path.startsWith("/auth/")) {
        headers.set("Authorization", `Bearer ${token}`);
    } else {
        headers.delete("Authorization");
    }

    let response;
    try {
        response = await fetch(`${BASE_URL}/api${path}`, {
            ...options,
            headers,
            credentials: "include",
        });
    } catch (cause) {
        const error = new Error(
            "서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.",
            { cause }
        );
        error.status = 0;
        error.code = "NETWORK_ERROR";
        notifyApiError({ status: 0, code: error.code, message: error.message });
        throw error;
    }

    // 일반 보호 API에서 401이 발생하면 Refresh Token으로 1회 갱신 후 재시도합니다.
    if (
        response.status === 401 &&
        retryAfterRefresh &&
        path !== "/auth/login" &&
        !path.startsWith("/auth/")
    ) {
        // 다른 요청이 먼저 갱신을 끝냈다면 저장된 최신 Access Token으로 바로 재시도합니다.
        const currentToken = localStorage.getItem(ACCESS_TOKEN_KEY);
        const refreshResult =
            currentToken && currentToken !== token
                ? { accessToken: currentToken, status: 200 }
                : await refreshAccessToken();

        if (refreshResult.accessToken) {
            // 재시도는 한 번만 허용합니다. 갱신 직후에도 거부되면 무한 루프 없이 오류로 끝납니다.
            return sendSpringRequest(path, options, false);
        }

        if (refreshResult && ![400, 401].includes(refreshResult.status)) {
            // 갱신 서버 장애를 로그인 만료와 혼동해 세션을 지우지 않고 실제 장애를 안내합니다.
            const status =
                refreshResult.status === 0
                    ? 0
                    : refreshResult.status >= 500
                    ? 500
                    : refreshResult.status;

            notifyApiError({
                status,
                code: refreshResult.code,
                message: refreshResult.message,
            });

            options = {
                ...options,
                suppressGlobalErrorStatuses: [
                    ...(options.suppressGlobalErrorStatuses || []),
                    401,
                ],
            };
        }
    }

    if (response.status === 204) {
        return null;
    }

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        const error = readError(response, data);

        // 호출 측에서 전역 오류 처리를 억제한 상태 코드는 그대로 호출부로 전달합니다.
        if (options.suppressGlobalErrorStatuses?.includes(response.status)) {
            throw error;
        }

        // 로그인 요청에서 발생한 401(비밀번호 불일치)과 403(탈퇴·비활성 계정 등)은
        // /error/403 전체 페이지로 이동시키지 않고 LoginView.vue 내부 errorMessage로 표시합니다.
        // 단, 관리자 차단·강퇴(MEMBER_BLOCKED, MEMBER_BANNED)는 전역 안내를 유지합니다.
        const isBannedOrBlocked = ["MEMBER_BLOCKED", "MEMBER_BANNED"].includes(
            error.code
        );
        const isLoginInlineFailure =
            path === "/auth/login" &&
            (response.status === 401 ||
                (response.status === 403 && !isBannedOrBlocked));

        const shouldShowPopup = [400, 409, 413].includes(response.status);
        if (shouldShowPopup) {
            notifyApiError({
                status: error.status,
                code: error.code,
                message: error.message,
                fieldErrors: error.fieldErrors,
            });
        } else if (response.status === 401 && !isLoginInlineFailure) {
            notifyApiError({
                status: 401,
                code: error.code,
                message: error.message,
            });
        } else if (
            [403, 404, 500].includes(response.status) &&
            !isLoginInlineFailure
        ) {
            notifyApiError({
                status: error.status,
                code: error.code,
                message: error.message,
            });
        }

        throw error;
    }

    return data;
}

export function springRequest(path, options = {}) {
    return sendSpringRequest(path, options, true);
}

export const jsonBody = (body) => JSON.stringify(body);