<!-- 관리자 상품 등록: 실제 multipart API와 SKU 분류 규칙을 연결합니다. -->
<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { adminManagementService } from "@/services/adminManagementService";

const router = useRouter();
const submitting = ref(false);
const error = ref("");
const imageInput = ref(null);
const selectedImages = ref([]);
const skuGuideOpen = ref(false);

const BRAND_CODES = [
  ["ACN", "아카나"], ["ANF", "ANF"], ["BBO", "바비온"], ["CAT", "캣잇"], ["DDP", "딩동펫"], ["HGN", "하겐"], ["HLS", "힐스"], ["HRM", "하림펫푸드"], ["HRP", "호머런펫"], ["INB", "이나바"], ["INS", "인스팅트"], ["LSP", "리스펫"], ["NBL", "내추럴발란스"], ["NTP", "뉴트리플랜"], ["ORJ", "오리젠"], ["PKT", "PETKIT"], ["PLB", "PETLIBRO"], ["PON", "퓨리나 원"], ["PPL", "퓨리나 프로플랜"], ["PSF", "펫세이프"], ["RYC", "로얄캐닌"], ["WLN", "웰니스"], ["XMI", "샤오미"],
].map(([code, name]) => ({ code, name }));

const CATEGORY_RULES = [
  ["DDF",3,"DOG","강아지관","사료","건식 사료","00001","1KG"],["DWF",4,"DOG","강아지관","사료","습식 / 화식","00101","001"],["DFF",5,"DOG","강아지관","사료","기능성 사료","00201","001"],["DAF",6,"DOG","강아지관","사료","연령별 사료","00301","001"],["DDT",8,"DOG","강아지관","간식","덴탈 간식","00401","001"],["DFT",9,"DOG","강아지관","간식","동결건조 / 트릿","00501","001"],["DJT",10,"DOG","강아지관","간식","육포 / 저키","00601","001"],["DPT",11,"DOG","강아지관","간식","츄르 / 파우치","00701","001"],["DJS",13,"DOG","강아지관","영양제","관절 / 뼈","00801","001"],["DSS",14,"DOG","강아지관","영양제","피부 / 모질","00901","001"],["DGS",15,"DOG","강아지관","영양제","장 / 유산균","01001","001"],["DES",16,"DOG","강아지관","영양제","눈 / 종합영양","01101","001"],
  ["CDF",19,"CAT","고양이관","사료","건식 사료","01201","1KG"],["CWF",20,"CAT","고양이관","사료","습식 / 화식","01301","001"],["CFF",21,"CAT","고양이관","사료","기능성 사료","01401","001"],["CAF",22,"CAT","고양이관","사료","연령별 사료","01501","001"],["CDT",24,"CAT","고양이관","간식","덴탈 간식","01601","001"],["CFT",25,"CAT","고양이관","간식","동결건조 / 트릿","01701","001"],["CJT",26,"CAT","고양이관","간식","육포 / 저키","01801","001"],["CPT",27,"CAT","고양이관","간식","츄르 / 파우치","01901","001"],["CJS",29,"CAT","고양이관","영양제","관절 / 뼈","02001","001"],["CSS",30,"CAT","고양이관","영양제","피부 / 모질","02101","001"],["CGS",31,"CAT","고양이관","영양제","장 / 유산균","02201","001"],["CES",32,"CAT","고양이관","영양제","눈 / 종합영양","02301","001"],
  ["AFD",35,"DEVICE","식기·급수관","급식기","자동 급식기","02401","001"],["MFD",36,"DEVICE","식기·급수관","급식기","수동 급식기","02431","001"],["PFD",37,"DEVICE","식기·급수관","급식기","휴대용 급식기","02461","001"],["WTR",39,"DEVICE","식기·급수관","급수기","자동 급수기","02491","001"],["PWT",40,"DEVICE","식기·급수관","급수기","휴대용 급수기","02521","001"],["CBW",42,"DEVICE","식기·급수관","일반 식기","도자기 식기","02551","001"],["SBW",43,"DEVICE","식기·급수관","일반 식기","스테인리스 식기","02581","001"],["HBW",44,"DEVICE","식기·급수관","일반 식기","높이 조절 식기","02611","001"],["WFL",46,"DEVICE","식기·급수관","정수 / 필터","정수 필터","02641","001"],["RFL",47,"DEVICE","식기·급수관","정수 / 필터","교체용 필터","02671","001"],["CLN",48,"DEVICE","식기·급수관","정수 / 필터","세척 용품","02701","001"],
].map(([prefix, categoryId, productType, hall, parent, name, sequence, suffix]) => ({ prefix, categoryId, productType, hall, parent, name, sequence, suffix }));

