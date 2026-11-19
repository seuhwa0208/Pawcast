<!-- 회원 프로필·배송지 저장과 탈퇴 처리를 담당하는 설정 화면입니다. -->
<script setup>
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";
import {
  getTemporaryMember,
  removeTemporaryMember,
  updateTemporaryMember,
} from "@/data/temporaryMembers";
import { loginTemporaryMember } from "@/data/temporaryMembers";
import { changeMemberPassword, withdrawMember } from "@/services/memberService";
import MemberSidebar from "@/components/MemberSidebar.vue";

const router = useRouter();
const { accessToken, setAccessToken } = useAuthSession();
const temporaryId = computed(() =>
  accessToken.value.startsWith("temporary-member:")
    ? accessToken.value.split(":").slice(1).join(":")
    : "",
);
const storedMember = getTemporaryMember(temporaryId.value);
const profile = reactive({
  username: storedMember?.username || "포캐스트 회원",
  userId: storedMember?.userId || "member",
  email: storedMember?.email || "",
  tel: formatTel(storedMember?.tel || ""),
  marketing: storedMember?.agreements?.marketing ?? false,
});
const profileSavedOpen = ref(false);
const addressFormOpen = ref(false);
const addressError = ref("");
const withdrawOpen = ref(false);
const withdrawPassword = ref("");
const withdrawMessage = ref("");
const withdrawLoading = ref(false);
const passwordForm = reactive({ current: "", next: "", confirm: "" });
const passwordMessage = ref("");
const passwordMessageType = ref("");
const passwordChanging = ref(false);
// 사용자별 키를 분리해 브라우저 한 대에서 여러 임시 계정의 부가정보가 섞이지 않게 합니다.
const dataKey = `pawcast-mypage:${profile.userId}`;
const saved = JSON.parse(localStorage.getItem(dataKey) || "{}");
const demoExtras =
  profile.userId === "test01"
    ? {
        addresses: [
          {
            id: "demo-address-1",
            name: "우리 집",
            recipient: "이수미",
            zipCode: "21390",
            address: "인천광역시 부평구 경원대로 1366",
            detailAddress: "101동 1203호",
            tel: "010-1234-5678",
          },
        ],
      }
    : { addresses: [] };
const shouldSeedDemoExtras =
  profile.userId === "test01" && !saved.demoExtrasSeeded;
const addresses = ref(
  shouldSeedDemoExtras
    ? [...(saved.addresses || []), ...demoExtras.addresses]
    : saved.addresses || [],
);
if (shouldSeedDemoExtras) {
  localStorage.setItem(
    dataKey,
    JSON.stringify({
      ...saved,
      addresses: addresses.value,
      demoExtrasSeeded: true,
    }),
  );
}
const addressForm = reactive({
  name: "",
  recipient: "",
  zipCode: "",
  address: "",
  detailAddress: "",
  tel: "",
});

