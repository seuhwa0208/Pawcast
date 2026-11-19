<!-- 식품과 급수·급식 기기의 서로 다른 등록 항목을 분리한 관리자 상품 등록 화면입니다. -->
<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { adminProductService } from "@/services/adminProductService";
import { useAuthSession } from "@/stores/auth";

const router = useRouter();
const { setAccessToken } = useAuthSession();
const sidebarCollapsed = ref(false);
const registrationType = ref("FEED");
const submitting = ref(false);
const message = ref("");
const messageType = ref("");
const categories = ref([]);
const categoryLoadError = ref("");
const base = reactive({ categoryId: "", name: "", brand: "", description: "", status: "ON_SALE" });
const food = reactive({ petType: "DOG", foodForm: "DRY", mainIngredient: "", lifeStage: "ALL", ingredientDescription: "" });
const device = reactive({ petType: "ALL", deviceKind: "BOWL", material: "", requiresPower: false, hasFilter: false, featureDescription: "" });
const images = ref([{ imagePath: "", main: true, sortOrder: 0 }]);

function emptyVariant() {
  return { skuCode: "", optionKey: "", optionLabel: "", color: "", weightG: "", volumeMl: "", capacityMl: "", heightCm: "", price: 0, costPrice: 0, discountRate: 0, stock: 0 };
}
const variants = ref([emptyVariant()]);

const productType = computed(() => registrationType.value === "DEVICE" ? "DEVICE" : "FOOD");
const sectionTitle = computed(() => ({ FEED: "사료 상세 정보", SNACK: "간식 상세 정보", DEVICE: "식기·급수용품 상세 정보" }[registrationType.value]));
const optionGuide = computed(() => productType.value === "FOOD" ? "중량·용량별 SKU 옵션" : "모델·색상·수용량별 SKU 옵션");

async function loadCategories() {
  try {
    const result = await adminProductService.getCategories();
    categories.value = Array.isArray(result) ? result : result?.content || [];
  } catch (error) {
    categoryLoadError.value = "카테고리를 불러오지 못해 ID 직접 입력으로 전환했습니다.";
  }
}

watch(registrationType, () => {
  variants.value = [emptyVariant()];
  message.value = "";
});

function logout() {
  setAccessToken("");
  router.push("/login");
}

