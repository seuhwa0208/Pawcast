// 로그인 회원의 배송지 목록 조회와 신규 배송지 등록 API를 제공합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const addressService = {
  getAddresses: () => springRequest("/members/me/addresses"),
  addAddress: (payload) =>
    springRequest("/members/me/addresses", {
      method: "POST",
      body: jsonBody(payload),
    }),
  setDefault: (addressId) =>
    springRequest(`/members/me/addresses/${addressId}/default`, {
      method: "PATCH",
    }),
};
