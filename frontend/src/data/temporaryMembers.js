// 백엔드 연결 전에도 가입·로그인·수정 흐름을 시험할 수 있는 localStorage 기반 임시 회원 저장소입니다.
// Frontend-only member storage for UI testing.
// Remove this module when the real member API is connected.
const STORAGE_KEY = "pawcast-temporary-members";
const DEMO_MEMBER = {
  userId: "test01",
  password: "password1!",
  username: "이수미",
  email: "test01@pawcast.com",
  tel: "01012345678",
  agreements: { terms: true, privacy: true, efinance: true, marketing: false },
  createdAt: "2026-11-15T00:00:00.000Z",
  temporary: true,
  role: "USER",
};
const DEMO_ADMIN = {
  userId: "admin",
  password: "password1!",
  username: "PAWCAST 관리자",
  email: "admin@pawcast.com",
  tel: "01000000000",
  agreements: { terms: true, privacy: true, efinance: true, marketing: false },
  createdAt: "2026-11-15T00:00:00.000Z",
  temporary: true,
  role: "ADMIN",
};

function readMembers() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
  } catch {
    return [];
  }
}

function writeMembers(members) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(members));
}

// 개발 서버에서는 일반 회원과 관리자 UI를 즉시 검수할 수 있도록 공통 계정을 자동 보충합니다.
const initialMembers = readMembers();
const demoAccounts = import.meta.env.DEV
  ? [DEMO_MEMBER, DEMO_ADMIN]
  : [DEMO_MEMBER];
const normalizedMembers = [...initialMembers];
demoAccounts.forEach((account) => {
  const index = normalizedMembers.findIndex(
    (member) => member.userId === account.userId,
  );
  if (index === -1) {
    normalizedMembers.push(account);
    return;
  }
  // 관리자 데모 계정은 이전 localStorage가 남아 있어도 지정된 권한과 비밀번호로 항상 검수할 수 있게 갱신합니다.
  if (account.role === "ADMIN") {
    normalizedMembers[index] = { ...normalizedMembers[index], ...account };
  }
});
if (JSON.stringify(normalizedMembers) !== JSON.stringify(initialMembers)) {
  writeMembers(normalizedMembers);
}

export function temporaryIdAvailable(userId) {
  return !readMembers().some((member) => member.userId === userId);
}

export function registerTemporaryMember(payload) {
  const members = readMembers();
  if (members.some((member) => member.userId === payload.userId))
    throw new Error("이미 가입된 아이디가 있습니다.");
  members.push({
    userId: payload.userId,
    password: payload.passwordHash,
    username: payload.username,
    email: payload.email,
    tel: payload.tel,
    agreements: {
      terms: payload.isAgreedTerms,
      privacy: payload.isAgreedPrivacy,
      efinance: payload.isAgreedEfinance,
      marketing: payload.isAgreedMarketing,
    },
    createdAt: new Date().toISOString(),
    temporary: true,
    role: "USER",
  });
  writeMembers(members);
}

export function loginTemporaryMember(userId, password) {
  const member = readMembers().find(
    (item) => item.userId === userId && item.password === password,
  );
  return member
    ? {
        accessToken:
          member.role === "ADMIN"
            ? `temporary-admin:${member.userId}`
            : `temporary-member:${member.userId}`,
        member,
      }
    : null;
}

export function getTemporaryMember(userId) {
  return readMembers().find((item) => item.userId === userId) || null;
}

export function updateTemporaryMember(userId, changes) {
  const members = readMembers();
  const index = members.findIndex((item) => item.userId === userId);
  if (index < 0) return null;
  members[index] = {
    ...members[index],
    ...changes,
    updatedAt: new Date().toISOString(),
  };
  writeMembers(members);
  return members[index];
}

export function removeTemporaryMember(userId) {
  const members = readMembers().filter((item) => item.userId !== userId);
  writeMembers(members);
}
// 백엔드 연결 전에도 가입·로그인·수정 흐름을 시험할 수 있는 localStorage 기반 임시 회원 저장소입니다.
