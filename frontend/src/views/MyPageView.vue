<!-- 회원 프로필·배송지 저장과 탈퇴 처리를 담당하는 설정 화면입니다. -->
<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";
import { getMyProfile, updateMyProfile, withdrawMember } from "@/services/memberService";
import { addressService } from "@/services/addressService";
import MemberSidebar from "@/components/MemberSidebar.vue";

const router = useRouter();
const { currentUserId, setAccessToken } = useAuthSession();
const profile = reactive({
  username: "",
  userId: currentUserId.value,
  email: "",
  tel: "",
  marketing: false,
});
const profileSavedOpen = ref(false);
const addressFormOpen = ref(false);
const addressError = ref("");
const withdrawOpen = ref(false);
const withdrawPassword = ref("");
const withdrawMessage = ref("");
const withdrawLoading = ref(false);
const addresses = ref([]);
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
async function saveProfile() {
  if (
    !profile.username.trim() ||
    !/^\S+@\S+\.\S+$/.test(profile.email) ||
    !/^010-\d{4}-\d{4}$/.test(profile.tel)
  )
    return;
  await updateMyProfile({
    username: profile.username,
    email: profile.email,
    tel: profile.tel.replace(/\D/g, ""),
    isAgreedMarketing: profile.marketing,
  });
  profileSavedOpen.value = true;
}
function loadPostcodeScript() {
  if (window.daum?.Postcode || window.kakao?.Postcode) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const existing = document.querySelector("script[data-powcast-postcode]");
    if (existing) {
      existing.addEventListener("load", resolve, { once: true });
      existing.addEventListener("error", reject, { once: true });
      return;
    }
    const script = document.createElement("script");
    script.src =
      "https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.dataset.powcastPostcode = "true";
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
async function addAddress() {
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
  const created = await addressService.addAddress({
    label: addressForm.name,
    recipient: addressForm.recipient,
    zipcode: addressForm.zipCode,
    address1: addressForm.address,
    address2: addressForm.detailAddress,
    tel: addressForm.tel,
    isDefault: addresses.value.length === 0,
  });
  addresses.value.push(created);
  Object.assign(addressForm, {
    name: "",
    recipient: "",
    zipCode: "",
    address: "",
    detailAddress: "",
    tel: "",
  });
  addressFormOpen.value = false;
}
async function makePrimary(id) {
  await addressService.setDefault(id);
  addresses.value = addresses.value.map((item) => ({ ...item, primary: item.id === id }));
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
    await withdrawMember(withdrawPassword.value);
    sessionStorage.removeItem("powcast-profile-verified");
    setAccessToken("");
    router.push("/");
  } catch (error) {
    withdrawMessage.value = error.message;
  } finally {
    withdrawLoading.value = false;
  }
}

onMounted(async () => {
  try {
    const member = await getMyProfile();
    Object.assign(profile, {
      username: member.username || member.name || "포캐스트 회원",
      userId: member.userId || currentUserId.value,
      email: member.email || "",
      tel: formatTel(member.tel || member.phone || ""),
      marketing: member.isAgreedMarketing ?? false,
    });
    const result = await addressService.getAddresses();
    addresses.value = result.content || result.items || result.addresses || [];
  } catch (error) {
    addressError.value = error.message || "회원정보를 불러오지 못했습니다.";
  }
});
</script>

<template>
  <main class="container member-settings">
    <header class="settings-heading">
      <span>POWCAST MY PAGE</span>
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
            <section class="password-management">
              <div>
                <b>비밀번호 관리</b>
                <p>
                  비밀번호 변경이 필요한 경우 관리자에게 초기화를 요청해주세요.
                  처리 완료 후 임시 비밀번호가 가입 이메일로 발송됩니다.
                </p>
              </div>
              <div>
                <button
                  type="button"
                  @click="
                    router.push({
                      path: '/password-reset',
                      query: { email: profile.email, userId: profile.userId },
                    })
                  "
                >
                  관리자에게 초기화 요청
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
