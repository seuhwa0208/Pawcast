// 관리자 상품 등록 요청을 상품 유형별 DTO로 백엔드에 전달합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const adminProductService = {
  getCategories() {
    return springRequest("/categories");
  },
  create(payload) {
    return springRequest("/admin/products", {
      method: "POST",
      body: jsonBody(payload),
    });
  },
};