const form = reactive({ name: "", brandCode: "RYC", categoryId: "3", description: "", mainIngredient: "닭고기", lifeStage: "전연령", ingredientDescription: "", material: "스테인리스", featureDescription: "" });
const options = ref([{ optionLabel: "기본 옵션", optionKey: "BASIC", skuSuffix: "1KG", weightG: "1000", volumeMl: "", capacityMl: "", heightCm: "", price: "", costPrice: "", stock: "" }]);
const selectedRule = computed(() => CATEGORY_RULES.find((rule) => rule.categoryId === Number(form.categoryId)) || CATEGORY_RULES[0]);
const isFood = computed(() => selectedRule.value.productType !== "DEVICE");
const selectedBrand = computed(() => BRAND_CODES.find((brand) => brand.code === form.brandCode));
const foodIngredients = ["닭고기", "오리", "연어", "소고기", "양고기", "참치", "곡물", "채소", "유산균"];
const lifeStages = ["전연령", "퍼피 / 키튼", "어덜트", "시니어"];
const materials = ["스테인리스", "도자기", "실리콘", "플라스틱", "유리", "복합 소재"];

watch(selectedRule, (rule) => {
  options.value.forEach((option, index) => {
    option.skuSuffix = index === 0 ? rule.suffix : String(index + 1).padStart(3, "0");
    option.optionKey = index === 0 ? "BASIC" : `OPTION_${index + 1}`;
  });
}, { immediate: true });

function skuFor(option, index) {
  const sequence = String(Number(selectedRule.value.sequence) + index).padStart(5, "0");
  return `${selectedRule.value.prefix}-${form.brandCode}-${sequence}-${(option.skuSuffix || String(index + 1).padStart(3, "0")).toUpperCase().replaceAll(/[^A-Z0-9]/g, "")}`;
}
function addOption() { options.value.push({ optionLabel: "", optionKey: `OPTION_${options.value.length + 1}`, skuSuffix: String(options.value.length + 1).padStart(3, "0"), weightG: "", volumeMl: "", capacityMl: "", heightCm: "", price: "", costPrice: "", stock: "" }); }
function removeOption(index) { if (options.value.length > 1) options.value.splice(index, 1); }
function numberOrNull(value) { return value === "" || value == null ? null : Number(value); }

function onImagesChange(event) {
  error.value = "";
  for (const file of Array.from(event.target.files ?? [])) {
    const ext = file.name.split(".").pop()?.toLowerCase();
    if (!["jpg", "jpeg", "png", "webp"].includes(ext) || file.size === 0 || file.size > 5 * 1024 * 1024) { error.value = "이미지는 JPG, JPEG, PNG, WebP 형식이며 한 장당 5MB 이하여야 합니다."; continue; }
    selectedImages.value.push({ file, previewUrl: URL.createObjectURL(file) });
  }
  if (imageInput.value) imageInput.value.value = "";
}
function removeImage(index) { const [item] = selectedImages.value.splice(index, 1); if (item) URL.revokeObjectURL(item.previewUrl); }
onBeforeUnmount(() => selectedImages.value.forEach((item) => URL.revokeObjectURL(item.previewUrl)));