function addVariant() {
  variants.value.push(emptyVariant());
}
function removeVariant(index) {
  if (variants.value.length > 1) variants.value.splice(index, 1);
}
function addImage() {
  images.value.push({ imagePath: "", main: false, sortOrder: images.value.length });
}
function removeImage(index) {
  if (images.value.length > 1) images.value.splice(index, 1);
  images.value.forEach((item, order) => (item.sortOrder = order));
  if (!images.value.some((item) => item.main)) images.value[0].main = true;
}
function selectMainImage(index) {
  images.value.forEach((item, itemIndex) => (item.main = itemIndex === index));
}
function positiveOrNull(value) {
  return value === "" || value == null ? null : Number(value);
}
function validateForm() {
  if (!base.name.trim() || !Number(base.categoryId)) return "카테고리와 상품명을 입력해주세요.";
  if (productType.value === "FOOD" && (!food.mainIngredient.trim() || !food.foodForm)) return "식품 형태와 주원료를 입력해주세요.";
  if (registrationType.value === "FEED" && !food.lifeStage) return "사료의 생애주기를 선택해주세요.";
  if (productType.value === "DEVICE" && (!device.deviceKind || !device.material.trim())) return "기기 종류와 재질을 입력해주세요.";
  const invalidVariant = variants.value.some((item) =>
    !item.skuCode.trim() || !item.optionKey.trim() || !item.optionLabel.trim() || Number(item.price) < 0 || Number(item.costPrice) < 0 || Number(item.stock) < 0,
  );
  if (invalidVariant) return "각 옵션의 SKU 코드, 옵션 키·이름, 판매가, 원가와 재고를 확인해주세요.";
  if (productType.value === "FOOD" && variants.value.some((item) => !positiveOrNull(item.weightG) && !positiveOrNull(item.volumeMl))) return "식품 옵션에는 중량 또는 용량을 입력해주세요.";
  if (productType.value === "DEVICE" && variants.value.some((item) => positiveOrNull(item.weightG) || positiveOrNull(item.volumeMl))) return "식기 옵션에는 식품용 중량·용량을 입력할 수 없습니다.";
  return "";
}
async function submit() {
  message.value = "";
  const validationMessage = validateForm();
  if (validationMessage) {
    message.value = validationMessage;
    messageType.value = "error";
    return;
  }
  const foodDetail = productType.value === "FOOD" ? {
    foodKind: registrationType.value,
    foodForm: food.foodForm,
    mainIngredient: food.mainIngredient,
    lifeStage: food.lifeStage,
    ingredientDescription: food.ingredientDescription || null,
  } : null;
  const deviceDetail = productType.value === "DEVICE" ? {
    deviceKind: device.deviceKind,
    material: device.material,
    requiresPower: device.requiresPower,
    hasFilter: device.hasFilter,
    featureDescription: [
      device.featureDescription,
      `전원 필요: ${device.requiresPower ? "예" : "아니오"}`,
      `필터 포함: ${device.hasFilter ? "예" : "아니오"}`,
    ].filter(Boolean).join(" / "),
  } : null;
  const payload = {
    productType: productType.value,
    petType: productType.value === "FOOD" ? food.petType : device.petType,
    status: base.status,
    categoryId: Number(base.categoryId),
    name: base.name,
    brand: base.brand || null,
    description: base.description || null,
    foodDetail,
    deviceDetail,
    images: images.value.filter((item) => item.imagePath.trim()).map((item, index) => ({ imagePath: item.imagePath.trim(), main: item.main, sortOrder: index })),
    variants: variants.value.map((item) => ({
      skuCode: item.skuCode.trim(),
      optionKey: item.optionKey.trim(),
      optionLabel: item.optionLabel.trim(),
      color: productType.value === "DEVICE" ? item.color.trim() || null : null,
      weightG: productType.value === "FOOD" ? positiveOrNull(item.weightG) : null,
      volumeMl: productType.value === "FOOD" ? positiveOrNull(item.volumeMl) : null,
      capacityMl: productType.value === "DEVICE" ? positiveOrNull(item.capacityMl) : null,
      heightCm: productType.value === "DEVICE" ? positiveOrNull(item.heightCm) : null,
      price: Number(item.price),
      costPrice: Number(item.costPrice),
      discountRate: Number(item.discountRate) || 0,
      stock: Number(item.stock),
    })),
  };
  try {
    submitting.value = true;
    await adminProductService.create(payload);
    message.value = "상품이 등록되었습니다.";
    messageType.value = "success";
  } catch (error) {
    message.value = error.message || "상품 등록에 실패했습니다.";
    messageType.value = "error";
  } finally {
    submitting.value = false;
  }
}

onMounted(loadCategories);
</script>

