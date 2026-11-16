// 현재 로그인 회원에게 귀속된 반려동물의 조회·등록·수정·삭제 API를 제공합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const petService = {
  getPets: () => springRequest("/members/me/pets"),
  addPet: (payload) =>
    springRequest("/members/me/pets", { method: "POST", body: jsonBody(payload) }),
  updatePet: (petId, payload) =>
    springRequest(`/members/me/pets/${petId}`, {
      method: "PATCH",
      body: jsonBody(payload),
    }),
  removePet: (petId) =>
    springRequest(`/members/me/pets/${petId}`, { method: "DELETE" }),
};
