// 소진일 관리 항목과 계산 설정을 백엔드 API에 연결하기 위한 도메인 어댑터입니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

// 백엔드 API가 확정되면 Store에서 이 어댑터를 호출하도록 연결합니다.
export const consumptionService = {
  getItems: () => springRequest("/members/me/consumption-items"),
  addItem: (payload) =>
    springRequest("/members/me/consumption-items", {
      method: "POST",
      body: jsonBody(payload),
    }),
  updateItem: (itemId, payload) =>
    springRequest(`/members/me/consumption-items/${itemId}`, {
      method: "PATCH",
      body: jsonBody(payload),
    }),
  saveSettings: (payload) =>
    springRequest("/members/me/consumption-settings", {
      method: "PUT",
      body: jsonBody(payload),
    }),
};
