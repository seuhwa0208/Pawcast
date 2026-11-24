// 로그인 회원의 배송지 조회·등록·수정 API를 제공합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const addressService = {
  getAddresses: () => springRequest("/members/me/addresses"),
  addAddress: (payload) =>
    springRequest("/members/me/addresses", {
      method: "POST",
      body: jsonBody(payload),
    }),
  updateAddress: (addressId, payload) =>
    springRequest(`/members/me/addresses/${addressId}`, {
      method: "PATCH",
      body: jsonBody(payload),
    }),
  setDefault: (addressId) =>
    springRequest(`/members/me/addresses/${addressId}/default`, {
      method: "PATCH",
    }),
};
