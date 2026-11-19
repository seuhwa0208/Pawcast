// 취소·반품·교환 화면의 상태별 디자인 검수에만 사용하는 test01 임시 데이터입니다.
import { products } from "@/data/products";

const previewProducts = products.filter((product) => product.stock > 0).slice(0, 5);

const claimCases = [
  {
    id: "demo-claim-1",
    type: "CANCEL",
    status: "REQUESTED",
    statusLabel: "취소 요청",
    description: "판매자가 요청 내용을 확인하고 있습니다.",
    requestedAt: "2026-11-12",
    orderNumber: "POW-20261111-002",
    reason: "주문 상품을 변경하고 싶어요.",
  },
  {
    id: "demo-claim-2",
    type: "CANCEL",
    status: "COMPLETED",
    statusLabel: "취소 완료",
    description: "결제 취소와 환불 처리가 완료되었습니다.",
    requestedAt: "2026-11-08",
    orderNumber: "POW-20261107-004",
    reason: "단순 변심",
    refundAmount: 25900,
  },
  {
    id: "demo-claim-3",
    type: "RETURN",
    status: "COLLECTING",
    statusLabel: "반품 회수중",
    description: "택배 기사님이 상품을 회수할 예정입니다.",
    requestedAt: "2026-11-03",
    orderNumber: "POW-20261029-001",
    reason: "상품 포장이 훼손되어 도착했어요.",
  },
  {
    id: "demo-claim-4",
    type: "RETURN",
    status: "COMPLETED",
    statusLabel: "반품 완료",
    description: "반품 검수와 환불 처리가 완료되었습니다.",
    requestedAt: "2026-10-24",
    orderNumber: "POW-20261020-003",
    reason: "상품이 설명과 달라요.",
    refundAmount: 14900,
  },
  {
    id: "demo-claim-5",
    type: "EXCHANGE",
    status: "PROCESSING",
    statusLabel: "교환 처리중",
    description: "교환 상품을 준비하고 있습니다.",
    requestedAt: "2026-10-16",
    orderNumber: "POW-20261012-006",
    reason: "다른 옵션으로 교환하고 싶어요.",
  },
];

export const temporaryClaims = claimCases.map((claim, index) => {
  const product = previewProducts[index] || products[index] || products[0];
  return {
    ...claim,
    orderItemId: `demo-claim-item-${index + 1}`,
    productId: product?.id,
    pet: product?.pet || "DOG",
    productName: product?.name || "PAWCAST 상품",
    optionLabel: product?.option || "기본 옵션",
    imageUrl: product?.image,
    amount: product?.price || 0,
    quantity: 1,
  };
});