async function submit() {
  error.value = "";
  if (!form.name.trim()) { error.value = "상품명을 입력해주세요."; return; }
  if (!selectedImages.value.length) { error.value = "대표 이미지를 한 장 이상 첨부해주세요."; return; }
  const variants = options.value.map((option, index) => ({ skuCode: skuFor(option, index), optionKey: option.optionKey, optionLabel: option.optionLabel, color: null, weightG: isFood.value ? numberOrNull(option.weightG) : null, volumeMl: isFood.value ? numberOrNull(option.volumeMl) : null, capacityMl: isFood.value ? null : numberOrNull(option.capacityMl), heightCm: isFood.value ? null : numberOrNull(option.heightCm), price: numberOrNull(option.price), costPrice: numberOrNull(option.costPrice), stock: numberOrNull(option.stock) }));
  if (variants.some((variant) => !variant.optionLabel || variant.price == null || variant.costPrice == null || variant.stock == null)) { error.value = "각 옵션의 이름, 판매가, 원가, 재고를 입력해주세요."; return; }
  const payload = { name: form.name, brand: selectedBrand.value?.name || null, productType: selectedRule.value.productType, categoryId: selectedRule.value.categoryId, description: form.description || null, variants, images: [], foodDetail: isFood.value ? { foodKind: selectedRule.value.parent, foodForm: selectedRule.value.name, mainIngredient: form.mainIngredient, lifeStage: form.lifeStage, ingredientDescription: form.ingredientDescription || null } : null, deviceDetail: isFood.value ? null : { deviceKind: selectedRule.value.parent, material: form.material, featureDescription: form.featureDescription || null } };
  const formData = new FormData();
  formData.append("request", new Blob([JSON.stringify(payload)], { type: "application/json" }));
  selectedImages.value.forEach(({ file }) => formData.append("images", file, file.name));
  submitting.value = true;
  try { await adminManagementService.createProduct(formData); await router.push("/admin/products"); }
  catch (cause) { error.value = cause.message || "상품을 등록하지 못했습니다."; }
  finally { submitting.value = false; }
}
</script>

