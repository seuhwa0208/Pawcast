import { jsonBody, springRequest } from "@/services/http/springClient";

export const adminManagementService = {
  getOrders: () => springRequest("/orders/admin"),
  changeOrderStatus: (orderId, orderStatus) => springRequest(`/orders/admin/${orderId}/status`, { method: "POST", body: jsonBody({ orderStatus }) }),
  getMembers: () => springRequest("/admin"),
  changeMemberStatus: (userId, status) => springRequest(`/admin/members/${userId}/status`, { method: "PATCH", body: jsonBody({ status }) }),
  getProducts: () => springRequest("/admin/products"),
  getManagedQnas: () => springRequest("/qna/manage"),
  getNotices: () => springRequest("/notices"),
};
