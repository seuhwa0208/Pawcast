<!-- 회원 프로필·배송지 저장과 탈퇴 처리를 담당하는 설정 화면입니다. -->
<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";
import { changeMemberPassword, getMyProfile, updateMyProfile, withdrawMember } from "@/services/memberService";
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
const profileMessage = ref("");
const passwordMessage = ref("");
const passwordSuccess = ref(false);
const passwordChanging = ref(false);
const passwordInputsReady = ref(false);
const passwordForm = reactive({
  password: "",
  newPassword: "",
  newPasswordCheck: "",
});
const addressFormOpen = ref(false);
const editingAddressId = ref(null);
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

function normalizeAddress(item) {
  return {
    id: item.id,
    name: item.name || item.label || "배송지",
    recipient: item.recipient || "",
    zipCode: item.zipCode || item.zipcode || "",
    address: item.address || item.address1 || "",
    detailAddress: item.detailAddress || item.address2 || "",
    tel: formatTel(item.tel || ""),
    primary: item.primary ?? item.isDefault ?? false,
  };
}
function resetAddressForm() {
  Object.assign(addressForm, {
    name: "", recipient: "", zipCode: "", address: "", detailAddress: "", tel: "",
  });
  editingAddressId.value = null;
}
function resetPasswordForm() {
  Object.assign(passwordForm, {
    password: "",
    newPassword: "",
    newPasswordCheck: "",
  });
}
function openAddAddress() {
  addressError.value = "";
  resetAddressForm();
  addressFormOpen.value = true;
}
function openEditAddress(item) {
  addressError.value = "";
  editingAddressId.value = item.id;
  Object.assign(addressForm, normalizeAddress(item));
  addressFormOpen.value = true;
}

