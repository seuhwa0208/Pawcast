<!-- 권한·경로·서버 오류처럼 현재 화면을 계속 사용할 수 없는 상태를 코드별 안내 페이지로 표시합니다. -->
<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { getRoleLandingPath, readUserRole } from "@/utils/roles";

const props = defineProps({ status: { type: String, default: "404" } });
const router = useRouter();

const errorContent = computed(() => {
  const contents = {
    403: {
      eyebrow: "ACCESS DENIED",
      title: "접근할 수 없는 페이지입니다",
      message: "이 페이지를 볼 수 있는 권한이 없습니다.",
      description:
        "로그인한 계정의 권한을 확인하거나 이전 페이지에서 다시 시도해주세요.",
    },
    404: {
      eyebrow: "PAGE NOT FOUND",
      title: "페이지를 찾을 수 없습니다",
      message: "주소가 잘못되었거나 페이지가 이동되었을 수 있습니다.",
      description: "입력한 주소를 확인하거나 홈에서 원하는 상품을 찾아보세요.",
    },
    500: {
      eyebrow: "SERVER ERROR",
      title: "잠시 문제가 발생했습니다",
      message: "요청을 처리하는 중 서버 오류가 발생했습니다.",
      description: "잠시 후 다시 시도해주세요. 같은 문제가 계속되면 문의해주세요.",
    },
  };
  return contents[props.status] || contents[404];
});

function primaryAction() {
  if (props.status === "500") {
    window.location.reload();
    return;
  }
  router.push(getRoleLandingPath(readUserRole()));
}
</script>

<template>
  <main class="container error-page">
    <section class="error-page-card">
      <div class="error-page-code" aria-hidden="true">{{ status }}</div>
      <small>{{ errorContent.eyebrow }}</small>
      <h1>{{ errorContent.title }}</h1>
      <strong>{{ errorContent.message }}</strong>
      <p>{{ errorContent.description }}</p>
      <div class="error-page-actions">
        <button type="button" class="secondary" @click="router.back()">
          이전 페이지
        </button>
        <button type="button" class="primary" @click="primaryAction">
          {{ status === "500" ? "다시 시도" : "홈으로 이동" }}
        </button>
      </div>
    </section>
  </main>
</template>
