<!-- # 사이드바: 회원 관리 - 실제 회원 상태·역할·등록 반려동물 수 관리 -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { adminManagementService } from "@/services/adminManagementService";
import PaginationBar from "@/components/PaginationBar.vue";
import "@/styles/admin-members.css";

const members = ref([]);
const loading = ref(false);
const error = ref("");
const search = ref("");
const statusFilter = ref("ALL");
const roleFilter = ref("ALL");
const page = ref(1);
const PAGE_SIZE = 10;

const statusLabel = { ACTIVE: "활성", INACTIVE: "비활성", KICKED: "차단", DELETED: "탈퇴" };
const roleLabel = { USER: "사용자", MANAGER: "매니저", ADMIN: "관리자" };
const filteredMembers = computed(() => members.value.filter((member) => {
  const keyword = search.value.trim().toLowerCase();
  const matchKeyword = !keyword || [member.userId, member.name, member.email, member.tel].filter(Boolean).join(" ").toLowerCase().includes(keyword);
  const matchStatus = statusFilter.value === "ALL" || member.status === statusFilter.value;
  const matchRole = roleFilter.value === "ALL" || member.role === roleFilter.value;
  return matchKeyword && matchStatus && matchRole;
}));
const pageCount = computed(() => Math.max(1, Math.ceil(filteredMembers.value.length / PAGE_SIZE)));
const pagedMembers = computed(() => filteredMembers.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE));
const stats = computed(() => {
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

  return {
    total: members.value.length,
    newMembers: members.value.filter((member) => {
      const createdAt = new Date(member.createdAt);
      return !Number.isNaN(createdAt.getTime()) && createdAt >= thirtyDaysAgo;
    }).length,
    active: members.value.filter((member) => member.status === "ACTIVE").length,
    inactive: members.value.filter((member) => member.status === "INACTIVE").length,
    kicked: members.value.filter((member) => member.status === "KICKED").length,
    dogs: members.value.reduce((sum, member) => sum + Number(member.dogCount || 0), 0),
    cats: members.value.reduce((sum, member) => sum + Number(member.catCount || 0), 0),
  };
});
const distribution = computed(() => [
  { key: "ACTIVE", label: "활성", count: stats.value.active, color: "#7d925a" },
  { key: "INACTIVE", label: "비활성", count: stats.value.inactive, color: "#e3ba8b" },
  { key: "KICKED", label: "차단", count: stats.value.kicked, color: "#cf786b" },
  { key: "DELETED", label: "탈퇴", count: members.value.filter((member) => member.status === "DELETED").length, color: "#cfc5b7" },
]);
const donutStyle = computed(() => {
  const total = stats.value.total || 1;
  let position = 0;
  const parts = distribution.value.map((item) => { const end = position + item.count / total * 100; const part = `${item.color} ${position}% ${end}%`; position = end; return part; });
  return { background: `conic-gradient(${parts.join(", ")})` };
});
const formatDate = (value) => value ? String(value).slice(0, 10).replaceAll("-", ".") : "-";
watch([search, statusFilter, roleFilter], () => { page.value = 1; });
watch(pageCount, (count) => { if (page.value > count) page.value = count; });

async function loadMembers() { loading.value = true; error.value = ""; try { const result = await adminManagementService.getMembers(); members.value = Array.isArray(result) ? result : []; page.value = 1; } catch (cause) { members.value = []; error.value = cause.message || "회원 목록을 불러오지 못했습니다."; } finally { loading.value = false; } }
async function changeStatus(member, event) { try { await adminManagementService.changeMemberStatus(member.id, event.target.value); await loadMembers(); } catch (cause) { error.value = cause.message || "회원 상태를 변경하지 못했습니다."; event.target.value = member.status; } }
async function changeRole(member, event) { try { await adminManagementService.changeMemberRole(member.id, event.target.value); await loadMembers(); } catch (cause) { error.value = cause.message || "회원 역할을 변경하지 못했습니다."; event.target.value = member.role; } }
onMounted(loadMembers);
</script>

