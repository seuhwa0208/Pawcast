// 로그인 회원의 비밀번호 변경과 회원 탈퇴 요청을 담당합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const changeMemberPassword = (payload) =>
  springRequest("/members/me/password", {
    method: "PATCH",
    body: jsonBody(payload),
  });

export const withdrawMember = (currentPassword) =>
  springRequest("/members/me", {
    method: "DELETE",
    body: jsonBody({ currentPassword }),
  });

export const getMyProfile = () => springRequest("/members/me");

export const updateMyProfile = (payload) =>
  springRequest("/members/me", {
    method: "PATCH",
    body: jsonBody(payload),
  });
