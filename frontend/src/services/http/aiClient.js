// FastAPI 직접 호출을 위한 인증, 단일 갱신 재시도, 제한 시간과 공통 오류 처리를 제공합니다.
import { refreshAccessToken } from "@/services/http/springClient";

const AI_BASE_URL = (import.meta.env.VITE_AI_BASE_URL || "/ai").replace(/\/$/, "");
const AI_TIMEOUT_MS = 60000;

function notifyApiError(detail) {
  window.dispatchEvent(new CustomEvent("powcast-api-error", { detail }));
}

export async function aiRequest(path, options = {}) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), AI_TIMEOUT_MS);
  const externalSignal = options.signal;
  const abortFromCaller = () => controller.abort(externalSignal.reason);
  externalSignal?.addEventListener("abort", abortFromCaller, { once: true });

  try {
    let retried = false;
    while (true) {
      const token = localStorage.getItem("powcast-access-token") || "";
      const headers = new Headers(options.headers || {});
      if (token) headers.set("Authorization", `Bearer ${token}`);
      else headers.delete("Authorization");

      let response;
      try {
        response = await fetch(`${AI_BASE_URL}${path}`, {
          ...options,
          headers,
          signal: controller.signal,
        });
      } catch (cause) {
        const timedOut = controller.signal.aborted && !externalSignal?.aborted;
        const error = new Error(
          timedOut ? "AI 서버 응답 시간이 초과되었습니다." : "AI 서버에 연결할 수 없습니다.",
          { cause },
        );
        error.status = 0;
        error.code = timedOut ? "AI_TIMEOUT" : "AI_NETWORK_ERROR";
        notifyApiError({ status: 0, code: error.code, message: error.message });
        throw error;
      }

      let refreshInfrastructureFailure = false;
      if (response.status === 401 && !retried && !path.includes("/auth/")) {
        // 같은 탭의 다른 요청이 먼저 토큰을 갱신했으면 회전된 Refresh Token을 다시 쓰지 않습니다.
        const currentToken = localStorage.getItem("powcast-access-token") || "";
        const refreshResult = currentToken && currentToken !== token
          ? { accessToken: currentToken, status: 200 }
          : await refreshAccessToken();
        if (refreshResult.accessToken) {
          // FastAPI 호출도 동일한 한 번의 제한된 재시도를 사용해 만료 루프를 막습니다.
          retried = true;
          continue;
        }

        if (![400, 401].includes(refreshResult.status)) {
          // Spring refresh의 5xx/네트워크 장애를 세션 만료 모달로 오인하지 않게 분리합니다.
          const status = refreshResult.status === 0
            ? 0
            : refreshResult.status >= 500
              ? 500
              : refreshResult.status;
          notifyApiError({
            status,
            code: refreshResult.code,
            message: refreshResult.message,
          });
          // 아래 FastAPI 401은 이미 안내한 인프라 오류를 중복으로 띄우지 않도록 표시합니다.
          refreshInfrastructureFailure = true;
        }
      }

      const contentType = response.headers.get("content-type") || "";
      const body = contentType.includes("application/json")
        ? await response.json().catch(() => ({}))
        : await response.blob();
      if (!response.ok) {
        const detail = body?.detail;
        const message = typeof detail === "string" ? detail : "AI 서버 요청에 실패했습니다.";
        const error = new Error(message);
        error.status = response.status;
        error.code = body?.code || "AI_REQUEST_ERROR";
        if (
          response.status === 401 &&
          !refreshInfrastructureFailure
        ) {
          notifyApiError({ status: 401, code: error.code, message });
        } else if ([403, 404, 500].includes(response.status)) {
          notifyApiError({ status: response.status, code: error.code, message });
        }
        throw error;
      }
      return { body, response };
    }
  } finally {
    window.clearTimeout(timeoutId);
    externalSignal?.removeEventListener("abort", abortFromCaller);
  }
}

export { AI_BASE_URL };