<template>
  <main class="admin-product-create">
    <header class="create-heading"><small>PRODUCT REGISTRATION</small><h1>상품 등록</h1><p>분류와 브랜드를 선택하면 상품 유형과 SKU 기본값이 자동으로 적용됩니다.</p></header>
    <form class="create-layout" @submit.prevent="submit">
      <section class="create-main">
        <article class="form-panel"><h2>상품 기본 정보</h2><div class="form-grid"><label>상품관<select v-model="form.categoryId"><optgroup v-for="hall in ['강아지관','고양이관','식기·급수관']" :key="hall" :label="hall"><option v-for="rule in CATEGORY_RULES.filter((item) => item.hall === hall)" :key="rule.categoryId" :value="rule.categoryId">{{ rule.parent }} · {{ rule.name }}</option></optgroup></select></label><label>브랜드<select v-model="form.brandCode"><option v-for="brand in BRAND_CODES" :key="brand.code" :value="brand.code">{{ brand.name }} ({{ brand.code }})</option></select></label><label class="wide">상품명<input v-model.trim="form.name" maxlength="200" placeholder="상품명을 입력해주세요" /></label><label class="wide">상품 설명<textarea v-model.trim="form.description" rows="4" maxlength="10000" placeholder="상품 상세 설명을 입력해주세요" /></label></div><div class="rule-summary"><b>{{ selectedRule.hall }} · {{ selectedRule.parent }} · {{ selectedRule.name }}</b><span>상품 유형 {{ selectedRule.productType }} · 분류 코드 {{ selectedRule.prefix }}</span></div></article>
        <article class="form-panel"><h2>{{ isFood ? '식품 상세 정보' : '식기·급수 상세 정보' }}</h2><div v-if="isFood" class="form-grid"><label>식품 종류<input :value="selectedRule.parent" readonly /></label><label>식품 형태<input :value="selectedRule.name" readonly /></label><label>주원료<select v-model="form.mainIngredient"><option v-for="ingredient in foodIngredients" :key="ingredient">{{ ingredient }}</option></select></label><label>생애 단계<select v-model="form.lifeStage"><option v-for="stage in lifeStages" :key="stage">{{ stage }}</option></select></label><label class="wide">원료 설명<textarea v-model.trim="form.ingredientDescription" rows="3" placeholder="원료 및 알레르기 안내" /></label></div><div v-else class="form-grid"><label>기기 종류<input :value="selectedRule.parent" readonly /></label><label>세부 분류<input :value="selectedRule.name" readonly /></label><label>재질<select v-model="form.material"><option v-for="item in materials" :key="item">{{ item }}</option></select></label><label class="wide">특징 설명<textarea v-model.trim="form.featureDescription" rows="3" placeholder="기능, 사용 방법, 주의사항" /></label></div></article>
        <article class="form-panel"><div class="panel-title"><div><h2>옵션 · 재고</h2><p>옵션을 추가하면 SKU의 일련번호가 자동 증가합니다.</p></div><button type="button" class="outline" @click="addOption">+ 옵션 추가</button></div><div class="option-list"><article v-for="(option, index) in options" :key="index" class="option-card"><header><b>옵션 {{ index + 1 }}</b><code>{{ skuFor(option, index) }}</code><button type="button" :disabled="options.length === 1" @click="removeOption(index)">삭제</button></header><div class="form-grid option-grid"><label>옵션명<input v-model.trim="option.optionLabel" placeholder="예: 1.8kg" /></label><label>옵션 키<input v-model.trim="option.optionKey" placeholder="예: WEIGHT_1800" /></label><label>SKU 끝 코드<input v-model.trim="option.skuSuffix" placeholder="예: 1KG 또는 001" /></label><label v-if="isFood">중량(g)<input v-model.number="option.weightG" type="number" min="1" /></label><label v-if="isFood">용량(ml)<input v-model.number="option.volumeMl" type="number" min="1" /></label><label v-if="!isFood">수용량(ml)<input v-model.number="option.capacityMl" type="number" min="1" /></label><label v-if="!isFood">높이(cm)<input v-model.number="option.heightCm" type="number" min="1" /></label><label>판매가<input v-model.number="option.price" type="number" min="0" /></label><label>원가<input v-model.number="option.costPrice" type="number" min="0" /></label><label>초기 재고<input v-model.number="option.stock" type="number" min="0" /></label></div></article></div></article>
        <details class="sku-guide" :open="skuGuideOpen" @toggle="skuGuideOpen = $event.currentTarget.open"><summary>SKU 규칙 보기</summary><p><b>형식:</b> 분류코드 - 브랜드코드 - 5자리 일련번호 - 옵션코드</p><p><b>예시:</b> {{ selectedRule.prefix }}-{{ form.brandCode }}-{{ selectedRule.sequence }}-{{ selectedRule.suffix }}</p><p>분류 코드와 시작 일련번호는 제공한 SKU 표를 따르며, 옵션 추가 시 일련번호가 1씩 증가합니다.</p></details>
      </section>
      <aside class="image-panel"><h2>상품 이미지</h2><p>첫 번째 첨부 이미지는 대표 이미지로 저장되어 상품 상세와 카테고리 목록에 표시됩니다.</p><label class="upload-button" for="product-images">이미지 첨부</label><input id="product-images" ref="imageInput" class="file-input" type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" multiple :disabled="submitting" @change="onImagesChange" /><small>JPG · JPEG · PNG · WebP / 장당 최대 5MB / 최대 20장</small><ul v-if="selectedImages.length" class="image-preview-list"><li v-for="(image,index) in selectedImages" :key="image.previewUrl"><img :src="image.previewUrl" :alt="image.file.name" /><b v-if="index===0">대표 이미지</b><span>{{ image.file.name }}</span><button type="button" @click="removeImage(index)">삭제</button></li></ul><p v-else class="image-empty">대표 이미지를 먼저 첨부해주세요.</p><p v-if="error" class="form-error">{{ error }}</p><div class="actions"><button type="button" class="secondary" :disabled="submitting" @click="router.back()">취소</button><button type="submit" :disabled="submitting">{{ submitting ? '등록 중' : '상품 등록' }}</button></div></aside>
    </form>
  </main>
</template>

