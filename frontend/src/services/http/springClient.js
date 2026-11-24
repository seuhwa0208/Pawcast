
// Spring Boot API의 주소, 인증 헤더, JSON 파싱과 공통 오류 처리를 중앙화합니다.
const BASE_URL = (import.meta.env.VITE_SPRING_BASE_URL || "").replace(
  /\/$/,
  "",
);

// UI 계층과 HTTP 모듈의 직접 의존을 피하면서
// App.vue가 공통 오류를 한 번만 안내하도록 이벤트를 전달합니다.
function notifyApiError(detail) {
  window.dispatchEvent(
    new CustomEvent("powcast-api-error", {
      detail,
    }),
  );
}

export async function springRequest(path, options = {}) {
  const token = localStorage.getItem("powcast-access-token");
  const isMemberOnlyRequest =
    path.startsWith("/members/me") ||
    path === "/orders" ||
    /^\/orders\/\d+(?:\/|$)/.test(path) ||
    path.startsWith("/cart") ||
    path.startsWith("/pets");

  // FormData 요청은 브라우저가 Content-Type과 boundary를 자동으로 설정합니다.
  const isFormData = options.body instanceof FormData;

  let response;

  try {
    response = await fetch(`${BASE_URL}/api${path}`, {
      credentials: "include",
      ...options,
      headers: {
        // 일반 JSON 요청에만 application/json을 지정합니다.
        ...(options.body && !isFormData
          ? {
              "Content-Type": "application/json",
            }
          : {}),

        // 기존 로그인 인증 토큰을 유지합니다.
        ...(token
          ? {
              Authorization: `Bearer ${token}`
            }
          : {}),

        ...options.headers,
      },
    });
  } catch (cause) {
    notifyApiError({
      status: 0,
      code: "NETWORK_ERROR",
      message: "서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.",
    });

    const error = new Error("서버에 연결할 수 없습니다.", {
      cause,
    });

    error.status = 0;
    error.code = "NETWORK_ERROR";

    throw error;
  }

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    const error = new Error(
      data.message || "요청을 처리하지 못했습니다.",
    );

    error.status = response.status;
    error.code = data.code || "UNKNOWN_ERROR";

    error.fieldErrors = Array.isArray(data.fieldErrors)
      ? data.fieldErrors
      : [];

    // 화면에서 빈 상태로 정상 처리하는 응답(예: 환불 내역 없음 404)은
    // 호출부가 직접 처리할 수 있도록 전역 오류 화면 이동을 생략한다.
    if (options.suppressGlobalErrorStatuses?.includes(response.status)) {
      throw error;
    }

    const isLoginFailure =
      path === "/auth/login" &&
      response.status === 401;

    const isBlockedLogin =
      path === "/auth/login" &&
      ["MEMBER_BLOCKED", "MEMBER_BANNED"].includes(error.code);

    const usesPopup =
      [400, 409, 413].includes(response.status) ||
      isBlockedLogin;

    if (usesPopup) {
      notifyApiError({
        status: response.status,
        code: error.code,
        message: error.message,
        fieldErrors: error.fieldErrors,
      });
    } else if (
      response.status === 401 &&
      !isLoginFailure
    ) {
      notifyApiError({
        status: 401,
        code: error.code,
        message: error.message,
      });
    } else if (response.status === 403 && token && isMemberOnlyRequest) {
      // Spring Security는 유효하지 않은 JWT가 없는 인증으로 처리되어
      // 401 대신 403을 반환할 수 있다. 회원 전용 API의 이 경우에는
      // 권한 화면이 아니라 로그인 만료 흐름으로 복귀한다.
      notifyApiError({
        status: 401,
        code: "TOKEN_INVALID_OR_EXPIRED",
        message: "로그인 정보가 만료되었거나 유효하지 않습니다.",
      });
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
