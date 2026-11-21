// Spring Boot API의 주소, 인증 헤더, JSON 파싱과 공통 오류 처리를 중앙화합니다.
const BASE_URL = (import.meta.env.VITE_SPRING_BASE_URL || "").replace(
  /\/$/,
  "",
);

// UI 계층과 HTTP 모듈의 직접 의존을 피하면서 App.vue가 공통 오류를 한 번만 안내하도록 이벤트를 전달합니다.
function notifyApiError(detail) {
  window.dispatchEvent(new CustomEvent("powcast-api-error", { detail }));
}

export async function springRequest(path, options = {}) {
  const token = localStorage.getItem("powcast-access-token");
  let response;
  try {
    response = await fetch(`${BASE_URL}/api${path}`, {
      credentials: "include",
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch (cause) {
    notifyApiError({
      status: 0,
      code: "NETWORK_ERROR",
      message: "서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.",
    });
    const error = new Error("서버에 연결할 수 없습니다.", { cause });
    error.status = 0;
    error.code = "NETWORK_ERROR";
    throw error;
  }

  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(data.message || "요청을 처리하지 못했습니다.");
    error.status = response.status;
    error.code = data.code || "UNKNOWN_ERROR";
    error.fieldErrors = Array.isArray(data.fieldErrors) ? data.fieldErrors : [];

    const isLoginFailure = path === "/auth/login" && response.status === 401;
    const isBlockedLogin =
      path === "/auth/login" &&
      ["MEMBER_BLOCKED", "MEMBER_BANNED"].includes(error.code);
    const usesPopup =
      [400, 409, 413].includes(response.status) || isBlockedLogin;
    if (usesPopup) {
      notifyApiError({
        status: response.status,
        code: error.code,
        message: error.message,
        fieldErrors: error.fieldErrors,
      });
    } else if (response.status === 401 && !isLoginFailure) {
      notifyApiError({ status: 401, code: error.code, message: error.message });
    } else if (
      [403, 404, 500].includes(response.status) &&
      ![
        "/orders/admin",
        "/admin",
        "/qna/manage",
        "/notices",
        "/qna",
        "/products",
      ].some((pagePath) => path.startsWith(pagePath))
    ) {
      notifyApiError({
        status: response.status,
        code: error.code,
        message: error.message,
      });
    }
    throw error;
  }
  return data;
}

export const jsonBody = (body) => JSON.stringify(body);
