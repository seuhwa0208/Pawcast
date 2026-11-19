// 관리자 처리 전 비밀번호 초기화 요청을 브라우저에 임시 보관하는 데모 저장소입니다.
// Frontend-only password reset request queue for UI testing.
// Replace this module with the member/admin password reset APIs later.
const STORAGE_KEY = "pawcast-password-reset-requests";

function readRequests() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
  } catch {
    return [];
  }
}
function writeRequests(items) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
}

export function createTemporaryPasswordResetRequest({ email, userId = "" }) {
  const requests = readRequests();
  const duplicated = requests.find(
    (item) =>
      item.status === "PENDING" &&
      ((userId && item.userId === userId) || item.email === email),
  );
  if (duplicated) return duplicated;
  const request = {
    id: `PWR-${Date.now()}`,
    email,
    userId,
    status: "PENDING",
    requestedAt: new Date().toISOString(),
    processedAt: null,
    delivery: "EMAIL",
  };
  requests.unshift(request);
  writeRequests(requests);
  return request;
}

export function getTemporaryPasswordResetRequests() {
  return readRequests();
}
// 관리자 처리 전 비밀번호 초기화 요청을 브라우저에 임시 보관하는 데모 저장소입니다.
