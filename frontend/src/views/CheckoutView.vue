<!-- 장바구니 상품, 배송지, 결제수단을 확인하고 백엔드 주문 생성 후 토스 결제창을 여는 화면입니다. -->
<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useCart } from "@/stores/cart";
import { useAuthSession } from "@/stores/auth";
import { addressService } from "@/services/addressService";
import { paymentService } from "@/services/paymentService";
import { getMyProfile } from "@/services/memberService";

const TOSS_CLIENT_KEY =
  import.meta.env.VITE_TOSS_CLIENT_KEY ||
  "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
const BASE_SHIPPING_FEE = 3000;
const FREE_SHIPPING_THRESHOLD = 20000;

const router = useRouter();
const route = useRoute();
const { items } = useCart();
const { accessToken, currentUserId } = useAuthSession();
const userId = computed(() => currentUserId.value);
const member = ref(null);
const isMember = computed(() => Boolean(accessToken.value));
const buyNowItem = ref(
  JSON.parse(sessionStorage.getItem("powcast-buy-now") || "null"),
);
const selectedCheckoutItems = ref(
  JSON.parse(sessionStorage.getItem("powcast-selected-checkout") || "[]"),
);
const isBuyNow = computed(
  () => route.query.mode === "buy-now" && Boolean(buyNowItem.value),
);
const isSelectedMode = computed(
  () =>
    route.query.mode === "selected" &&
    Array.isArray(selectedCheckoutItems.value) &&
    selectedCheckoutItems.value.length > 0,
);
const checkoutItems = computed(() => {
  if (isBuyNow.value) return [buyNowItem.value];
  if (isSelectedMode.value) return selectedCheckoutItems.value;
  return items.value;
});
const checkoutTotal = computed(() =>
  checkoutItems.value.reduce(
    (sum, item) => sum + Number(item.price) * Number(item.quantity),
    0,
  ),
);
const shippingFee = computed(() =>
  checkoutTotal.value >= FREE_SHIPPING_THRESHOLD ? 0 : BASE_SHIPPING_FEE,
);
const finalAmount = computed(() => checkoutTotal.value + shippingFee.value);
const orderName = computed(() => {
  const firstName = checkoutItems.value[0]?.name || "POWCAST 상품";
  return checkoutItems.value.length > 1
    ? `${firstName} 외 ${checkoutItems.value.length - 1}건`
    : firstName;
});

const addresses = ref([]);
const selectedAddressId = ref("");
const addressLoading = ref(false);
const addressFormOpen = ref(false);
const agreed = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const deliveryMessageOption = ref("문 앞에 놓아주세요.");
const customDeliveryMessage = ref("");
const buyerEmail = ref(member.value?.email || "");
const ordererName = ref(member.value?.username || "");
const ordererPhone = ref(member.value?.tel || "");
const sameAsOrderer = ref(false);
const addressForm = reactive({
  label: "우리 집",
  recipient: "",
  tel: "",
  zipcode: "",
  address1: "",
  address2: "",
  deliveryMessage: "문 앞에 놓아주세요.",
});

const selectedAddress = computed(() =>
  isMember.value
    ? addresses.value.find(
        (address) => String(address.id) === String(selectedAddressId.value),
      )
    : normalizeAddress({ id: "guest", ...addressForm }),
);

// API 응답 필드명을 결제 화면 모델로 통일합니다.
function normalizeAddress(address) {
  return {
    id: address.id,
    label: address.label || address.name || "배송지",
    recipient: address.recipient,
    tel: address.tel,
    zipcode: address.zipcode || address.zipCode,
    address1: address.address1 || address.address,
    address2: address.address2 || address.detailAddress,
    deliveryMessage: address.deliveryMessage || "",
    isDefault: address.isDefault ?? address.primary ?? false,
  };
}

async function loadAddresses() {
  try {
    addressLoading.value = true;
    const result = await addressService.getAddresses();
    const serverAddresses = Array.isArray(result)
      ? result
      : result?.content || result?.items || result?.addresses || [];
    addresses.value = serverAddresses.map(normalizeAddress);
    const initial =
      addresses.value.find((address) => address.isDefault) || addresses.value[0];
    selectedAddressId.value = initial?.id || "";
  } catch (error) {
    errorMessage.value = error.message || "배송지를 불러오지 못했습니다.";
  } finally {
    addressLoading.value = false;
  }
}

