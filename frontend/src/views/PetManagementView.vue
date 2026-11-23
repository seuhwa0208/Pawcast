<!-- 로그인 회원의 반려동물 정보를 PETS API로 관리하는 화면입니다. -->
<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import MemberSidebar from "@/components/MemberSidebar.vue";
import { useAuthSession } from "@/stores/auth";
import { petService } from "@/services/petService";

const router = useRouter();
const { currentUserId } = useAuthSession();
const userId = computed(() => currentUserId.value);
const displayName = computed(
  () => userId.value || "포캐스트 회원",
);
const pets = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const formOpen = ref(false);
const editingId = ref(null);
const toast = ref("");
const formError = ref("");
const MAX_PET_COUNT = 3;
const form = reactive({
  name: "",
  type: "DOG",
  birth: "",
  breed: "",
  weight: "",
  allergies: [],
});
const ALLERGY_OPTIONS_BY_TYPE = {
  DOG: ["닭고기", "소고기", "오리", "연어", "양고기", "돼지고기", "유제품", "곡물"],
  CAT: ["닭고기", "참치", "연어", "소고기", "오리", "유제품", "곡물"],
};
const canAddPet = computed(() => pets.value.length < MAX_PET_COUNT);
const allergyOptions = computed(() => ALLERGY_OPTIONS_BY_TYPE[form.type] || []);

function parseAllergies(value) {
  return String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}
function toggleAllergy(value) {
  if (form.allergies.includes(value)) {
    removeAllergy(value);
    return;
  }
  form.allergies.push(value);
}
function removeAllergy(value) {
  form.allergies = form.allergies.filter((item) => item !== value);
}