function formatTel(value) {
  const numbers = String(value).replace(/\D/g, "").slice(0, 11);
  if (numbers.length <= 3) return numbers;
  if (numbers.length <= 7) return `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
  return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7)}`;
}
function persistExtra() {
  // 배송지를 저장할 때 PETS 화면/API가 관리하는 반려동물 데이터를 덮어쓰지 않습니다.
  const current = JSON.parse(localStorage.getItem(dataKey) || "{}");
  localStorage.setItem(
    dataKey,
    JSON.stringify({
      ...current,
      addresses: addresses.value,
      demoExtrasSeeded:
        current.demoExtrasSeeded || saved.demoExtrasSeeded || shouldSeedDemoExtras,
    }),
  );
}
function saveProfile() {
  if (
    !profile.username.trim() ||
    !/^\S+@\S+\.\S+$/.test(profile.email) ||
    !/^010-\d{4}-\d{4}$/.test(profile.tel)
  )
    return;
  if (temporaryId.value)
    updateTemporaryMember(temporaryId.value, {
      username: profile.username,
      email: profile.email,
      tel: profile.tel.replace(/\D/g, ""),
      agreements: {
        ...(storedMember?.agreements || {}),
        marketing: profile.marketing,
      },
    });
  profileSavedOpen.value = true;
}
async function changePassword() {
  passwordMessage.value = "";
  if (!passwordForm.current) {
    passwordMessage.value = "현재 비밀번호를 입력해주세요.";
    passwordMessageType.value = "error";
    return;
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/.test(passwordForm.next)) {
    passwordMessage.value = "새 비밀번호는 영문·숫자·특수문자를 포함한 8~20자여야 합니다.";
    passwordMessageType.value = "error";
    return;
  }
  if (passwordForm.next !== passwordForm.confirm) {
    passwordMessage.value = "새 비밀번호 확인이 일치하지 않습니다.";
    passwordMessageType.value = "error";
    return;
  }
  try {
    passwordChanging.value = true;
    if (temporaryId.value) {
      if (!loginTemporaryMember(temporaryId.value, passwordForm.current)) throw new Error("현재 비밀번호가 일치하지 않습니다.");
      updateTemporaryMember(temporaryId.value, { password: passwordForm.next });
    } else {
      await changeMemberPassword({
        password: passwordForm.current,
        newPassword: passwordForm.next,
        newPasswordCheck: passwordForm.confirm,
      });
    }
    Object.assign(passwordForm, { current: "", next: "", confirm: "" });
    passwordMessage.value = "비밀번호가 변경되었습니다.";
    passwordMessageType.value = "success";
  } catch (error) {
    passwordMessage.value = error.message || "비밀번호를 변경하지 못했습니다.";
    passwordMessageType.value = "error";
  } finally {
    passwordChanging.value = false;
  }
}
function loadPostcodeScript() {
  if (window.daum?.Postcode || window.kakao?.Postcode) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const existing = document.querySelector("script[data-pawcast-postcode]");
    if (existing) {
      existing.addEventListener("load", resolve, { once: true });
      existing.addEventListener("error", reject, { once: true });
      return;
    }
    const script = document.createElement("script");
    script.src =
      "https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.dataset.pawcastPostcode = "true";
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });
}
async function openPostcode() {
  try {
    addressError.value = "";
    await loadPostcodeScript();
    const Postcode = window.daum?.Postcode || window.kakao?.Postcode;
    new Postcode({
      oncomplete(data) {
        addressForm.zipCode = data.zonecode;
        addressForm.address =
          data.userSelectedType === "R" ? data.roadAddress : data.jibunAddress;
        requestAnimationFrame(() =>
          document.querySelector(".address-detail-input")?.focus(),
        );
      },
    }).open();
  } catch {
    addressError.value =
      "주소 검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.";
  }
}
function addAddress() {
  addressError.value = "";
  if (
    !addressForm.name ||
    !addressForm.recipient ||
    !addressForm.zipCode ||
    !addressForm.address ||
    !addressForm.tel
  ) {
    addressError.value =
      "배송지 이름, 받는 분, 주소, 연락처를 모두 입력해주세요.";
    return;
  }
  if (!/^010-\d{4}-\d{4}$/.test(addressForm.tel)) {
    addressError.value = "연락처를 010-1234-5678 형식으로 입력해주세요.";
    return;
  }
  addresses.value.push({
    id: Date.now(),
    ...addressForm,
    primary: addresses.value.length === 0,
  });
  Object.assign(addressForm, {
    name: "",
    recipient: "",
    zipCode: "",
    address: "",
    detailAddress: "",
    tel: "",
  });
  addressFormOpen.value = false;
  persistExtra();
}
function makePrimary(id) {
  addresses.value = addresses.value.map((item) => ({
    ...item,
    primary: item.id === id,
  }));
  persistExtra();
}
function openWithdraw() {
  withdrawPassword.value = "";
  withdrawMessage.value = "";
  withdrawOpen.value = true;
}
async function withdraw() {
  withdrawMessage.value = "";
  if (!withdrawPassword.value) {
    withdrawMessage.value = "현재 비밀번호를 입력해주세요.";
    return;
  }
  try {
    withdrawLoading.value = true;
    if (temporaryId.value) {
      if (!loginTemporaryMember(temporaryId.value, withdrawPassword.value)) {
        withdrawMessage.value = "비밀번호가 일치하지 않습니다.";
        return;
      }
      removeTemporaryMember(temporaryId.value);
      localStorage.removeItem(dataKey);
    } else {
      await withdrawMember(withdrawPassword.value);
    }
    sessionStorage.removeItem("pawcast-profile-verified");
    setAccessToken("");
    router.push("/");
  } catch (error) {
    withdrawMessage.value = error.message;
  } finally {
    withdrawLoading.value = false;
  }
}
</script>

