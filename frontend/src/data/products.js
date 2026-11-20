// 레거시 화면 호환을 위한 반응형 컬렉션입니다. 정적 상품 데이터는 포함하지 않습니다.
import { reactive } from "vue";
import { productService } from "@/services/productService";

export const products = reactive([]);

export async function loadProducts() {
  const [dog, cat, device] = await Promise.all([
    productService.list({ categoryId: 1 }, "DOG"),
    productService.list({ categoryId: 17 }, "CAT"),
    productService.list({ categoryId: 33 }, "ALL"),
  ]);
  products.splice(0, products.length, ...dog, ...cat, ...device);
  return products;
}
