// 상품 API 응답을 화면에서 공통으로 사용할 모델로 변환합니다. 로컬 시드에는 의존하지 않습니다.
import { springRequest } from "@/services/http/springClient";

export function toProductCard(product, pet = "") {
  const category = product.categoryName || "기타";
  return {
    id: product.id,
    name: product.name,
    brand: product.brand || "PAWCAST",
    category,
    pet,
    mainCategory: pet === "ALL" ? "식기·급수관" : "식품관",
    image: product.imagePath || "/images/product-placeholder.svg",
    price: Number(product.price || 0),
    stock: product.soldOut ? 0 : 1,
    salesCount: Number(product.salesCount || 0),
    reviewCount: Number(product.reviewCount || 0),
    rating: Number(product.averageRating || 0),
    variantId: product.variantId,
  };
}

export const productService = {
  list: async (filters = {}, pet = "") => {
    const query = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") query.set(key, String(value));
    });
    const response = await springRequest(`/products${query.size ? `?${query}` : ""}`);
    return Array.isArray(response) ? response.map((product) => toProductCard(product, pet)) : [];
  },
  detail: (productId) => springRequest(`/products/${productId}`),
  categories: () => springRequest("/categories"),
  bestReviews: () => springRequest("/products/best-reviews"),
};