async function loadMemberProfile() {
  if (!isMember.value) return;
  try {
    const res = await getMyProfile();
    const profile = res?.member || res?.profile || res?.data || res;
    if (!profile) return;

    member.value = profile;

    const name = profile.username || profile.name || profile.nickname || "";
    const tel = profile.tel || profile.phone || profile.phoneNumber || "";
    const email = profile.email || "";

    if (!ordererName.value && name) {
      ordererName.value = name;
    }
    if (!ordererPhone.value && tel) {
      ordererPhone.value = formatPhoneNumber(tel);
    }
    if (!buyerEmail.value && email) {
      buyerEmail.value = email;
    }
  } catch (error) {
    // 주문서 작성 자체를 막지 않도록 조용히 넘기거나 필요 시 콘솔만 남깁니다.
    console.warn("회원 정보를 불러오지 못했습니다:", error);
  }
}

function initializeGuestOrder() {
  Object.assign(addressForm, {
    label: "비회원 배송지",
    recipient: "",
    tel: "",
    zipcode: "",
    address1: "",
    address2: "",
    deliveryMessage: "문 앞에 놓아주세요.",
  });
  deliveryMessageOption.value = "문 앞에 놓아주세요.";
  customDeliveryMessage.value = "";
}

watch([deliveryMessageOption, customDeliveryMessage], () => {
  addressForm.deliveryMessage =
    deliveryMessageOption.value === "CUSTOM"
      ? customDeliveryMessage.value.trim()
      : deliveryMessageOption.value;
});

function formatPhoneNumber(value) {
  const digits = String(value || "").replace(/\D/g, "").slice(0, 11);
  if (digits.length <= 3) return digits;
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
}

// 비회원이 주문자와 수령인이 같다고 선택하면 연락처까지 함께 동기화합니다.
watch([ordererName, ordererPhone, sameAsOrderer], () => {
  if (!sameAsOrderer.value) return;
  addressForm.recipient = ordererName.value;
  addressForm.tel = ordererPhone.value;
});

function openAddressForm() {
  Object.assign(addressForm, {
    label: "우리 집",
    recipient: member.value?.username || "",
    tel: member.value?.tel || "",
    zipcode: "",
    address1: "",
    address2: "",
    deliveryMessage: "문 앞에 놓아주세요.",
  });
  addressFormOpen.value = true;
}

function openPostcode() {
  const Postcode = window.kakao?.Postcode || window.daum?.Postcode;
  if (!Postcode) {
    errorMessage.value = "주소 검색 서비스를 불러오지 못했습니다.";
    return;
  }
  new Postcode({
    oncomplete(data) {
      addressForm.zipcode = data.zonecode;
      addressForm.address1 = data.roadAddress || data.jibunAddress;
    },
  }).open();
}

async function saveAddress() {
  if (
    !addressForm.recipient ||
    !addressForm.tel ||
    !addressForm.zipcode ||
    !addressForm.address1
  ) {
    errorMessage.value = "받는 분, 연락처와 주소를 모두 입력해주세요.";
    return;
  }
  try {
    const savedAddress = normalizeAddress(
      await addressService.addAddress({ ...addressForm, isDefault: false }),
    );
    addresses.value.push(savedAddress);
    selectedAddressId.value = savedAddress.id;
    addressFormOpen.value = false;
  } catch (error) {
    errorMessage.value = error.message || "배송지를 저장하지 못했습니다.";
  }
}

function loadTossSdk() {
  if (window.TossPayments) return Promise.resolve(window.TossPayments);
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://js.tosspayments.com/v2/standard";
    script.onload = () => resolve(window.TossPayments);
    script.onerror = () => reject(new Error("토스 결제 SDK를 불러오지 못했습니다."));
    document.head.appendChild(script);
  });
}

// Toss customerKey는 영문·숫자 및 -._=@ 만 허용하고 2~50자여야 한다.
// 회원 아이디가 한글이거나 비어 있어도 결제창을 열 수 있도록 안전한 키를 만든다.
function createTossCustomerKey() {
  const rawMemberKey = String(userId.value || "member");
  const storageKey = `powcast-toss-customer-key:${rawMemberKey}`;
  const savedKey = localStorage.getItem(storageKey);
  if (savedKey) return savedKey;
  const safeMemberKey = rawMemberKey
    .replace(/[^A-Za-z0-9\-._=@]/g, "")
    .slice(0, 24) || "member";

  const customerKey = `powcast_${safeMemberKey}_${crypto.randomUUID()}`.slice(0, 50);
  localStorage.setItem(storageKey, customerKey);
  return customerKey;
}