// API 응답 필드명을 화면 전용 모델로 통일합니다.
function normalizePet(pet) {
  return {
    id: pet.id,
    name: pet.name,
    type: pet.petType || pet.type,
    birth: pet.birthday || pet.birth || "",
    breed: pet.breedName || pet.breed || "",
    weight: pet.weightKg ?? pet.weight ?? "",
    allergy: pet.allergies || pet.allergy || "",
  };
}
async function loadPets() {
  try {
    loading.value = true;
    const result = await petService.getPets();
    const serverPets = Array.isArray(result)
      ? result
      : result.content || result.items || result.pets || [];
    pets.value = serverPets.map(normalizePet);
  } catch (error) {
    pets.value = [];
    errorMessage.value = error.message || "반려동물 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
function openAddForm() {
  if (!canAddPet.value) {
    showToast("반려동물은 최대 3마리까지 등록할 수 있습니다.");
    return;
  }
  editingId.value = null;
  formError.value = "";
  Object.assign(form, { name: "", type: "DOG", birth: "", breed: "", weight: "", allergies: [] });
  formOpen.value = true;
}
function openEditForm(pet) {
  editingId.value = pet.id;
  formError.value = "";
  Object.assign(form, {
    name: pet.name,
    type: pet.type,
    birth: pet.birth,
    breed: pet.breed,
    weight: pet.weight,
    allergies: parseAllergies(pet.allergy),
  });
  formOpen.value = true;
}
function toApiPayload() {
  return {
    name: form.name,
    petType: form.type,
    birthday: form.birth || null,
    breedName: form.breed,
    weightKg: Number(form.weight) || null,
    allergies: form.allergies.join(", ") || null,
  };
}
async function submitPet() {
  if (!form.name.trim() || !form.breed.trim()) {
    formError.value = "이름과 품종을 모두 입력해주세요.";
    return;
  }
  if (!editingId.value && !canAddPet.value) {
    formError.value = "반려동물은 최대 3마리까지 등록할 수 있습니다.";
    return;
  }
  try {
    formError.value = "";
    if (editingId.value) {
      const updated = await petService.updatePet(editingId.value, toApiPayload());
      const index = pets.value.findIndex((pet) => pet.id === editingId.value);
      pets.value[index] = normalizePet(updated);
    } else {
      pets.value.unshift(normalizePet(await petService.addPet(toApiPayload())));
    }
    formOpen.value = false;
    showToast(editingId.value ? "반려동물 정보가 수정되었습니다." : "반려동물이 등록되었습니다.");
  } catch (error) {
    formError.value = error.message || "반려동물 정보를 저장하지 못했습니다.";
  }
}
async function removePet(pet) {
  if (!window.confirm(`${pet.name}의 정보를 삭제할까요?`)) return;
  try {
    await petService.removePet(pet.id);
    pets.value = pets.value.filter((item) => item.id !== pet.id);
    showToast("반려동물 정보가 삭제되었습니다.");
  } catch (error) {
    errorMessage.value = error.message || "반려동물 정보를 삭제하지 못했습니다.";
  }
}
function showToast(message) {
  toast.value = message;
  window.setTimeout(() => (toast.value = ""), 2200);
}
function requestSettings() {
  router.push("/mypage?verify=1");
}

onMounted(loadPets);
</script>

<template>
  <main class="container member-dashboard pet-management-page">
    <header class="dashboard-heading">
      <span>POWCAST PET PROFILE</span>
      <h1>반려동물 정보</h1>
      <p>아이의 기본 정보를 등록하고 맞춤 서비스에 활용하세요.</p>
    </header>
    <div class="dashboard-layout">
      <MemberSidebar
        :name="displayName"
        :user-id="userId"
        active="pets"
        @settings="requestSettings"
      />
      <div class="dashboard-content">
        <section class="pet-management-card">
          <header>
            <div>
              <h2>등록된 반려동물</h2>
              <p>이름, 품종, 체중과 알레르기 정보를 관리할 수 있습니다.</p>
            </div>
            <button type="button" :disabled="!canAddPet" @click="openAddForm">
              {{ canAddPet ? "+ 반려동물 추가" : "최대 3마리 등록 완료" }}
            </button>
          </header>
          <p v-if="loading" class="pet-management-state">
            정보를 불러오는 중입니다.
          </p>
          <div v-else-if="pets.length" class="pet-management-list">
            <article v-for="pet in pets" :key="pet.id">
              <div class="pet-identity">
                <h3>{{ pet.name }}</h3>
                <span>{{ pet.type === "CAT" ? "고양이" : "강아지" }}</span>
              </div>
              <dl class="pet-meta">
                <div><dt>품종</dt><dd>{{ pet.breed || "미등록" }}</dd></div>
                <div><dt>생일</dt><dd>{{ pet.birth || "미등록" }}</dd></div>
                <div><dt>알레르기</dt><dd>{{ pet.allergy || "없음" }}</dd></div>
                <div><dt>체중</dt><dd>{{ pet.weight ? `${pet.weight}kg` : "미등록" }}</dd></div>
              </dl>
              <div class="pet-management-actions">
                <button type="button" @click="openEditForm(pet)">수정</button>
                <button type="button" @click="removePet(pet)">삭제</button>
              </div>
            </article>
          </div>
          <p
            v-else
            class="pet-management-state"
            :class="{ error: errorMessage }"
          >
            {{ errorMessage || "등록된 반려동물이 없습니다." }}
          </p>
        </section>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="formOpen"
        class="common-modal-backdrop"
        @click.self="formOpen = false"
      >
        <form class="common-modal pet-form-modal" @submit.prevent="submitPet">
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="formOpen = false"
          >
            ×
          </button>
          <h2>{{ editingId ? "반려동물 정보 수정" : "반려동물 추가" }}</h2>
          <div class="pet-form-grid">
            <label>이름 *<input v-model.trim="form.name" maxlength="20" required /></label>
            <label
              >종류 *<select v-model="form.type" class="pet-type-select" required>
                <option value="DOG">강아지</option>
                <option value="CAT">고양이</option>
              </select></label
            >
            <label>품종 *<input v-model.trim="form.breed" maxlength="50" required placeholder="예) 포메라니안, 믹스견" /></label>
            <label>생일<input v-model="form.birth" type="date" /></label>
            <label
              >체중(kg)<input
                v-model="form.weight"
                type="number"
                min="0.01"
                max="999.99"
                step=".01"
            /></label>
            <div class="pet-allergy-field">
              <span>알레르기 원료 선택</span>
              <div class="pet-allergy-options" role="group" aria-label="알레르기 원료">
                <label v-for="allergy in allergyOptions" :key="allergy">
                  <input
                    type="checkbox"
                    :checked="form.allergies.includes(allergy)"
                    @change="toggleAllergy(allergy)"
                  />
                  <span>{{ allergy }}</span>
                </label>
              </div>
              <div v-if="form.allergies.length" class="pet-allergy-tags">
                <button
                  v-for="allergy in form.allergies"
                  :key="allergy"
                  type="button"
                  @click="removeAllergy(allergy)"
                >
                  {{ allergy }} <span aria-hidden="true">×</span>
                </button>
              </div>
            </div>
          </div>
          <p v-if="formError" class="pet-form-error">{{ formError }}</p>
          <div class="common-modal-actions">
            <button type="button" class="secondary" @click="formOpen = false">
              취소
            </button>
            <button class="primary">
              {{ editingId ? "수정하기" : "등록하기" }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>
    <Transition name="toast">
      <div v-if="toast" class="site-toast" role="status">✓ {{ toast }}</div>
    </Transition>
  </main>
</template>