<template>
  <div class="admin-product-layout">
    <aside class="admin-unified-sidebar">
      <header class="admin-unified-brand"><b>PAWCAST</b><span>관리자 페이지</span></header>
      <nav class="admin-unified-nav">
        <button type="button" @click="router.push('/admin/dashboard')"><span>최종 대시보드</span></button>
        <button type="button" @click="router.push('/admin/sales')"><span>매출 분석</span></button>
        <button type="button" @click="router.push('/admin/orders')"><span>주문·배송 관리</span></button>
        <button type="button" @click="router.push('/admin/products')"><span>상품 관리</span></button>
        <button type="button" class="active"><span>상품 등록</span></button>
        <button type="button" @click="router.push('/admin/members')"><span>회원 관리</span></button>
        <button type="button" @click="router.push('/admin/notices')"><span>공지사항 관리</span></button>
        <button type="button" @click="router.push('/admin/qna')"><span>QnA 관리</span></button>
      </nav>
      <button type="button" class="admin-unified-logout" @click="logout"><span>로그아웃</span></button>
    </aside>
    <main class="admin-product-page">
    <header class="admin-product-heading">
      <div><small>PRODUCT MANAGEMENT</small><h1>상품 등록</h1><p>판매관에 맞는 정보와 옵션을 등록하세요.</p></div>
      <button type="button" @click="router.push('/admin/dashboard')">대시보드로</button>
    </header>

    <form class="admin-product-form" @submit.prevent="submit">
      <section class="type-selector">
        <h2>등록할 상품 유형</h2>
        <div>
          <label :class="{ selected: registrationType === 'FEED' }"><input v-model="registrationType" type="radio" value="FEED" /><b>사료</b><span>건식·습식·동결건조 사료</span></label>
          <label :class="{ selected: registrationType === 'SNACK' }"><input v-model="registrationType" type="radio" value="SNACK" /><b>간식</b><span>트릿·저키·껌·동결건조 간식</span></label>
          <label :class="{ selected: registrationType === 'DEVICE' }"><input v-model="registrationType" type="radio" value="DEVICE" /><b>식기·급수용품</b><span>식기·급수기·급식기·필터</span></label>
        </div>
      </section>

      <section class="form-card">
        <h2>공통 상품 정보</h2>
        <div class="form-grid">
          <label v-if="categories.length"><span>카테고리 *</span><select v-model="base.categoryId" required><option disabled value="">카테고리를 선택해주세요</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
          <label v-else><span>카테고리 ID *</span><input v-model.number="base.categoryId" type="number" min="1" required /></label>
          <label><span>상품명 *</span><input v-model.trim="base.name" maxlength="200" required /></label>
          <label><span>브랜드</span><input v-model.trim="base.brand" maxlength="100" /></label>
          <label><span>판매 상태 *</span><select v-model="base.status" required><option value="ON_SALE">판매중</option><option value="SOLD_OUT">품절</option><option value="HIDDEN">숨김</option></select></label>
          <label class="wide"><span>상품 설명</span><textarea v-model.trim="base.description" rows="4" maxlength="10000" /></label>
          <p v-if="categoryLoadError" class="field-note wide">{{ categoryLoadError }}</p>
        </div>
      </section>

      <section class="form-card">
        <h2>{{ sectionTitle }}</h2>
        <div v-if="productType === 'FOOD'" class="form-grid">
          <label><span>대상 반려동물 *</span><select v-model="food.petType"><option value="DOG">강아지</option><option value="CAT">고양이</option><option value="ALL">공용</option></select></label>
          <label><span>{{ registrationType === 'FEED' ? '사료 형태' : '간식 형태' }} *</span><select v-model="food.foodForm" required><template v-if="registrationType === 'FEED'"><option value="DRY">건식</option><option value="WET">습식</option><option value="FREEZE_DRY">동결건조</option><option value="RAW">생식</option></template><template v-else><option value="TREAT">트릿</option><option value="JERKY">저키</option><option value="CHEW">껌</option><option value="FREEZE_DRY">동결건조</option><option value="WET">습식 간식</option></template></select></label>
          <label><span>주원료 *</span><input v-model.trim="food.mainIngredient" maxlength="255" placeholder="예: 닭고기, 연어" required /></label>
          <label><span>생애주기 *</span><select v-model="food.lifeStage" required><option value="ALL">전연령</option><option value="PUPPY_KITTEN">퍼피·키튼</option><option value="ADULT">어덜트</option><option value="SENIOR">시니어</option></select></label>
          <label class="wide"><span>원료·성분 설명</span><textarea v-model.trim="food.ingredientDescription" rows="4" placeholder="원재료, 영양성분과 알레르기 유발 가능 성분을 입력하세요." /></label>
          <p class="field-note wide">대상 반려동물은 현재 상품 등록 화면의 분류에 사용하며, 품종 목록처럼 별도 마스터 테이블을 만들지 않습니다.</p>
        </div>
        <div v-else class="form-grid">
          <label><span>대상 반려동물 *</span><select v-model="device.petType"><option value="DOG">강아지</option><option value="CAT">고양이</option><option value="ALL">공용</option></select></label>
          <label><span>용품 유형 *</span><select v-model="device.deviceKind" required><option value="BOWL">일반 식기</option><option value="FEEDER">자동 급식기</option><option value="FOUNTAIN">급수기</option><option value="FILTER">정수·교체 필터</option></select></label>
          <label><span>재질 *</span><input v-model.trim="device.material" maxlength="255" placeholder="예: 스테인리스, 세라믹, ABS" required /></label>
          <label class="check-field"><input v-model="device.requiresPower" type="checkbox" /><span>전원이 필요한 제품</span></label>
          <label class="check-field"><input v-model="device.hasFilter" type="checkbox" /><span>필터가 포함된 제품</span></label>
          <label class="wide"><span>기능·제품 설명</span><textarea v-model.trim="device.featureDescription" rows="4" placeholder="세척 방식, 전원 방식, 구성품 등 주요 기능을 입력하세요." /></label>
        </div>
      </section>

      <section class="form-card image-card">
        <header><div><h2>상품 이미지</h2><p>대표 이미지 한 개와 추가 이미지를 등록할 수 있습니다.</p></div><button type="button" @click="addImage">+ 이미지 추가</button></header>
        <div v-for="(image, index) in images" :key="index" class="image-row">
          <label><span>이미지 경로</span><input v-model.trim="image.imagePath" maxlength="255" placeholder="/uploads/products/... 또는 https://..." /></label>
          <label class="main-image-radio"><input :checked="image.main" type="radio" name="main-image" @change="selectMainImage(index)" /><span>대표 이미지</span></label>
          <button type="button" class="remove-option" :disabled="images.length === 1" @click="removeImage(index)">삭제</button>
        </div>
      </section>

      <section class="form-card variant-card">
        <header><div><h2>판매 옵션(SKU)</h2><p>{{ optionGuide }}을 입력하세요.</p></div><button type="button" @click="addVariant">+ 옵션 추가</button></header>
        <div v-for="(variant, index) in variants" :key="index" class="variant-row">
          <label><span>SKU 코드 *</span><input v-model.trim="variant.skuCode" maxlength="255" placeholder="예: DOG-FEED-2KG-001" required /></label>
          <label><span>옵션 키 *</span><input v-model.trim="variant.optionKey" maxlength="150" :placeholder="productType === 'FOOD' ? 'WEIGHT_2KG' : 'WHITE_3L'" required /></label>
          <label><span>옵션 표시명 *</span><input v-model.trim="variant.optionLabel" maxlength="150" :placeholder="productType === 'FOOD' ? '2kg' : '화이트 / 3L'" required /></label>
          <template v-if="productType === 'FOOD'">
            <label><span>중량</span><div class="unit-input"><input v-model.number="variant.weightG" type="number" min="1" /><i>g</i></div></label>
            <label><span>용량</span><div class="unit-input"><input v-model.number="variant.volumeMl" type="number" min="1" /><i>ml</i></div></label>
          </template>
          <template v-else>
            <label><span>색상</span><input v-model.trim="variant.color" maxlength="50" placeholder="예: 크림" /></label>
            <label><span>수용량</span><div class="unit-input"><input v-model.number="variant.capacityMl" type="number" min="1" /><i>ml</i></div></label>
            <label><span>높이</span><div class="unit-input"><input v-model.number="variant.heightCm" type="number" min="1" /><i>cm</i></div></label>
          </template>
          <label><span>판매가 *</span><div class="unit-input"><input v-model.number="variant.price" type="number" min="0" /><i>원</i></div></label>
          <label><span>원가 *</span><div class="unit-input"><input v-model.number="variant.costPrice" type="number" min="0" /><i>원</i></div></label>
          <label><span>할인율</span><div class="unit-input"><input v-model.number="variant.discountRate" type="number" min="0" max="100" step=".01" /><i>%</i></div></label>
          <label><span>재고 *</span><div class="unit-input"><input v-model.number="variant.stock" type="number" min="0" /><i>개</i></div></label>
          <button type="button" class="remove-option" :disabled="variants.length === 1" @click="removeVariant(index)">삭제</button>
        </div>
      </section>

      <p v-if="message" class="submit-message" :class="messageType">{{ message }}</p>
      <footer class="form-actions"><button type="button" @click="router.push('/admin/dashboard')">취소</button><button class="primary" :disabled="submitting">{{ submitting ? '등록 중...' : '상품 등록' }}</button></footer>
    </form>
    </main>
  </div>
