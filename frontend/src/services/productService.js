// 상품 API 응답을 화면에서 공통으로 사용할 모델로 변환합니다. 로컬 시드에는 의존하지 않습니다.
import { springRequest } from "@/services/http/springClient";
import { aiRequest } from "@/services/http/aiClient";

const brandFallbackImages = {
  이나바: "/assets/products/inaba.jpg",
  ANF: "/assets/products/anf.png",
};

// # Spring API는 하위 카테고리명을 반환한다. 목록 화면의 상위 탭과 함께 쓰도록 두 단계를 분리한다.
const categoryGroups = {
  "건식 사료": "사료",
  "습식 / 화식": "사료",
  "기능성 사료": "사료",
  "연령별 사료": "사료",
  "덴탈 간식": "간식",
  "동결건조 / 트릿": "간식",
  "육포 / 저키": "간식",
  "츄르 / 파우치": "간식",
  "관절 / 뼈": "영양제",
  "피부 / 모질": "영양제",
  "장 / 유산균": "영양제",
  "눈 / 종합영양": "영양제",
  "자동 급식기": "급식기",
  "수동 급식기": "급식기",
  "휴대용 급식기": "급식기",
  "자동 급수기": "급수기",
  "휴대용 급수기": "급수기",
  "도자기 식기": "일반 식기",
  "스테인리스 식기": "일반 식기",
  "높이 조절 식기": "일반 식기",
  "정수 필터": "정수 / 필터",
  "교체용 필터": "정수 / 필터",
  "세척 용품": "정수 / 필터",
};

function categoryGroupFor(category) {
  return categoryGroups[category] || category || "기타";
}

function fallbackImageFor(product) {
  return (
    brandFallbackImages[product.brand] ||
    "/assets/products/generated/dry-food.png"
  );
}

export function toProductCard(product, pet = "") {
  const resolvedPet = pet || product.productType || "ALL";
  const subcategory = product.categoryName || "기타";
  const category = categoryGroupFor(subcategory);
  return {
    id: product.id,
    name: product.name,
    brand: product.brand || "PAWCAST",
    category,
    subcategory,
    pet: resolvedPet,
    mainCategory: resolvedPet === "ALL" ? "식기·급수관" : "식품관",
    image: product.imagePath || fallbackImageFor(product),
    imageFallback: fallbackImageFor(product),
    price: Number(product.price || 0),
    // # 할인 표시는 백엔드가 제공하는 정가·할인율을 기준으로 하며, 프론트에서 임의 계산하지 않는다.
    originalPrice: Number(
      product.originalPrice || product.listPrice || product.price || 0,
    ),
    discountRate: Number(product.discountRate || 0),
    stock: product.soldOut ? 0 : 1,
    salesCount: Number(product.salesCount || 0),
    reviewCount: Number(product.reviewCount || 0),
    rating: Number(product.averageRating || 0),
    variantId: product.variantId,
    bestReview: product.bestReview || null,
    categoryId: product.categoryId,
    productType: product.productType,
    mainIngredient: product.mainIngredient,
    lifeStage: product.lifeStage,
  };
}

export const productService = {
  list: async (filters = {}, pet = "") => {
    const query = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "")
        query.set(key, String(value));
    });
    const response = await springRequest(
      `/products${query.size ? `?${query}` : ""}`,
    );
    return Array.isArray(response)
      ? response.map((product) => toProductCard(product, pet))
      : [];
  },
  detail: (productId) => springRequest(`/products/${productId}`),
  categories: () => springRequest("/categories"),
  bestReviews: async (filters = {}, pet = "") => {
    const limit = Number(filters.limit || 5);
    const { body: response } = await aiRequest(
      `/api/reviews/best-products?limit=${limit}`,
    );
    return Array.isArray(response)
      ? response.map((product) => toProductCard(product, pet))
      : [];
  },
};
