<!-- 권한·경로·요청·서버 오류처럼 현재 화면을 계속 사용할 수 없는 상태를 코드별 안내 페이지로 표시합니다. -->
<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { getRoleLandingPath, readUserRole } from "@/utils/roles";

const props = defineProps({ status: { type: String, default: "404" } });
const router = useRouter();

const normalizedStatus = computed(() => String(props.status || "404").trim());

const errorContent = computed(() => {
  const contents = {
    400: {
      eyebrow: "BAD REQUEST",
      title: "잘못된 요청입니다",
      message: "요청 값이 올바르지 않거나 결제 금액 등 정보가 일치하지 않습니다.",
      description:
        "입력한 내용을 다시 확인한 뒤 시도해주세요. 문제가 계속되면 문의해주세요.",
    },
    401: {
      eyebrow: "UNAUTHORIZED",
      title: "로그인이 필요한 페이지입니다",
      message: "로그인에 실패했거나 인증 토큰이 만료되었습니다.",
      description:
        "다시 로그인한 뒤 이용해주세요. 문제가 계속되면 관리자에게 문의해주세요.",
    },
    403: {
      eyebrow: "FORBIDDEN",
      title: "접근할 수 없는 페이지입니다",
      message: "접근 권한이 부족하거나 탈퇴·비활성 상태의 계정입니다.",
      description:
        "로그인한 계정의 권한과 상태를 확인하거나 이전 페이지에서 다시 시도해주세요.",
    },
    404: {
      eyebrow: "NOT FOUND",
      title: "요청한 정보를 찾을 수 없습니다",
      message: "회원·상품·주문 등의 정보나 페이지를 찾을 수 없습니다.",
      description:
        "입력한 주소를 확인하거나 홈에서 원하는 상품과 정보를 찾아보세요.",
    },
    405: {
      eyebrow: "METHOD NOT ALLOWED",
      title: "허용되지 않은 요청 방식입니다",
      message: "현재 경로에서 지원하지 않는 요청 방식이 호출되었습니다.",
      description:
        "이전 화면으로 돌아가 다시 시도하거나 올바른 경로로 접속해주세요.",
    },
    406: {
      eyebrow: "NOT ACCEPTABLE",
      title: "요청한 응답 형식을 제공할 수 없습니다",
      message: "클라이언트가 요청한 응답 형식(Accept)과 일치하는 데이터가 없습니다.",
      description:
        "요청 형식을 조정하거나 이전 화면으로 돌아가 다시 시도해주세요.",
    },
    409: {
      eyebrow: "CONFLICT",
      title: "요청이 충돌했습니다",
      message:
        "아이디·이메일 중복 또는 재고 부족 등으로 요청을 처리할 수 없습니다.",
      description:
        "입력값을 변경하거나 재고 및 상태를 확인한 뒤 다시 시도해주세요.",
    },
    410: {
      eyebrow: "GONE",
      title: "만료된 요청입니다",
      message: "이메일 인증번호 등 유효기간이 만료된 리소스입니다.",
      description:
        "인증번호 재발송 또는 처음부터 다시 진행한 뒤 시도해주세요.",
    },
    413: {
      eyebrow: "PAYLOAD TOO LARGE",
      title: "요청 용량이 너무 큽니다",
      message: "업로드하거나 전송한 데이터의 크기가 허용 한도를 초과했습니다.",
      description:
        "파일 크기를 줄이거나 전송 항목을 나눈 뒤 다시 시도해주세요.",
    },
    415: {
      eyebrow: "UNSUPPORTED MEDIA TYPE",
      title: "지원하지 않는 파일·데이터 형식입니다",
      message: "전송한 파일이나 요청 본문(Content-Type) 형식을 처리할 수 없습니다.",
      description:
        "지원하는 형식(예: 이미지 확장자 등)으로 변경한 뒤 다시 시도해주세요.",
    },
    500: {
      eyebrow: "INTERNAL SERVER ERROR",
      title: "잠시 문제가 발생했습니다",
      message:
        "서버 오류 또는 이메일 발송·이미지 업로드 처리 중 문제가 발생했습니다.",
      description: "잠시 후 다시 시도해주세요. 같은 문제가 계속되면 문의해주세요.",
    },
    503: {
      eyebrow: "SERVICE UNAVAILABLE",
      title: "서비스를 일시적으로 이용할 수 없습니다",
      message:
        "게시글 내용 검사 또는 판매 예측 서비스 등 외부/부가 기능을 일시적으로 이용할 수 없습니다.",
      description: "잠시 후 다시 접속해주세요.",
    },
  };

  return contents[normalizedStatus.value] || contents["404"];
});

const isReloadStatus = computed(() =>
  ["500", "503"].includes(normalizedStatus.value)
);

const primaryButtonLabel = computed(() => {
  if (normalizedStatus.value === "401") return "로그인하기";
  if (isReloadStatus.value) return "다시 시도";
  return "홈으로 이동";
});

function primaryAction() {
  if (normalizedStatus.value === "401") {
    router.push("/login"); // 프로젝트의 로그인 라우트 경로에 맞게 조정하세요
    return;
  }
  if (isReloadStatus.value) {
    window.location.reload();
    return;
  }
  router.push(getRoleLandingPath(readUserRole()));
}
</script>

<template>
  <main class="container error-page">
    <section class="error-page-card">
      <div class="error-page-code" aria-hidden="true">{{ normalizedStatus }}</div>
      <small>{{ errorContent.eyebrow }}</small>
      <h1>{{ errorContent.title }}</h1>
      <strong>{{ errorContent.message }}</strong>
      <p>{{ errorContent.description }}</p>
      <div class="error-page-actions">
        <button type="button" class="secondary" @click="router.back()">
          이전 페이지
        </button>
        <button type="button" class="primary" @click="primaryAction">
          {{ primaryButtonLabel }}
        </button>
      </div>
    </section>
  </main>
</template>