function formatTel(value) {
  const numbers = String(value).replace(/\D/g, "").slice(0, 11);
  if (numbers.length <= 3) return numbers;
  if (numbers.length <= 7) return `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
  return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7)}`;
}
function enablePasswordInputs() {
  passwordInputsReady.value = true;
}
async function saveProfile() {
  profileMessage.value = "";
  if (!profile.username.trim()) {
    profileMessage.value = "이름을 입력해주세요.";
    return;
  }
  if (!/^\S+@\S+\.\S+$/.test(profile.email)) {
    profileMessage.value = "올바른 이메일 형식을 입력해주세요.";
    return;
  }
  if (!/^010-\d{4}-\d{4}$/.test(profile.tel)) {
    profileMessage.value = "휴대전화를 010-1234-5678 형식으로 입력해주세요.";
    return;
  }
  try {
    await updateMyProfile({
      name: profile.username,
      tel: profile.tel.replace(/\D/g, ""),
      isAgreedMarketing: profile.marketing,
    });
    profileSavedOpen.value = true;
  } catch (error) {
    profileMessage.value = error.message || "회원정보를 저장하지 못했습니다.";
  }
}
async function savePassword() {
  passwordMessage.value = "";
  passwordSuccess.value = false;
  if (!passwordForm.password || !passwordForm.newPassword || !passwordForm.newPasswordCheck) {
    passwordMessage.value = "현재 비밀번호와 새 비밀번호를 모두 입력해주세요.";
    return;
  }
  if (passwordForm.newPassword !== passwordForm.newPasswordCheck) {
    passwordMessage.value = "새 비밀번호와 확인 값이 일치하지 않습니다.";
    return;
  }
  try {
    passwordChanging.value = true;
    await changeMemberPassword({ ...passwordForm });
    // 로그인 페이지로 이동하지 않고 현재 화면에 유지하며 입력값만 초기화합니다.
    resetPasswordForm();
    passwordSuccess.value = true;
    passwordMessage.value = "비밀번호가 변경되었습니다.";
  } catch (error) {
    passwordMessage.value = error.message || "비밀번호를 변경하지 못했습니다.";
  } finally {
    passwordChanging.value = false;
  }
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
async function saveAddress() {
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
  const payload = {
    label: addressForm.name,
    recipient: addressForm.recipient,
    zipcode: addressForm.zipCode,
    address1: addressForm.address,
    address2: addressForm.detailAddress,
    tel: addressForm.tel,
    isDefault: editingAddressId.value
      ? addresses.value.find((item) => item.id === editingAddressId.value)?.primary ?? false
      : addresses.value.length === 0,
  };
  try {
    const saved = editingAddressId.value
      ? await addressService.updateAddress(editingAddressId.value, payload)
      : await addressService.addAddress(payload);
    const normalized = normalizeAddress(saved);
    if (editingAddressId.value) {
      addresses.value = addresses.value.map((item) =>
        item.id === editingAddressId.value ? normalized : item,
      );
    } else {
      addresses.value.push(normalized);
    }
    resetAddressForm();
    addressFormOpen.value = false;
  } catch (error) {
    addressError.value = error.message || "배송지를 저장하지 못했습니다.";
  }
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
    const serverAddresses = Array.isArray(result)
      ? result
      : result?.content || result?.items || result?.addresses || [];
    addresses.value = serverAddresses.map(normalizeAddress);
  } catch (error) {
    addressError.value = error.message || "회원정보를 불러오지 못했습니다.";
  }
  // 브라우저의 저장 비밀번호 자동 주입을 비워, 변경 의사를 가진 사용자가 직접 입력하게 한다.
  window.setTimeout(() => {
    resetPasswordForm();
  }, 0);
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
          <form class="profile-form" autocomplete="off" @submit.prevent="saveProfile">
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
                maxlength="13"
                autocomplete="tel"
                @input="profile.tel = formatTel($event.target.value)"
            /></label>
            <section class="password-management">
              <div>
                <b>비밀번호 관리</b>
                <p>
                  현재 비밀번호를 확인한 뒤 새 비밀번호로 직접 변경할 수 있습니다.
                </p>
              </div>
              <div class="password-change-fields">
                <input
                  v-model="passwordForm.password"
                  type="password"
                  name="member-password-current"
                  autocomplete="new-password"
                  :readonly="!passwordInputsReady"
                  placeholder="현재 비밀번호"
                  @focus="enablePasswordInputs"
                />
                <input
                  v-model="passwordForm.newPassword"
                  type="password"
                  name="member-password-new"
                  autocomplete="new-password"
                  :readonly="!passwordInputsReady"
                  placeholder="새 비밀번호 (8~20자)"
                  @focus="enablePasswordInputs"
                />
                <input
                  v-model="passwordForm.newPasswordCheck"
                  type="password"
                  name="member-password-confirm"
                  autocomplete="new-password"
                  :readonly="!passwordInputsReady"
                  placeholder="새 비밀번호 확인"
                  @focus="enablePasswordInputs"
                />
                <button type="button" :disabled="passwordChanging" @click="savePassword">
                  {{ passwordChanging ? "변경 중..." : "비밀번호 변경" }}
                </button>
              </div>
              <p
                v-if="passwordMessage"
                class="password-message"
                :class="{ success: passwordSuccess }"
              >
                {{ passwordMessage }}
              </p>
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
            <p v-if="profileMessage" class="profile-message">{{ profileMessage }}</p>
            <button class="save-profile" type="submit">변경사항 저장</button>
          </form>
        </section>

        <section id="mypage-address" class="settings-card management-card">
          <header>
            <h2>배송지 관리</h2>
            <button type="button" @click="addressFormOpen ? (addressFormOpen = false) : openAddAddress()">
              추가
            </button>
          </header>
          <form
            v-if="addressFormOpen"
            class="inline-add-form address-add-form"
            @submit.prevent="saveAddress"
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
            <button type="submit">{{ editingAddressId ? "수정 저장" : "저장" }}</button>
          </form>
          <div v-if="addresses.length" class="management-list">
            <article v-for="item in addresses" :key="item.id">
              <div class="address-info">
                <div class="address-label-row">
                  <span class="address-tag" :class="{ primary: item.primary }">
                    {{ item.primary ? "기본" : item.name }}
                  </span>
                  <h3 v-if="item.primary">{{ item.name }}</h3>
                </div>
                <strong>{{ item.recipient }} <small>{{ item.tel }}</small></strong>
                <p>[{{ item.zipCode }}] {{ item.address }} {{ item.detailAddress }}</p>
              </div>
              <div class="address-actions">
                <button type="button" @click="openEditAddress(item)">수정</button>
                <button v-if="!item.primary" type="button" @click="makePrimary(item.id)">기본으로 지정</button>
              </div>
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