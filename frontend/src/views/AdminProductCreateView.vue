<!-- # Spring ProductCreateRequest와 VariantCreateRequest의 필수값만 사용하는 실제 상품 등록 폼. -->
<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import AdminSidebar from "@/components/AdminSidebar.vue";
import { productService } from "@/services/productService";
import { adminManagementService } from "@/services/adminManagementService";

const router = useRouter();
const categories = ref([]);
const loadingCategories = ref(true);
const submitting = ref(false);
const error = ref("");
const form = reactive({
  name: "", brand: "", productType: "DOG", categoryId: "", description: "",
  foodKind: "사료", foodForm: "", mainIngredient: "", lifeStage: "", ingredientDescription: "",
  deviceKind: "", material: "", featureDescription: "",
  skuCode: "", optionKey: "기본", optionLabel: "기본 옵션", weightG: "", volumeMl: "",
  capacityMl: "", heightCm: "", price: "", costPrice: "", stock: "",
});
const isFood = computed(() => ["DOG", "CAT"].includes(form.productType));
const matchingCategories = computed(() => categories.value.filter((item) => !item.parentId));
function numberOrNull(value) { return value === "" ? null : Number(value); }
async function submit() {
  error.value = "";
  if (!form.categoryId) { error.value = "카테고리를 선택해주세요."; return; }
  const variant = {
    skuCode: form.skuCode, optionKey: form.optionKey, optionLabel: form.optionLabel,
    color: isFood.value ? null : null,
    weightG: isFood.value ? numberOrNull(form.weightG) : null,
    volumeMl: isFood.value ? numberOrNull(form.volumeMl) : null,
    capacityMl: isFood.value ? null : numberOrNull(form.capacityMl),
    heightCm: isFood.value ? null : numberOrNull(form.heightCm),
    price: numberOrNull(form.price), costPrice: numberOrNull(form.costPrice), stock: numberOrNull(form.stock),
  };
  const payload = {
    name: form.name, brand: form.brand || null, productType: form.productType, categoryId: Number(form.categoryId), description: form.description || null,
    variants: [variant], images: [],
    foodDetail: isFood.value ? { foodKind: form.foodKind || null, foodForm: form.foodForm || null, mainIngredient: form.mainIngredient || null, lifeStage: form.lifeStage || null, ingredientDescription: form.ingredientDescription || null } : null,
    deviceDetail: isFood.value ? null : { deviceKind: form.deviceKind, material: form.material || null, featureDescription: form.featureDescription || null },
  };
  submitting.value = true;
  try { await adminManagementService.createProduct(payload); router.push("/admin/products"); }
  catch (cause) { error.value = cause.message || "상품을 등록하지 못했습니다."; }
  finally { submitting.value = false; }
}
onMounted(async () => { try { categories.value = await productService.categories(); } catch (cause) { error.value = cause.message || "카테고리를 불러오지 못했습니다."; } finally { loadingCategories.value = false; } });
</script>

<template>
  <div class="admin-product-create"><AdminSidebar /><main>
    <header><small>PRODUCT REGISTRATION</small><h1>상품 등록</h1><p>상품 종류에 따라 백엔드 검증 규칙에 맞는 정보와 기본 옵션 1개를 등록합니다.</p></header>
    <form class="product-create-form" @submit.prevent="submit">
      <section><h2>기본 정보</h2><div class="form-grid">
        <label>상품 종류<select v-model="form.productType"><option value="DOG">강아지관</option><option value="CAT">고양이관</option><option value="DEVICE">식기·급수관</option></select></label>
        <label>카테고리<select v-model="form.categoryId" :disabled="loadingCategories"><option value="">카테고리 선택</option><option v-for="item in matchingCategories" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
        <label>상품명<input v-model.trim="form.name" required maxlength="200" /></label><label>브랜드<input v-model.trim="form.brand" maxlength="100" /></label>
        <label class="wide">상품 설명<textarea v-model.trim="form.description" maxlength="10000" rows="4" /></label>
      </div></section>
      <section v-if="isFood"><h2>식품 정보</h2><div class="form-grid"><label>식품 종류<input v-model.trim="form.foodKind" /></label><label>식품 형태<input v-model.trim="form.foodForm" placeholder="건식, 습식, 트릿" /></label><label>주원료<input v-model.trim="form.mainIngredient" /></label><label>생애 단계<input v-model.trim="form.lifeStage" placeholder="전연령, 퍼피" /></label><label class="wide">원료 설명<textarea v-model.trim="form.ingredientDescription" rows="3" /></label></div></section>
      <section v-else><h2>식기·급수 정보</h2><div class="form-grid"><label>기기 종류<input v-model.trim="form.deviceKind" required placeholder="급식기, 급수기, 식기" /></label><label>재질<input v-model.trim="form.material" /></label><label class="wide">특징 설명<textarea v-model.trim="form.featureDescription" rows="3" /></label></div></section>
      <section><h2>기본 옵션·재고</h2><div class="form-grid"><label>SKU 코드<input v-model.trim="form.skuCode" required /></label><label>옵션명<input v-model.trim="form.optionLabel" required /></label><label>옵션 키<input v-model.trim="form.optionKey" required /></label><label v-if="isFood">중량(g)<input v-model.number="form.weightG" type="number" min="1" /></label><label v-if="isFood">용량(ml)<input v-model.number="form.volumeMl" type="number" min="1" /></label><label v-if="!isFood">수용량(ml)<input v-model.number="form.capacityMl" type="number" min="1" /></label><label v-if="!isFood">높이(cm)<input v-model.number="form.heightCm" type="number" min="1" /></label><label>판매가<input v-model.number="form.price" type="number" min="0" required /></label><label>원가<input v-model.number="form.costPrice" type="number" min="0" required /></label><label>초기 재고<input v-model.number="form.stock" type="number" min="0" required /></label></div></section>
      <p v-if="error" class="form-error">{{ error }}</p><div class="form-actions"><button type="button" class="secondary" @click="router.back()">취소</button><button :disabled="submitting">{{ submitting ? "등록 중" : "상품 등록" }}</button></div>
    </form>
  </main></div>
</template>

<style scoped>
.admin-product-create{display:grid;grid-template-columns:260px minmax(0,1fr);min-height:100dvh;background:#f7f5f2;color:#3d281b}.admin-product-create main{padding:clamp(28px,3vw,52px) clamp(24px,4vw,72px) 56px}.admin-product-create header small{color:#a66838;font-weight:900;letter-spacing:.12em}.admin-product-create h1{margin:8px 0}.admin-product-create header p{color:#917866}.product-create-form{display:grid;gap:16px;max-width:980px;margin-top:28px}.product-create-form section{border:1px solid #e8d8ca;border-radius:14px;background:#fffdfb;padding:22px}.product-create-form h2{margin:0 0 16px;font-size:19px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.form-grid label{display:grid;gap:7px;font-weight:800;font-size:14px}.form-grid .wide{grid-column:1/-1}.form-grid input,.form-grid select,.form-grid textarea{box-sizing:border-box;width:100%;border:1px solid #d8c4b3;border-radius:8px;background:#fff;padding:11px;font:inherit}.form-error{color:#b24d37;font-weight:700}.form-actions{display:flex;justify-content:flex-end;gap:10px}.form-actions button{border:1px solid #a66b3f;border-radius:8px;background:#a66b3f;padding:12px 20px;color:#fff;font:inherit;font-weight:800}.form-actions .secondary{background:#fff;color:#765037}@media(max-width:760px){.admin-product-create{grid-template-columns:1fr}.admin-product-create main{padding:24px 16px}.form-grid{grid-template-columns:1fr}}
</style>
