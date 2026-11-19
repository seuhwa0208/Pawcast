<!-- 장바구니 상품, 배송지, 결제수단을 확인하고 백엔드 주문 생성 후 토스 결제창을 여는 화면입니다. -->
<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useCart } from "@/stores/cart";
import { useAuthSession } from "@/stores/auth";
import { addressService } from "@/services/addressService";
import { paymentService } from "@/services/paymentService";

const TOSS_CLIENT_KEY =
  import.meta.env.VITE_TOSS_CLIENT_KEY ||
  "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
const SHIPPING_FEE = 3000;

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
const isBuyNow = computed(
  () => route.query.mode === "buy-now" && Boolean(buyNowItem.value),
);
const checkoutItems = computed(() =>
  isBuyNow.value ? [buyNowItem.value] : items.value,
);
const checkoutTotal = computed(() =>
  checkoutItems.value.reduce(
    (sum, item) => sum + Number(item.price) * Number(item.quantity),
    0,
  ),
);
const finalAmount = computed(() => checkoutTotal.value + SHIPPING_FEE);
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
const paymentMethod = ref("CARD");
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

// 서버가 확정한 주문번호와 금액을 받은 뒤에만 PG 결제창을 실행합니다.
async function requestPayment() {
  errorMessage.value = "";
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
    const order = await paymentService.createOrder({
      items: checkoutItems.value.map((item) => ({
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
        mode: isBuyNow.value ? "buy-now" : "cart",
      }),
    );
    const TossPayments = await loadTossSdk();
    const customerKey = `powcast-${userId.value}-${String(Date.now())}`;
    const payment = TossPayments(TOSS_CLIENT_KEY).payment({ customerKey });
    const card = ["TOSSPAY", "KAKAOPAY"].includes(paymentMethod.value)
      ? { flowMode: "DIRECT", easyPay: paymentMethod.value }
      : { flowMode: "DEFAULT" };
    await payment.requestPayment({
      method: paymentMethod.value === "TRANSFER" ? "TRANSFER" : "CARD",
      amount: { currency: "KRW", value: finalAmount.value },
      orderId: String(orderNumber),
      orderName: orderName.value,
      customerName: selectedAddress.value.recipient,
      customerEmail: buyerEmail.value,
      successUrl: `${window.location.origin}/payment/result?status=success`,
      failUrl: `${window.location.origin}/payment/result?status=fail`,
      ...(paymentMethod.value === "TRANSFER" ? {} : { card }),
    });
  } catch (error) {
    errorMessage.value =
      error.message || "결제를 시작하지 못했습니다. 백엔드 연결을 확인해주세요.";
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  if (!checkoutItems.value.length) router.replace("/cart");
  else if (isMember.value) loadAddresses();
  else initializeGuestOrder();
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
            ? "상품과 배송지를 확인한 후 결제수단을 선택해주세요."
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
                type="tel"
                placeholder="010-1234-5678"
                :readonly="sameAsOrderer"
                required
              />
            </label>
            <label class="address-search-field">
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
              <select v-model="addressForm.deliveryMessage">
                <option value="문 앞에 놓아주세요.">문 앞에 놓아주세요.</option>
                <option value="경비실에 맡겨주세요.">
                  경비실에 맡겨주세요.
                </option>
                <option value="배송 전에 연락해주세요.">
                  배송 전에 연락해주세요.
                </option>
                <option value="">요청사항 없음</option>
              </select>
            </label>
          </div>
        </section>

        <section class="checkout-card">
          <header><h2>결제수단</h2></header>
          <div class="payment-method-grid">
            <label
              v-for="method in [
                ['CARD', '신용·체크카드'],
                ['TOSSPAY', '토스페이'],
                ['KAKAOPAY', '카카오페이'],
                ['TRANSFER', '계좌이체'],
              ]"
              :key="method[0]"
              :class="{ on: paymentMethod === method[0] }"
            >
              <input
                v-model="paymentMethod"
                type="radio"
                :value="method[0]"
              />
              <span>{{ method[1] }}</span>
            </label>
          </div>
          <p class="payment-method-note">
            선택한 결제수단은 토스페이먼츠 결제창에서 안전하게 처리됩니다.
          </p>
        </section>
      </div>

      <aside class="checkout-summary">
        <h2>최종 결제금액</h2>
        <dl>
          <dt>상품 금액</dt>
          <dd>{{ checkoutTotal.toLocaleString() }}원</dd>
          <dt>배송비</dt>
          <dd>{{ SHIPPING_FEE.toLocaleString() }}원</dd>
          <dt>총 결제 금액</dt>
          <dd>{{ finalAmount.toLocaleString() }}원</dd>
        </dl>
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
              : `${finalAmount.toLocaleString()}원 테스트 결제하기`
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
              >연락처<input v-model.trim="addressForm.tel" required
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
