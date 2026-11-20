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
const form = reactive({ name: "", type: "DOG", birth: "", breed: "", weight: "", allergy: "" });
const BREEDS_BY_TYPE = {
  DOG: ["말티즈", "푸들", "말티푸", "포메라니안", "비숑 프리제", "시츄", "치와와", "요크셔테리어", "웰시코기", "골든리트리버", "래브라도리트리버", "진돗개", "시바견", "프렌치불도그", "믹스견"],
  CAT: ["코리안 숏헤어", "브리티시 숏헤어", "러시안 블루", "샴", "페르시안", "스코티시 폴드", "아메리칸 숏헤어", "노르웨이 숲", "메인쿤", "랙돌", "아비시니안", "벵갈", "먼치킨", "스핑크스", "믹스묘"],
};
const breedOptions = computed(() => BREEDS_BY_TYPE[form.type] || []);

function handleTypeChange() {
  form.breed = "";
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
    const serverPets = result.content || result.items || result.pets || [];
    pets.value = serverPets.map(normalizePet);
  } catch (error) {
    pets.value = [];
    errorMessage.value = error.message || "반려동물 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
function openAddForm() {
  editingId.value = null;
  Object.assign(form, { name: "", type: "DOG", birth: "", breed: "", weight: "", allergy: "" });
  formOpen.value = true;
}
function openEditForm(pet) {
  editingId.value = pet.id;
  Object.assign(form, pet);
  formOpen.value = true;
}
function toApiPayload() {
  return {
    name: form.name,
    petType: form.type,
    birthday: form.birth || null,
    breedName: form.breed,
    weightKg: Number(form.weight) || null,
    allergies: form.allergy || null,
  };
}
async function submitPet() {
  if (!form.name.trim() || !form.breed.trim()) return;
  try {
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
    errorMessage.value = error.message || "반려동물 정보를 저장하지 못했습니다.";
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
            <button type="button" @click="openAddForm">
              + 반려동물 추가
            </button>
          </header>
          <p v-if="loading" class="pet-management-state">
            정보를 불러오는 중입니다.
          </p>
          <div v-else-if="pets.length" class="pet-management-list">
            <article v-for="pet in pets" :key="pet.id">
              <div class="pet-avatar">
                {{ pet.type === "CAT" ? "🐱" : "🐶" }}
              </div>
              <div>
                <small>{{ pet.type === "CAT" ? "고양이" : "강아지" }}</small>
                <h3>{{ pet.name }}</h3>
                <p>
                  {{ pet.breed }}
                  <template v-if="pet.weight">· {{ pet.weight }}kg</template>
                </p>
                <span v-if="pet.birth">생일 {{ pet.birth }}</span>
                <span v-if="pet.allergy">알레르기: {{ pet.allergy }}</span>
              </div>
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
              >종류 *<select v-model="form.type" required @change="handleTypeChange">
                <option value="DOG">강아지</option>
                <option value="CAT">고양이</option>
              </select></label
            >
            <label>품종 *<select v-model="form.breed" required>
              <option disabled value="">품종을 선택해주세요</option>
              <option v-if="form.breed && !breedOptions.includes(form.breed)" :value="form.breed">{{ form.breed }}</option>
              <option v-for="breed in breedOptions" :key="breed" :value="breed">{{ breed }}</option>
            </select></label>
            <label>생일<input v-model="form.birth" type="date" /></label>
            <label
              >체중(kg)<input
                v-model="form.weight"
                type="number"
                min="0.01"
                max="999.99"
                step=".01"
            /></label>
            <label>알레르기<input v-model.trim="form.allergy" /></label>
          </div>
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