// 서버가 확정한 주문번호와 금액을 받은 뒤에만 PG 결제창을 실행합니다.
async function requestPayment() {
  errorMessage.value = "";
  let paymentWindowOpened = false;
  if (!checkoutItems.value.length) {
    router.push("/cart");
    return;
  }
  if (!selectedAddress.value) {
    errorMessage.value = "결제에 사용할 배송지를 선택해주세요.";
    return;
  }
  if (!/^\S+@\S+\.\S+$/.test(buyerEmail.value)) {
    errorMessage.value = "주문 확인에 사용할 이메일을 입력해주세요.";
    return;
  }
  if (!ordererName.value.trim() || !ordererPhone.value.trim()) {
    errorMessage.value = "주문자 이름과 전화번호를 입력해주세요.";
    return;
  }
  if (
    !selectedAddress.value.recipient ||
    !selectedAddress.value.tel ||
    !selectedAddress.value.zipcode ||
    !selectedAddress.value.address1
  ) {
    errorMessage.value = "받는 분, 연락처와 배송지를 모두 입력해주세요.";
    return;
  }
  if (!agreed.value) {
    errorMessage.value = "주문 내용과 결제 진행에 동의해주세요.";
    return;
  }
  try {
    submitting.value = true;
    const orderdItems = [...checkoutItems.value];
    const order = await paymentService.createOrder({
      items: orderdItems.map((item) => ({
        productVariantId: item.variantId || item.id,
        quantity: item.quantity,
      })),
      recipientName: selectedAddress.value.recipient,
      recipientPhone: selectedAddress.value.tel,
      recipientEmail: buyerEmail.value,
      ordererName: ordererName.value,
      ordererPhone: ordererPhone.value,
      deliveryZipcode: selectedAddress.value.zipcode,
      deliveryAddress1: selectedAddress.value.address1,
      deliveryAddress2: selectedAddress.value.address2,
      deliveryMessage: selectedAddress.value.deliveryMessage || "",
    });
    const orderId = order.id || order.orderId;
    const orderNumber = order.orderNumber || orderId;
    const orderAccessToken =
      order.orderAccessToken || order.accessToken || order.guestOrderToken;
    if (!orderId) throw new Error("백엔드 주문 생성 응답에 주문 ID가 없습니다.");

    sessionStorage.setItem(
      "powcast-payment-context",
      JSON.stringify({
        orderId,
        orderNumber,
        orderAccessToken,
        orderEmail: buyerEmail.value,
        isMember: isMember.value,
        gusetCartKeys: !isMember.value && !isBuyNow.value
        ? orderedItems.map((itme) => item.key).filter(Boolean)
        : [],
        mode: isBuyNow.value
          ? "buy-now"
          : isSelectedMode.value
            ? "selected"
            : "cart",
      }),
    );
    const TossPayments = await loadTossSdk();
    // test_gck/live_gck 키는 구형 payment()가 아니라 widgets() 흐름에 사용합니다.
    const tossPayments = TossPayments(TOSS_CLIENT_KEY);
    const widgets = tossPayments.widgets({
      customerKey: isMember.value
        ? createTossCustomerKey()
        : TossPayments.ANONYMOUS,
    });
    await widgets.setAmount({
      currency: "KRW",
      value: Number(finalAmount.value),
    });
    const paymentWindow = await widgets.renderPaymentWindow({
      orderName: orderName.value,
    });
    paymentWindowOpened = true;
    paymentWindow.on("paymentRequest", () =>
      widgets.requestPayment({
        orderId: String(orderNumber),
        orderName: orderName.value,
        customerName: selectedAddress.value.recipient,
        customerEmail: buyerEmail.value,
        customerMobilePhone: String(ordererPhone.value).replace(/\D/g, ""),
        successUrl: `${window.location.origin}/payment/result?status=success`,
        failUrl: `${window.location.origin}/payment/result?status=fail`,
      }),
    );
    paymentWindow.on("cancel", () => {
      submitting.value = false;
    });
    return;
  } catch (error) {
    errorMessage.value =
      error.message || "결제를 시작하지 못했습니다. 백엔드 연결을 확인해주세요.";
  } finally {
    // 결제창이 열린 뒤에는 사용자의 취소 이벤트에서만 버튼 상태를 되돌립니다.
    if (!paymentWindowOpened) submitting.value = false;
  }
}

