// 백엔드 주문 데이터가 준비되기 전 test01 화면 검수에만 사용하는 API 응답 형태의 더미 데이터입니다.
import { products } from "@/data/products";

const firstProduct =
  products.find((item) => item.pet === "DOG" && item.stock > 0) || products[0];
const secondProduct =
  products.find((item) => item.category === "영양제" && item.stock > 0) ||
  products[5];
const thirdProduct = products.find(
  (item) => item.id !== firstProduct?.id && item.stock > 0,
);
const fourthProduct = products.find(
  (item) =>
    item.id !== firstProduct?.id &&
    item.id !== thirdProduct?.id &&
    item.stock > 0,
);

export const temporaryOrders = [
  {
    id: "demo-order-1",
    source: "temporary",
    orderNumber: "POW-20261104-001",
    status: "SHIPPING",
    totalAmount: 68800,
    finalAmount: 68800,
    recipientName: "이수미",
    recipientPhone: "010-1234-5678",
    deliveryZipcode: "21390",
    deliveryAddress1: "인천광역시 부평구 경원대로 1366",
    deliveryAddress2: "101동 1203호",
    deliveryMessage: "문 앞에 놓아주세요.",
    discountAmount: 0,
    shippingFee: 0,
    paymentMethod: "카드 결제",
    createdAt: "2026-11-04T10:30:00+09:00",
    orderItems: [
      {
        id: "demo-order-item-1",
        productId: firstProduct?.id,
        variantId: 1001,
        productName:
          firstProduct?.name || "더리얼 그레인프리 오븐베이크드 닭고기",
        optionLabel: firstProduct?.option || "1.6kg",
        quantity: 1,
        unitPrice: 28000,
        imageUrl: firstProduct?.image,
      },
      {
        id: "demo-order-item-1-2",
        productId: thirdProduct?.id,
        variantId: 1003,
        productName: thirdProduct?.name || "차오츄르 닭가슴살 가리비",
        optionLabel: thirdProduct?.option || "14g × 20개",
        quantity: 1,
        unitPrice: 14900,
        imageUrl: thirdProduct?.image,
      },
      {
        id: "demo-order-item-1-3",
        productId: fourthProduct?.id,
        variantId: 1004,
        productName: fourthProduct?.name || "퓨리나 프로플랜 치킨",
        optionLabel: fourthProduct?.option || "120g",
        quantity: 1,
        unitPrice: 25900,
        imageUrl: fourthProduct?.image,
      },
    ],
  },
  {
    id: "demo-order-2",
    source: "temporary",
    orderNumber: "POW-20261018-001",
    status: "DELIVERED",
    totalAmount: 25000,
    finalAmount: 25000,
    recipientName: "이수미",
    recipientPhone: "010-1234-5678",
    deliveryZipcode: "21390",
    deliveryAddress1: "인천광역시 부평구 경원대로 1366",
    deliveryAddress2: "101동 1203호",
    deliveryMessage: "문 앞에 놓아주세요.",
    discountAmount: 0,
    shippingFee: 0,
    paymentMethod: "카드 결제",
    createdAt: "2026-10-18T15:20:00+09:00",
    orderItems: [
      {
        id: "demo-order-item-2",
        productId: secondProduct?.id,
        variantId: 1002,
        productName: secondProduct?.name || "뉴트리션 관절·뼈 영양제",
        optionLabel: secondProduct?.option || "240g",
        quantity: 1,
        unitPrice: 25000,
        imageUrl: secondProduct?.image,
      },
    ],
  },
];