<template>
  <main class="admin-members">
    <header class="members-heading"><small>MEMBER MANAGEMENT</small><h1>회원 관리</h1><p>관리자 전용 · 회원 상태와 역할, 등록 반려동물 현황을 관리합니다.</p></header>
    <!-- # 회원 관리: 실제 회원/반려동물 집계 -->
    <section class="member-summary"><article><small>전체 회원</small><strong>{{ stats.total.toLocaleString() }}<em>명</em></strong><p>관리자 조회 기준</p></article><article><small>신규 회원 (30일)</small><strong>{{ stats.newMembers.toLocaleString() }}<em>명</em></strong><p>가입일 기준</p></article><article><small>활성 회원</small><strong>{{ stats.active.toLocaleString() }}<em>명</em></strong><p>현재 활성 상태</p></article><article><small>비활성 회원</small><strong>{{ stats.inactive.toLocaleString() }}<em>명</em></strong><p>상태 변경 가능</p></article><article class="warning"><small>차단 회원</small><strong>{{ stats.kicked.toLocaleString() }}<em>명</em></strong><p>차단 상태 기준</p></article><article><small>등록 반려동물</small><strong>{{ (stats.dogs + stats.cats).toLocaleString() }}<em>마리</em></strong><p>강아지 {{ stats.dogs }} · 고양이 {{ stats.cats }}</p></article></section>
    <p v-if="loading" class="member-message">실제 회원 목록을 불러오는 중입니다.</p><p v-else-if="error" class="member-message error">{{ error }}</p>
    <section v-else class="member-layout">
      <article class="member-panel list-panel"><header class="panel-heading"><div><h2>회원 목록</h2><p>검색과 필터 결과를 10명씩 표시합니다.</p></div><button type="button" @click="loadMembers">새로고침</button></header><div class="member-filters"><input v-model="search" type="search" placeholder="이름 · 이메일 · 아이디 · 연락처 검색" /><select v-model="statusFilter"><option value="ALL">상태 전체</option><option v-for="(label,key) in statusLabel" :key="key" :value="key">{{ label }}</option></select><select v-model="roleFilter"><option value="ALL">역할 전체</option><option v-for="(label,key) in roleLabel" :key="key" :value="key">{{ label }}</option></select></div><div class="table-scroll"><table><thead><tr><th>아이디</th><th>이름</th><th>이메일</th><th>연락처</th><th>반려동물</th><th>역할</th><th>계정 상태</th><th>가입일</th></tr></thead><tbody><tr v-for="member in pagedMembers" :key="member.id"><td>{{ member.userId || '-' }}</td><td><b>{{ member.name || '-' }}</b></td><td>{{ member.email }}</td><td>{{ member.tel || '-' }}</td><td>강아지 {{ member.dogCount || 0 }} · 고양이 {{ member.catCount || 0 }}</td><td><select class="inline-select" :value="member.role" @change="changeRole(member,$event)"><option v-for="(label,key) in roleLabel" :key="key" :value="key">{{ label }}</option></select></td><td><select class="inline-select" :value="member.status" @change="changeStatus(member,$event)"><option v-for="(label,key) in statusLabel" :key="key" :value="key">{{ label }}</option></select></td><td>{{ formatDate(member.createdAt) }}</td></tr><tr v-if="!pagedMembers.length"><td colspan="8" class="empty-row">검색 조건에 맞는 회원이 없습니다.</td></tr></tbody></table></div><PaginationBar v-model:page="page" :total-pages="pageCount" label="회원 목록 페이지" /><p class="member-note">상태·역할 변경은 백엔드 권한 API를 호출합니다. 본인 계정 및 관리자 변경 정책은 별도 운영 규칙이 필요합니다.</p></article>
      <aside class="member-panel distribution-panel"><h2>회원 상태 분포</h2><div class="member-donut" :style="donutStyle"><span><small>전체</small><b>{{ stats.total.toLocaleString() }}명</b></span></div><ul><li v-for="item in distribution" :key="item.key"><i :style="{background:item.color}" /><span>{{ item.label }}</span><b>{{ item.count.toLocaleString() }}명</b></li></ul></aside>
    </section>
  </main>
</template>