<style scoped>
.admin-product-create{min-height:100dvh;box-sizing:border-box;padding:34px;background:#f7f5f1;color:#3d281b}.create-heading small{color:#a66838;font-weight:900;letter-spacing:.12em}.create-heading h1{margin:8px 0;font-size:36px}.create-heading p,.form-panel p,.image-panel p{margin:0;color:#917866}.create-layout{display:grid;grid-template-columns:minmax(0,2fr) minmax(280px,1fr);gap:18px;max-width:1380px;margin-top:27px}.create-main{display:grid;gap:18px}.form-panel,.image-panel,.sku-guide{border:1px solid #eadfd5;border-radius:18px;background:#fffdfb;padding:22px}.form-panel h2,.image-panel h2{margin:0 0 16px;font-size:20px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.form-grid label{display:grid;gap:7px;color:#624531;font-size:13px;font-weight:800}.form-grid .wide{grid-column:1/-1}.form-grid input,.form-grid select,.form-grid textarea{width:100%;box-sizing:border-box;border:1px solid #dcc8b7;border-radius:9px;background:#fff;padding:11px 12px;color:#412c1d;font:inherit}.form-grid input[readonly]{background:#fbf4ed;color:#8c705c}.rule-summary{display:flex;flex-wrap:wrap;gap:10px;margin-top:17px;border-radius:10px;background:#fff5e9;padding:11px 13px;color:#8a603d;font-size:13px}.panel-title,.option-card header,.actions{display:flex;align-items:center;justify-content:space-between;gap:10px}.panel-title p{font-size:13px}.outline,.option-card header button,.image-preview-list button{border:1px solid #d8bda9;border-radius:8px;background:#fff;color:#805237;padding:8px 11px;font:inherit;font-size:13px;font-weight:800;cursor:pointer}.option-list{display:grid;gap:12px;margin-top:17px}.option-card{border:1px solid #eee1d7;border-radius:12px;padding:15px}.option-card header{margin-bottom:13px}.option-card code{overflow:hidden;color:#a66535;text-overflow:ellipsis;white-space:nowrap;font-size:12px}.option-card header button:disabled{opacity:.4;cursor:not-allowed}.sku-guide summary{cursor:pointer;color:#8d5734;font-weight:900}.sku-guide p{margin:9px 0 0;color:#735a49;font-size:13px}.image-panel{align-self:start}.image-panel>small{display:block;margin-top:9px;color:#987d69;font-size:12px}.upload-button{display:block;margin-top:19px;border-radius:9px;background:#a66b3f;padding:12px;color:#fff;text-align:center;font-weight:900;cursor:pointer}.file-input{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)}.image-preview-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;margin:18px 0;padding:0;list-style:none}.image-preview-list li{position:relative;display:grid;gap:6px;min-width:0;border:1px solid #eadfd5;border-radius:9px;padding:7px}.image-preview-list img{width:100%;aspect-ratio:1;object-fit:cover;border-radius:6px}.image-preview-list b{position:absolute;top:11px;left:11px;border-radius:5px;background:#a66b3f;padding:3px 5px;color:#fff;font-size:10px}.image-preview-list span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:11px}.image-empty{margin:18px 0!important;border:1px dashed #dfcbb9;border-radius:9px;padding:22px;text-align:center;font-size:13px}.form-error{margin-top:14px!important;color:#b34f3c!important;font-weight:800}.actions{margin-top:20px}.actions button{flex:1;border:1px solid #a66b3f;border-radius:9px;background:#a66b3f;padding:12px;color:#fff;font:inherit;font-weight:900;cursor:pointer}.actions .secondary{background:#fff;color:#805237}.actions button:disabled{opacity:.55;cursor:not-allowed}@media(max-width:980px){.create-layout{grid-template-columns:1fr}.image-panel{order:-1}}@media(max-width:620px){.admin-product-create{padding:24px 16px}.form-grid{grid-template-columns:1fr}.option-card header{align-items:flex-start;flex-direction:column}.image-preview-list{grid-template-columns:repeat(3,minmax(0,1fr))}}
</style>
