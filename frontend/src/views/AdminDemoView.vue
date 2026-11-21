<!-- 시연용 관리자 보조 기능: 백엔드 모듈이 준비되기 전 조작 흐름을 확인합니다. -->
<script setup>
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import AdminSidebar from "@/components/AdminSidebar.vue";

const route = useRoute();
const isCategory = computed(() => route.params.tool === "category");

const categories = ref(["강아지관", "고양이관", "식기·급수관"]);
const categoryName = ref("");
const maintenance = ref(false);
const notice = ref("");

function addCategory() {
  const trimmed = categoryName.value.trim();
  if (!trimmed) return;

  categories.value.push(trimmed);
  categoryName.value = "";
  notice.value = "카테고리가 시연 화면에 추가되었습니다.";
}

function saveSettings() {
  notice.value = "환경 설정이 시연용 브라우저 상태에 저장되었습니다.";
}
</script>

<template>
  <div class="demo">
    <AdminSidebar />

    <main>
      <header>
        <small>ADMIN TOOL</small>
        <h1>{{ isCategory ? "상품 등록" : "환경 설정" }}</h1>
        <p>백엔드 API 준비 상태에 따라 저장 기능을 연결합니다.</p>
      </header>

      <section v-if="isCategory">
        <h2>상품 카테고리</h2>
        <form @submit.prevent="addCategory">
          <input v-model="categoryName" placeholder="카테고리 이름" />
          <button type="submit">추가</button>
        </form>
        <ul>
          <li v-for="item in categories" :key="item">{{ item }}</li>
        </ul>
      </section>

      <section v-else>
        <h2>운영 환경</h2>
        <label>
          <input v-model="maintenance" type="checkbox" />
          점검 모드 표시
        </label>
        <button type="button" @click="saveSettings">설정 저장</button>
      </section>

      <p v-if="notice" class="notice">{{ notice }}</p>
    </main>
  </div>
</template>

<style scoped>
.demo {
  background: #f7f5f2;
  color: #3d281b;
}

.demo main {
  min-width: 0;
}

small {
  color: #a66838;
  font-weight: 900;
  letter-spacing: 0.12em;
}

h1 {
  margin: 8px 0;
}

.demo main > header p {
  color: #917866;
}

.demo section {
  display: grid;
  gap: 16px;
  max-width: 650px;
  margin-top: 28px;
  border: 1px solid #e8d8ca;
  border-radius: 16px;
  background: #fff;
  padding: 24px;
}

.demo form {
  display: flex;
  gap: 8px;
}

.demo input {
  border: 1px solid #dbc8b6;
  border-radius: 9px;
  padding: 12px;
  font: inherit;
}

.demo section > button,
.demo form button {
  width: max-content;
  border: 1px solid #a66b3f;
  background: #a66b3f;
  color: #fff;
}

.demo ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.demo li {
  border-bottom: 1px solid #f0e5dc;
  padding: 10px 0;
}

.notice {
  color: #3e7a4b;
}
</style>