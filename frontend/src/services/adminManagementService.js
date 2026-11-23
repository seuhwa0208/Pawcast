import { jsonBody, springRequest } from "@/services/http/springClient";

export const adminManagementService = {
  getOrders: () => springRequest("/orders/admin"),
  changeOrderStatus: (orderId, orderStatus) =>
    springRequest(`/orders/admin/${orderId}/status`, {
      method: "POST",
      body: jsonBody({ orderStatus }),
    }),
  getMembers: () => springRequest("/admin"),
  changeMemberStatus: (userId, status) =>
    springRequest(`/admin/members/${userId}/status`, {
      method: "PATCH",
      body: jsonBody({ status }),
    }),
  changeMemberRole: (userId, role) =>
    springRequest(`/admin/members/${userId}/role`, {
      method: "PATCH",
      body: jsonBody({ role }),
    }),
  getRefunds: () => springRequest("/admin/refunds"),
  approveRefund: (refundId) =>
    springRequest(`/admin/refunds/${refundId}/approve`, { method: "PATCH" }),
  getProducts: () => springRequest("/admin/products"),
  createProduct: (formData) =>
  springRequest("/admin/products", {
    method: "POST",
    body: formData,
  }),
  getBestProducts: () => springRequest("/products/best-sales"),
  getWorstProducts: () => springRequest("/products/worst-sales"),
  getManagedQnas: () => springRequest("/qna/manage"),
  // # 매니저·관리자가 실제 문의 답변을 등록하는 Spring API다.
  createQnaAnswer: (qnaId, content) =>
    springRequest(`/inquiries/${qnaId}`, {
      method: "POST",
      body: jsonBody({ content }),
    }),
  updateQnaAnswer: (qnaId, content) =>
    springRequest(`/inquiries/${qnaId}`, {
      method: "PUT",
      body: jsonBody({ content }),
    }),
  getNotices: () => springRequest("/notices"),
};
