// 화면 전반에서 공유하는 상품 데이터와 이미지/재고/판매 지표를 구성합니다.
// Temporary frontend-only product repository.
// Delete catalogSeed.js and replace this module with the backend API adapter later.
import { catalogSeedRows } from "./catalogSeed";

const foodImages = {
  DOG: {
    사료: [
      "/assets/01_harim_the_real.jpg",
      "/assets/02_anf_6free.jpeg",
      "/assets/03_royal_canin.jpeg",
    ],
    간식: ["/assets/04_inaba_wan_churu.jpg", "/assets/05_matroos_chicken.jpeg"],
    영양제: ["/assets/06_forcans_joint.png"],
  },
  CAT: {
    사료: [
      "/assets/04_nutriplan_hairball.jpg",
      "/assets/03_royal_canin.jpeg",
      "/assets/01_harim_the_real.jpg",
    ],
    간식: ["/assets/02_inaba_churu.jpg", "/assets/03_matroos_chicken.jpeg"],
    영양제: ["/assets/05_anicare_joint.png"],
  },
};

const brandImages = {
  로얄캐닌: "/assets/products/royal-canin.jpg",
  "퓨리나 프로플랜": "/assets/products/purina-pro-plan.png",
  힐스: "/assets/products/hills.jpg",
  오리젠: "/assets/products/orijen.jpg",
  아카나: "/assets/products/acana.png",
  하림펫푸드: "/assets/products/harim-petfood.jpg",
  ANF: "/assets/products/anf.png",
  내추럴발란스: "/assets/products/natural-balance.jpg",
  인스팅트: "/assets/products/instinct.jpg",
  웰니스: "/assets/products/wellness.jpg",
  "퓨리나 원": "/assets/products/purina-one.png",
  뉴트리플랜: "/assets/products/nutriplan.jpg",
  이나바: "/assets/products/inaba.jpg",
  PETKIT: "/assets/products/petkit.png",
  PETLIBRO: "/assets/products/petlibro.png",
  샤오미: "/assets/products/xiaomi.jpg",
  펫세이프: "/assets/products/petsafe.jpg",
  캣잇: "/assets/products/catit.jpg",
  호머런펫: "/assets/products/homerunpet.jpg",
  딩동펫: "/assets/products/dingdongpet.webp",
  바비온: "/assets/products/babion.jpg",
  리스펫: "/assets/products/dingdongpet.webp",
  하겐: "/assets/products/hagen.webp",
};

function generatedProductImage(id, name, category, subcategory) {
  if (category === "사료" || category === "간식") {
    if (/연어/.test(name))
      return "/assets/products/generated/recipe-salmon.png";
    if (/참치/.test(name)) return "/assets/products/generated/recipe-tuna.png";
    if (/대구|흰살생선|복어/.test(name))
      return "/assets/products/generated/recipe-whitefish.png";
    if (/닭|치킨/.test(name))
      return "/assets/products/generated/recipe-chicken.png";
    if (/오리/.test(name)) return "/assets/products/generated/recipe-duck.png";
    if (/소고기|양고기|램/.test(name))
      return "/assets/products/generated/recipe-red-meat.png";
  }
  if (category === "사료") {
    const images =
      subcategory === "습식 / 화식"
        ? [
            "/assets/products/generated/wet-food.png",
            "/assets/products/generated/wet-food-cans.png",
          ]
        : [
            "/assets/products/generated/dry-food.png",
            "/assets/products/generated/dry-food-sage.png",
            "/assets/products/generated/dry-food-terracotta.png",
          ];
    return images[id % images.length];
  }
  if (category === "간식") {
    if (subcategory === "덴탈 간식")
      return "/assets/products/generated/treat-dental.png";
    if (subcategory === "동결건조 / 트릿")
      return id % 2
        ? "/assets/products/generated/treat-freeze-salmon.png"
        : "/assets/products/generated/treat.png";
    return "/assets/products/generated/treat.png";
  }
  if (category === "영양제") return "/assets/products/generated/supplement.png";
  if (category === "급식기") return "/assets/products/generated/feeder.png";
  if (category === "급수기" || category === "정수 / 필터")
    return "/assets/products/generated/fountain.png";
  if (category === "일반 식기") return "/assets/products/generated/bowl.png";
  return "";
}

function fallbackImage(id, pet, category, brand, subcategory, name) {
  const generatedImage = generatedProductImage(id, name, category, subcategory);
  if (generatedImage) return generatedImage;
  if (brandImages[brand]) return brandImages[brand];
  if (pet === "ALL") return "/assets/pawcast-logo-no-tagline.png";
  const images = foodImages[pet]?.[category] ||
    foodImages[pet]?.["사료"] || ["/assets/pawcast-logo-no-tagline.png"];
  return images[(id - 1) % images.length];
}

// 상품 종류에 맞는 임시 판매 옵션을 한곳에서 구성합니다.
// 상세 상단 드롭다운과 하단 상품 설명이 이 배열을 함께 사용하므로 옵션 표기가 서로 어긋나지 않습니다.
function productOptions(category, subcategory, pet) {
  if (pet === "ALL") {
    if (category === "정수 / 필터") return ["1개", "3개입", "6개입"];
    if (category === "일반 식기") return ["소형", "중형", "대형"];
    return ["기본형", "대용량", "프리미엄형"];
  }

  if (category === "사료") {
    return subcategory === "습식 / 화식"
      ? ["100g", "200g", "500g", "1kg"]
      : ["100g", "300g", "1kg", "2kg", "5kg", "10kg"];
  }

  if (category === "간식") return ["50g", "100g", "300g", "500g"];
  if (category === "영양제") return ["30정", "60정", "120정"];
  return ["100g", "300g", "1kg", "2kg"];
}

// 첫 번째 옵션의 상품가를 기준으로 용량 단계가 커질수록 가격이 증가하도록 임시 가격표를 만듭니다.
// 백엔드 연동 시에는 이 값을 상품 옵션 API가 내려주는 optionId와 salePrice로 교체합니다.
function optionPriceMap(options, basePrice) {
  const priceRates = [1, 1.25, 1.6, 2, 2.7, 3.6];
  return Object.fromEntries(
    options.map((option, index) => [
      option,
      Math.round((basePrice * (priceRates[index] || 1 + index * 0.55)) / 100) *
        100,
    ]),
  );
}

export const products = catalogSeedRows.map((row, index) => {
  const [id, mainCategory, category, subcategory, brand, name, price, stock] =
    row;
  const pet =
    mainCategory === "강아지관"
      ? "DOG"
      : mainCategory === "고양이관"
        ? "CAT"
        : "ALL";
  const rating = Math.round((4.1 + ((id * 17) % 9) / 10) * 10) / 10;
  const salesCount = 20 + ((id * 73) % 2980);
  const reviewCount = 1 + ((id * 29) % 780);
  const options = productOptions(category, subcategory, pet);
  const optionPrices = optionPriceMap(options, price);
  return {
    id,
    pet,
    mainCategory,
    category,
    subcategory,
    brand,
    image: fallbackImage(id, pet, category, brand, subcategory, name),
    name,
    price,
    rating,
    stock,
    option: options[0],
    options,
    optionPrices,
    discountRate: [0, 5, 8, 10, 12, 15, 18, 20][id % 8],
    salesCount,
    reviewCount,
    popularity: Math.round(rating * 15 + salesCount / 50),
    createdAt: `2026-11-${String((index % 10) + 1).padStart(2, "0")}`,
    temporarySeed: true,
  };
});
