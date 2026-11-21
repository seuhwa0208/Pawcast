// Vue가 Spring을 경유하지 않고 FastAPI의 조회·분석 API를 직접 호출합니다.
const AI_BASE_URL = (import.meta.env.VITE_AI_BASE_URL || "/ai").replace(/\/$/, "");

export async function aiRequest(path, options = {}) {
  const token = localStorage.getItem("powcast-access-token") || "";
  const headers = new Headers(options.headers || {});
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const response = await fetch(`${AI_BASE_URL}${path}`, { ...options, headers });
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json().catch(() => ({}))
    : await response.blob();
  if (!response.ok) {
    const detail = body?.detail;
    throw new Error(typeof detail === "string" ? detail : "AI 서버 요청에 실패했습니다.");
  }
  return { body, response };
}

export { AI_BASE_URL };