onMounted(() => {
  if (!checkoutItems.value.length) {
    router.replace("/cart");
  } else if (isMember.value) {
    loadMemberProfile();
    loadAddresses();
  } else {
    initializeGuestOrder();
  }
});
</script>

<template>
  <main class="container checkout-page">
    <header class="checkout-heading">
      <small>POWCAST CHECKOUT</small>
      <h1>주문·결제</h1>
      <p>
        {{
          isMember
            ? "상품과 배송지를 확인한 후 결제를 진행해주세요."
            : "비회원 주문 확인에 필요한 이메일과 배송지 정보를 입력해주세요."
        }}
      </p>
      <ol class="checkout-steps" aria-label="결제 단계">
        <li>장바구니</li>
        <li class="on">주문·결제</li>
        <li>완료</li>
      </ol>
    </header>

    <div class="checkout-layout">
      <div class="checkout-content">
        <section class="checkout-card">
          <header>
            <h2>주문 상품</h2>
            <span>총 {{ checkoutItems.length }}종</span>
          </header>
          <article
            v-for="item in checkoutItems"
            :key="item.key"
            class="checkout-item"
          >
            <img :src="item.image" :alt="item.name" />
            <div>
              <h3>{{ item.name }}</h3>
              <p>옵션: {{ item.option || "기본 옵션" }} · 수량 {{ item.quantity }}개</p>
            </div>
            <b>{{ (item.price * item.quantity).toLocaleString() }}원</b>
          </article>
        </section>

        <section class="checkout-card">
          <header>
            <h2>주문자 정보</h2>
            <span v-if="!isMember">비회원 주문</span>
          </header>
          <div class="checkout-orderer-form">
            <label>
              주문자 이름
              <input
                v-model.trim="ordererName"
                autocomplete="name"
                required
              />
            </label>
            <label>
              주문자 전화번호
              <input
                v-model.trim="ordererPhone"
                @input="ordererPhone = formatPhoneNumber($event.target.value)"
                type="tel"
                autocomplete="tel"
                placeholder="010-1234-5678"
                required
              />
            </label>
            <label class="wide">
              주문 이메일
              <input
                v-model.trim="buyerEmail"
                type="email"
                autocomplete="email"
                placeholder="example@email.com"
                required
              />
              <small v-if="!isMember"
                >주문번호 발송과 비회원 주문 조회 본인 확인에 사용됩니다.</small
              >
            </label>
          </div>
        </section>

        <section class="checkout-card checkout-addresses">
          <header>
            <h2>배송지</h2>
            <button v-if="isMember" type="button" @click="openAddressForm">
              + 신규 배송지
            </button>
          </header>
          <p v-if="isMember && addressLoading" class="checkout-state">
            배송지를 불러오는 중입니다.
          </p>
          <div
            v-else-if="isMember && addresses.length"
            class="checkout-address-list"
          >
            <label v-for="address in addresses" :key="address.id">
              <input
                v-model="selectedAddressId"
                type="radio"
                :value="address.id"
              />
              <span>
                <b>{{ address.label }}</b>
                <strong>{{ address.recipient }} · {{ address.tel }}</strong>
                <small
                  >[{{ address.zipcode }}] {{ address.address1 }}
                  {{ address.address2 }}</small
                >
              </span>
            </label>
          </div>
          <p v-else-if="isMember" class="checkout-state">
            등록된 배송지가 없습니다. 신규 배송지를 추가해주세요.
          </p>
          <div v-else class="guest-order-form">
            <label class="guest-same-orderer wide">
              <input v-model="sameAsOrderer" type="checkbox" />
              <span>주문자 정보와 수령인 정보가 같습니다.</span>
            </label>
            <label>
              받는 분
              <input
                v-model.trim="addressForm.recipient"
                :readonly="sameAsOrderer"
                required
              />
            </label>
            <label>
              연락처
              <input
                v-model.trim="addressForm.tel"
                @input="addressForm.tel = formatPhoneNumber($event.target.value)"
                type="tel"
                placeholder="010-1234-5678"
                :readonly="sameAsOrderer"
                required
              />
            </label>
            <label class="address-search-field wide">
              우편번호
              <span>
                <input v-model="addressForm.zipcode" readonly required />
                <button type="button" @click="openPostcode">
                  주소 검색
                </button>
              </span>
            </label>
            <label>
              기본 주소
              <input v-model="addressForm.address1" readonly required />
            </label>
            <label>
              상세 주소
              <input v-model.trim="addressForm.address2" />
            </label>
            <label class="wide">
              배송 요청사항
              <select v-model="deliveryMessageOption" class="delivery-message-select">
                <option value="문 앞에 놓아주세요.">문 앞에 놓아주세요.</option>
                <option value="경비실에 맡겨주세요.">
                  경비실에 맡겨주세요.
                </option>
                <option value="배송 전에 연락해주세요.">
                  배송 전에 연락해주세요.
                </option>
                <option value="">요청사항 없음</option>
                <option value="CUSTOM">직접 입력</option>
              </select>
              <input
                v-if="deliveryMessageOption === 'CUSTOM'"
                v-model.trim="customDeliveryMessage"
                class="delivery-message-custom"
                maxlength="255"
                placeholder="배송 요청사항을 직접 입력해주세요."
              />
            </label>
          </div>
        </section>

      </div>

      <aside class="checkout-summary">
        <h2>최종 결제금액</h2>
        <dl>
          <dt>상품 금액</dt>
          <dd>{{ checkoutTotal.toLocaleString() }}원</dd>
          <dt>배송비</dt>
          <dd>{{ shippingFee ? `${shippingFee.toLocaleString()}원` : "무료" }}</dd>
          <dt>총 결제 금액</dt>
          <dd>{{ finalAmount.toLocaleString() }}원</dd>
        </dl>
        <p class="checkout-shipping-notice">* 20,000원 이상 구매 시 배송비 무료</p>
        <label class="checkout-agreement">
          <input v-model="agreed" type="checkbox" />
          <span
            >주문 상품, 배송지와 결제 내용을 확인했으며 결제 진행에
            동의합니다.</span
          >
        </label>
        <p v-if="errorMessage" class="checkout-error">{{ errorMessage }}</p>
        <button type="button" :disabled="submitting" @click="requestPayment">
          {{
            submitting
              ? "결제 준비 중..."
              : `${finalAmount.toLocaleString()}원 결제하기`
          }}
        </button>
        <small>실제 승인 여부는 Spring Boot 결제 API 연결 상태에 따라 결정됩니다.</small>
      </aside>
    </div>

    <Teleport to="body">
      <div
        v-if="addressFormOpen"
        class="common-modal-backdrop"
        @click.self="addressFormOpen = false"
      >
        <form
          class="common-modal checkout-address-modal"
          @submit.prevent="saveAddress"
        >
          <button
            type="button"
            class="common-modal-close"
            @click="addressFormOpen = false"
          >
            ×
          </button>
          <h2>신규 배송지</h2>
          <div class="checkout-address-form">
            <label
              >배송지 이름<input v-model.trim="addressForm.label" required
            /></label>
            <label
              >받는 분<input v-model.trim="addressForm.recipient" required
            /></label>
            <label
              >연락처<input v-model.trim="addressForm.tel" @input="addressForm.tel = formatPhoneNumber($event.target.value)" type="tel" placeholder="010-1234-5678" required
            /></label>
            <label class="address-search-field"
              >우편번호<span
                ><input v-model="addressForm.zipcode" readonly required /><button
                  type="button"
                  @click="openPostcode"
                >
                  주소 검색
                </button></span
              ></label
            >
            <label
              >기본 주소<input v-model="addressForm.address1" readonly required
            /></label>
            <label
              >상세 주소<input v-model.trim="addressForm.address2"
            /></label>
            <label
              >배송 요청사항<input
                v-model.trim="addressForm.deliveryMessage"
            /></label>
          </div>
          <div class="common-modal-actions">
            <button
              type="button"
              class="secondary"
              @click="addressFormOpen = false"
            >
              취소
            </button>
            <button class="primary">배송지 사용하기</button>
          </div>
        </form>
      </div>
    </Teleport>
  </main>
</template>