</template>

<style scoped>
.admin-product-layout{display:grid;grid-template-columns:238px minmax(0,1fr);width:100%;min-width:0;min-height:100vh;transition:grid-template-columns 180ms ease-out}.admin-product-layout.collapsed{grid-template-columns:72px minmax(0,1fr)}.product-admin-sidebar{position:sticky;top:0;display:flex;flex-direction:column;width:238px;height:100vh;min-width:0;border-right:1px solid #eadfd3;background:#fffcf7;padding:34px 18px 20px;transition:width 180ms ease-out,padding 180ms ease-out}.product-admin-sidebar.collapsed{width:72px;padding-inline:10px}.product-admin-sidebar header{display:grid;gap:4px;padding:0 14px 28px;border-bottom:1px solid #eadfd3}.product-admin-sidebar header b{color:#8b5e3c;font-size:26px}.product-admin-sidebar header span{color:#8e7767;font-size:12px}.product-admin-sidebar nav{display:grid;gap:7px;margin-top:28px}.product-admin-sidebar nav button,.product-admin-logout{min-height:50px;border:0;border-radius:11px;background:transparent;padding:0 14px;color:#4a3521;text-align:left;font-weight:800;white-space:nowrap;transition:color 180ms ease-out,background-color 180ms ease-out}.product-admin-sidebar nav button:hover:not(.active){background:#fffbf1;color:#8b5e3c}.product-admin-sidebar nav button.active{background:#8b5e3c;color:#fff}.product-admin-logout{margin-top:auto;border:1px solid #d7b892;text-align:center}.product-admin-logout:hover{background:#74492e;color:#fff}.product-sidebar-toggle{position:absolute;top:22px;right:-15px;z-index:2;width:30px;height:30px;border:1px solid #d7b892;border-radius:50%;background:#fff;color:#8b5e3c}.product-admin-sidebar.collapsed header{padding-inline:0;text-align:center}.product-admin-sidebar.collapsed header b{font-size:0}.product-admin-sidebar.collapsed header b::first-letter{font-size:22px}.product-admin-sidebar.collapsed header span,.product-admin-sidebar.collapsed nav span,.product-admin-sidebar.collapsed .product-admin-logout span{display:none}.product-admin-sidebar.collapsed nav button{text-align:center;padding:0}.product-admin-sidebar :is(button):focus-visible{outline:2px solid #d59a3a;outline-offset:2px}
.admin-product-page{width:100%;min-width:0;max-width:100%;min-height:100vh;overflow-x:clip;background:#f7f4f0;padding:34px clamp(24px,5vw,80px);color:#3d281b}.admin-product-heading{display:flex;justify-content:space-between;align-items:end;width:min(1200px,100%);min-width:0;margin:0 auto 24px}.admin-product-heading small{color:#a66838;font-weight:800;letter-spacing:.14em}.admin-product-heading h1{margin:6px 0;font-size:36px}.admin-product-heading p{margin:0;color:#947864}.admin-product-heading button,.form-actions button,.variant-card header button{border:1px solid #d7b892;border-radius:10px;background:#fff;padding:12px 20px;color:#4a3521;font-weight:700;transition:color 180ms ease-out,background-color 180ms ease-out,border-color 180ms ease-out}.admin-product-heading button:hover,.form-actions button:not(.primary):hover,.variant-card header button:hover{border-color:#8b5e3c;color:#8b5e3c}.admin-product-form{width:min(1200px,100%);min-width:0;margin:auto;display:grid;gap:18px}.type-selector,.form-card{min-width:0;background:#fff;border:1px solid #ead7c3;border-radius:18px;padding:24px}.type-selector h2,.form-card h2{margin:0 0 18px}.type-selector>div{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.type-selector label{min-width:0;display:grid;gap:5px;border:1px solid #e3d2c1;border-radius:14px;padding:18px;cursor:pointer;transition:background-color 180ms ease-out,border-color 180ms ease-out}.type-selector label:hover{border-color:#d7b892;background:#fffbf1}.type-selector label.selected{border-color:#8b5e3c;background:#fffbf1}.type-selector input{position:absolute;opacity:0}.type-selector label:has(input:focus-visible){outline:2px solid #d59a3a;outline-offset:2px}.type-selector span{color:#967c68}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.form-grid label,.variant-row label{min-width:0;display:grid;gap:8px;font-weight:700}.form-grid .wide{grid-column:1/-1}.admin-product-page input,.admin-product-page select,.admin-product-page textarea{width:100%;min-width:0;box-sizing:border-box;border:1px solid #dbc8b6;border-radius:10px;padding:13px;background:#fff;font:inherit;color:#3d281b}.admin-product-page input:focus-visible,.admin-product-page select:focus-visible,.admin-product-page textarea:focus-visible,.admin-product-page button:focus-visible{outline:2px solid #d59a3a;outline-offset:2px}.admin-product-page textarea{resize:vertical}.unit-input{min-width:0;display:flex;align-items:center;border:1px solid #dbc8b6;border-radius:10px;overflow:hidden}.unit-input input{border:0}.unit-input i{padding-right:13px;font-style:normal;color:#8b715e;white-space:nowrap}.variant-card header{display:flex;justify-content:space-between;align-items:center;gap:16px}.variant-card header h2{margin-bottom:4px}.variant-card header p{margin:0;color:#947864}.variant-row{min-width:0;display:grid;grid-template-columns:minmax(0,1.4fr) minmax(0,1fr) minmax(0,1fr) auto;align-items:end;gap:12px;padding-top:18px;margin-top:18px;border-top:1px solid #eee1d4}.remove-option{height:46px;border:1px solid #d6b28d;border-radius:10px;background:#fff;color:#9b512f;transition:color 180ms ease-out,background-color 180ms ease-out,border-color 180ms ease-out}.remove-option:hover:not(:disabled){border-color:#e57a63;background:#fff5f2;color:#d86650}.remove-option:active:not(:disabled){color:#bf523e}.remove-option:disabled{opacity:.4}.form-actions{display:flex;justify-content:flex-end;gap:10px}.form-actions .primary{background:#8b5e3c;color:#fff;border-color:#8b5e3c}.form-actions .primary:hover{background:#74492e}.form-actions .primary:active{background:#603a25}.submit-message{margin:0;padding:14px;border-radius:10px}.submit-message.success{background:#edf7ef;color:#347343}.submit-message.error{background:#fff0ed;color:#ad4939}@media(max-width:900px){.admin-product-layout,.admin-product-layout.collapsed{grid-template-columns:1fr}.product-admin-sidebar{position:relative;width:100%;height:auto}.product-sidebar-toggle{display:none}.product-admin-sidebar nav{grid-template-columns:repeat(2,minmax(0,1fr))}.product-admin-logout{margin-top:20px}.product-admin-sidebar.collapsed header span,.product-admin-sidebar.collapsed nav span,.product-admin-sidebar.collapsed .product-admin-logout span{display:inline}}@media(max-width:760px){.admin-product-heading{align-items:start;gap:14px;flex-direction:column}.type-selector>div,.form-grid{grid-template-columns:1fr}.form-grid .wide{grid-column:auto}.variant-row{grid-template-columns:1fr}.admin-product-page{padding:24px 14px}.product-admin-sidebar nav{grid-template-columns:1fr}}
.type-selector>div{grid-template-columns:repeat(3,minmax(0,1fr))}
.field-note{margin:0;color:#9a7a65;font-size:12px}
.check-field{display:flex!important;grid-template-columns:auto 1fr;align-items:center;align-self:end;min-height:48px;border:1px solid #e7d8ca;border-radius:10px;padding:0 14px}
.check-field input,.main-image-radio input{width:auto}
.variant-card header,.image-card header{display:flex;justify-content:space-between;align-items:center;gap:16px}
.image-card header h2{margin-bottom:4px}.image-card header p{margin:0;color:#947864}
.image-card header button{border:1px solid #d7b892;border-radius:10px;background:#fff;padding:12px 20px;color:#4a3521;font-weight:700}
.image-card header button:hover{border-color:#8b5e3c;color:#8b5e3c}
.variant-row{grid-template-columns:repeat(3,minmax(0,1fr))}
.variant-row>.remove-option{justify-self:end}
.image-row{display:grid;grid-template-columns:minmax(0,1fr) auto auto;align-items:end;gap:12px;padding-top:14px;margin-top:14px;border-top:1px solid #eee1d4}
.image-row label{min-width:0;display:grid;gap:8px;font-weight:700}
.main-image-radio{display:flex!important;align-items:center;min-height:46px}
@media(max-width:900px){.variant-row{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:760px){.type-selector>div,.variant-row,.image-row{grid-template-columns:1fr}}
</style>
