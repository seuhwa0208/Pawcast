// 백엔드에 이미 존재하는 관리자 API만 모아, 임시 화면 데이터와 구분합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const adminManagementService = {
  getOrders: () => springRequest("/orders/admin"),
  changeOrderStatus: (orderId, orderStatus) =>
    springRequest(`/orders/admin/${orderId}/status`, {
      method: "POST", body: jsonBody({ orderStatus }),
    }),
  getMembers: () => springRequest("/admin"),
  changeMemberStatus: (memberId, status) =>
    springRequest(`/admin/members/${memberId}/status`, {
      method: "PATCH", body: jsonBody({ status }),
    }),
  getProducts: () => springRequest("/admin/products"),
  adjustStock: (variantId, quantityChange) =>
    springRequest(`/admin/variants/${variantId}/stock?quantityChange=${quantityChange}`, { method: "PATCH" }),
  getManagedQnas: () => springRequest("/qna/manage"),
  answerQna: (qnaId, content) =>
    springRequest(`/inquiries/${qnaId}`, { method: "POST", body: jsonBody({ content }) }),
  getNotices: () => springRequest("/notices"),
  createNotice: (payload) =>
    springRequest("/notices", { method: "POST", body: jsonBody(payload) }),
  updateNotice: (noticeId, payload) =>
    springRequest(`/notices/${noticeId}`, { method: "PATCH", body: jsonBody(payload) }),
  deleteNotice: (noticeId) => springRequest(`/notices/${noticeId}`, { method: "DELETE" }),
};
