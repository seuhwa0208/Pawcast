// 브라우저, Spring 응답, JWT에서 올 수 있는 ROLE_ 접두사 차이를 한곳에서 정규화합니다.
export function normalizeRole(value) {
  const role = String(value || "").trim().toUpperCase();
  return role.startsWith("ROLE_") ? role.slice("ROLE_".length) : role;
}

export function readUserRole() {
  return normalizeRole(sessionStorage.getItem("powcast-user-role"));
}

export function isAdminRole(value) {
  return normalizeRole(value) === "ADMIN";
}

export function isManagerRole(value) {
  return normalizeRole(value) === "MANAGER";
}

export function canManageAdminArea(value) {
  return isAdminRole(value) || isManagerRole(value);
}

export function getRoleLandingPath(value) {
  return canManageAdminArea(value) ? "/admin/sales" : "/member";
}


/**
 * 로그인 후 돌아갈 경로를 앱 내부의 단일 슬래시 경로로 정규화합니다.
 * `//host/path`는 브라우저에서 외부 프로토콜 상대 URL로 해석될 수 있으므로,
 * 내부 경로의 앞쪽 중복 슬래시만 하나로 합치고 외부 URL이나 잘못된 값은 거부합니다.
 */
export function getPostLoginPath(redirect, role) {
  const fallback = getRoleLandingPath(role);
  if (typeof redirect !== "string") {
    return fallback;
  }

  const candidate = redirect.trim();
  if (!candidate.startsWith("/") || candidate.includes("\\")) {
    return fallback;
  }

  // query와 hash 값은 유지하면서 경로 시작 부분의 `//` 또는 `///`만 `/`로 줄입니다.
  const normalized = candidate.replace(/^\/+/, "/");
  return normalized || fallback;
}