<template>
  <main class="container member-settings">
    <header class="settings-heading">
      <span>PAWCAST MY PAGE</span>
      <h1>회원정보 설정</h1>
      <p>회원정보와 배송지를 안전하게 관리하세요.</p>
    </header>
    <div class="settings-layout">
      <MemberSidebar
        :name="profile.username"
        :user-id="profile.userId"
        active="settings"
      />

      <div class="settings-content">
        <section id="mypage-profile" class="settings-card profile-card">
          <header>
            <h2>기본 회원정보</h2>
            <p>회원님의 소중한 정보는 안전하게 보호됩니다.</p>
          </header>
          <form class="profile-form" @submit.prevent="saveProfile">
            <label
              ><span>이름</span><input v-model.trim="profile.username"
            /></label>
            <label
              ><span>아이디</span
              ><input
                v-model="profile.userId"
                class="immutable-user-id"
                disabled
            /></label>
            <label
              ><span>이메일</span
              ><input v-model.trim="profile.email" type="email"
            /></label>
            <label
              ><span>휴대전화</span
              ><input
                :value="profile.tel"
                inputmode="numeric"
                @input="profile.tel = formatTel($event.target.value)"
            /></label>
            <section class="password-management password-change-panel">
              <div>
                <b>비밀번호 관리</b>
                <p>현재 비밀번호를 확인한 뒤 직접 새 비밀번호로 변경할 수 있습니다.</p>
              </div>
              <div class="password-change-form">
                <input v-model="passwordForm.current" type="password" autocomplete="current-password" placeholder="현재 비밀번호" />
                <input v-model="passwordForm.next" type="password" autocomplete="new-password" placeholder="새 비밀번호 (영문·숫자·특수문자 8~20자)" />
                <input v-model="passwordForm.confirm" type="password" autocomplete="new-password" placeholder="새 비밀번호 확인" />
                <p v-if="passwordMessage" :class="['password-change-message', passwordMessageType]">{{ passwordMessage }}</p>
                <button type="button" :disabled="passwordChanging" @click="changePassword">{{ passwordChanging ? "변경 중..." : "비밀번호 변경" }}</button>
                <button
                  type="button"
                  @click="
                    router.push({
                      path: '/password-reset',
                      query: { email: profile.email, userId: profile.userId },
                    })
                  "
                >
                  비밀번호를 잊으셨나요? 임시 비밀번호 받기
                </button>
              </div>
            </section>
            <div class="marketing-row">
              <div>
                <b>이메일 마케팅 수신</b>
                <p>신상품, 맞춤 추천과 할인 소식을 이메일로 받아요.</p>
              </div>
              <button
                type="button"
                class="switch"
                :class="{ on: profile.marketing }"
                :aria-pressed="profile.marketing"
                @click="profile.marketing = !profile.marketing"
              >
                <i />
              </button>
            </div>
            <button class="save-profile" type="submit">변경사항 저장</button>
          </form>
        </section>

        <section id="mypage-address" class="settings-card management-card">
          <header>
            <h2>배송지 관리</h2>
            <button type="button" @click="addressFormOpen = !addressFormOpen">
              추가
            </button>
          </header>
          <form
            v-if="addressFormOpen"
            class="inline-add-form address-add-form"
            @submit.prevent="addAddress"
          >
            <input
              v-model.trim="addressForm.name"
              placeholder="배송지 이름"
            /><input
              v-model.trim="addressForm.recipient"
              placeholder="받는 분"
            />
            <div class="postcode-row">
              <input
                v-model="addressForm.zipCode"
                readonly
                placeholder="우편번호"
              /><button type="button" @click="openPostcode">
                우편번호 찾기
              </button>
            </div>
            <input
              v-model="addressForm.address"
              class="wide"
              readonly
              placeholder="주소 검색을 이용해주세요"
            /><input
              v-model.trim="addressForm.detailAddress"
              class="wide address-detail-input"
              placeholder="상세주소를 입력해주세요 (선택)"
            /><input
              :value="addressForm.tel"
              placeholder="연락처"
              inputmode="numeric"
              @input="addressForm.tel = formatTel($event.target.value)"
            />
            <p v-if="addressError" class="address-error">{{ addressError }}</p>
            <button type="submit">저장</button>
          </form>
          <div v-if="addresses.length" class="management-list">
            <article v-for="item in addresses" :key="item.id">
              <div>
                <span v-if="item.primary">기본</span>
                <h3>{{ item.name }} · {{ item.recipient }}</h3>
                <p>
                  [{{ item.zipCode }}] {{ item.address }}
                  {{ item.detailAddress }}
                </p>
                <small>{{ item.tel }}</small>
              </div>
              <button v-if="!item.primary" @click="makePrimary(item.id)">
                기본으로 지정
              </button>
            </article>
          </div>
          <p v-else class="empty-setting">등록된 배송지가 없습니다.</p>
        </section>

        <section class="settings-card withdraw-card">
          <div>
            <h2>회원 탈퇴</h2>
            <p>탈퇴 시 주문 내역과 포인트 등 회원 정보를 복구할 수 없습니다.</p>
          </div>
          <button type="button" @click="openWithdraw">탈퇴 안내 확인</button>
        </section>
      </div>
    </div>
    <Teleport to="body"
      ><div
        v-if="withdrawOpen"
        class="common-modal-backdrop"
        @click.self="withdrawOpen = false"
      >
        <form
          class="common-modal withdraw-confirm-modal"
          @submit.prevent="withdraw"
        >
          <button
            type="button"
            class="common-modal-close"
            aria-label="닫기"
            @click="withdrawOpen = false"
          >
            ×
          </button>
          <div class="modal-status-icon warning">!</div>
          <h2>정말 탈퇴하시겠어요?</h2>
          <p>
            탈퇴 시 주문 내역과 포인트 등 회원 정보를 복구할 수 없습니다.<br />본인
            확인을 위해 현재 비밀번호를 입력해주세요.
          </p>
          <label
            ><span>현재 비밀번호</span
            ><input
              v-model="withdrawPassword"
              type="password"
              autocomplete="current-password"
              placeholder="현재 비밀번호 입력"
              autofocus /></label
          ><small v-if="withdrawMessage">{{ withdrawMessage }}</small>
          <div class="common-modal-actions">
            <button
              type="button"
              class="secondary"
              @click="withdrawOpen = false"
            >
              취소</button
            ><button class="danger" :disabled="withdrawLoading">
              {{ withdrawLoading ? "확인 중..." : "회원 탈퇴" }}
            </button>
          </div>
        </form>
      </div></Teleport
    >
    <Teleport to="body"
      ><div
        v-if="profileSavedOpen"
        class="common-modal-backdrop"
        @click.self="profileSavedOpen = false"
      >
        <section class="common-modal profile-saved-modal">
          <div class="modal-status-icon signup-success">
            <svg viewBox="0 0 24 24"><path d="m6 12.5 4 4L18 8" /></svg>
          </div>
          <h2>회원정보가 수정되었습니다</h2>
          <p>변경한 회원정보가 정상적으로 저장되었습니다.</p>
          <div class="common-modal-actions single">
            <button class="primary" @click="profileSavedOpen = false">
              확인
            </button>
          </div>
        </section>
      </div></Teleport
    >
  </main>
</template>
